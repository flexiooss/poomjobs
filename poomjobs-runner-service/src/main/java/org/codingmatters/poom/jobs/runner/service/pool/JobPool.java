package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobPool {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobPool.class);

    private final PendingWorkers pendingWorkers;

    private final ExecutorService workerPool;
    private final List<JobWorker>  workers;

    public JobPool(int capacity, JobRunner jobRunner) {
        log.info("starting job pool...");
        this.pendingWorkers = new PendingWorkers(capacity);
        this.workerPool = Executors.newFixedThreadPool(
                capacity,
                runnable -> new Thread(new ThreadGroup("job-workers"), runnable)
        );
        this.workers = new ArrayList<>(capacity);

        for (int i = 0; i < capacity; i++) {
            JobWorker jobWorker = new JobWorker(this.pendingWorkers, jobRunner);
            this.workerPool.submit(jobWorker);
            this.workers.add(jobWorker);
        }
        while(this.pendingWorkers.waitingCount() < this.workers.size()); {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
        log.info("job pool ready.");
    }

    public void feed(Job job) throws PoolBusyException {
        this.pendingWorkers.submit(job);
    }

    public void addJobPoolListener(JobPoolListener listener) {
        this.pendingWorkers.addJobPoolListener(listener);
    }

    public void stop() {
        log.info("stopping job pool...");
        for (JobWorker worker : this.workers) {
            worker.stop();
        }
        for (JobWorker worker : this.workers) {
            while(!worker.stopped()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
        }
        this.workerPool.shutdown();
        log.info("job pool stopped.");
    }
}
