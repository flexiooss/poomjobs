package org.codingmatters.poom.pattern.execution.pool.workers;

import org.codingmatters.poom.pattern.execution.pool.workers.exceptions.WorkerProcessorException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.tests.Eventually;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorkerTest {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(WorkerTest.class);

    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    static private final List<String> processed = Collections.synchronizedList(new LinkedList<>());
    static private final List<String> statusChanges = Collections.synchronizedList(new LinkedList<>());

    static private final WorkerProcessor<String> NOOP_PROCESSOR = processable -> {};
    static private final WorkerProcessor<String> ACCUMULATING_PROCESSOR = processable -> {
        processed.add(processable);
        log.info("PROCESSED {}", processable);
    };

    static private final WorkerListener STATUS_CHANGED_LOGGER = new WorkerListener() {
        @Override
        public void busy() {
            statusChanges.add("busy");
        }

        @Override
        public void idle() {
            statusChanges.add("idle");
        }
    };

    @Before
    public void setUp() throws Exception {
        statusChanges.clear();
        processed.clear();
    }

    @Test
    public void whenInitialized__thenIdle() throws Exception {
        Worker<String> worker = new Worker<>(NOOP_PROCESSOR, WorkerListener.NOOP);

        assertThat(worker.isBusy(), is(false));
    }

    @Test
    public void whenSubmitted__thenTrue_andBusy() throws Exception {
        Worker<String> worker = new Worker<>(NOOP_PROCESSOR, WorkerListener.NOOP);

        assertTrue(worker.submit("to-process", "why not"));

        assertThat(worker.isBusy(), is(true));
    }

    @Test
    public void givenBusy__whenSubmitted__thenFalse_andBusy() throws Exception {
        Worker<String> worker = new Worker<>(NOOP_PROCESSOR, WorkerListener.NOOP);
        worker.submit("to-process", "why not");

        assertFalse(worker.submit("to-process", "why not"));

        assertThat(worker.isBusy(), is(true));
    }

    @Test
    public void givenSubmitted__whenStarting__thenProcessed_andIdle() throws Exception {
        Worker<String> worker = new Worker<>(ACCUMULATING_PROCESSOR, WorkerListener.NOOP);
        try {
            worker.submit("to-process", "why not");
            this.pool.submit(worker);

            Eventually.defaults().assertThat(() -> processed, contains("to-process"));
            Eventually.defaults().assertThat(() -> worker.isBusy(), is(false));
        } finally {
            worker.stop();
        }
    }

    @Test
    public void givenStarted__whenSubmitted__thenProcessed_andIdle() throws Exception {
        Worker<String> worker = new Worker<>(ACCUMULATING_PROCESSOR, WorkerListener.NOOP);
        try {
            this.pool.submit(worker);
            Thread.sleep(200);
            worker.submit("to-process", "why not");

            Eventually.defaults().assertThat(() -> processed, contains("to-process"));
            Eventually.defaults().assertThat(() -> worker.isBusy(), is(false));
        } finally {
            worker.stop();
        }
    }

    @Test
    public void givenStarted__whenSubmitted_andExceptionThrown__thenProcessed_andIdle() throws Exception {
        Worker<String> worker = new Worker<>(processable -> {
            ACCUMULATING_PROCESSOR.process(processable);
            throw new WorkerProcessorException("test failure");
        }, WorkerListener.NOOP);
        try {
            this.pool.submit(worker);
            Thread.sleep(200);
            worker.submit("to-process", "why not");

            Eventually.defaults().assertThat(() -> processed, contains("to-process"));
            Eventually.defaults().assertThat(() -> worker.isBusy(), is(false));
        } finally {
            worker.stop();
        }
    }

    @Test
    public void whenNothingSubmitted__thenNoStatusChange() throws Exception {
        Worker<String> worker = new Worker<>(NOOP_PROCESSOR, STATUS_CHANGED_LOGGER);
        try {
            this.pool.submit(worker);
            Thread.sleep(200);

            assertThat(statusChanges, is(empty()));
        } finally {
            worker.stop();
        }
    }

    @Test
    public void givenStarted__whenSubmitted__thenStatusChangesToBusy_thenIdle() throws Exception {
        Worker<String> worker = new Worker<>(NOOP_PROCESSOR, STATUS_CHANGED_LOGGER);
        try {
            this.pool.submit(worker);
            Thread.sleep(200);

            worker.submit("to-process", "why not");

            Eventually.defaults().assertThat(() -> statusChanges, contains("busy", "idle"));
        } finally {
            worker.stop();
        }
    }

    @Test
    public void givenStarted__whenSubmitted_andExceptionThrown__thenStatusChangesToBusy_thenIdle() throws Exception {
        Worker<String> worker = new Worker<>(processable -> {
            throw new WorkerProcessorException("test failure");
        }, STATUS_CHANGED_LOGGER);
        try {
            this.pool.submit(worker);
            Thread.sleep(200);

            worker.submit("to-process", "why not");

            Eventually.defaults().assertThat(() -> statusChanges, contains("busy", "idle"));
        } finally {
            worker.stop();
        }
    }

    @Test
    public void givenSubmitted__whenStarted__thenStatusChangesToBusy_thenIdle() throws Exception {
        Worker<String> worker = new Worker<>(NOOP_PROCESSOR, STATUS_CHANGED_LOGGER);
        try {
            worker.submit("to-process", "why not");
            this.pool.submit(worker);
            Thread.sleep(200);


            Eventually.defaults().assertThat(() -> statusChanges, contains("busy", "idle"));
        } finally {
            worker.stop();
        }
    }


}