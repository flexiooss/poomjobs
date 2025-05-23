package org.codingmatters.poom.runner;

import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;

@FunctionalInterface
public interface JobProcessor {
    Job process() throws JobProcessingException;

    default void shutDownProperly() {

    }

    interface Factory {
        JobProcessor createFor(Job job);
    }
}
