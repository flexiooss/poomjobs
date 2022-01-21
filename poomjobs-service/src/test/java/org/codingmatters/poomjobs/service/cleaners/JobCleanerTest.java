package org.codingmatters.poomjobs.service.cleaners;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.Eventually;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobCleanerTest {

    private final Repository<JobValue, PropertyQuery> jobs = InMemoryRepositoryWithPropertyQuery.validating(JobValue.class);
    private final JobCleaner cleaner = new JobCleaner(Executors.newSingleThreadScheduledExecutor(), 500, TimeUnit.MILLISECONDS, this.jobs, 12, ChronoUnit.DAYS);

    @Test
    public void whenNoJob__thenNoChange() throws Exception {
        this.cleaner.cleanup();

        assertThat(this.jobs.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenOneJob__whenJobIsRunning__thenNoChange() throws Exception {
        this.jobs.create(JobValue.builder().status(status -> status.run(Status.Run.RUNNING)).processing(p -> p.submitted(UTC.now().minusDays(42))).build());

        this.cleaner.cleanup();

        assertThat(this.jobs.all(0, 0).total(), is(1L));
    }

    @Test
    public void givenOneJob__whenJobIsPending__thenNoChange() throws Exception {
        this.jobs.create(JobValue.builder().status(status -> status.run(Status.Run.PENDING)).processing(p -> p.submitted(UTC.now().minusDays(42))).build());

        this.cleaner.cleanup();

        assertThat(this.jobs.all(0, 0).total(), is(1L));
    }

    @Test
    public void givenOneJob__whenJobIsDone_andJobSubmittedAfterKeptDelay__thenNoChange() throws Exception {
        this.jobs.create(JobValue.builder().status(status -> status.run(Status.Run.DONE)).processing(p -> p.submitted(UTC.now().minusDays(10))).build());

        this.cleaner.cleanup();

        assertThat(this.jobs.all(0, 0).total(), is(1L));
    }

    @Test
    public void givenOneJob__whenJobIsDone_andJobSubmittedBeforeKeptDelay__thenCleanup() throws Exception {
        this.jobs.create(JobValue.builder().status(status -> status.run(Status.Run.DONE)).processing(p -> p.submitted(UTC.now().minusDays(42))).build());

        this.cleaner.cleanup();

        assertThat(this.jobs.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenJobNeedsCleanup__whenScheduled__thenJobEventuallyGetsCleanup() throws Exception {
        this.jobs.create(JobValue.builder().status(status -> status.run(Status.Run.DONE)).processing(p -> p.submitted(UTC.now().minusDays(42))).build());
        try {
            this.cleaner.start();

            Eventually.timeout(1000).assertThat(() -> this.jobs.all(0, 0).total(), is(0L));
        } finally {
            this.cleaner.stop();
        }
    }
}