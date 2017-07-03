package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.LoggingContext;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Function;

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
                int pageSize = this.maxPageSize;
                int page = 0;
                PagedEntityList<JobValue> list = this.repository.all(page, pageSize);

                Collection<Job> jobs = this.resultList(list);
                return JobCollectionGetResponse.builder()
                        .status200(Status200.builder()
                                .contentRange(String.format("Job %d-%d/%d", page * pageSize, page * pageSize + list.size() - 1, list.totalSize()))
                                .acceptRange(String.format("Job %d", pageSize))
                                .payload(jobs)
                                .build())
                        .build();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            return null;
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
