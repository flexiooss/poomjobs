package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.runners.RunnerValueChange;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.rest.protocol.ResourcePutProtocol;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerPatchRequest;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.runnerpatchresponse.Status200;
import org.codingmatters.poomjobs.api.runnerpatchresponse.Status400;
import org.codingmatters.poomjobs.api.runnerpatchresponse.Status404;
import org.codingmatters.poomjobs.api.runnerpatchresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.RunnerEntityTransformation;
import org.codingmatters.poomjobs.service.RunnerValueMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class RunnerPatchHandler implements ResourcePutProtocol<RunnerValue, RunnerQuery, RunnerPatchRequest, RunnerPatchResponse> {

    static private final Logger log = LoggerFactory.getLogger(RunnerPatchHandler.class);

    private final Repository<RunnerValue, RunnerQuery> repository;

    public RunnerPatchHandler(Repository<RunnerValue, RunnerQuery> repository) {
        this.repository = repository;
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public Repository<RunnerValue, RunnerQuery> repository(RunnerPatchRequest request) {
        return this.repository;
    }

    @Override
    public String entityId(RunnerPatchRequest request) {
        return request.runnerId();
    }

    @Override
    public Change<RunnerValue> valueUpdate(RunnerPatchRequest request, Entity<RunnerValue> entity) {
        RunnerValue current = entity.value();
        RunnerValue newValue = RunnerValueMerger.merge(current).with(request.payload());
        newValue = newValue.withRuntime(newValue.runtime().withLastPing(LocalDateTime.now()));
        return RunnerValueChange.from(current).to(newValue);
    }

    @Override
    public RunnerPatchResponse entityUpdated(Entity<RunnerValue> entity) {
        return RunnerPatchResponse.builder()
                .status200(Status200.builder()
                        .payload(RunnerEntityTransformation.transform(entity).asRunner())
                        .build())
                .build();
    }

    @Override
    public RunnerPatchResponse invalidUpdate(Change<RunnerValue> change, String errorToken) {
        return RunnerPatchResponse.builder()
                .status400(Status400.builder()
                        .payload(Error.builder()
                                .code(Error.Code.ILLEGAL_RUNNER_STATUS_CHANGE)
                                .token(errorToken)
                                .description(change.validation().message())
                                .build())
                        .build())
                .build();
    }

    @Override
    public RunnerPatchResponse entityNotFound(String errorToken) {
        return RunnerPatchResponse.builder()
                .status404(Status404.builder()
                        .payload(Error.builder()
                                .code(Error.Code.RUNNER_NOT_FOUND)
                                .token(errorToken)
                                .description("no runner found with the given runner-id")
                                .build())
                        .build())
                .build();
    }

    @Override
    public RunnerPatchResponse unexpectedError(RepositoryException e, String errorToken) {
        return RunnerPatchResponse.builder()
                .status500(Status500.builder()
                        .payload(Error.builder()
                                .code(Error.Code.UNEXPECTED_ERROR)
                                .token(errorToken)
                                .description("unexpected error, see logs")
                                .build())
                        .build())
                .build();
    }
}
