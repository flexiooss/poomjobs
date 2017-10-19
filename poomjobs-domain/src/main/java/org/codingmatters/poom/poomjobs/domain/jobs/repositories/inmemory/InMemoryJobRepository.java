package org.codingmatters.poom.poomjobs.domain.jobs.repositories.inmemory;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.stream.Stream;

/**
 * Created by nelt on 6/6/17.
 */
public class InMemoryJobRepository extends InMemoryRepository<JobValue, JobQuery> {
    @Override
    public PagedEntityList<JobValue> search(JobQuery query, long startIndex, long endIndex) throws RepositoryException {
        Stream<Entity<JobValue>> filtered = this.stream();
        for (JobCriteria jobCriteria : query.criteria()) {
            filtered = this.applyCriterionFilter(filtered, jobCriteria);
        }
        return this.paged(filtered, startIndex, endIndex);
    }

    private Stream<Entity<JobValue>> applyCriterionFilter(Stream<Entity<JobValue>> filtered, JobCriteria jobCriteria) {
        if (jobCriteria.opt().category().isPresent()) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().opt().category().filter(s -> s.matches(jobCriteria.category())).isPresent()
            );
        }
        if (jobCriteria.opt().name().isPresent()) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().opt().name().filter(s -> s.matches(jobCriteria.name())).isPresent()
            );
        }
        if (jobCriteria.opt().runStatus().isPresent()) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().opt().status().run().filter(run -> run.name().matches(jobCriteria.runStatus())).isPresent()
            );
        }
        if (jobCriteria.opt().exitStatus().isPresent()) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().opt().status().exit().filter(exit -> exit.name().matches(jobCriteria.exitStatus())).isPresent()
        );
        }
        return filtered;
    }
}
