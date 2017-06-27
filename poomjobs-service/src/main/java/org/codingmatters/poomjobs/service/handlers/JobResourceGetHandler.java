package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.LoggingContext;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourceGetRequest;
import org.codingmatters.poomjobs.api.JobResourceGetResponse;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status200;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status404;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Function;

import static org.codingmatters.poomjobs.service.JobEntityTransformation.transform;

/**
 * Created by nelt on 6/15/17.
 */
public class JobResourceGetHandler implements Function<JobResourceGetRequest, JobResourceGetResponse> {
    static private Logger log = LoggerFactory.getLogger(JobResourceGetHandler.class);

    private final Repository<JobValue, JobQuery> repository;

    public JobResourceGetHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public JobResourceGetResponse apply(JobResourceGetRequest jobResourceGetRequest) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            try {
                Entity<JobValue> jobEntity = this.repository.retrieve(jobResourceGetRequest.jobId());
                if (jobEntity != null) {
                    return this.job(jobEntity);
                } else {
                    return this.jobNotFound(jobResourceGetRequest);
                }
            } catch (RepositoryException e) {
                return this.unexpectedError(jobResourceGetRequest, e);
            }
        }
    }

    private JobResourceGetResponse job(Entity<JobValue> jobEntity) {
        log.info("request for job {} returns version {}", jobEntity.id(), jobEntity.version());
        return JobResourceGetResponse.builder()
                .status200(Status200.builder()
                        .payload(transform(jobEntity).asJob())
                        .build())
                .build();
    }


    private JobResourceGetResponse jobNotFound(JobResourceGetRequest jobResourceGetRequest) {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);
        log.info("no job found with id: {}", jobResourceGetRequest.jobId());

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

    private JobResourceGetResponse unexpectedError(JobResourceGetRequest jobResourceGetRequest, RepositoryException e) {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);

        log.error("unexpected error while looking up job : {}", jobResourceGetRequest.jobId());
        log.debug("unexpected exception", e);

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
