package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;

public interface RunnerStatusProvider {
    RunnerStatus status();
}
