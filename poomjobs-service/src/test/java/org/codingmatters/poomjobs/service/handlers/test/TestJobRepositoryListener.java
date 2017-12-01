package org.codingmatters.poomjobs.service.handlers.test;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.junit.rules.ExternalResource;

import java.util.concurrent.atomic.AtomicReference;

public class TestJobRepositoryListener extends ExternalResource implements PoomjobsJobRepositoryListener {

    private final AtomicReference<Entity<JobValue>> justCreated = new AtomicReference<>();
    private final AtomicReference<Entity<JobValue>> justUpdated = new AtomicReference<>();

    public Entity<JobValue> created() {
        return this.justCreated.get();
    }

    public Entity<JobValue> updated() {
        return this.justUpdated.get();
    }

    @Override
    public void jobCreated(Entity<JobValue> entity) {
        this.justCreated.set(entity);
    }

    @Override
    public void jobUpdated(Entity<JobValue> entity) {
        this.justUpdated.set(entity);

    }

    @Override
    protected void before() throws Throwable {
        this.justCreated.set(null);
        this.justUpdated.set(null);
    }

    @Override
    protected void after() {
        this.justCreated.set(null);
        this.justUpdated.set(null);
    }
}
