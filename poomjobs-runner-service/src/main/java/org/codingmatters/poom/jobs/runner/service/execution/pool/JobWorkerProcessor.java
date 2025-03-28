package org.codingmatters.poom.jobs.runner.service.execution.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.pattern.execution.pool.workers.WorkerProcessor;
import org.codingmatters.poom.pattern.execution.pool.workers.exceptions.WorkerProcessorException;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

public class JobWorkerProcessor implements WorkerProcessor<Job> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobWorkerProcessor.class);
    private final JobProcessorRunner jobProcessorRunner;

    public JobWorkerProcessor(JobProcessorRunner.JobUpdater updatedJobConsumer, JobProcessor.Factory processorFactory, JobContextSetup contextSetup) {
        this.jobProcessorRunner = new JobProcessorRunner(updatedJobConsumer, processorFactory, contextSetup);
    }

    @Override
    public void process(Job job) throws WorkerProcessorException {
        try {
            this.jobProcessorRunner.runWith(job);
        } catch (JobProcessingException e) {
            log.error("[GRAVE] job processing exception : " + job.withStatus((Status) null), e);
        } catch (JobProcessorRunner.JobUpdateFailure e) {
            log.error("[GRAVE] job was executed, but got update failure, job final status may be wrong : " + job.withStatus((Status) null), e);
        }
    }

    public void shutdownProperly() {
        this.jobProcessorRunner.shutdownProperlyAllProcessors();
    }

    public void updateAllRemainingJobToFailure() {
        this.jobProcessorRunner.updateAllRemainingJobToFailure();
    }

}
