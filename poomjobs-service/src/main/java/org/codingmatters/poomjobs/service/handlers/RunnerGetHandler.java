package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerGetRequest;
import org.codingmatters.poomjobs.api.RunnerGetResponse;
import org.codingmatters.poomjobs.api.runnergetresponse.Status200;
import org.codingmatters.poomjobs.api.runnergetresponse.Status404;
import org.codingmatters.poomjobs.api.runnergetresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.RunnerEntityTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nelt on 7/18/17.
 */
public class RunnerGetHandler implements ResourceGetProtocol<RunnerValue, RunnerQuery, RunnerGetRequest, RunnerGetResponse> {
    static private Logger log = LoggerFactory.getLogger(RunnerGetHandler.class);

    private final Repository<RunnerValue, RunnerQuery> repository;

    public RunnerGetHandler(Repository<RunnerValue, RunnerQuery> repository) {
        this.repository = repository;
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public Repository<RunnerValue, RunnerQuery> repository() {
        return this.repository;
    }

    @Override
    public String entityId(RunnerGetRequest request) {
        return request.runnerId();
    }

    @Override
    public RunnerGetResponse entityFound(Entity<RunnerValue> entity) {
        return RunnerGetResponse.builder()
                .status200(Status200.builder()
                        .payload(RunnerEntityTransformation.transform(entity).asRunner())
                        .build())
                .build();
    }

    @Override
    public RunnerGetResponse entityNotFound(String errorToken) {
        return RunnerGetResponse.builder()
                .status404(Status404.builder()
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.RUNNER_NOT_FOUND)
                                .description("no runner found with the given runner id")
                                .build())
                        .build())
                .build();
    }

    @Override
    public RunnerGetResponse unexpectedError(String errorToken) {
        return RunnerGetResponse.builder()
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
