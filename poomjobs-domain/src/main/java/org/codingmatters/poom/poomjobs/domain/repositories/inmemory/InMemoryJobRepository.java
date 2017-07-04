package org.codingmatters.poom.poomjobs.domain.repositories.inmemory;

import org.codingmatters.poom.poomjobs.domain.values.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
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
        if (jobCriteria.category() != null) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().category() != null && jobValueEntity.value().category().matches(jobCriteria.category())
            );
        }
        if (jobCriteria.name() != null) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().name() != null && jobValueEntity.value().name().matches(jobCriteria.name())
            );
        }
        if (jobCriteria.runStatus() != null) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().status() != null
                            && jobValueEntity.value().status().run() != null
                            && jobValueEntity.value().status().run().name().matches(jobCriteria.runStatus())
            );
        }
        if (jobCriteria.exitStatus() != null) {
            filtered = filtered.filter(jobValueEntity ->
                    jobValueEntity.value().status() != null
                            && jobValueEntity.value().status().exit() != null
                            && jobValueEntity.value().status().exit().name().matches(jobCriteria.exitStatus())
            );
        }
        return filtered;
    }
}
