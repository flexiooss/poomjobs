package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by nelt on 6/29/17.
 */
public class JobCollectionGetHandler implements Function<JobCollectionGetRequest, JobCollectionGetResponse> {
    static private final Logger log = LoggerFactory.getLogger(JobCollectionGetHandler.class);

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

                Rfc7233Pager<JobValue, JobQuery> pager = Rfc7233Pager.forRequestedRange(request.range())
                        .unit("Job")
                        .maxPageSize(this.maxPageSize)
                        .pager(this.repository);


                Rfc7233Pager.Page page;
                if(request.name() != null || request.category() != null || request.runStatus() != null || request.exitStatus() != null) {
                    List<JobCriteria> criteria = new LinkedList<>();
                    if(request.name() != null) {
                        criteria.add(JobCriteria.builder().name(request.name()).build());
                    }
                    if(request.category() != null) {
                        criteria.add(JobCriteria.builder().category(request.category()).build());
                    }
                    if(request.runStatus() != null) {
                        criteria.add(JobCriteria.builder().runStatus(request.runStatus()).build());
                    }
                    if(request.exitStatus() != null) {
                        criteria.add(JobCriteria.builder().exitStatus(request.exitStatus()).build());
                    }
                    JobQuery query = JobQuery.builder().criteria(criteria).build();
                    log.info("job list requested with filter : {}", query);
                    page = pager.page(query);
                } else {
                    page = pager.page();
                }

                if(page.isValid()) {
                    if (page.isPartial()) {
                        return this.partialJobList(page);
                    } else {
                        return this.completeList(page);
                    }
                } else {
                    return this.invalidRangeQuery(page);
                }
            } catch (RepositoryException e) {
                return this.unexpectedError(e);
            }
        }
    }

    private JobCollectionGetResponse partialJobList(Rfc7233Pager.Page page) {
        Collection<Job> jobs = this.resultList(page.list());
        log.info("returning partial job list ({})", page.contentRange());
        return JobCollectionGetResponse.builder()
                .status206(Status206.builder()
                        .contentRange(page.contentRange())
                        .acceptRange(page.acceptRange())
                        .payload(jobs)
                        .build())
                .build();
    }

    private JobCollectionGetResponse completeList(Rfc7233Pager.Page page) {
        Collection<Job> jobs = this.resultList(page.list());
        log.info("returning complete job list ({} elements)", jobs.size());
        return JobCollectionGetResponse.builder()
                .status200(Status200.builder()
                        .contentRange(page.contentRange())
                        .acceptRange(page.acceptRange())
                        .payload(jobs)
                        .build())
                .build();
    }

    private JobCollectionGetResponse invalidRangeQuery(Rfc7233Pager.Page page) throws RepositoryException {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);

        log.info(page.validationMessage() + " (requested range: {})", page.requestedRange());
        return JobCollectionGetResponse.builder()
                .status416(Status416.builder()
                        .contentRange(page.contentRange())
                        .acceptRange(page.acceptRange())
                        .payload(Error.builder()
                                .token(errorToken)
                                .code(Error.Code.ILLEGAL_RANGE_SPEC)
                                .description(page.validationMessage())
                                .build())
                        .build())
                .build();
    }

    private Collection<Job> resultList(PagedEntityList<JobValue> list) {
        Collection<Job> result = new LinkedList<>();
        for (Entity<JobValue> jobValueEntity : list) {
            result.add(JobEntityTransformation.transform(jobValueEntity).asJob());
        }

        return result;
    }

    private JobCollectionGetResponse unexpectedError(RepositoryException e) {
        String errorToken = UUID.randomUUID().toString();
        MDC.put("error-token", errorToken);
        log.error("unexpected error while handling job list query", e);

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
