package org.codingmatters.poom.jobs.runner.service.jobs;

import io.flexio.services.support.api.ApiService;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poom.services.support.logging.LoggingContext;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * takes a reserved Job (status = RUNNING), executes it
 */
public class JobProcessorRunner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobProcessorRunner.class);
    private static final String JOB_START_STOP_POLICY = "JOB_START_STOP_POLICY";

    private final JobUpdater updatedJobConsumer;
    private final JobProcessor.Factory processorFactory;
    private final JobContextSetup contextSetup;
    private final JobStartStopLogPolicy jobStartStopLogPolicy;
    private final List<JobProcessor> runningProcessors = Collections.synchronizedList(new ArrayList<>());

    public JobProcessorRunner(JobUpdater updatedJobConsumer, JobProcessor.Factory processorFactory, JobContextSetup contextSetup) {
        this.updatedJobConsumer = updatedJobConsumer;
        this.processorFactory = processorFactory;
        this.contextSetup = contextSetup;

        JobStartStopLogPolicy policy;
        try {
            policy = JobStartStopLogPolicy.valueOf(
                    Env.optional(JOB_START_STOP_POLICY).orElse(new Env.Var(JobStartStopLogPolicy.INFO.name())).asString()
            );
        } catch (IllegalArgumentException e) {
            policy = JobStartStopLogPolicy.INFO;
        }
        this.jobStartStopLogPolicy = policy;
    }

    public void shutdownProperlyAllProcessors() {
        synchronized (runningProcessors) {
            for (JobProcessor runningProcessor : runningProcessors) {
                runningProcessor.shutDownProperly();
            }
        }
    }

    public void runWith(Job job) throws JobProcessingException, JobUpdateFailure {
        JobProcessor processor = null;
        try (LoggingContext loggingContext = LoggingContext.start()) {
            ApiService.propagator().toLog().propagate();

            if (!Status.Run.RUNNING.equals(job.opt().status().run().orElse(null))) {
                throw new JobProcessingException("Job has not been reserved, will not execute. Run status should be RUNNING, was : " + job.opt().status().run().orElse(null));
            }
            processor = this.processorFactory.createFor(job);
            synchronized (runningProcessors) {
                runningProcessors.add(processor);
            }
            this.contextSetup.setup(job, processor);

            if (this.jobStartStopLogPolicy.equals(JobStartStopLogPolicy.DEBUG)) {
                log.debug("starting processing job {}", job);
            } else {
                log.info("starting processing job {}", job);
            }
            Job updated;
            try {
                updated = processor.process();
                updated = this.withFinalStatus(updated);
            } catch (JobProcessingException e) {
                throw e;
            } catch (Exception e) {
                log.error("[GRAVE] unexpected exception while processing job " + job, e);
                updated = this.withErrorStatus(job);
            }

            log.debug("job processed, will update status with {}", updated);
            updated = this.updatedJobConsumer.update(updated);

            if (this.jobStartStopLogPolicy.equals(JobStartStopLogPolicy.DEBUG)) {
                log.debug("job processed : {}", updated);
            } else {
                log.info("job processed : {}", updated);
            }
        } finally {
            synchronized (runningProcessors) {
                if (processor != null) {
                    runningProcessors.remove(processor);
                }
            }
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

    private Job withErrorStatus(Job job) {
        return job.withStatus(
                Status.builder()
                        .run(Status.Run.DONE)
                        .exit(job.opt().status().exit().orElse(Status.Exit.FAILURE))
                        .build()
        );
    }

    @FunctionalInterface
    public interface JobUpdater {
        Job update(Job job) throws JobUpdateFailure;
    }

    public interface PendingJobManager {
        ValueList<Job> pendingJobs();
        Job reserve(Job job) throws JobProcessorRunner.JobUpdateFailure;
        Job release(Job reserved) throws JobUpdateFailure;
    }

    public static class JobUpdateFailure extends Exception {
        public JobUpdateFailure(String message) {
            super(message);
        }

        public JobUpdateFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class JobUpdateInvalid extends Exception {
        public JobUpdateInvalid(String message) {
            super(message);
        }

        public JobUpdateInvalid(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public List<JobProcessor> runningProcessors() {
        return runningProcessors;
    }
}
