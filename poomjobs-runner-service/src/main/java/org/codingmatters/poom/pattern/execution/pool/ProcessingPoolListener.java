package org.codingmatters.poom.pattern.execution.pool;

public interface ProcessingPoolListener {
    void accepting();
    void full();

    ProcessingPoolListener NOOP = new ProcessingPoolListener() {
        @Override
        public void accepting() {
        }

        @Override
        public void full() {
        }
    };
}
