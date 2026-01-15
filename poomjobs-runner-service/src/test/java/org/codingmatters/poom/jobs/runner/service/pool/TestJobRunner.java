package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TestJobRunner implements JobRunner {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(TestJobRunner.class);

    public final List<Job> doneJobs = Collections.synchronizedList(new LinkedList<>());

    @Override
    public void runWith(Job job) throws JobProcessingException, JobProcessorRunner.JobUpdateFailure {
        log.info("executing job " + job.name());
        String name = job.opt().name().orElse("SHORT");
        try {
            switch (name) {
                case "SHORT":
                    Thread.sleep(100);
                    break;
                case "MEDIUM":
                    Thread.sleep(500);
                    break;
                case "LONG":
                    Thread.sleep(1000);
                    break;
                case "VERY_LONG":
                    Thread.sleep(2000);
                    break;
                default:
                    Thread.sleep(100);
                    break;
            }
        } catch (InterruptedException e) {
            log.error("interrupted while waiting for job " + job.name(), e);
        }
        this.doneJobs.add(job);
        log.info("done running job " + name);
    }
}
