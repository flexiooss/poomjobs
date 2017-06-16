package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;

import java.util.function.Function;

/**
 * Created by nelt on 6/15/17.
 */
public class JobCollectionPostHandler implements Function<JobCollectionPostRequest, JobCollectionPostResponse> {
    private final Repository<JobValue, JobQuery> repository;

    public JobCollectionPostHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public JobCollectionPostResponse apply(JobCollectionPostRequest jobCollectionPostRequest) {
        return null;
    }
}
