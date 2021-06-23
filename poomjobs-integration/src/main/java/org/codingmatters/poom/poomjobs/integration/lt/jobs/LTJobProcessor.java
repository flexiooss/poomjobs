package org.codingmatters.poom.poomjobs.integration.lt.jobs;

import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicLong;

public class LTJobProcessor implements JobProcessor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(LTJobProcessor.class);

    static public final AtomicLong PENDING = new AtomicLong(0);
    static public final AtomicLong COMPLETED = new AtomicLong(0);


    private final Job job;
    private final long delay;

    public LTJobProcessor(Job job, long delay) {
        this.job = job;
        this.delay = delay;
    }

    @Override
    public Job process() throws JobProcessingException {
        PENDING.incrementAndGet();
        long start = System.currentTimeMillis();
        do {
            this.someWork();
        } while(System.currentTimeMillis() - start < this.delay);
        log.info("done processing {} job.", this.job.name());
        COMPLETED.incrementAndGet();
        PENDING.decrementAndGet();
        log.info("Completed {} jobs \t\t\t({} pending)", LTJobProcessor.COMPLETED.get(), LTJobProcessor.PENDING.get());
        return this.job.withStatus(status -> status.withRun(Status.Run.DONE).withExit(Status.Exit.SUCCESS));
    }

    private void someWork() throws JobProcessingException {
        try {
            File file = File.createTempFile("ltjob-", ".txt");
            try(FileWriter writer = new FileWriter(file)) {
                for (int i = 0; i < 100; i++) {
                    writer.write("Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                            "Nam iaculis vehicula urna quis efficitur. " +
                            "Phasellus quis hendrerit velit.\n");
                    writer.flush();
                }
            } finally {
                file.delete();
            }
        } catch (Exception e) {
            throw new JobProcessingException("error handling some work", e);
        }
    }
}
