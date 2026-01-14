package org.codingmatters.poom.jobs.runner.service.pool;

public interface JobPoolListener {
    void poolIsFull();
    void poolIsAcceptingJobs();
}
