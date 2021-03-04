package org.codingmatters.poom.jobs.runner.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.codingmatters.poom.jobs.runner.service.exception.JobNotSubmitableException;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerBusyException;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerServiceInitializationException;
import org.codingmatters.poom.jobs.runner.service.manager.RunnerPool;
import org.codingmatters.poom.jobs.runner.service.manager.RunnerStatusMonitor;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobRunnerRunnable;
import org.codingmatters.poom.jobs.runner.service.manager.jobs.JobManager;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.*;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.RunnerData;
import org.codingmatters.poomjobs.api.types.runnerdata.Competencies;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;
import org.codingmatters.poomjobs.service.api.PoomjobsRunnerAPIProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RunnerService implements JobRunnerRunnable.JobRunnerRunnableErrorListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerService.class);

    private static final long MIN_TTL = 1000L;
    public static final long DEFAULT_TTL = 30 * 1000L;

    static public JobSetup setup() {
        return new Builder();
    }

    public interface JobSetup {
        ClientsSetup jobs(String category, String [] names, JobProcessor.Factory factory);
    }
    public interface ClientsSetup {
        RunnerSetup clients(PoomjobsRunnerRegistryAPIClient runnerRegistryClient, PoomjobsJobRegistryAPIClient jobRegistryClient);
    }
    public interface RunnerSetup {
        EndpointSetup concurrency(int concurrentJobCount);
    }
    public interface EndpointSetup {
        OptionsSetup endpoint(String host, int port);
    }
    public interface OptionsSetup {
        OptionsSetup ttl(long ttl);
        OptionsSetup contextSetup(JobContextSetup contextSetup);
        RunnerService build();
    }

    static private class Builder implements JobSetup, ClientsSetup, RunnerSetup, OptionsSetup, EndpointSetup {
        private PoomjobsRunnerRegistryAPIClient runnerRegistryClient;
        private PoomjobsJobRegistryAPIClient jobRegistryClient;

        private String jobCategory;
        private String[] jobNames;
        private JobProcessor.Factory jobProcessorFactory;

        private int concurrentJobCount;

        private int jobRequestEndpointPort;
        private String jobRequestEndpointHost;

        private long ttl = DEFAULT_TTL;
        private JobContextSetup contextSetup = JobContextSetup.NOOP;


        @Override
        public ClientsSetup jobs(String category, String[] names, JobProcessor.Factory factory) {
            this.jobCategory = category;
            this.jobNames = names;
            this.jobProcessorFactory = factory;

            return this;
        }

        @Override
        public RunnerSetup clients(PoomjobsRunnerRegistryAPIClient runnerRegistryClient, PoomjobsJobRegistryAPIClient jobRegistryClient) {
            this.runnerRegistryClient = runnerRegistryClient;
            this.jobRegistryClient = jobRegistryClient;

            return this;
        }

        @Override
        public EndpointSetup concurrency(int concurrentJobCount) {
            this.concurrentJobCount = concurrentJobCount;

            return this;
        }

        @Override
        public OptionsSetup endpoint(String host, int port) {
            this.jobRequestEndpointHost = host;
            this.jobRequestEndpointPort = port;

            return this;
        }

        @Override
        public OptionsSetup ttl(long ttl) {
            this.ttl = Math.max(MIN_TTL, ttl);;
            return this;
        }

        @Override
        public OptionsSetup contextSetup(JobContextSetup contextSetup) {
            this.contextSetup = contextSetup != null ? contextSetup : JobContextSetup.NOOP;
            return this;
        }

        @Override
        public RunnerService build() {
            return new RunnerService(
                    this.runnerRegistryClient,
                    this.jobRegistryClient,
                    this.concurrentJobCount,
                    this.ttl,
                    this.jobCategory,
                    this.jobNames,
                    this.jobProcessorFactory,
                    this.contextSetup,
                    this.jobRequestEndpointHost,
                    this.jobRequestEndpointPort);
        }
    }

    private final PoomjobsRunnerRegistryAPIClient runnerRegistryClient;
    private final PoomjobsJobRegistryAPIClient jobRegistryClient;

    private final int concurrentJobCount;
    private final long ttl;

    private final String jobCategory;
    private final String[] jobNames;
    private final JobProcessor.Factory jobProcessorFactory;

    private final JobContextSetup contextSetup;

    private final int jobRequestEndpointPort;
    private final String jobRequestEndpointHost;

    private String runnerId;
    private Undertow jobRequestEndpointServer;
    private final String jobRequestEndpointUrl;

    private RunnerStatusMonitor monitor;
    private RunnerPool runnerPool;
    private JobManager jobManager;

    private final AtomicReference<String> errorToken = new AtomicReference<>(null);
    private Object stopMonitor = new Object();

    public RunnerService(
            PoomjobsRunnerRegistryAPIClient runnerRegistryClient,
            PoomjobsJobRegistryAPIClient jobRegistryClient,
            int concurrentJobCount,
            long ttl,
            String jobCategory, String[] jobNames, JobProcessor.Factory jobProcessorFactory,
            JobContextSetup contextSetup,
            String jobRequestEndpointHost, int jobRequestEndpointPort
    ) {
        this.runnerRegistryClient = runnerRegistryClient;
        this.jobRegistryClient = jobRegistryClient;
        this.concurrentJobCount = concurrentJobCount;
        this.ttl = ttl;
        this.jobCategory = jobCategory;
        this.jobNames = jobNames;
        this.jobProcessorFactory = jobProcessorFactory;
        this.contextSetup = contextSetup;
        this.jobRequestEndpointPort = jobRequestEndpointPort;
        this.jobRequestEndpointHost = jobRequestEndpointHost;
        this.jobRequestEndpointUrl = String.format("http://%s:%s", this.jobRequestEndpointHost, this.jobRequestEndpointPort);
    }

    public void run() throws RunnerServiceInitializationException {
        this.registerRunner();
        this.startJobRequestEndpoint();
        this.startMonitor();
        this.createJobManager();
        this.startRunnerPool();

        synchronized (this.stopMonitor) {
            try {
                this.stopMonitor.wait();
            } catch (InterruptedException e) {}
        }

        if(this.errorToken.get() != null) {
            throw new RuntimeException("error in runner service, see logs with " + this.errorToken.get());
        }
    }

    public void stop() {
        synchronized (this.stopMonitor) {
            this.stopMonitor.notify();
        }
    }

    private void registerRunner() throws RunnerServiceInitializationException {
        try {
            RunnerCollectionPostResponse response = this.runnerRegistryClient.runnerCollection().post(RunnerCollectionPostRequest.builder()
                    .payload(RunnerData.builder()
                            .callback(this.jobRequestEndpointUrl)
                            .ttl(this.ttl)
                            .competencies(Competencies.builder()
                                    .categories(this.jobCategory)
                                    .names(this.jobNames)
                                    .build())
                            .build())
                    .build());
            response.opt().status201().orElseThrow(() -> new RunnerServiceInitializationException("error registering runner, expected 201, got " + response));

            String[] splitted = response.status201().location().split("/");
            this.runnerId = splitted[splitted.length - 1];
        } catch (IOException e) {
            throw new RunnerServiceInitializationException("failed reaching runner registry", e);
        }
    }

    private void startJobRequestEndpoint() {
        PathHandler pathHandlers = Handlers.path();
        pathHandlers.addPrefixPath( "/", new CdmHttpUndertowHandler( new PoomjobsRunnerAPIProcessor(
                "",
                new JsonFactory(),
                new PoomjobsRunnerAPIHandlers.Builder()
                        .runningJobPutHandler(this::jobExecutionRequested)
                        .build()
        )));
        this.jobRequestEndpointServer = Undertow.builder()
                .addHttpListener(this.jobRequestEndpointPort, this.jobRequestEndpointHost)
                .setHandler(pathHandlers)
                .build();
        this.jobRequestEndpointServer.start();
    }

    private void startMonitor() {
        this.monitor = new RunnerStatusMonitor("runner-" + this.runnerId);

    }

    private void createJobManager() {
        this.jobManager = new JobManager(
                this.jobRegistryClient,
                this.runnerId,
                this.jobCategory,
                this.jobNames
        );
    }

    private void startRunnerPool() {
        this.runnerPool = new RunnerPool(
                this.concurrentJobCount,
                this.jobManager,
                this.jobProcessorFactory,
                this.contextSetup,
                this,
                this.monitor
                );
        this.runnerPool.start();
        this.runnerPool.awaitReady(30, TimeUnit.SECONDS);
    }

    private RunningJobPutResponse jobExecutionRequested(RunningJobPutRequest request) {
        Job job = request.payload();
        try {
            this.runnerPool.submit(job);
        } catch (RunnerBusyException e) {
            return RunningJobPutResponse.builder().status409(status -> status.payload(error -> error
                    .code(Error.Code.RUNNER_IS_BUSY)
                    .token(log.tokenized().info("runner became busy", e))
                    .description("runner busy, come back later")
            )).build();
        } catch (JobNotSubmitableException e) {
            return RunningJobPutResponse.builder().status409(status -> status.payload(error -> error
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("problem submitting job for execution", e))
                    .description("failed taking job request")
            )).build();
        }
        return RunningJobPutResponse.builder().status201(status -> status
                .location("%s/%s", this.jobRequestEndpointUrl, job.id())
        ).build();
    }

    @Override
    public void unexpectedExceptionThrown(RunnerToken token, Exception e) {
        this.errorToken.set(log.tokenized().error("[GRAVE] unexpected error in job runner, unrecoverable, will stop", e));
        this.stop();
    }

    @Override
    public void processingExceptionThrown(RunnerToken token, JobProcessingException e) {
        log.error("error processing job from " + token, e);
    }
}
