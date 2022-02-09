package org.codingmatters.poom.jobs.runner.service.jobs;

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

public class JobManager implements JobProcessorRunner.JobUpdater, JobProcessorRunner.PendingJobManager {
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
    public Job update(Job job) throws JobProcessorRunner.JobUpdateFailure {
        return this.update(job, false);
    }

    public Job reserve(Job job) throws JobProcessorRunner.JobUpdateFailure {
        job = job.withStatus(org.codingmatters.poomjobs.api.types.job.Status.builder()
                .run(org.codingmatters.poomjobs.api.types.job.Status.Run.RUNNING)
                .build()
        );
        return this.update(
                job,
                true
        );
    }

    private Job update(Job job, boolean strictly) throws JobProcessorRunner.JobUpdateFailure {
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
            throw new JobProcessorRunner.JobUpdateFailure(String.format(
                    "Failed updating job, tried %s time, will not retry.", tried
            ));
        } else if(response.opt().status200().isEmpty()) {
            throw new JobProcessorRunner.JobUpdateFailure("Failed updating job, got response : " + response);
        } else {
            return response.status200().payload();
        }
    }

    private JobResourcePatchResponse patchJob(Job job, boolean strictly) throws IOException {
        log.debug("patching job");
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
        log.debug("job patch response " + response);
        return response;
    }

    private Status translated(OptionalStatus status) {
        return Status.builder()
                .run(status.run().isPresent() ? Status.Run.valueOf(status.run().get().name()) : null)
                .exit(status.exit().isPresent() ? Status.Exit.valueOf(status.exit().get().name()) : null)
                .build();
    }

    public ValueList<Job> pendingJobs() {
        JobCollectionGetResponse response;
        try {
            response = this.client.jobCollection().get(builder -> builder
                .accountId(this.accountId)
                .category(this.jobCategory)
                .names(this.jobNames)
                .runStatus(org.codingmatters.poomjobs.api.types.job.Status.Run.PENDING.name())
                .range("0-19")
            );
        } catch (IOException e) {
            log.error("while getting candidate jobs, couldn't reach job registry, nothing to process", e);
            return ValueList.<Job>builder().build();
        }

        ValueList<Job> candidates;
        if(response.opt().status200().isPresent()) {
            candidates = response.status200().payload();
        } else if(response.opt().status206().isPresent()) {
            candidates = response.status206().payload();
        } else {
            log.error("failed getting candidate job list, expected 200 or 206, got : {}", response);
            return null;
        }
        return candidates;
    }

}
