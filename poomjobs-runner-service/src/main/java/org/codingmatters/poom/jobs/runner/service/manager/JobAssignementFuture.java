package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poomjobs.api.types.Job;

import java.util.concurrent.CompletableFuture;

public class JobAssignementFuture extends CompletableFuture<JobAssignementFuture.Status> {
    public enum Status {
        SUCCESS, FAILURE
    }
    private final Job job;

    public JobAssignementFuture(Job job) {
        this.job = job;
    }

    public Job job() {
        return job;
    }
}
