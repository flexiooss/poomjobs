package org.codingmatters.poom.runner.manager.metrics;

import com.codahale.metrics.Gauge;

import java.util.concurrent.atomic.AtomicLong;

public class MaxTimeCounter implements Gauge<Long> {

    private final AtomicLong waitTimeSeconds;

    public MaxTimeCounter() {
        waitTimeSeconds = new AtomicLong(0L);
    }

    @Override
    public Long getValue() {
        return waitTimeSeconds.getAndSet(0);
    }

    public void newTime(long waitTimeSec) {
        waitTimeSeconds.set(Math.max(waitTimeSec, waitTimeSeconds.get()));
    }
}
