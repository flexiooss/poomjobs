package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.JobValueChange;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.rest.protocol.ResourcePutProtocol;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourcePutRequest;
import org.codingmatters.poomjobs.api.JobResourcePutResponse;
import org.codingmatters.poomjobs.api.jobresourceputresponse.Status200;
import org.codingmatters.poomjobs.api.jobresourceputresponse.Status400;
import org.codingmatters.poomjobs.api.jobresourceputresponse.Status404;
import org.codingmatters.poomjobs.api.jobresourceputresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codingmatters.poomjobs.service.JobValueMerger.merge;

/**
 * Created by nelt on 6/15/17.
 */
public class JobResourcePutHandler implements ResourcePutProtocol<JobValue, JobQuery, JobResourcePutRequest, JobResourcePutResponse> {
    static private final Logger log = LoggerFactory.getLogger(JobResourcePutHandler.class);

    private final Repository<JobValue, JobQuery> repository;

    public JobResourcePutHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public String entityId(JobResourcePutRequest request) {
        return request.jobId();
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public Repository<JobValue, JobQuery> repository() {
        return repository;
    }

    @Override
    public Change<JobValue> valueUpdate(JobResourcePutRequest request, Entity<JobValue> entity) {
        JobValue currentValue = entity.value();
        JobValue newValue = merge(currentValue).with(request.payload());
        return JobValueChange.from(currentValue).to(newValue);
    }

    @Override
    public JobResourcePutResponse entityUpdated(Entity<JobValue> entity) {
        return JobResourcePutResponse.builder()
                .status200(Status200.builder()
                        .payload(JobEntityTransformation.transform(entity).asJob())
                        .build())
                .build();
    }

    @Override
    public JobResourcePutResponse invalidUpdate(Change<JobValue> change, String errorToken) {
        return JobResourcePutResponse.builder()
                .status400(Status400.builder()
                        .payload(Error.builder()
                                .code(Error.Code.ILLEGAL_JOB_CHANGE)
                                .description(change.validation().message())
                                .token(errorToken)
                                .build())
                        .build())
                .build();
    }

    @Override
    public JobResourcePutResponse entityNotFound(String errorToken) {
        return JobResourcePutResponse.builder()
                .status404(Status404.builder()
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.JOB_NOT_FOUND)
                                .description("no job found with the given jobId")
                                .build())
                        .build())
                .build();
    }

    @Override
    public JobResourcePutResponse unexpectedError(RepositoryException e, String errorToken) {
        return JobResourcePutResponse.builder()
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
