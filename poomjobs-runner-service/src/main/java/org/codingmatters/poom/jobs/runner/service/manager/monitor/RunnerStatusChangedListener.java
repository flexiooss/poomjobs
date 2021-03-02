package org.codingmatters.poom.jobs.runner.service.manager.monitor;

public interface RunnerStatusChangedListener {
    void onIdle(RunnerStatus was);
    void onBusy(RunnerStatus was);

    RunnerStatusChangedListener NOOP = new RunnerStatusChangedListener() {
        @Override
        public void onIdle(RunnerStatus was) {
        }

        @Override
        public void onBusy(RunnerStatus was) {
        }
    };
}
