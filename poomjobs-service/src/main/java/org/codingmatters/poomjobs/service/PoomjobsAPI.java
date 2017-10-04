package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.PoomjobsAPIHandlers;
import org.codingmatters.poomjobs.service.handlers.*;

/**
 * Created by nelt on 6/15/17.
 */
public class PoomjobsAPI {
    private final PoomjobsAPIHandlers handlers;

    public PoomjobsAPI(
            Repository<JobValue, JobQuery> jobRepository,
            Repository<RunnerValue, RunnerQuery> runnerRepository
    ) {
        this.handlers = new PoomjobsAPIHandlers.Builder()
                .jobCollectionGetHandler(new JobCollectionGetHandler(jobRepository))
                .jobCollectionPostHandler(new JobCollectionPostHandler(jobRepository))
                .jobResourceGetHandler(new JobResourceGetHandler(jobRepository))
                .jobResourcePatchHandler(new JobResourcePutHandler(jobRepository))
//                .jobResourcePutHandler(new JobResourcePutHandler(jobRepository))

                .runnerCollectionGetHandler(new RunnerCollectionGetHandler(runnerRepository))
                .runnerCollectionPostHandler(new RunnerCollectionPostHandler(runnerRepository))
                .runnerGetHandler(new RunnerGetHandler(runnerRepository))
                .runnerPatchHandler(new RunnerPatchHandler(runnerRepository))
                .build();
    }

    public PoomjobsAPIHandlers handlers() {
        return this.handlers;
    }
}
