package org.codingmatters.poom.jobs.runner.service.manager.flow;

import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;

import java.util.function.Supplier;

/**
 * invoked wit a reserved Job (status = RUNNING)
 *
 */
public class JobConsumer {

    private final JobProcessorRunner runner;
    private final NextJobSupplier nextJob;

    public JobConsumer(JobProcessorRunner runner, NextJobSupplier nextJob) {
        this.runner = runner;
        this.nextJob = nextJob;
    }

    public void runWith(Job job) throws JobProcessingException, JobProcessorRunner.JobUpdateFailure {
        for(Job current = job ; current != null ; current = this.nextJob.nextJob()) {
            this.runner.runWith(current);
        }
    }

    @FunctionalInterface
    public interface NextJobSupplier {
        Job nextJob();
    }
}
