package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.manager.exception.UnregisteredTokenException;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatusChangedListener;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RunnerStatusMonitor implements RunnerStatusListener {
    private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerStatusMonitor.class);

    private final List<RunnerToken> runnerTokens = new LinkedList<>();
    private final Map<RunnerToken, RunnerStatus> runnerStatuses = new HashMap<>();
    private final String name;

    private final RunnerStatusChangedListener statusChangedListener;

    private RunnerStatus status = RunnerStatus.BUSY;

    public RunnerStatusMonitor(String name, RunnerStatusChangedListener statusChangedListener) {
        this.name = name;
        this.statusChangedListener = statusChangedListener;
    }

    public synchronized RunnerToken addToken() {
        RunnerToken token = RunnerToken.builder()
                .label("%s-runner-%03d", this.name, this.runnerTokens.size())
                .build();
        this.runnerTokens.add(token);
        this.runnerStatuses.put(token, RunnerStatus.UNKNOWN);
        return token;
    }

    public synchronized RunnerStatus status(RunnerToken token) throws UnregisteredTokenException {
        this.failIfUnregisteredToken(token);
        RunnerStatus status = this.runnerStatuses.get(token);
        return status;
    }

    @Override
    public synchronized void statusFor(RunnerToken token, RunnerStatus status) throws UnregisteredTokenException {
        this.failIfUnregisteredToken(token);
        if(RunnerStatus.UNKNOWN.equals(status)) throw new IllegalArgumentException("cannot set status to UNKNOWN, this is reserved for initial value");

        this.runnerStatuses.put(token, status);

        RunnerStatus previousStatus = this.status;
        this.status = this.computeStatus();
        this.notifyStatusChanged(previousStatus, this.status);
    }

    private void notifyStatusChanged(RunnerStatus from, RunnerStatus to) {
        log.debug("status change request from {} to {}", from, to);
        if(from != null && ! from.equals(to)) {
            if(RunnerStatus.IDLE.equals(to)) {
                this.statusChangedListener.onIdle(from);
            } else if(RunnerStatus.BUSY.equals(to)) {
                this.statusChangedListener.onBusy(from);
            }
        }
    }

    private void failIfUnregisteredToken(RunnerToken token) throws UnregisteredTokenException {
        if(token == null) {
            throw new UnregisteredTokenException("token is null");
        }
        if(! this.runnerTokens.contains(token)) {
            throw new UnregisteredTokenException("token is not registered, must be created by monitor with the addToken method");
        }
    }

    public synchronized RunnerStatus status() {
        return this.status;
    }

    private RunnerStatus computeStatus() {
        if(this.runnerStatuses.values().stream().filter(runnerStatus -> RunnerStatus.IDLE.equals(runnerStatus)).findAny().isPresent()) {
            return RunnerStatus.IDLE;
        } else {
            return RunnerStatus.BUSY;
        }
    }
}
