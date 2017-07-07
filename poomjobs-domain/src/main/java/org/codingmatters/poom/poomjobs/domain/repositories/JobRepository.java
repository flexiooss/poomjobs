package org.codingmatters.poom.poomjobs.domain.repositories;

import org.codingmatters.poom.poomjobs.domain.repositories.inmemory.InMemoryJobRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;

/**
 * Created by nelt on 6/6/17.
 */
public class JobRepository {
    static public Repository<JobValue, JobQuery> createInMemory() {
        return new InMemoryJobRepository();
    }

}
