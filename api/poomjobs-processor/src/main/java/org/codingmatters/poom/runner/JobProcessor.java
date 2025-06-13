package org.codingmatters.poom.runner;

import org.codingmatters.poom.runner.exception.JobMonitorError;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

@FunctionalInterface
public interface JobProcessor {
    Job process() throws JobProcessingException;

    interface Factory {
        JobProcessor createFor(Job job, JobMonitor monitor);
    }

    interface JobMonitor {
        boolean isShutdownRequested();

        default void canContinue() throws JobMonitorError {
            if (isShutdownRequested()) {
                throw new JobMonitorError(Status.Exit.ABORTED);
            }
        }
    }
}
