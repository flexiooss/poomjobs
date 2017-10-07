package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.PoomjobsRunnerRegistryAPIHandlers;
import org.codingmatters.poomjobs.service.handlers.RunnerCollectionGetHandler;
import org.codingmatters.poomjobs.service.handlers.RunnerCollectionPostHandler;
import org.codingmatters.poomjobs.service.handlers.RunnerGetHandler;
import org.codingmatters.poomjobs.service.handlers.RunnerPatchHandler;

public class PoomjobsRunnerRegistryAPI {
    private final PoomjobsRunnerRegistryAPIHandlers handlers;

    public PoomjobsRunnerRegistryAPI(
            Repository<RunnerValue, RunnerQuery> runnerRepository
    ) {
        this.handlers = new PoomjobsRunnerRegistryAPIHandlers.Builder()
                .runnerCollectionGetHandler(new RunnerCollectionGetHandler(runnerRepository))
                .runnerCollectionPostHandler(new RunnerCollectionPostHandler(runnerRepository))
                .runnerGetHandler(new RunnerGetHandler(runnerRepository))
                .runnerPatchHandler(new RunnerPatchHandler(runnerRepository))
                .build();
    }

    public PoomjobsRunnerRegistryAPIHandlers handlers() {
        return this.handlers;
    }
}
