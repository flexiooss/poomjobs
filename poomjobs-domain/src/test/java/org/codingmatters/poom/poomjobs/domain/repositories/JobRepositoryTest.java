package org.codingmatters.poom.poomjobs.domain.repositories;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

/**
 * Created by nelt on 6/5/17.
 */
public class JobRepositoryTest {
    private Repository<JobValue, JobQuery> repository = new InMemoryRepository<JobValue, JobQuery>() {
        @Override
        public PagedEntityList<JobValue> search(JobQuery query, int page, int pageSize) throws RepositoryException {
            return null;
        }
    };
}