package org.codingmatters.poom.runner;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.exception.RunnerInitializationException;
import org.codingmatters.poom.runner.internal.JobManager;
import org.codingmatters.poom.runner.internal.RunnerEndpoint;
import org.codingmatters.poom.runner.internal.StatusManager;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
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
                    this.processorFactory
            );

            this.endpoint = new RunnerEndpoint(
                    this.statusManager,
                    this.jobManager,
                    this.jobRegistryUrl,
                    this.endpointHost,
                    this.endpointPort
            );
            this.endpoint.start();

            this.processPendingJobs();
        } catch (IOException e) {
            log.error("cannot connect to runner registry", e);
            throw new RunnerInitializationException("cannot connect to runner registry", e);
        }
    }

    public void stop() {
        this.endpoint.stop();
        this.updateWorker.shutdown();
        try {
            this.updateWorker.awaitTermination(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("failed waiting for update worker clean shutdown");
        }
        if(!this.updateWorker.isTerminated()) {
            int stoppedJobCount = this.updateWorker.shutdownNow().size();
            log.warn("couldn't cleanly stop update worker, forcing shutdown, {} jobs have been cancelled.");
        }
        this.statusManager = null;
        this.jobManager = null;
    }

    public String id() {
        return this.id;
    }

    private void processPendingJobs() {
        try {
            JobCollectionGetResponse response = this.jobRegistryAPIClient.jobCollection().get(request ->
                    request
                            .category(this.jobCategory)
                            .name(this.jobName)
                            .runStatus("PENDING")
                            .range("0-1")
            );

            ValueList<Job> jobs = response.opt().status200().payload()
                    .orElseGet(() ->
                            response.opt().status206().payload()
                                    .orElse(new ValueList.Builder<Job>().build())
                    );

            if (!jobs.isEmpty()) {
                this.jobManager.processJob(jobs.get(0));
                this.processPendingJobs();
            }
        } catch (IOException e) {
            log.error("error retrieving jobs from repository", e);
        }
    }

}
