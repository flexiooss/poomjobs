package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.JobValueChange;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.logging.LoggingContext;
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
import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Function;

import static org.codingmatters.poomjobs.service.JobValueMerger.merge;

/**
 * Created by nelt on 6/15/17.
 */
public class JobResourcePutHandler implements Function<JobResourcePutRequest, JobResourcePutResponse> {
    static private final Logger log = LoggerFactory.getLogger(JobResourcePutHandler.class);

    private final Repository<JobValue, JobQuery> repository;

    public JobResourcePutHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public JobResourcePutResponse apply(JobResourcePutRequest request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            try {
                Entity<JobValue> entity = this.repository.retrieve(request.jobId());
                if(entity != null) {
                    return this.existingJob(request, entity);
                } else {
                    return this.unexistingJob(request);
                }
            } catch (RepositoryException e) {
                return this.unexpectedRepositoryError(request, e);
            }
        }
    }

    private JobResourcePutResponse existingJob(JobResourcePutRequest request, Entity<JobValue> entity) throws RepositoryException {
        MDC.put("job-id", entity.id());

        JobValue currentValue = entity.value();
        JobValue newValue = merge(currentValue).with(request.payload());
        JobValueChange change = JobValueChange.from(currentValue).to(newValue);

        if(change.validation().isValid()) {
            newValue = change.applied();

            entity = this.repository.update(entity, newValue);

            log.info("updated job");
            return JobResourcePutResponse.builder()
                    .status200(Status200.builder()
                            .payload(JobEntityTransformation.transform(entity).asJob())
                            .build())
                    .build();
        } else {
            String errorToken = UUID.randomUUID().toString();
            MDC.put("error-token", errorToken);
            log.info("illegal job change: {}", change.validation().message());
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
    }

    private JobResourcePutResponse unexistingJob(JobResourcePutRequest request) {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);
        log.info("no job found with id: {}", request.jobId());
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

    private JobResourcePutResponse unexpectedRepositoryError(JobResourcePutRequest request, RepositoryException e) {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);

        log.error("unexpected error while looking up job : {}", request.jobId());
        log.debug("unexpected exception", e);

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
