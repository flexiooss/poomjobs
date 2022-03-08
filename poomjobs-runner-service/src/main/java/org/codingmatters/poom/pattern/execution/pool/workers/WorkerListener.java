package org.codingmatters.poom.pattern.execution.pool.workers;

public interface WorkerListener {
    void busy();
    void idle();

    static WorkerListener NOOP = new WorkerListener() {
        @Override
        public void busy() {

        }

        @Override
        public void idle() {

        }
    };
}
