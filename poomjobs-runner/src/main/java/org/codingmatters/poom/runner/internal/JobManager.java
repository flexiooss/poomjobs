package org.codingmatters.poom.runner.internal;

import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.runner.exception.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.JobResourcePatchResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class JobManager {

    private static CategorizedLogger log = CategorizedLogger.getLogger(JobManager.class);

    private final StatusManager statusManager;
    private final PoomjobsJobRegistryAPIClient jobRegistryAPIClient;
    private final ExecutorService jobWorker;
    private final JobProcessor.Factory processorFactory;
    private final String jobCategory;
    private final String[] jobNames;
    private final String runnerId;

    private final JobContextSetup jobContextSetup;

    public JobManager(
            StatusManager statusManager,
            PoomjobsJobRegistryAPIClient jobRegistryAPIClient,
            ExecutorService jobWorker,
            JobProcessor.Factory processorFactory,
            String jobCategory,
            String[] jobNames,
            String runnerId,
            JobContextSetup jobContextSetup
    ) {
        this.statusManager = statusManager;
        this.jobRegistryAPIClient = jobRegistryAPIClient;
        this.jobWorker = jobWorker;
        this.processorFactory = processorFactory;
        this.jobCategory = jobCategory;
        this.jobNames = jobNames;
        this.runnerId = runnerId;
        this.jobContextSetup = jobContextSetup;
    }

    public void processIncommingJob(Job job) {
        this.processJob(job);
        this.processPendingJobs();
    }

    public void processPendingJobs() {
        try {
            for(Optional<Job> job = this.nextJob(); job.isPresent() ; job = this.nextJob()) {
                log.info("running job {}", job.get().id());
                this.processJob(job.get());
                log.info("job finished {}", job.get().id());
            }
            log.info("no job to process, setting status to IDLE");
            this.statusManager.updateStatus(RunnerStatusData.Status.IDLE);
        } catch (IOException e) {
            log.error("error retrieving jobs from repository", e);
        }
    }

    private void processJob(Job job) {
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
                            .accountId(this.runnerId)
            );
            if (response.opt().status200().isPresent()) {
                try {
                    this.jobWorker.submit(this.jobProcessor(job)).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("error running job " + job, e);
                }
            } else {
                log.warn("job repository refused our RUNNING update on job : {}", job);
                this.statusManager.updateStatus(RunnerStatusData.Status.IDLE);
            }
        } catch (IOException e) {
            log.error("error updating run status for job " + job, e);
        }

    }

    private Optional<Job> nextJob() throws IOException {
        JobCollectionGetRequest getPendingJobs = JobCollectionGetRequest.builder()
                .category(this.jobCategory)
                .names(this.jobNames)
                .runStatus("PENDING")
                .range("0-0")
                .accountId(this.runnerId)
                .build();

        JobCollectionGetResponse response = this.jobRegistryAPIClient.jobCollection().get(
                getPendingJobs
        );
        ValueList<Job> jobs = response.opt().status200().payload()
                .orElseGet(() ->
                        response.opt().status206().payload()
                                .orElse(new ValueList.Builder<Job>().build())
                );
        log.info("Jobs: " + jobs.size());

        if(! jobs.isEmpty()) {
            return Optional.of(jobs.get(0));
        } else {
            return Optional.empty();
        }
    }

    private Runnable jobProcessor(Job job) {
        JobProcessor processor = this.processorFactory.createFor(job);
        return () -> this.setupAndProcess(job, processor);
    }

    private void setupAndProcess(Job job, JobProcessor processor) {
        try(LoggingContext loggingContext = LoggingContext.start()) {
            this.jobContextSetup.setup(job, processor);
            log.info("processing job {}", job);
            this.process(processor);
        }
    }

    private void process(JobProcessor processor) {
        try {
            Job job = processor.process();
            try {
                this.jobRegistryAPIClient.jobCollection().jobResource().patch(req -> req
                        .jobId(job.id())
                        .accountId(this.runnerId)
                        .payload(payload -> payload
                                .status(this.patchStatus(job.status()))
                                .result(job.result())
                        )
                );
                log.info("done processing job {}", job);
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
