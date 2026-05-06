package org.codingmatters.poom.runner;

import org.codingmatters.poom.runner.exception.FailedJobTerminationException;
import org.codingmatters.poom.runner.exception.JobMonitorError;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.io.IOException;

public interface JobProcessor {

    Job process() throws JobProcessingException;

    void terminateFailedJob(Job job) throws FailedJobTerminationException;

    interface Factory {
        JobProcessor createFor(Job job, JobMonitor monitor);
    }

    interface JobMonitor {

        void doNotRestartThisJobAtThisPoint() throws IOException;

        void canRestartThisJobFromTheBeginning() throws IOException;

        boolean isShutdownRequested();

        default void canContinue() throws JobMonitorError {
            if (isShutdownRequested()) {
                throw new JobMonitorError(Status.Exit.ABORTED);
            }
        }
    }

}
