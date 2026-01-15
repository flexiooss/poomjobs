package org.codingmatters.poom.pattern.execution.pool.utils;

import org.codingmatters.poom.jobs.runner.service.StatusManager;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;

import static org.codingmatters.poomjobs.api.types.RunnerStatusData.Status.IDLE;
import static org.codingmatters.poomjobs.api.types.RunnerStatusData.Status.RUNNING;

public class TestStatusManager implements StatusManager {

    private RunnerStatusData.Status status;

    @Override
    public void becameIdle() {
        this.status = IDLE;
    }

    @Override
    public void becameBusy() {
        this.status = RUNNING;
    }

    public RunnerStatusData.Status status() {
        return status;
    }
}
