package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.JobValueCreation;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Accounting;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.LoggingContext;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.jobcollectionpostresponse.Status201;
import org.codingmatters.poomjobs.api.jobcollectionpostresponse.Status400;
import org.codingmatters.poomjobs.api.jobcollectionpostresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.JobValueMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Function;

/**
 * Created by nelt on 6/15/17.
 */
public class JobCollectionPostHandler implements Function<JobCollectionPostRequest, JobCollectionPostResponse> {
    static private final Logger log = LoggerFactory.getLogger(JobCollectionPostHandler.class);

    private final Repository<JobValue, JobQuery> repository;

    public JobCollectionPostHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public JobCollectionPostResponse apply(JobCollectionPostRequest request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            JobValue jobValue = JobValueMerger.create()
                    .with(request.payload())
                    .withAccounting(Accounting.builder()
                            .accountId(request.accountId())
                            .build());

            JobValueCreation creation = JobValueCreation.with(jobValue);
            if (creation.validation().isValid()) {
                return this.createJobAndReturnJobURI(creation);
            } else {
                return this.returnInvalidJobCreation(creation);
            }
        }
    }

    private JobCollectionPostResponse createJobAndReturnJobURI(JobValueCreation creation) {
        try {
            Entity<JobValue> entity = this.repository.create(creation.applied());
            MDC.put("job-id", entity.id());
            log.info("created entity {}", entity.id());

            return JobCollectionPostResponse.builder()
                    .status201(Status201.builder()
                            .location("%API_PATH%/jobs/" + entity.id())
                            .build())
                    .build();
        } catch (RepositoryException e) {
            return this.returnUnexpectedError(e);
        }
    }

    private JobCollectionPostResponse returnInvalidJobCreation(JobValueCreation creation) {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);
        log.info("job creation request with invalid job spec: {}", creation.validation().message());
        return JobCollectionPostResponse.builder()
                .status400(Status400.builder()
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.ILLEGAL_JOB_SPEC)
                                .description(creation.validation().message())
                                .build())
                        .build())
                .build();
    }

    private JobCollectionPostResponse returnUnexpectedError(RepositoryException e) {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);
        log.error("unexpected error while creating job", e);
        return JobCollectionPostResponse.builder()
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
