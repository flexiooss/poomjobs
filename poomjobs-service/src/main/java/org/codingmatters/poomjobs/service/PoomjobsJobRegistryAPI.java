package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.codingmatters.poomjobs.service.handlers.JobCollectionGetHandler;
import org.codingmatters.poomjobs.service.handlers.JobCollectionPostHandler;
import org.codingmatters.poomjobs.service.handlers.JobResourceGetHandler;
import org.codingmatters.poomjobs.service.handlers.JobResourcePutHandler;

/**
 * Created by nelt on 6/15/17.
 */
public class PoomjobsJobRegistryAPI {
    private final PoomjobsJobRegistryAPIHandlers handlers;

    public PoomjobsJobRegistryAPI(
            Repository<JobValue, JobQuery> jobRepository
    ) {
        this.handlers = new PoomjobsJobRegistryAPIHandlers.Builder()
                .jobCollectionGetHandler(new JobCollectionGetHandler(jobRepository))
                .jobCollectionPostHandler(new JobCollectionPostHandler(jobRepository))
                .jobResourceGetHandler(new JobResourceGetHandler(jobRepository))
                .jobResourcePatchHandler(new JobResourcePutHandler(jobRepository))
                .build();
    }

    public PoomjobsJobRegistryAPIHandlers handlers() {
        return this.handlers;
    }
}
