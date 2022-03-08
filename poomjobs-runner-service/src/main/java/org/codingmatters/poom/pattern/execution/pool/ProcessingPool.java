package org.codingmatters.poom.pattern.execution.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;

public interface ProcessingPool<P> {
    void process(P p, String reason) throws LockingFailed, PoolBusyException;
    Status status();

    void start();
    void stop(long timeout);

    enum Status {
        ACCEPTING, FULL
    }
}
