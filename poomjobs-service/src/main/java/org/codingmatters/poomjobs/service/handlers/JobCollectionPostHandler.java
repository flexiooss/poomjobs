package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.jobcollectionpostresponse.Status201;

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
    public JobCollectionPostResponse apply(JobCollectionPostRequest request) {
        JobValue jobValue = JobValue.Builder.builder()
                .category(request.payload().category())
                .name(request.payload().name())
                .arguments(request.payload().arguments() != null ? request.payload().arguments().toArray(new String[request.payload().arguments().size()]) : null)
                .build();
        try {
            Entity<JobValue> entity = this.repository.create(jobValue);
            return JobCollectionPostResponse.Builder.builder()
                    .status201(Status201.Builder.builder()
                            .location("%API_PATH%/jobs/" + entity.id())
                            .build())
                    .build();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return null;
    }
}
