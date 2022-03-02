package org.codingmatters.poom.patterns.pool;

import org.codingmatters.poom.patterns.pool.exception.NotIdleException;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class FeederPoolTest {

    @Test
    public void whenJustCreated__thenIdle() throws Exception {
        assertThat(new FeederPool<UUID>(5).isIdle(), is(true));
    }

    @Test
    public void givenAllFeederReserved_whenNotFeed__thenPoolIsIdle_andNextReservationRaisesException() throws Exception {
        FeederPool<UUID> pool = new FeederPool<UUID>(5);

        for (int i = 1; i <= 4; i++) {
            pool.reserve();
            assertThat("reserved " + i + " feeder, pool is idle", pool.isIdle(), is(true));
        }

        pool.reserve();
        assertThat(pool.isIdle(), is(true));

        assertThrows(NotIdleException.class, () -> pool.reserve());
    }

    @Test
    public void givenAllFeedersReserved__whenOneMonitorGetsDone__thenPoolBecomesIdle() throws Exception {
        FeederPool<UUID> pool = new FeederPool<UUID>(5);
        for (int i = 0; i < 5; i++) {
            pool.reserve();
        }

        pool.monitors()[3].done();

        assertThat(pool.isIdle(), is(true));
        assertThat(pool.reserve(), is(notNullValue()));
    }
}