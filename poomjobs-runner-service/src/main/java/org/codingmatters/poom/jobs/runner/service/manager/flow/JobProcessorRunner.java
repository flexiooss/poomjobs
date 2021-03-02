package org.codingmatters.poom.jobs.runner.service.manager.flow;

import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.util.function.Consumer;

/**
 * takes a reserved Job (status = RUNNING), executes it
 */
public class JobProcessorRunner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobProcessorRunner.class);

    private final JobUpdater updatedJobConsumer;
    private final JobProcessor.Factory processorFactory;
    private final JobContextSetup contextSetup;

    public JobProcessorRunner(JobUpdater updatedJobConsumer, JobProcessor.Factory processorFactory, JobContextSetup contextSetup) {
        this.updatedJobConsumer = updatedJobConsumer;
        this.processorFactory = processorFactory;
        this.contextSetup = contextSetup;
    }

    public void runWith(Job job) throws JobProcessingException {
        try(LoggingContext loggingContext = LoggingContext.start()) {
            if (!Status.Run.RUNNING.equals(job.opt().status().run().orElse(null))) {
                throw new JobProcessingException("Job has not been reserved, will not execute. Run status should be RUNNING, was : " + job.opt().status().run().orElse(null));
            }
            JobProcessor processor = this.processorFactory.createFor(job);
            this.contextSetup.setup(job, processor);
            log.info("starting processing job {}", job);
            Job updated = processor.process();
            updated = this.withFinalStatus(updated);

            log.debug("job processed, will update status with {}", updated);
            this.updatedJobConsumer.update(updated);

            log.debug("job processed : {}", updated);
        }
    }

    private Job withFinalStatus(Job job) {
        return job.withStatus(
                Status.builder()
                        .run(Status.Run.DONE)
                        .exit(job.opt().status().exit().orElse(Status.Exit.SUCCESS))
                        .build()
        );
    }

    @FunctionalInterface
    public interface JobUpdater {
        void update(Job job);
    }
}
