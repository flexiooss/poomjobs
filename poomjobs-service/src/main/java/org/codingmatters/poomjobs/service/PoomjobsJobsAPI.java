package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.PoomjobsJobsAPIHandlers;
import org.codingmatters.poomjobs.service.handlers.JobCollectionGetHandler;
import org.codingmatters.poomjobs.service.handlers.JobCollectionPostHandler;
import org.codingmatters.poomjobs.service.handlers.JobResourceGetHandler;
import org.codingmatters.poomjobs.service.handlers.JobResourcePutHandler;

/**
 * Created by nelt on 6/15/17.
 */
public class PoomjobsJobsAPI {
    private final PoomjobsJobsAPIHandlers handlers;

    public PoomjobsJobsAPI(
            Repository<JobValue, JobQuery> jobRepository
    ) {
        this.handlers = new PoomjobsJobsAPIHandlers.Builder()
                .jobCollectionGetHandler(new JobCollectionGetHandler(jobRepository))
                .jobCollectionPostHandler(new JobCollectionPostHandler(jobRepository))
                .jobResourceGetHandler(new JobResourceGetHandler(jobRepository))
                .jobResourcePatchHandler(new JobResourcePutHandler(jobRepository))
                .build();
    }

    public PoomjobsJobsAPIHandlers handlers() {
        return this.handlers;
    }
}
