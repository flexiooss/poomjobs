package org.codingmatters.poom.poomjobs.domain.runners.repositories.inmemory;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerCriteria;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by nelt on 7/10/17.
 */
public class InMemoryRunnerRepository extends InMemoryRepository<RunnerValue, RunnerQuery> {
    @Override
    public PagedEntityList<RunnerValue> search(RunnerQuery query, long startIndex, long endIndex) throws RepositoryException {
        Stream<Entity<RunnerValue>> filtered = this.stream();
        for (RunnerCriteria criteria : query.criteria()) {
            filtered = this.applyCriterionFilter(filtered, criteria);
        }
        return this.paged(filtered, startIndex, endIndex);
    }

    private Stream<Entity<RunnerValue>> applyCriterionFilter(Stream<Entity<RunnerValue>> filtered, RunnerCriteria criteria) {
        if(criteria.categoryCompetency() != null) {
            filtered = filtered.filter(runnerValueEntity -> this.hasCategory(runnerValueEntity, criteria));
        }
        if(criteria.nameCompetency() != null) {
            filtered = filtered.filter(runnerValueEntity -> this.hasName(runnerValueEntity, criteria));
        }
        if(criteria.runtimeStatus() != null) {
            filtered = filtered.filter(runnerValueEntity -> this.hasStatus(runnerValueEntity, criteria));
        }
        return filtered;
    }

    private boolean hasCategory(Entity<RunnerValue> entity, RunnerCriteria criteria) {
        return
                entity.value().competencies() != null &&
                        entity.value().competencies().categories() != null &&
                        Arrays.stream(entity.value().competencies().categories().toArray(new String[0]))
                                .filter(category -> category.matches(criteria.categoryCompetency())).findFirst().isPresent();
    }

    private boolean hasName(Entity<RunnerValue> entity, RunnerCriteria criteria) {
        return
                entity.value().competencies() != null &&
                        entity.value().competencies().names() != null &&
                        Arrays.stream(entity.value().competencies().names().toArray(new String[0]))
                                .filter(name -> name.matches(criteria.nameCompetency())).findFirst().isPresent();
    }

    private boolean hasStatus(Entity<RunnerValue> entity, RunnerCriteria criteria) {
        return
                entity.value().runtime() != null &&
                        entity.value().runtime().status() != null &&
                        entity.value().runtime().status().name().matches(criteria.runtimeStatus());
    }
}
