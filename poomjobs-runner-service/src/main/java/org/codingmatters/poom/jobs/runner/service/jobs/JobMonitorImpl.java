package org.codingmatters.poom.jobs.runner.service.jobs;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner.JobUpdater;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poomjobs.api.types.Job;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobMonitorImpl implements JobProcessor.JobMonitor {

    private final AtomicBoolean shutdownRequested;
    private final JobUpdater updatedJobConsumer;
    private final Job job;

    public JobMonitorImpl(AtomicBoolean shutdownRequested, JobUpdater updatedJobConsumer, Job job) {
        this.shutdownRequested = shutdownRequested;
        this.updatedJobConsumer = updatedJobConsumer;
        this.job = job;
    }

    @Override
    public void doNotRestartThisJobAtThisPoint() throws IOException {
        try {
            updatedJobConsumer.update(job.withChangedRunner(runner -> runner.idempotent(false)));
        } catch (Exception e) {
            throw new IOException("Cannot update job", e);
        }
    }

    @Override
    public void canRestartThisJobFromTheBeginning() throws IOException {
        try {
            updatedJobConsumer.update(job.withChangedRunner(runner -> runner.idempotent(true)));
        } catch (Exception e) {
            throw new IOException("Cannot update job", e);
        }
    }

    @Override
    public boolean isShutdownRequested() {
        return shutdownRequested.get();
    }

}
