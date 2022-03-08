package org.codingmatters.poom.pattern.execution.pool.processable;

import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.UnlockingFailed;

public interface ProcessableManager<P> {
    P lock(P p) throws LockingFailed;
    P release(P p) throws UnlockingFailed;
}
