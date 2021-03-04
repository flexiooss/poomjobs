package org.codingmatters.poom.jobs.runner.service.manager.status;

import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.services.tests.Eventually;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class RunnerStatusManagerTest {

    private final AtomicReference<RunnerStatus> nextStatus = new AtomicReference<>();
    private final List<RunnerStatus> notifiedStatuses = Collections.synchronizedList(new LinkedList<>());

    private final RunnerStatusManager manager = new RunnerStatusManager(
            status -> notifiedStatuses.add(status),
            () -> nextStatus.get(),
            Executors.newSingleThreadScheduledExecutor(),
            1000
    );

    @After
    public void tearDown() throws Exception {
        this.manager.stop();
    }

    @Test
    public void whenNoStatusChange__thenStatusNotifiedRegularly() throws Exception {
        this.nextStatus.set(RunnerStatus.BUSY);
        this.manager.start();

        Eventually.defaults().assertThat(() -> this.notifiedStatuses, contains(RunnerStatus.BUSY));
        Eventually.defaults().assertThat(() -> this.notifiedStatuses, contains(RunnerStatus.BUSY, RunnerStatus.BUSY));
        Eventually.defaults().assertThat(() -> this.notifiedStatuses, contains(RunnerStatus.BUSY, RunnerStatus.BUSY, RunnerStatus.BUSY));
    }

    @Test
    public void whenOnBusyTriggeredFromIDLE__thenStatusChangedNow_andStatusGatheredFromProvider() throws Exception {
        this.nextStatus.set(RunnerStatus.BUSY);
        this.manager.start();
        this.nextStatus.set(RunnerStatus.UNKNOWN);

        this.manager.onBusy(RunnerStatus.IDLE);

        assertThat(this.notifiedStatuses.size(), is(2));
        assertThat(this.notifiedStatuses.get(1), is(RunnerStatus.UNKNOWN));
    }

    @Test
    public void whenOnIdleTriggeredFromBusy__thenStatusChangedNow_andStatusGatheredFromProvider() throws Exception {
        this.nextStatus.set(RunnerStatus.BUSY);
        this.manager.start();
        this.nextStatus.set(RunnerStatus.UNKNOWN);

        this.manager.onIdle(RunnerStatus.BUSY);

        assertThat(this.notifiedStatuses.size(), is(2));
        assertThat(this.notifiedStatuses.get(1), is(RunnerStatus.UNKNOWN));
    }

    @Test
    public void whenOnIdleTriggeredFromIDLE__thenStatusNotChanged() throws Exception {
        this.nextStatus.set(RunnerStatus.BUSY);
        this.manager.start();

        this.manager.onIdle(RunnerStatus.IDLE);
        assertThat(this.notifiedStatuses.size(), is(1));
    }

    @Test
    public void whenOnBusyTriggeredFromBUSY__thenStatusNotChanged() throws Exception {
        this.nextStatus.set(RunnerStatus.BUSY);
        this.manager.start();

        this.manager.onBusy(RunnerStatus.BUSY);
        assertThat(this.notifiedStatuses.size(), is(1));
    }
}