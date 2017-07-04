package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status206;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status416;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nelt on 6/29/17.
 */
public class JobCollectionGetHandler implements Function<JobCollectionGetRequest, JobCollectionGetResponse> {
    private static final int DEFAULT_MAX_PAGE_SIZE = 100;

    private final Repository<JobValue, JobQuery> repository;
    private final int maxPageSize = DEFAULT_MAX_PAGE_SIZE;

    public JobCollectionGetHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public JobCollectionGetResponse apply(JobCollectionGetRequest request) {
        try(LoggingContext ctx = LoggingContext.start()) {
            MDC.put("request-id", UUID.randomUUID().toString());
            try {
                long startIndex = 0;
                long endIndex = startIndex + this.maxPageSize - 1;

                if(request.range() != null) {
                    Pattern RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
                    Matcher rangeMatcher = RANGE_PATTERN.matcher(request.range());
                    if(rangeMatcher.matches()) {
                        startIndex = Long.parseLong(rangeMatcher.group(1));
                        endIndex = Long.parseLong(rangeMatcher.group(2));
                    }
                }

                if(startIndex > endIndex) {
                    String errorToken = UUID.randomUUID().toString();
                    MDC.put("error-token", errorToken);
                    return JobCollectionGetResponse.builder()
                            .status416(Status416.builder()
                                    .contentRange(String.format("Job */%d",
                                            this.repository.all(0, 0).total()
                                    ))
                                    .acceptRange(String.format("Job %d", this.maxPageSize))
                                    .payload(Error.builder()
                                            .token(errorToken)
                                            .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                            .description("start must be before end of range")
                                            .build())
                                    .build())
                            .build();
                }

                PagedEntityList<JobValue> list = this.repository.all(startIndex, endIndex);

                Collection<Job> jobs = this.resultList(list);
                long resultRangeStart = list.startIndex();
                long resultRangeEnd = list.endIndex();

                if(list.size() < list.total()) {
                    return JobCollectionGetResponse.builder()
                            .status206(Status206.builder()
                                    .contentRange(String.format("Job %d-%d/%d",
                                            resultRangeStart,
                                            resultRangeEnd,
                                            list.total()
                                    ))
                                    .acceptRange(String.format("Job %d", this.maxPageSize))
                                    .payload(jobs)
                                    .build())
                            .build();
                } else {
                    return JobCollectionGetResponse.builder()
                            .status200(Status200.builder()
                                    .contentRange(String.format("Job %d-%d/%d",
                                            resultRangeStart,
                                            resultRangeEnd,
                                            list.total()
                                    ))
                                    .acceptRange(String.format("Job %d", this.maxPageSize))
                                    .payload(jobs)
                                    .build())
                            .build();
                }
            } catch (RepositoryException e) {
                String errorToken = UUID.randomUUID().toString();
                MDC.put("error-token", errorToken);
                return JobCollectionGetResponse.builder()
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
    }

    private Collection<Job> resultList(PagedEntityList<JobValue> list) {
        Collection<Job> result = new LinkedList<>();
        for (Entity<JobValue> jobValueEntity : list) {
            result.add(JobEntityTransformation.transform(jobValueEntity).asJob());
        }

        return result;
    }
}
