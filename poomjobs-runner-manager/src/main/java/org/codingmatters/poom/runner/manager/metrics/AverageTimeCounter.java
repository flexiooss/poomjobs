package org.codingmatters.poom.runner.manager.metrics;

import com.codahale.metrics.Gauge;

import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;

public class AverageTimeCounter implements Gauge<Double> {

    private final List<Long> waitTimeSeconds;

    public AverageTimeCounter() {
        waitTimeSeconds = new LinkedList<>();
    }

    @Override
    public Double getValue() {
        OptionalDouble average = waitTimeSeconds.stream().mapToLong(Long::longValue).average();
        waitTimeSeconds.clear();
        return average.orElse(0);
    }

    public void newWaitTime(long waitTimeSec) {
        waitTimeSeconds.add(waitTimeSec);
    }
}
