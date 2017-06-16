package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobResourcePutRequest;
import org.codingmatters.poomjobs.api.JobResourcePutResponse;

import java.util.function.Function;

/**
 * Created by nelt on 6/15/17.
 */
public class JobResourcePutHandler implements Function<JobResourcePutRequest, JobResourcePutResponse> {
    private final Repository<JobValue, JobQuery> repository;

    public JobResourcePutHandler(Repository<JobValue, JobQuery> repository) {
        this.repository = repository;
    }

    @Override
    public JobResourcePutResponse apply(JobResourcePutRequest jobResourcePutRequest) {
        return null;
    }
}
