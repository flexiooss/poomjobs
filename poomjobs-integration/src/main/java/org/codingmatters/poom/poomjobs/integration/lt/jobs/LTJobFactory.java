package org.codingmatters.poom.poomjobs.integration.lt.jobs;

import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobMonitorError;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.api.types.Job;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LTJobFactory implements JobProcessor.Factory {
    private static final String QUICK_DELAY_IN_S = "QUICK_DELAY_MS";
    private static final String SLOW_DELAY_IN_S = "SLOW_DELAY_MS";
    private static final String VERY_SLOW_DELAY_IN_S = "VERY_SLOW_DELAY_MS";

    public static final String CATEGORY = "LT";

    public enum JobName {
        QUICK, SLOW, VERY_SLOW
    }

    static private Random RAND = new Random();

    @Override
    public JobProcessor createFor(Job job, JobProcessor.JobMonitor monitor) {
        if(job.name().equals(JobName.QUICK.name())) {
            return this.quick(job);
        } else if(job.name().equals(JobName.SLOW.name())) {
            return this.slow(job);
        } else if(job.name().equals(JobName.VERY_SLOW.name())) {
            return this.verySlow(job);
        }
        return null;
    }

    private JobProcessor quick(Job job) {
        long delay = this.randomDelay(Env.optional(QUICK_DELAY_IN_S).orElse(new Env.Var("10")).asInteger());
        return new LTJobProcessor(job, delay);
    }

    private JobProcessor slow(Job job) {
        long delay = this.randomDelay(Env.optional(SLOW_DELAY_IN_S).orElse(new Env.Var("20")).asInteger());
        return new LTJobProcessor(job, delay);
    }

    private JobProcessor verySlow(Job job) {
        long delay = this.randomDelay(Env.optional(VERY_SLOW_DELAY_IN_S).orElse(new Env.Var("40")).asInteger());
        return new LTJobProcessor(job, delay);
    }

    private long randomDelay(int around) {
        long aroundMs = TimeUnit.MILLISECONDS.convert(around, TimeUnit.SECONDS);
        int demiMargin = Math.round(aroundMs / 10);
        return aroundMs - demiMargin + RAND.nextInt(2 * demiMargin);
    }
}
