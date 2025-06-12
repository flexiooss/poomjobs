package org.codingmatters.poom.runner.exception;

import org.codingmatters.poomjobs.api.types.job.Status;

public class JobMonitorError extends Error {

    private final Status.Exit exit;

    public JobMonitorError() {
        this(Status.Exit.ABORTED);
    }

    public JobMonitorError(Status.Exit exit) {
        this.exit = exit;
    }

    public Status.Exit exit() {
        return exit;
    }
}
