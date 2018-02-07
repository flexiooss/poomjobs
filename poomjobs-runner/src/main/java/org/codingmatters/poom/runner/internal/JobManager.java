package org.codingmatters.poom.runner.internal;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.JobResourcePatchResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class JobManager {

    private static Logger log = LoggerFactory.getLogger(JobManager.class);

    private final StatusManager statusManager;
    private final PoomjobsJobRegistryAPIClient jobRegistryAPIClient;
    private final ExecutorService jobWorker;
    private final JobProcessor.Factory processorFactory;
    private final String jobCategory;
    private final String jobName;
    private final String runnerId;

    public JobManager(
            StatusManager statusManager,
            PoomjobsJobRegistryAPIClient jobRegistryAPIClient,
            ExecutorService jobWorker,
            JobProcessor.Factory processorFactory,
            String jobCategory,
            String jobName,
            String runnerId
    ) {
        this.statusManager = statusManager;
        this.jobRegistryAPIClient = jobRegistryAPIClient;
        this.jobWorker = jobWorker;
        this.processorFactory = processorFactory;
        this.jobCategory = jobCategory;
        this.jobName = jobName;
        this.runnerId = runnerId;
    }


    public void processJob(Job job) {
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
                            .accountId(this.runnerId)
            );
            if (response.opt().status200().isPresent()) {
                try {
                    this.jobWorker.submit(this.jobProcessor(job)).get();
                    this.processPendingJobs();
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

    public void processPendingJobs() {
        try {
            JobCollectionGetRequest getPendingJobs = JobCollectionGetRequest.builder()
                    .category(this.jobCategory)
                    .name(this.jobName)
                    .runStatus("PENDING")
                    .range("0-1")
                    .accountId(this.runnerId)
                    .build();

            JobCollectionGetResponse response = this.jobRegistryAPIClient.jobCollection().get(
                    getPendingJobs
            );
            log.info("Jobs: " + response.status200().payload().size());
            ValueList<Job> jobs = response.opt().status200().payload()
                    .orElseGet(() ->
                            response.opt().status206().payload()
                                    .orElse(new ValueList.Builder<Job>().build())
                    );

            if (!jobs.isEmpty()) {
                log.info("running job {}", jobs.get(0).id());
                this.processJob(jobs.get(0));
                log.info("job finished {}", jobs.get(0).id());
                this.processPendingJobs();
            } else {
                log.info("no job to process, setting status to IDLE");
                this.statusManager.updateStatus(RunnerStatusData.Status.IDLE);
            }
        } catch (IOException e) {
            log.error("error retrieving jobs from repository", e);
        }
    }

    private Runnable jobProcessor(Job job) {
        JobProcessor processor = this.processorFactory.createFor(job);
        return () -> process(processor);
    }

    private void process(JobProcessor processor) {
        try {
            Job job = processor.process();
            try {
                this.jobRegistryAPIClient.jobCollection().jobResource().patch(req -> req
                        .jobId(job.id()).payload(payload -> payload.status(this.patchStatus(job.status()))).accountId(this.runnerId)
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
