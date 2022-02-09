package org.codingmatters.poom.patterns.pool;

import org.codingmatters.poom.services.tests.Eventually;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class FeederTest {

    Feeder<UUID> feeder = new Feeder<>();
    @Test
    public void whenJustCreated__thenIdle() throws Exception {
        assertThat(this.feeder.isIdle(), is(true));
    }

    @Test
    public void whenReserved__thenBusy() throws Exception {
        this.feeder.reserve();

        assertThat(feeder.isIdle(), is(false));
    }

    @Test
    public void givenReserved__whenReleased__thenIdle() throws Exception {
        Feeder.Handle<UUID> handle = this.feeder.reserve();

        handle.release();

        assertThat(feeder.isIdle(), is(true));
    }

    @Test
    public void givenReserved__whenFeeding__thenBusy_andMonitorExposesIn() throws Exception {
        Feeder.Handle<UUID> handle = this.feeder.reserve();

        UUID uuid = UUID.randomUUID();
        handle.feed(uuid);

        assertThat(feeder.isIdle(), is(false));
        assertThat(this.feeder.monitor().in(), is(uuid));
    }

    @Test
    public void givenReserved_thanFeed__whenMonitorIsDone__thenIdle() throws Exception {
        Feeder.Handle<UUID> handle = this.feeder.reserve();
        handle.feed(UUID.randomUUID());

        this.feeder.monitor().done();

        assertThat(feeder.isIdle(), is(true));
    }

    @Test
    public void givenListenerRegisterd_andReserved_thanFeed__whenMonitorIsDone__thenListenerCalled() throws Exception {
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        this.feeder.addListener(new Feeder.Listener() {
            @Override
            public void becameIdle() {
                listenerCalled.set(true);
            }

            @Override
            public void becameBusy() {}
        });

        Feeder.Handle<UUID> handle = this.feeder.reserve();
        handle.feed(UUID.randomUUID());

        this.feeder.monitor().done();

        assertThat(listenerCalled.get(), is(true));
    }

    @Test
    public void givenListenerRegisterd_andReserved__whenFeed__thenListenerCalled() throws Exception {
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        this.feeder.addListener(new Feeder.Listener() {
            @Override
            public void becameIdle() {}

            @Override
            public void becameBusy() {
                listenerCalled.set(true);
            }
        });

        Feeder.Handle<UUID> handle = this.feeder.reserve();
        handle.feed(UUID.randomUUID());

        assertThat(listenerCalled.get(), is(true));
    }

    @Test
    public void givenOtherThreadWaitingOnFeederMonitor__whenReserved_thenFeed__thenOtherThreadNotified() throws Exception {
        Feeder.Handle<UUID> handle = this.feeder.reserve();

        AtomicBoolean notified = new AtomicBoolean(false);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.submit(() -> {
            synchronized (this.feeder.monitor()) {
                try {
                    this.feeder.monitor().wait();
                    notified.set(true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread.sleep(500);

        handle.feed(UUID.randomUUID());

        Eventually.defaults().assertThat(() -> notified.get(), is(true));
    }
}