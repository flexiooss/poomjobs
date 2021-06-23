package org.codingmatters.poom.runner.exception;

import org.codingmatters.poomjobs.api.types.Job;

@FunctionalInterface
public interface JobProcessor {
    Job process() throws JobProcessingException;

    interface Factory {
        JobProcessor createFor(Job job);
    }
}
