package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poomjobs.api.types.Job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class PendingWorkers {
    private final ArrayBlockingQueue<JobWorker> queue;
    private final List<JobPoolListener>  listeners = Collections.synchronizedList(new ArrayList<JobPoolListener>());

    public PendingWorkers(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public void submit(Job job) throws PoolBusyException {
        JobWorker worker = this.queue.poll();
        if(worker == null) {
            throw new PoolBusyException("no pending worker");
        } else {
            worker.submit(job);
            this.notifyPoolState();
        }
    }

    public void idle(JobWorker worker) {
        this.queue.offer(worker);
        this.notifyPoolState();
    }

    public boolean isFull() {
        return this.queue.isEmpty();
    }
    public int waitingCount() {
        return this.queue.size();
    }

    public void addJobPoolListener(JobPoolListener listener) {
        this.listeners.add(listener);
    }

    private void notifyPoolState() {
        boolean full = this.isFull();
        for(JobPoolListener listener : this.listeners) {
            if(full) {
                listener.poolIsFull();
            } else {
                listener.poolIsAcceptingJobs();
            }
        }
    }
}
