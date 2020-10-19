package org.codingmatters.poom.runner;

import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poomjobs.api.types.Job;



@FunctionalInterface
public interface JobContextSetup {
    void setup(Job job, JobProcessor processor);

    JobContextSetup NOOP = new JobContextSetup() {
        @Override
        public void setup(Job job, JobProcessor processor) {}
    };
}
