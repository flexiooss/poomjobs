package org.codingmatters.poomjobs.service.handlers.mocks;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

/**
 * Created by nelt on 7/11/17.
 */
public class MockedRunnerRepository implements Repository<RunnerValue, RunnerQuery> {
    @Override
    public Entity<RunnerValue> create(RunnerValue withValue) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public Entity<RunnerValue> retrieve(String id) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public Entity<RunnerValue> update(Entity<RunnerValue> entity, RunnerValue withValue) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public void delete(Entity<RunnerValue> entity) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public PagedEntityList<RunnerValue> all(long startIndex, long endIndex) throws RepositoryException {
        throw new RepositoryException("");
    }

    @Override
    public PagedEntityList<RunnerValue> search(RunnerQuery query, long startIndex, long endIndex) throws RepositoryException {
        throw new RepositoryException("");
    }
}
