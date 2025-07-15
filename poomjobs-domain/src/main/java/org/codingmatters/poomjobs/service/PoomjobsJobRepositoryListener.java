package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.entities.Entity;

public interface PoomjobsJobRepositoryListener {
    void jobCreated(Entity<JobValue> entity);
    void jobUpdated(Entity<JobValue> entity, JobValue value);

    PoomjobsJobRepositoryListener NOOP = new PoomjobsJobRepositoryListener() {
        @Override
        public void jobCreated(Entity<JobValue> entity) {}

        @Override
        public void jobUpdated(Entity<JobValue> entity, JobValue oldValue) {}
    };
}
