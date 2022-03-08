package org.codingmatters.poom.pattern.execution.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.pattern.execution.pool.processable.ProcessableManager;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.LockingFailed;
import org.codingmatters.poom.pattern.execution.pool.processable.exceptions.UnlockingFailed;
import org.codingmatters.poom.pattern.execution.pool.workers.WorkerProcessor;
import org.codingmatters.poom.services.tests.Eventually;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

public class WorkerProcessingPoolTest {

    static class LockLog {
        public final String processable;
        public final String action;

        public LockLog(String processable, String action) {
            this.processable = processable;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LockLog lockLog = (LockLog) o;
            return Objects.equals(processable, lockLog.processable) && Objects.equals(action, lockLog.action);
        }

        @Override
        public int hashCode() {
            return Objects.hash(processable, action);
        }

        @Override
        public String toString() {
            return "LockLog{" +
                    "processable='" + processable + '\'' +
                    ", action='" + action + '\'' +
                    '}';
        }
    }

    static private LockLog lock(String processable) {
        return new LockLog(processable, "lock");
    }
    static private LockLog unlock(String processable) {
        return new LockLog(processable, "unlock");
    }

    static private List<LockLog> LOCK_LOGS = Collections.synchronizedList(new LinkedList<>());

    private static final ProcessableManager<String> YES_MANAGER = new ProcessableManager<>() {
        @Override
        public String lock(String s) throws LockingFailed {
            LOCK_LOGS.add(WorkerProcessingPoolTest.lock(s));
            return s;
        }

        @Override
        public String release(String s) throws UnlockingFailed {
            LOCK_LOGS.add(WorkerProcessingPoolTest.unlock(s));
            return s;
        }
    };

    private static final ProcessableManager<String> LOCK_FAILS_MANAGER = new ProcessableManager<>() {
        @Override
        public String lock(String s) throws LockingFailed {
            LOCK_LOGS.add(WorkerProcessingPoolTest.lock(s));
            throw new LockingFailed("lock failure");
        }

        @Override
        public String release(String s) throws UnlockingFailed {
            LOCK_LOGS.add(WorkerProcessingPoolTest.unlock(s));
            return s;
        }
    };

    private static final ProcessableManager<String> UNLOCK_FAILS_MANAGER = new ProcessableManager<>() {
        @Override
        public String lock(String s) throws LockingFailed {
            LOCK_LOGS.add(WorkerProcessingPoolTest.lock(s));
            return s;
        }

        @Override
        public String release(String s) throws UnlockingFailed {
            LOCK_LOGS.add(WorkerProcessingPoolTest.unlock(s));
            throw new UnlockingFailed("unlock failure");
        }
    };

    private static List<String> PROCESSED = Collections.synchronizedList(new LinkedList<>());
    private static final WorkerProcessor<String> YES_PROCESSOR = processable -> {
        PROCESSED.add(processable);
    };

    @Before
    public void setUp() throws Exception {
        PROCESSED.clear();
        LOCK_LOGS.clear();
    }

    @Test
    public void whenNotStarted__thenStatusAccepting() throws Exception {
        ProcessingPool<String> pool = new WorkerProcessingPool<>(5, YES_MANAGER, YES_PROCESSOR, ProcessingPoolListener.NOOP);
        assertThat(pool.status(), is(ProcessingPool.Status.ACCEPTING));
    }

    @Test
    public void whenStarted__thenStatusAccepting() throws Exception {
        ProcessingPool<String> pool = new WorkerProcessingPool<>(5, YES_MANAGER, YES_PROCESSOR, ProcessingPoolListener.NOOP);
        try {
            pool.start();
            Thread.sleep(500);
            assertThat(pool.status(), is(ProcessingPool.Status.ACCEPTING));
        } finally {
            pool.stop(1000);
        }
    }

    @Test
    public void whenProcess__thenProcessableWasLocked_andProcessed() throws Exception {
        ProcessingPool<String> pool = new WorkerProcessingPool<>(5, YES_MANAGER, YES_PROCESSOR, ProcessingPoolListener.NOOP);
        try {
            pool.start();
            Thread.sleep(500);

            pool.process("to-process", "why not");

            Eventually.defaults().assertThat(() -> LOCK_LOGS, contains(lock("to-process")));
            Eventually.defaults().assertThat(() -> PROCESSED, contains("to-process"));
            assertThat(pool.status(), is(ProcessingPool.Status.ACCEPTING));

            assertThat(LOCK_LOGS, contains(lock("to-process")));
        } finally {
            pool.stop(1000);
        }
    }

    @Test
    public void whenPoolIsFull__thenPoolBusyException_andNotProcessed_andUnlocked() throws Exception {
        Object monitor = new Object();
        ProcessingPool<String> pool = new WorkerProcessingPool<>(1, YES_MANAGER, processable -> {
            synchronized (monitor) {
                try {
                    PROCESSED.add(processable);
                    monitor.wait();
                } catch (InterruptedException e) {}
            }
        }, ProcessingPoolListener.NOOP);
        try {
            pool.start();
            Thread.sleep(500);

            pool.process("long running", "why not");
            Thread.sleep(200);

            assertThrows(PoolBusyException.class, () -> pool.process("to-process", "why not"));
            Thread.sleep(500);

            assertThat(PROCESSED, contains("long running"));
            assertThat(LOCK_LOGS, contains(lock("long running"), lock("to-process"), unlock("to-process")));

            synchronized (monitor) {
                monitor.notifyAll();
            }
        } finally {
            pool.stop(1000);
        }
    }

    @Test
    public void givenPoolIsFull__whenUnlockingFails__thenPoolBusyException_andNotProcessed_andUnlocked() throws Exception {
        Object monitor = new Object();
        ProcessingPool<String> pool = new WorkerProcessingPool<>(1, UNLOCK_FAILS_MANAGER, processable -> {
            synchronized (monitor) {
                try {
                    PROCESSED.add(processable);
                    monitor.wait();
                } catch (InterruptedException e) {}
            }
        }, ProcessingPoolListener.NOOP);
        try {
            pool.start();
            Thread.sleep(500);

            pool.process("long running", "why not");
            Thread.sleep(200);

            assertThrows(PoolBusyException.class, () -> pool.process("to-process", "why not"));
            Thread.sleep(500);

            assertThat(PROCESSED, contains("long running"));
            assertThat(LOCK_LOGS, contains(lock("long running"), lock("to-process"), unlock("to-process")));

            synchronized (monitor) {
                monitor.notifyAll();
            }
        } finally {
            pool.stop(1000);
        }
    }

    @Test
    public void whenLockFails__thenLockingFailedException_andNotProcessed() throws Exception {
        ProcessingPool<String> pool = new WorkerProcessingPool<>(5, LOCK_FAILS_MANAGER, YES_PROCESSOR, ProcessingPoolListener.NOOP);
        try {
            pool.start();
            Thread.sleep(500);

            assertThrows(LockingFailed.class, () -> pool.process("to-process", "why not"));

            Thread.sleep(500);
            assertThat(PROCESSED, is(empty()));
        } finally {
            pool.stop(1000);
        }
    }

    @Test
    public void whenPoolIsReachesLimit_andGoesDown__thenStateChangesWhenHittingPoolSize_andLeavingPoolSize() throws Exception {
        List<String> statusChanges = Collections.synchronizedList(new LinkedList<>());
        Object monitor = new Object();
        ProcessingPool<String> pool = new WorkerProcessingPool<>(2, YES_MANAGER,
                processable -> {
                    synchronized (monitor) {
                        try {
                            PROCESSED.add(processable);
                            monitor.wait();
                        } catch (InterruptedException e) {}
                    }
                },
                new ProcessingPoolListener() {
            @Override
            public void accepting() {
                statusChanges.add("accepting");
            }

            @Override
            public void full() {
                statusChanges.add("full");
            }
        });
        try {
            pool.start();
            Thread.sleep(200);

            pool.process("p1", "why not");
            Thread.sleep(200);
            assertThat(statusChanges, is(empty()));

            pool.process("p2", "why not");
            Thread.sleep(200);
            assertThat(statusChanges, contains("full"));

            synchronized (monitor) {
                monitor.notify();
            }
            Thread.sleep(200);
            assertThat(statusChanges, contains("full", "accepting"));

            synchronized (monitor) {
                monitor.notify();
            }
            Thread.sleep(200);
            assertThat(statusChanges, contains("full", "accepting"));

        } finally {
            pool.stop(1000);
        }

    }
}