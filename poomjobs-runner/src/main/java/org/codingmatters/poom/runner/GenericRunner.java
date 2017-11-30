package org.codingmatters.poom.runner;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.runner.exception.RunnerInitializationException;
import org.codingmatters.poom.runner.internal.StatusManager;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.JobResourcePatchResponse;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
    private final JobProcessor.Factory prcessorFactory;

    private final ScheduledExecutorService updateWorker = Executors.newSingleThreadScheduledExecutor();

    private String id;

    private StatusManager statusManager;

    public GenericRunner(RunnerConfiguration configuration) {
        this.jobRegistryAPIClient = configuration.jobRegistryAPIClient();
        this.runnerRegistryAPIClient = configuration.runnerRegistryAPIClient();
        this.jobWorker = configuration.jobWorker();
        this.callbackBaseUrl = configuration.callbackBaseUrl();
        this.ttl = Math.max(MIN_TTL, configuration.ttl());
        this.jobCategory = configuration.jobCategory();
        this.jobName = configuration.jobName();
        this.prcessorFactory = configuration.processorFactory();
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

            this.statusManager = new StatusManager(this.id, this.runnerRegistryAPIClient, this.ttl, this.updateWorker);
            this.statusManager.scheduleNextStatusUpdate(firstPing);

            this.lookupPendingJobs();
        } catch (IOException e) {
            log.error("cannot connect to runner registry", e);
            throw new RunnerInitializationException("cannot connect to runner registry", e);
        }
    }

    public String id() {
        return this.id;
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

            if (!jobs.isEmpty()) {
                this.processJob(jobs.get(0));
            }
        } catch (IOException e) {
            log.error("error retrieving jobs from repository", e);
        }
    }

    private void processJob(Job job) {
        log.info("will process job {}", job);
        this.statusManager.updateStatus(RunnerStatusData.Status.RUNNING);

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
            if (response.opt().status200().isPresent()) {
                this.jobWorker.submit(this.jobProcessor(job));
            } else {
                log.warn("job repository refused our RUNNING update on job : {}", job);
                this.statusManager.updateStatus(RunnerStatusData.Status.IDLE);
            }
        } catch (IOException e) {
            log.error("error updating run status for job " + job, e);
        }

    }

    private Runnable jobProcessor(Job job) {
        JobProcessor processor = this.prcessorFactory.createFor(job);
        return () -> process(processor);
    }

    private void process(JobProcessor processor) {
        try {
            Job job = processor.process();
            try {
                this.jobRegistryAPIClient.jobCollection().jobResource().patch(req -> req
                        .jobId(job.id()).payload(payload -> payload.status(this.patchStatus(job.status())))
                );
            } catch (IOException e) {
                log.error("GRAVE : failed to update job status for job " + job.id(), e);
            }
        } catch (JobProcessingException e) {
            log.error("error processing job with processor : " + processor, e);

        }
    }

    private Status patchStatus(org.codingmatters.poomjobs.api.types.job.Status status) {
        return Status.builder()
                .run(Status.Run.valueOf(
                        status.opt().run()
                                .orElse(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE)
                                .name()
                        )
                )
                .exit(Status.Exit.valueOf(
                        status.opt().exit()
                                .orElse(org.codingmatters.poomjobs.api.types.job.Status.Exit.SUCCESS)
                                .name()
                        )
                )
                .build();
    }
}
