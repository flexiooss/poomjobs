package org.codingmatters.poom.jobs.runner.service;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerServiceInitializationException;
import org.codingmatters.poom.jobs.runner.service.execution.pool.JobProcessingPoolManager;
import org.codingmatters.poom.jobs.runner.service.jobs.JobManager;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.api.PoomjobsRunnerAPIHandlers;
import org.codingmatters.poomjobs.api.RunnerCollectionPostRequest;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.codingmatters.poomjobs.api.types.RunnerData;
import org.codingmatters.poomjobs.api.types.runnerdata.Competencies;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poomjobs.service.api.PoomjobsRunnerAPIProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class RunnerService {
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
        OptionsSetup handlers(PathHandler pathHandlers);
        OptionsSetup exitOnUnrecoverableError(boolean exit);
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
        private boolean exitOnUnrecoverableError = true;
        private PathHandler pathHandlers;


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
        public EndpointSetup handlers(PathHandler pathHandlers) {
            this.pathHandlers = pathHandlers;
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
        public OptionsSetup exitOnUnrecoverableError(boolean exit) {
            this.exitOnUnrecoverableError = exit;
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
                    this.pathHandlers != null ? this.pathHandlers : Handlers.path(),
                    this.jobRequestEndpointHost,
                    this.jobRequestEndpointPort
            );
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

    private JobManager jobManager;
    private JobProcessingPoolManager jobProcessingPoolManager;
    private RunnerStatusManager runnerStatusManager;

    private final AtomicReference<String> errorToken = new AtomicReference<>(null);
    private final Object stopMonitor = new Object();

    private final ExecutorService statusManagerExecutor = Executors.newSingleThreadExecutor();

    private final PathHandler pathHandlers;

    public RunnerService(
            PoomjobsRunnerRegistryAPIClient runnerRegistryClient,
            PoomjobsJobRegistryAPIClient jobRegistryClient,
            int concurrentJobCount,
            long ttl,
            String jobCategory, String[] jobNames, JobProcessor.Factory jobProcessorFactory,
            JobContextSetup contextSetup,
            PathHandler pathHandlers,
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
        this.pathHandlers = pathHandlers;
        this.jobRequestEndpointPort = jobRequestEndpointPort;
        this.jobRequestEndpointHost = jobRequestEndpointHost;
        this.jobRequestEndpointUrl = Env.mandatory(Env.SERVICE_URL).asString();
    }

    public void run() throws RunnerServiceInitializationException {
        this.registerRunner();
        this.createJobManager();
        this.createRunnerStatusManager();
        this.createJobProcessingPoolManager();
        this.startJobRequestEndpoint();

        synchronized (this.stopMonitor) {
            try {
                this.stopMonitor.wait();
                log.info("runner service stop requested...");
            } catch (InterruptedException e) {
                log.error("runner service was interrupted");
            }
        }

        if(this.errorToken.get() != null) {
            throw new RuntimeException("error in runner service, see logs with " + this.errorToken.get());
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

    private void createRunnerStatusManager() {
        this.runnerStatusManager = new RunnerStatusManager(this.runnerRegistryClient, this.runnerId, this.ttl - (this.ttl / 10), this.ttl);
        this.runnerStatusManager.start();
        this.statusManagerExecutor.submit(this.runnerStatusManager);
    }

    private void createJobProcessingPoolManager() {
        this.jobProcessingPoolManager = new JobProcessingPoolManager(
                this.concurrentJobCount,
                this.jobManager,
                this.jobProcessorFactory,
                this.contextSetup,
                this.jobRequestEndpointUrl,
                this.runnerStatusManager
        );
        this.jobProcessingPoolManager.start();
    }


    private void startJobRequestEndpoint() {
        pathHandlers.addPrefixPath( "/", new CdmHttpUndertowHandler( new PoomjobsRunnerAPIProcessor(
                "",
                new JsonFactory(),
                new PoomjobsRunnerAPIHandlers.Builder()
                        .runningJobPutHandler(this.jobProcessingPoolManager::jobExecutionRequested)
                        .build()
        )));
        this.jobRequestEndpointServer = Undertow.builder()
                .addHttpListener(this.jobRequestEndpointPort, this.jobRequestEndpointHost)
                .setHandler(pathHandlers)
                .build();
        this.jobRequestEndpointServer.start();
    }

    private void createJobManager() {
        this.jobManager = new JobManager(
                this.jobRegistryClient,
                this.runnerId,
                this.jobCategory,
                this.jobNames
        );
    }

    public void stop() {
        try {
            this.runnerStatusManager.stop();
        } catch (Exception e) {}
        try {
            this.statusManagerExecutor.shutdown();
        } catch (Exception e) {}
        try {
            this.jobRequestEndpointServer.stop();
        } catch (Exception e) {}
        try {
            this.jobProcessingPoolManager.stop(10 * 1000L);
        } catch (Exception e) {}

        synchronized (this.stopMonitor) {
            this.stopMonitor.notify();
        }
    }

}
