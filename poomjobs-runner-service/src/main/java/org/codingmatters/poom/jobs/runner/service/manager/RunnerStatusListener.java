package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.manager.exception.UnregisteredTokenException;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;

public interface RunnerStatusListener {
    void statusFor(RunnerToken token, RunnerStatus status) throws UnregisteredTokenException;
}
