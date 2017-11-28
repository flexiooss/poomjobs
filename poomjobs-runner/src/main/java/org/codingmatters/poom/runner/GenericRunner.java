package org.codingmatters.poom.runner;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.exception.RunnerInitializationException;
import org.codingmatters.poomjobs.api.*;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GenericRunner {

    static private Logger log = LoggerFactory.getLogger(GenericRunner.class);

    static private long MIN_TTL = 1000L;
    static private long NOTIFY_BEFORE_TTL = 500L;

    private final PoomjobsJobRegistryAPIClient jobRegistryAPIClient;
    private final PoomjobsRunnerRegistryAPIClient runnerRegistryAPIClient;
    private final ExecutorService jobWorker;

    private final String callbackBaseUrl;
    private final Long ttl;
    private final String jobCategory;
    private final String jobName;

    private final ScheduledExecutorService updateWorker = Executors.newSingleThreadScheduledExecutor();

    private String id;
    private final AtomicReference<RunnerStatusData.Status> currentStatus = new AtomicReference<>(RunnerStatusData.Status.IDLE);


    public GenericRunner(RunnerConfiguration configuration) {
        this.jobRegistryAPIClient = configuration.jobRegistryAPIClient();
        this.runnerRegistryAPIClient = configuration.runnerRegistryAPIClient();
        this.jobWorker = configuration.jobWorker();
        this.callbackBaseUrl = configuration.callbackBaseUrl();
        this.ttl = Math.max(MIN_TTL, configuration.ttl());
        this.jobCategory = configuration.jobCategory();
        this.jobName = configuration.jobName();
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
            if(response.status201() != null) {
                String[] splitted = response.status201().location().split("/");
                this.id = splitted[splitted.length - 1];
            } else {
                log.error("registry refused to register runner : {}", response);
                throw new RunnerInitializationException("registry refused to register runner : " + response.toString());
            }

            this.scheduleNextStatusUpdate(firstPing);
            this.lookupPendingJobs();
        } catch (IOException e) {
            log.error("cannot connect to runner registry", e);
            throw new RunnerInitializationException("cannot connect to runner registry", e);
        }
    }

    public String id() {
        return this.id;
    }

    private void updateStatus() {
        try {
            RunnerStatusData.Status status = this.currentStatus.get();
            RunnerPatchResponse response = this.runnerRegistryAPIClient.runnerCollection().runner().patch(request ->
                    request
                            .runnerId(this.id())
                            .payload(payload -> payload.status(status))
            );
            if(response.status200() != null) {
                log.debug("updated status for {} with status : {}", this.id(), status);
                this.scheduleNextStatusUpdate(response.status200().payload().runtime().lastPing());
            } else {
                log.error("runner registry refused our status notification for runner {} with response : {}",
                        this.id(),
                        response
                );
                return;
            }
        } catch (IOException e) {
            log.error("error notifying status to runner repository for runner " + this.id(), e);
        }
    }

    private void scheduleNextStatusUpdate(LocalDateTime lastPing) {
        LocalDateTime nextNotification = lastPing.plus(this.ttl, ChronoUnit.MILLIS);
        this.updateWorker.schedule(
                this::updateStatus,
                Duration.between(LocalDateTime.now(), nextNotification).toMillis(), TimeUnit.MILLISECONDS
        );
    }

    private void lookupPendingJobs() {
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

            if(! jobs.isEmpty()) {
                this.processJob(jobs.get(0));
            }
        } catch (IOException e) {
            log.error("error retrieving jobs from repository", e);
        }
    }

    private void processJob(Job job) {
        log.info("will process job {}", job);
        this.currentStatus.set(RunnerStatusData.Status.RUNNING);
        this.updateStatus();

        try {
            JobResourcePatchResponse response = this.jobRegistryAPIClient.jobCollection().jobResource().patch(request ->
                    request
                            .jobId(job.id()).payload(
                            payload -> payload
                                    .status(Status.builder()
                                            .run(Status.Run.RUNNING)
                                            .build())
                    )
            );
            if(response.opt().status200().isPresent()) {
                this.jobWorker.submit(this.jobProcessor(job));
            } else {
                log.warn("job repository refused our RUNNING update on job : {}", job);
                this.currentStatus.set(RunnerStatusData.Status.IDLE);
                this.updateStatus();
            }
        } catch (IOException e) {
            log.error("error updating run status for job " + job, e);
        }

    }

    private Runnable jobProcessor(Job job) {
        return () -> {
            try {
                Thread.sleep(3 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }
}
