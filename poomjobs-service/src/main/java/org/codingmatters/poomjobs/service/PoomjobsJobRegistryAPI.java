package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.jobs.collections.jobs.JobRegistryHandlersBuilder;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

/**
 * Created by nelt on 6/15/17.
 */
public class PoomjobsJobRegistryAPI {
    private final PoomjobsJobRegistryAPIHandlers handlers;

    public PoomjobsJobRegistryAPI(
            Repository<JobValue, PropertyQuery> jobRepository) {
        this(jobRepository, PoomjobsJobRepositoryListener.NOOP, null);
    }
    public PoomjobsJobRegistryAPI(
            Repository<JobValue, PropertyQuery> jobRepository,
            PoomjobsJobRepositoryListener jobRepositoryListener,
            Function<JobCollectionPostRequest, ObjectValue> contextualizer) {
        this.handlers = new JobRegistryHandlersBuilder(jobRepository, "", contextualizer, jobRepositoryListener).build();
    }

    public PoomjobsJobRegistryAPIHandlers handlers() {
        return this.handlers;
    }
}
