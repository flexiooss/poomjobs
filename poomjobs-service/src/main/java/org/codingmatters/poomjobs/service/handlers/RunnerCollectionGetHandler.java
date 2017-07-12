package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.RunnerCollectionGetRequest;
import org.codingmatters.poomjobs.api.RunnerCollectionGetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Created by nelt on 7/12/17.
 */
public class RunnerCollectionGetHandler implements Function<RunnerCollectionGetRequest, RunnerCollectionGetResponse> {
    static private final Logger log = LoggerFactory.getLogger(RunnerCollectionGetHandler.class);

    private final Repository<RunnerValue, RunnerQuery> runnerRepository;

    public RunnerCollectionGetHandler(Repository<RunnerValue, RunnerQuery> runnerRepository) {
        this.runnerRepository = runnerRepository;
    }

    @Override
    public RunnerCollectionGetResponse apply(RunnerCollectionGetRequest runnerCollectionGetRequest) {
        return null;
    }
}
