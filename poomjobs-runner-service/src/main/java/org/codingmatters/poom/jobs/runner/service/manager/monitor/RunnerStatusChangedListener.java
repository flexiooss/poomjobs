package org.codingmatters.poom.jobs.runner.service.manager.monitor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

    class Cluster implements RunnerStatusChangedListener {
        private final List<RunnerStatusChangedListener> listeners = Collections.synchronizedList(new LinkedList<>());

        public void add(RunnerStatusChangedListener listener) {
            this.listeners.add(listener);
        }

        @Override
        public void onIdle(RunnerStatus was) {
            this.listeners.forEach(listener -> listener.onIdle(was));
        }

        @Override
        public void onBusy(RunnerStatus was) {
            this.listeners.forEach(listener -> listener.onBusy(was));
        }
    }

}
