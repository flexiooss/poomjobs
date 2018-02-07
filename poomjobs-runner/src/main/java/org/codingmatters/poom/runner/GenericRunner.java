package org.codingmatters.poom.runner;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.exception.RunnerInitializationException;
import org.codingmatters.poom.runner.internal.JobManager;
import org.codingmatters.poom.runner.internal.RunnerEndpoint;
import org.codingmatters.poom.runner.internal.StatusManager;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GenericRunner {

    static private Logger log = LoggerFactory.getLogger(GenericRunner.class);

    static private long MIN_TTL = 1000L;

    private final PoomjobsJobRegistryAPIClient jobRegistryAPIClient;
    private final PoomjobsRunnerRegistryAPIClient runnerRegistryAPIClient;
    private final ExecutorService jobWorker;

    private final String callbackBaseUrl;
    private final Long ttl;
    private final String jobCategory;
    private final String jobName;
    private final JobProcessor.Factory processorFactory;

    private final String jobRegistryUrl;
    private final String endpointHost;
    private final int endpointPort;

    private final ScheduledExecutorService updateWorker = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService boostrapPool = Executors.newSingleThreadScheduledExecutor();

    private String id;

    private StatusManager statusManager;
    private JobManager jobManager;
    private RunnerEndpoint endpoint;

    public GenericRunner(RunnerConfiguration configuration) {
        this.jobRegistryAPIClient = configuration.jobRegistryAPIClient();
        this.runnerRegistryAPIClient = configuration.runnerRegistryAPIClient();
        this.jobWorker = configuration.jobWorker();
        this.callbackBaseUrl = configuration.callbackBaseUrl();
        this.ttl = Math.max(MIN_TTL, configuration.ttl());
        this.jobCategory = configuration.jobCategory();
        this.jobName = configuration.jobName();
        this.processorFactory = configuration.processorFactory();
        this.jobRegistryUrl = configuration.jobRegistryUrl();
        this.endpointHost = configuration.endpointHost();
        this.endpointPort = configuration.endpointPort();
    }

    public void start() throws RunnerInitializationException {
        log.info("starting runner by registering to runners service");
        RunnerCollectionPostResponse response = null;
        try {
            LocalDateTime firstPing = LocalDateTime.now();
            response = this.runnerRegistryAPIClient.runnerCollection().post(request ->
                    request
                            .payload(payload ->
                                    payload
                                            .callback(this.callbackBaseUrl)
                                            .ttl(this.ttl)
                                            .competencies(competencies ->
                                                    competencies
                                                            .categories(this.jobCategory)
                                                            .names(this.jobName)
                                            )
                            )
                            .build());
            log.debug("runners service responded : {}", response);
            if (response.status201() != null) {
                String[] splitted = response.status201().location().split("/");
                this.id = splitted[splitted.length - 1];
            } else {
                log.error("registry refused to register runner : {}", response);
                throw new RunnerInitializationException("registry refused to register runner : " + response.toString());
            }

            this.statusManager = new StatusManager(
                    this.id,
                    this.runnerRegistryAPIClient,
                    this.ttl,
                    this.updateWorker
            );
            this.statusManager.scheduleNextStatusUpdate(firstPing);
            this.jobManager = new JobManager(
                    this.statusManager,
                    this.jobRegistryAPIClient,
                    this.jobWorker,
                    this.processorFactory,
                    this.jobCategory,
                    this.jobName,
                    this.id
            );

            this.endpoint = new RunnerEndpoint(
                    this.statusManager,
                    this.jobManager,
                    this.jobRegistryUrl,
                    this.endpointHost,
                    this.endpointPort
            );
            this.endpoint.start();

            this.bootstrap();
        } catch (IOException e) {
            log.error("cannot connect to runner registry", e);
            throw new RunnerInitializationException("cannot connect to runner registry", e);
        }
    }

    private void bootstrap() {
        this.boostrapPool.submit(() -> this.jobManager.processPendingJobs());
    }

    public void stop() {
        this.endpoint.stop();
        this.shutdownPool(this.updateWorker, "update worker");
        this.shutdownPool(this.boostrapPool, "bootstrap pool");
        this.statusManager = null;
        this.jobManager = null;
    }

    private void shutdownPool(ExecutorService pool, String name) {
        pool.shutdown();
        try {
            pool.awaitTermination(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("failed waiting for {} clean shutdown", name);
        }
        if(!pool.isTerminated()) {
            int stoppedJobCount = pool.shutdownNow().size();
            log.warn("couldn't cleanly stop {}, forcing shutdown, {} jobs have been cancelled.", name, stoppedJobCount);
        }
    }

    public String id() {
        return this.id;
    }


}
