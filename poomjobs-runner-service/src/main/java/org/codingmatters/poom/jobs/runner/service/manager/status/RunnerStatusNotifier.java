package org.codingmatters.poom.jobs.runner.service.manager.status;

import org.codingmatters.poom.jobs.runner.service.exception.NotificationFailedException;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;

@FunctionalInterface
public interface RunnerStatusNotifier {
    void notify(RunnerStatus status) throws NotificationFailedException;
}
