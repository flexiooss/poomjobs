package org.codingmatters.poomjobs.service.handlers.mocks;

import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

/**
 * Created by nelt on 6/20/17.
 */
public class MockedJobRepository implements Repository<JobValue, JobQuery> {
    @Override
    public Entity<JobValue> create(JobValue withValue) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public Entity<JobValue> retrieve(String id) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public Entity<JobValue> update(Entity<JobValue> entity, JobValue withValue) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public void delete(Entity<JobValue> entity) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public PagedEntityList<JobValue> all(long startIndex, long endIndex) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public PagedEntityList<JobValue> search(JobQuery query, long startIndex, long endIndex) throws RepositoryException {
        throw new RepositoryException("");
    }
}
