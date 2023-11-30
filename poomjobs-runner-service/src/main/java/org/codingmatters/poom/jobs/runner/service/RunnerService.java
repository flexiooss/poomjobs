package org.codingmatters.poom.jobs.runner.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.ApiContainerRuntimeBuilder;
import org.codingmatters.poom.containers.ServerShutdownException;
import org.codingmatters.poom.containers.ServerStartupException;
import org.codingmatters.poom.containers.runtime.netty.NettyApiContainerRuntime;
import org.codingmatters.poom.containers.runtime.undertow.UndertowApiContainerRuntime;
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
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class RunnerService {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerService.class);

    private static final long MIN_TTL = 1000L;
    public static final long DEFAULT_TTL = 30 * 1000L;

    public static final String SERVICE_RUNTIME = "SERVICE_RUNTIME";

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
        OptionsSetup containerRuntimeBuilder(ApiContainerRuntimeBuilder containerRuntimeBuilder);
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
        private ApiContainerRuntimeBuilder containerRuntimeBuilder;


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
        public OptionsSetup containerRuntimeBuilder(ApiContainerRuntimeBuilder containerRuntimeBuilder) {
            this.containerRuntimeBuilder = containerRuntimeBuilder;
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
                    this.containerRuntimeBuilder != null ? this.containerRuntimeBuilder : new ApiContainerRuntimeBuilder(),
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
    private final String jobRequestEndpointUrl;

    private JobManager jobManager;
    private JobProcessingPoolManager jobProcessingPoolManager;
    private RunnerStatusManager runnerStatusManager;

    private final AtomicReference<String> errorToken = new AtomicReference<>(null);
    private final Object stopMonitor = new Object();

    private final ExecutorService statusManagerExecutor = Executors.newSingleThreadExecutor();

    private final ApiContainerRuntimeBuilder containerRuntimeBuilder;
    private ApiContainerRuntime runtime;

    public RunnerService(
            PoomjobsRunnerRegistryAPIClient runnerRegistryClient,
            PoomjobsJobRegistryAPIClient jobRegistryClient,
            int concurrentJobCount,
            long ttl,
            String jobCategory, String[] jobNames, JobProcessor.Factory jobProcessorFactory,
            JobContextSetup contextSetup,
            ApiContainerRuntimeBuilder containerRuntimeBuilder,
            String jobRequestEndpointHost,
            int jobRequestEndpointPort
    ) {
        this.runnerRegistryClient = runnerRegistryClient;
        this.jobRegistryClient = jobRegistryClient;
        this.concurrentJobCount = concurrentJobCount;
        this.ttl = ttl;
        this.jobCategory = jobCategory;
        this.jobNames = jobNames;
        this.jobProcessorFactory = jobProcessorFactory;
        this.contextSetup = contextSetup;
        this.containerRuntimeBuilder = containerRuntimeBuilder;
        this.jobRequestEndpointPort = jobRequestEndpointPort;
        this.jobRequestEndpointHost = jobRequestEndpointHost;
        this.jobRequestEndpointUrl = Env.mandatory(Env.SERVICE_URL).asString();
    }

    public void run() throws RunnerServiceInitializationException {
        this.run(this::createRunime);
    }

    private ApiContainerRuntime createRunime(String host, int port, CategorizedLogger logger) {
        if(Env.optional(SERVICE_RUNTIME).orElse(new Env.Var("UNDERTOW")).asString().equals("NETTY")) {
            return new NettyApiContainerRuntime(host, port, logger);
        } else {
            return new UndertowApiContainerRuntime(host, port, logger);
        }
    }

    public void run(RuntimeInitializer runtimeInitializer) throws RunnerServiceInitializationException {
        this.registerRunner();
        this.createJobManager();
        this.createRunnerStatusManager();
        this.createJobProcessingPoolManager();
        try {
            this.startJobRequestEndpoint(runtimeInitializer.initialize(this.jobRequestEndpointHost, this.jobRequestEndpointPort, log));
        } catch (Exception e) {
            throw new RunnerServiceInitializationException("failed initializing runtime", e);
        }

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


    private void startJobRequestEndpoint(ApiContainerRuntime withRuntime) {
        Processor processor = new PoomjobsRunnerAPIProcessor(
                "",
                new JsonFactory(),
                new PoomjobsRunnerAPIHandlers.Builder()
                        .runningJobPutHandler(this.jobProcessingPoolManager::jobExecutionRequested)
                        .build()
        );
        this.containerRuntimeBuilder.withApi(new Api() {
            @Override
            public String name() {
                return "runner";
            }

            @Override
            public String version() {
                return Api.versionFrom(PoomjobsRunnerAPIProcessor.class);
            }

            @Override
            public Processor processor() {
                return (requestDelegate, responseDelegate) -> {
                    processor.process(requestDelegate, responseDelegate);
                };
            }

            @Override
            public String path() {
                return "/";
            }
        });

        this.runtime = this.containerRuntimeBuilder
                .onShutdown(this::onStop)
                .build(withRuntime)
        ;

        try {
            this.runtime.handle().start();
        } catch (ServerStartupException e) {
            throw new RuntimeException("error starting container runtime", e);
        }
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
            this.runtime.handle().stop();
        } catch (ServerShutdownException e) {
            throw new RuntimeException("error stopping container runtime", e);
        }
    }

    private void onStop() {
        try {
            this.runnerStatusManager.stop();
        } catch (Exception e) {}
        try {
            this.statusManagerExecutor.shutdown();
        } catch (Exception e) {}
        try {
            this.jobProcessingPoolManager.stop(10 * 1000L);
        } catch (Exception e) {}

        synchronized (this.stopMonitor) {
            this.stopMonitor.notify();
        }
    }


    public interface RuntimeInitializer {
        ApiContainerRuntime initialize(String host, int port, CategorizedLogger log) throws Exception;
    }

}
