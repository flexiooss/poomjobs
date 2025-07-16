package org.codingmatters.poom.runner.manager;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;

public class ChainedListener implements PoomjobsJobRepositoryListener {

    private final PoomjobsJobRepositoryListener[] listeners;

    public ChainedListener(PoomjobsJobRepositoryListener... listeners) {
        this.listeners = listeners;
    }

    @Override
    public void jobCreated(Entity<JobValue> entity) {
        for (PoomjobsJobRepositoryListener listener : listeners) {
            listener.jobCreated(entity);
        }
    }

    @Override
    public void jobUpdated(Entity<JobValue> entity, JobValue oldValue) {
        for (PoomjobsJobRepositoryListener listener : listeners) {
            listener.jobUpdated(entity, oldValue);
        }
    }

}
