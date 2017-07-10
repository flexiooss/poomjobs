package org.codingmatters.poom.poomjobs.domain.runners.repositories;

import org.codingmatters.poom.poomjobs.domain.runners.repositories.inmemory.InMemoryRunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;

/**
 * Created by nelt on 7/10/17.
 */
public class RunnerRepository {
    static public Repository<RunnerValue, RunnerQuery> createInMemory() {
        return new InMemoryRunnerRepository();
    }
}
