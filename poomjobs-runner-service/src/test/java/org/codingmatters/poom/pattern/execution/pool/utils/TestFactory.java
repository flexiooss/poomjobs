package org.codingmatters.poom.pattern.execution.pool.utils;

import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestFactory implements JobProcessor.Factory {

    List<PilotableTestJobProcessor> processors = new ArrayList<>();

    @Override
    public JobProcessor createFor(Job job, JobProcessor.JobMonitor monitor) {
        PilotableTestJobProcessor pilotableTestJobProcessor = new PilotableTestJobProcessor(job);
        processors.add(pilotableTestJobProcessor);
        return pilotableTestJobProcessor;
    }

    public List<PilotableTestJobProcessor> processors() {
        return processors;
    }

    public static class PilotableTestJobProcessor implements JobProcessor {

        private final Job job;
        private final AtomicBoolean finished = new AtomicBoolean(false);

        public PilotableTestJobProcessor(Job job) {
            this.job = job;
        }

        public void terminate() {
            finished.set(true);
        }

        @Override
        public Job process() throws JobProcessingException {
            try {
                do {
                    Thread.sleep(1000);
                } while (!finished.get());

            } catch (InterruptedException e) {
                throw new JobProcessingException("Error", e);
            }
            return job.withStatus(Status.builder()
                    .run(Status.Run.DONE)
                    .exit(Status.Exit.SUCCESS)
                    .build());
        }
    }
}
