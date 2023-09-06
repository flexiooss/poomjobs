package org.codingmatters.poomjobs.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.jobs.collections.jobs.JobRegistryHandlersBuilder;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.codingmatters.poomjobs.service.api.PoomjobsJobRegistryAPIProcessor;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.function.Function;

/**
 * Created by nelt on 6/15/17.
 */
public class PoomjobsJobRegistryAPI implements Api {
    public static final String VERSION = Api.versionFrom(PoomjobsJobRegistryAPI.class);

    private final PoomjobsJobRegistryAPIHandlers handlers;
    private final Processor processor;

    public PoomjobsJobRegistryAPI(Repository<JobValue, PropertyQuery> jobRepository, JsonFactory jsonFactory) {
        this(jobRepository, PoomjobsJobRepositoryListener.NOOP, null, jsonFactory);
    }

    public PoomjobsJobRegistryAPI(
            Repository<JobValue, PropertyQuery> jobRepository,
            PoomjobsJobRepositoryListener jobRepositoryListener,
            Function<JobCollectionPostRequest, ObjectValue> contextualizer,
            JsonFactory jsonFactory) {
        this.handlers = new JobRegistryHandlersBuilder(jobRepository, "", contextualizer, jobRepositoryListener).build();
        this.processor = new PoomjobsJobRegistryAPIProcessor(
                this.path(),
                jsonFactory,
                this.handlers
        );
    }

    public PoomjobsJobRegistryAPIHandlers handlers() {
        return this.handlers;
    }

    @Override
    public String name() {
        return "poomjobs-jobs";
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public Processor processor() {
        return this.processor;
    }

    @Override
    public String path() {
        return "/poomjobs-jobs/v1";
    }
}
