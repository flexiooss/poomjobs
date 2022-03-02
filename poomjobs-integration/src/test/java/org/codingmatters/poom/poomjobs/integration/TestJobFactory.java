package org.codingmatters.poom.poomjobs.integration;

import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

public class TestJobFactory implements JobProcessor.Factory {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(TestJobFactory.class);

    @Override
    public JobProcessor createFor(Job job) {
        return () -> {
            log.info("PROCESSING TEST JOB {}", job);
            try {
                if (job.name().equals("short")) {
                    Thread.sleep(500L);
                } else {
                    Thread.sleep(5000L);
                }
            } catch (InterruptedException e) {
            }

            log.info("DONE PROCESSING TEST JOB {}", job);
            return job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build());
        };
    }
}
