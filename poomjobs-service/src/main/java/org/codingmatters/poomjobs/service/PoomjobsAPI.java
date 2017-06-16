package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.service.api.PoomjobsAPIHandlers;
import org.codingmatters.poomjobs.service.handlers.JobCollectionPostHandler;
import org.codingmatters.poomjobs.service.handlers.JobResourceGetHandler;
import org.codingmatters.poomjobs.service.handlers.JobResourcePutHandler;

/**
 * Created by nelt on 6/15/17.
 */
public class PoomjobsAPI {
    private final PoomjobsAPIHandlers handlers;

    public PoomjobsAPI(Repository<JobValue, JobQuery> repository) {
        this.handlers = new PoomjobsAPIHandlers.Builder()
                .jobCollectionPostHandler(new JobCollectionPostHandler(repository))
                .jobResourceGetHandler(new JobResourceGetHandler(repository))
                .jobResourcePutHandler(new JobResourcePutHandler(repository))
                .build();
    }

    public PoomjobsAPIHandlers handlers() {
        return this.handlers;
    }
}
