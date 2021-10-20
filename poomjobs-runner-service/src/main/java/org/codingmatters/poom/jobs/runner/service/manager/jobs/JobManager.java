package org.codingmatters.poom.jobs.runner.service.manager.jobs;

import org.codingmatters.poom.jobs.runner.service.manager.flow.JobConsumer;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobProcessorRunner;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.JobResourcePatchRequest;
import org.codingmatters.poomjobs.api.JobResourcePatchResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.api.types.job.optional.OptionalStatus;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;

import java.io.IOException;

public class JobManager implements JobConsumer.NextJobSupplier, JobProcessorRunner.JobUpdater {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobManager.class);

    static private final int JOB_UPDATE_MAX_RETRIES = Env.optional("JOB_UPDATE_MAX_RETRIES").orElse(new Env.Var("5")).asInteger();
    static private final long JOB_UPDATE_RETRY_DELAY = Env.optional("JOB_UPDATE_RETRY_DELAY_IN_MS").orElse(new Env.Var("2000")).asInteger();

    private final PoomjobsJobRegistryAPIClient client;

    private final String accountId;
    private final String jobCategory;
    private final String[] jobNames;

    public JobManager(PoomjobsJobRegistryAPIClient client, String accountId, String jobCategory, String[] jobNames) {
        this.client = client;
        this.accountId = accountId;
        this.jobCategory = jobCategory;
        this.jobNames = jobNames;
    }

    @Override
    public synchronized Job update(Job job) throws JobProcessorRunner.JobUpdateFailure {
        return this.update(job, false);
    }

    public synchronized Job reserve(Job job) throws JobProcessorRunner.JobUpdateFailure {
        job = job.withStatus(org.codingmatters.poomjobs.api.types.job.Status.builder()
                .run(org.codingmatters.poomjobs.api.types.job.Status.Run.RUNNING)
                .build()
        );
        return this.update(
                job,
                true
        );
    }

    private synchronized Job update(Job job, boolean strictly) throws JobProcessorRunner.JobUpdateFailure {
        int tried = 0;
        JobResourcePatchResponse response = null;
        while(tried < JOB_UPDATE_MAX_RETRIES && response == null) {
            try {
                response = this.patchJob(job, strictly);
            } catch (IOException e) {
                tried++;
                log.warn("error updating job " + job.id() + " will retry in " + JOB_UPDATE_RETRY_DELAY + "ms (tried " + tried + " time)", e);
                try {
                    Thread.sleep(tried * JOB_UPDATE_RETRY_DELAY);
                } catch (InterruptedException ie) {}
            }
        }
        if(response == null) {
            String errorToken = log.tokenized().error(
                    "[GRAVE] failed updating job {} {} times (see previous logs). " +
                            "Will not retry. " +
                            "This is not recoverable, job data is lost, failing fast.",
                    job, tried);
            throw new JobProcessorRunner.JobUpdateFailure("Unrecoverable error updating job status. See logs with token : " + errorToken);
        } else if(response.opt().status200().isEmpty()) {
            String errorToken = log.tokenized().error(
                    "[GRAVE] failed updating job {}. Got response {} " +
                            "Will not retry. " +
                            "This is not recoverable, job data is lost, failing fast.",
                    job, response
            );
            throw new JobProcessorRunner.JobUpdateFailure("Unrecoverable error updating job status. See logs with token : " + errorToken);
        } else {
            return response.status200().payload();
        }
    }

    private JobResourcePatchResponse patchJob(Job job, boolean strictly) throws IOException {
        JobResourcePatchResponse response = this.client.jobCollection().jobResource().patch(JobResourcePatchRequest.builder()
                .accountId(this.accountId)
                .jobId(job.id())
                .currentVersion(job.version())
                .strict(strictly)
                .payload(JobUpdateData.builder()
                        .status(this.translated(job.opt().status()))
                        .result(job.result())
                        .build())
                .build());
        return response;
    }

    private Status translated(OptionalStatus status) {
        return Status.builder()
                .run(status.run().isPresent() ? Status.Run.valueOf(status.run().get().name()) : null)
                .exit(status.exit().isPresent() ? Status.Exit.valueOf(status.exit().get().name()) : null)
                .build();
    }



    @Override
    public synchronized Job nextJob() {
        try {
            Job job = null;
            while(job == null) {
                JobCollectionGetResponse response = this.client.jobCollection().get(builder -> builder
                        .accountId(this.accountId)
                        .category(this.jobCategory)
                        .names(this.jobNames)
                        .runStatus(org.codingmatters.poomjobs.api.types.job.Status.Run.PENDING.name())
                        .range("0-9")
                );
                if (response.opt().status200().isPresent()) {
                    if (response.status200().payload() == null || response.status200().payload().isEmpty()) return null;
                    job = this.firstRunnableJob(response.status200().payload());
                } else if (response.opt().status206().isPresent()) {
                    job = this.firstRunnableJob(response.status206().payload());
                } else {
                    log.error("failed getting candidate job list, expected 200 or 206, got : {}", response);
                    return null;
                }
            }
            return job;
        } catch (IOException e) {
            log.error("couldn't reach job registry, nothing to process", e);
            return null;
        }
    }

    private Job firstRunnableJob(ValueList<Job> candidates) throws IOException {
        for (Job candidate : candidates) {
            try {
                return this.reserve(candidate);
            } catch (JobProcessorRunner.JobUpdateFailure e) {
                e.printStackTrace();
            }
            log.debug("trying to reserve candidate job {}", candidate);
        }
        return null;
    }

}
