package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourceGetRequest;
import org.codingmatters.poomjobs.api.JobResourceGetResponse;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status200;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status404;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codingmatters.poomjobs.service.JobEntityTransformation.transform;

/**
 * Created by nelt on 6/15/17.
 */
public class JobResourceGetHandler implements ResourceGetProtocol<JobValue, JobQuery, JobResourceGetRequest, JobResourceGetResponse> {
    static private Logger log = LoggerFactory.getLogger(JobResourceGetHandler.class);

    private final Repository<JobValue, JobQuery> repository;

    public JobResourceGetHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public Repository<JobValue, JobQuery> repository() {
        return this.repository;
    }

    @Override
    public String entityId(JobResourceGetRequest request) {
        return request.jobId();
    }

    @Override
    public JobResourceGetResponse entityFound(Entity<JobValue> entity) {
        return JobResourceGetResponse.builder()
                .status200(Status200.builder()
                        .payload(transform(entity).asJob())
                        .build())
                .build();
    }

    @Override
    public JobResourceGetResponse entityNotFound(String errorToken) {
        return JobResourceGetResponse.builder()
                .status404(Status404.builder()
                        .payload(Error.builder()
                                .code(Error.Code.JOB_NOT_FOUND)
                                .description("no job found with the given jobId")
                                .token(errorToken)
                                .build())
                        .build())
                .build();
    }

    @Override
    public JobResourceGetResponse unexpectedError(String errorToken) {
        return JobResourceGetResponse.builder()
                .status500(Status500.builder()
                        .payload(Error.builder()
                                .code(Error.Code.UNEXPECTED_ERROR)
                                .description("unexpected error, see logs")
                                .token(errorToken)
                                .build())
                        .build())
                .build();
    }
}
