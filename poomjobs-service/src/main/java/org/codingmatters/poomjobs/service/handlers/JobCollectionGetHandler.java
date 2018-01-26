package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.rest.protocol.CollectionGetProtocol;
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nelt on 6/29/17.
 */
public class JobCollectionGetHandler implements CollectionGetProtocol<JobValue, JobQuery, JobCollectionGetRequest, JobCollectionGetResponse> {
    static private final Logger log = LoggerFactory.getLogger(JobCollectionGetHandler.class);

    private static final int DEFAULT_MAX_PAGE_SIZE = 100;

    private final Repository<JobValue, JobQuery> repository;
    private final int maxPageSize = DEFAULT_MAX_PAGE_SIZE;

    public JobCollectionGetHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    private Collection<Job> resultList(PagedEntityList<JobValue> list) {
        Collection<Job> result = new LinkedList<>();
        for (Entity<JobValue> jobValueEntity : list) {
            result.add(JobEntityTransformation.transform(jobValueEntity).asJob());
        }

        return result;
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public Repository<JobValue, JobQuery> repository(JobCollectionGetRequest request) {
        return this.repository;
    }

    @Override
    public int maxPageSize() {
        return this.maxPageSize;
    }

    @Override
    public String rfc7233Unit() {
        return "Job";
    }

    @Override
    public String rfc7233Range(JobCollectionGetRequest request) {
        return request.range();
    }

    @Override
    public JobQuery parseQuery(JobCollectionGetRequest request) {
        JobQuery query = null;
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
            query = JobQuery.builder().criteria(criteria).build();
        }
        return query;
    }

    @Override
    public JobCollectionGetResponse partialList(Rfc7233Pager.Page page, JobCollectionGetRequest request) {
        Collection<Job> jobs = this.resultList(page.list());
        return JobCollectionGetResponse.builder()
                .status206(Status206.builder()
                        .contentRange(page.contentRange())
                        .acceptRange(page.acceptRange())
                        .payload(jobs)
                        .build())
                .build();
    }

    @Override
    public JobCollectionGetResponse completeList(Rfc7233Pager.Page page, JobCollectionGetRequest request) {
        Collection<Job> jobs = this.resultList(page.list());
        return JobCollectionGetResponse.builder()
                .status200(Status200.builder()
                        .contentRange(page.contentRange())
                        .acceptRange(page.acceptRange())
                        .payload(jobs)
                        .build())
                .build();
    }

    @Override
    public JobCollectionGetResponse invalidRangeQuery(Rfc7233Pager.Page page, String errorToken, JobCollectionGetRequest request) {
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

    @Override
    public JobCollectionGetResponse unexpectedError(RepositoryException e, String errorToken) {
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
