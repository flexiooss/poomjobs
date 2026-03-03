package org.codingmatters.poom.jobs.runner.service;

import org.codingmatters.poomjobs.api.types.RunnerStatusData;

public interface StatusManager {

    public RunnerStatusData.Status status();
}
