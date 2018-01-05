package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.runners.RunnerValueCreation;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.rest.protocol.CollectionPostProtocol;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerCollectionPostRequest;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.codingmatters.poomjobs.api.runnercollectionpostresponse.Status201;
import org.codingmatters.poomjobs.api.runnercollectionpostresponse.Status400;
import org.codingmatters.poomjobs.api.runnercollectionpostresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.RunnerValueMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnerCollectionPostHandler implements CollectionPostProtocol<RunnerValue, RunnerQuery, RunnerCollectionPostRequest, RunnerCollectionPostResponse> {
    static private final Logger log = LoggerFactory.getLogger(RunnerCollectionPostHandler.class);

    private final Repository<RunnerValue, RunnerQuery> repository;

    public RunnerCollectionPostHandler(Repository<RunnerValue, RunnerQuery> repository) {
        this.repository = repository;
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public Repository<RunnerValue, RunnerQuery> repository(RunnerCollectionPostRequest request) {
        return this.repository;
    }

    @Override
    public Change<RunnerValue> valueCreation(RunnerCollectionPostRequest request) {
        return RunnerValueCreation.with(RunnerValueMerger.create().with(request.payload()));
    }

    @Override
    public RunnerCollectionPostResponse entityCreated(RunnerCollectionPostRequest request, Change<RunnerValue> creation, Entity<RunnerValue> entity) {
        return RunnerCollectionPostResponse.builder()
                .status201(Status201.builder()
                        .location("%API_PATH%/runners/" + entity.id())
                        .build())
                .build();
    }

    @Override
    public RunnerCollectionPostResponse invalidCreation(Change<RunnerValue> creation, String errorToken) {
        return RunnerCollectionPostResponse.builder()
                .status400(Status400.builder()
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.ILLEGAL_RUNNER_SPEC)
                                .description(creation.validation().message())
                                .build())
                        .build())
                .build();
    }

    @Override
    public RunnerCollectionPostResponse unexpectedError(Change<RunnerValue> creation, RepositoryException e, String errorToken) {
        return RunnerCollectionPostResponse.builder()
                .status500(Status500.builder()
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.UNEXPECTED_ERROR)
                                .description("unexpected error, see logs")
                                .build())
                        .build())
                .build();
    }
}
