package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourceGetRequest;
import org.codingmatters.poomjobs.api.JobResourceGetResponse;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status200;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status404;
import org.codingmatters.poomjobs.api.jobresourcegetresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Function;

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
        try {
            Entity<JobValue> jobEntity = this.repository.retrieve(jobResourceGetRequest.jobId());
            if(jobEntity == null) {
                String errorToken = UUID.randomUUID().toString();
                log.info("[token={}] job not found : {}", errorToken, jobResourceGetRequest.jobId());

                return JobResourceGetResponse.Builder.builder()
                        .status404(Status404.Builder.builder()
                                .payload(Error.Builder.builder()
                                        .code(Error.Code.JOB_NOT_FOUND)
                                        .description("no job found with the given jobId")
                                        .token(errorToken)
                                        .build())
                                .build())
                        .build();
            }
            log.info("request for job {} returns version {}", jobEntity.id(), jobEntity.version());
            return JobResourceGetResponse.Builder.builder()
                    .status200(Status200.Builder.builder()
                            .payload(JobEntityTransformation.transform(jobEntity).asJob())
                            .build())
                    .build();
        } catch (RepositoryException e) {
            String errorToken = UUID.randomUUID().toString();
            log.error("[token={}] unexpected error while looking up job : {}", errorToken, jobResourceGetRequest.jobId());
            log.trace("[token=" + errorToken +"] unexpected exception", e);

            return JobResourceGetResponse.Builder.builder()
                    .status500(Status500.Builder.builder()
                            .payload(Error.Builder.builder()
                                    .code(Error.Code.UNEXPECTED_ERROR)
                                    .description("unexpected error, see logs")
                                    .token(errorToken)
                                    .build())
                            .build())
                    .build();
        }
    }
}
