package org.codingmatters.poomjobs.service.cleaners;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;

import java.time.temporal.TemporalUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RunnerCleaner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerCleaner.class);

    private final ScheduledExecutorService scheduler;
    private final long every;
    private final TimeUnit everyUnit;
    private final Repository<RunnerValue, PropertyQuery> runnerRepository;
    private final long kept;
    private final TemporalUnit keptUnit;
    private ScheduledFuture<?> task;

    public RunnerCleaner(ScheduledExecutorService scheduler, long every, TimeUnit everyUnit, Repository<RunnerValue, PropertyQuery> runnerRepository, long kept, TemporalUnit keptUnit) {
        this.scheduler = scheduler;
        this.every = every;
        this.everyUnit = everyUnit;
        this.runnerRepository = runnerRepository;
        this.kept = kept;
        this.keptUnit = keptUnit;
    }

    public void start() {
        this.task = this.scheduler.scheduleWithFixedDelay(this::cleanup, this.every / 2, this.every, this.everyUnit);
    }
    public void stop() {
        this.task.cancel(true);
    }

    public void cleanup() {
        try {
            PropertyQuery query = PropertyQuery.builder()
                    .filter("runtime.status == 'DISCONNECTED' && runtime.lastPing < %s", UTC.now().minus(this.kept, this.keptUnit))
                    .build();
            this.runnerRepository.deleteFrom(query);
        } catch (RepositoryException e) {
            log.error("[GRAVE] error cleaning runner repository", e);
        }
    }
}
