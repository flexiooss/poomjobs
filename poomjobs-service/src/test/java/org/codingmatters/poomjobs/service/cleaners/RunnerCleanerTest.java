package org.codingmatters.poomjobs.service.cleaners;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
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

public class RunnerCleanerTest {

    private final Repository<RunnerValue, PropertyQuery> runners = InMemoryRepositoryWithPropertyQuery.validating(RunnerValue.class);
    private final RunnerCleaner cleaner = new RunnerCleaner(Executors.newSingleThreadScheduledExecutor(), 500, TimeUnit.MILLISECONDS, this.runners, 12, ChronoUnit.DAYS);

    @Test
    public void whenNoRunner__thenNoChange() throws Exception {
        this.cleaner.cleanup();

        assertThat(this.runners.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenOneRunner__whenRunnerIsRunning__thenNoChange() throws Exception {
        this.runners.create(RunnerValue.builder().runtime(run -> run.status(Runtime.Status.RUNNING).lastPing(UTC.now().minusDays(42))).build());

        this.cleaner.cleanup();

        assertThat(this.runners.all(0, 0).total(), is(1L));
    }

    @Test
    public void givenOneRunner__whenJobIsIdle__thenNoChange() throws Exception {
        this.runners.create(RunnerValue.builder().runtime(run -> run.status(Runtime.Status.IDLE).lastPing(UTC.now().minusDays(42))).build());

        this.cleaner.cleanup();

        assertThat(this.runners.all(0, 0).total(), is(1L));
    }

    @Test
    public void givenOneRunner__whenRunnerIsDisconnected_andLastPingAfterKeptDelay__thenNoChange() throws Exception {
        this.runners.create(RunnerValue.builder().runtime(run -> run.status(Runtime.Status.DISCONNECTED).lastPing(UTC.now().minusDays(10))).build());

        this.cleaner.cleanup();

        assertThat(this.runners.all(0, 0).total(), is(1L));
    }

    @Test
    public void givenOneRunner__whenRunnerIsDisconnected_andLastPingBeforeKeptDelay__thenCleanup() throws Exception {
        this.runners.create(RunnerValue.builder().runtime(run -> run.status(Runtime.Status.DISCONNECTED).lastPing(UTC.now().minusDays(42))).build());

        this.cleaner.cleanup();

        assertThat(this.runners.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenRunnerNeedsCleanup__whenScheduled__thenRunnerEventuallyGetsCleanup() throws Exception {
        this.runners.create(RunnerValue.builder().runtime(run -> run.status(Runtime.Status.DISCONNECTED).lastPing(UTC.now().minusDays(42))).build());
        try {
            this.cleaner.start();

            Eventually.timeout(1000).assertThat(() -> this.runners.all(0, 0).total(), is(0L));
        } finally {
            this.cleaner.stop();
        }
    }
}