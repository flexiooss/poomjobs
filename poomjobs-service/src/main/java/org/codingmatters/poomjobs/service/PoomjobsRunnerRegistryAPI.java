package org.codingmatters.poomjobs.service;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.PoomjobsRunnerRegistryAPIHandlers;
import org.codingmatters.poomjobs.service.api.PoomjobsRunnerRegistryAPIProcessor;
import org.codingmatters.poomjobs.service.handlers.RunnerCollectionGetHandler;
import org.codingmatters.poomjobs.service.handlers.RunnerCollectionPostHandler;
import org.codingmatters.poomjobs.service.handlers.RunnerGetHandler;
import org.codingmatters.poomjobs.service.handlers.RunnerPatchHandler;
import org.codingmatters.rest.api.Api;
import org.codingmatters.rest.api.Processor;

public class PoomjobsRunnerRegistryAPI implements Api {
    public static final String VERSION = Api.versionFrom(PoomjobsRunnerRegistryAPI.class);

    private final PoomjobsRunnerRegistryAPIHandlers handlers;

    private final Processor processor;

    public PoomjobsRunnerRegistryAPI(
            Repository<RunnerValue, RunnerQuery> runnerRepository, JsonFactory jsonFactory
    ) {
        this.handlers = new PoomjobsRunnerRegistryAPIHandlers.Builder()
                .runnerCollectionGetHandler(new RunnerCollectionGetHandler(runnerRepository))
                .runnerCollectionPostHandler(new RunnerCollectionPostHandler(runnerRepository))
                .runnerGetHandler(new RunnerGetHandler(runnerRepository))
                .runnerPatchHandler(new RunnerPatchHandler(runnerRepository))
                .build();
        this.processor = new PoomjobsRunnerRegistryAPIProcessor(
                this.path(),
                jsonFactory,
                this.handlers
        );
    }

    public PoomjobsRunnerRegistryAPIHandlers handlers() {
        return this.handlers;
    }


    @Override
    public String name() {
        return "poomjobs-runners";
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
        return "/" + this.name() + "/v1";
    }
}
