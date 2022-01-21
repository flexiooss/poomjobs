package org.codingmatters.poomjobs.service.cleaners;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;

import java.time.temporal.TemporalUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class JobCleaner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobCleaner.class);

    private final ScheduledExecutorService scheduler;
    private final long every;
    private final TimeUnit everyUnit;
    private final Repository<JobValue, PropertyQuery> jobRepository;
    private final long kept;
    private final TemporalUnit keptUnit;
    private ScheduledFuture<?> task;

    public JobCleaner(ScheduledExecutorService scheduler, long every, TimeUnit everyUnit, Repository<JobValue, PropertyQuery> jobRepository, long kept, TemporalUnit keptUnit) {
        this.scheduler = scheduler;
        this.every = every;
        this.everyUnit = everyUnit;
        this.jobRepository = jobRepository;
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
                    .filter("status.run == 'DONE' && processing.submitted < %s", UTC.now().minus(this.kept, this.keptUnit))
                    .build();
            this.jobRepository.deleteFrom(query);
        } catch (RepositoryException e) {
            log.error("[GRAVE] error cleaning job repository", e);
        }
    }
}
