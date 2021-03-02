package org.codingmatters.poom.jobs.runner.service.manager.flow;

import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;
import org.junit.After;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobRunnerRunnableTest {

    private Queue<Job> jobQueue = new ConcurrentLinkedQueue<>();

    private final RunnerToken runnerToken = RunnerToken.builder().label("test-runnable").build();

    private final List<RunnerStatus> statusHistory = Collections.synchronizedList(new LinkedList<>());

    private final AtomicReference<RuntimeException> nextUnexpectedExeption = new AtomicReference<>();
    private final AtomicReference<RuntimeException> unexpectedExeption = new AtomicReference<>();
    private final AtomicReference<JobProcessingException> nextJobProcessingException = new AtomicReference<>();
    private final AtomicReference<JobProcessingException> jobProcessingException = new AtomicReference<>();

    private final List<Job> ranJobs = Collections.synchronizedList(new LinkedList<>());

    private JobRunnerRunnable runnable = new JobRunnerRunnable(
            runnerToken,
            job -> {},
            job -> () -> {
                if(nextJobProcessingException.get() != null) {
                    throw nextJobProcessingException.get();
                }
                if(nextUnexpectedExeption.get() != null) {
                    throw nextUnexpectedExeption.get();
                }
                ranJobs.add(job);
                return job;
            },
            () -> jobQueue.poll(),
            (token, status) -> {
                assertThat(token, is(runnerToken));
                statusHistory.add(status);
            },
            new JobRunnerRunnable.JobRunnerRunnableErrorListener() {
                @Override
                public void unexpectedExceptionThrown(RunnerToken token, Exception e) {
                    assertThat(token, is(runnerToken));
                    unexpectedExeption.set((RuntimeException) e);
                }

                @Override
                public void processingExceptionThrown(RunnerToken token, JobProcessingException e) {
                    assertThat(token, is(runnerToken));
                    jobProcessingException.set(e);
                }
            },
            JobContextSetup.NOOP
    );

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @After
    public void tearDown() throws Exception {
        this.runnable.shutdown();
        Eventually.defaults().assertThat(() -> this.runnable.running(), is(false));

        this.executor.shutdownNow();
    }

    @Test
    public void givenNothingToProcess__whenStartedThenShutdown__thenStatusSetToIDLE_andStops() throws Exception {
        this.executor.execute(this.runnable);


        Eventually.defaults().assertThat(() -> this.statusHistory, contains(RunnerStatus.BUSY, RunnerStatus.IDLE));
        assertThat(this.ranJobs, is(empty()));
        assertThat(this.jobProcessingException.get(), is(nullValue()));
        assertThat(this.unexpectedExeption.get(), is(nullValue()));
    }

    @Test
    public void givenOneJobAvailable__whenStartedThenShutdown__thenSQtatusBUSYThenIDLE_andJobIsRan_andStops() throws Exception {
        Job job = this.runningJob("available-job");
        this.jobQueue.add(job);

        this.executor.execute(this.runnable);

        this.runnable.shutdown();
        Eventually.defaults().assertThat(() -> this.runnable.running(), is(false));

        assertThat(this.statusHistory, contains(RunnerStatus.BUSY, RunnerStatus.IDLE));
        assertThat(this.ranJobs, contains(job));
        assertThat(this.jobProcessingException.get(), is(nullValue()));
        assertThat(this.unexpectedExeption.get(), is(nullValue()));
    }

    @Test
    public void givenNoJobAvailable__whenOneJobAssigned__thenJobRanWhenAssigned() throws Exception {
        Job job = this.runningJob("assigned-job");

        this.executor.execute(this.runnable);
        this.runnable.assign(job);


        Eventually.defaults().assertThat(() -> this.statusHistory, contains(RunnerStatus.BUSY, RunnerStatus.IDLE, RunnerStatus.BUSY, RunnerStatus.IDLE));
        assertThat(this.ranJobs, contains(job));
        assertThat(this.jobProcessingException.get(), is(nullValue()));
        assertThat(this.unexpectedExeption.get(), is(nullValue()));
    }

    @Test
    public void givenNoJobInitiallyAvailable__whenOneJobAssisgned_andAJobBecomesAvailable__thenBecameAvailableRanAfterAssigned() throws Exception {
        Job assigned = this.runningJob("assigned");
        Job becameAvailableJob = this.runningJob("became-available");

        this.executor.execute(this.runnable);

        Eventually.defaults().assertThat(() -> this.statusHistory, contains(RunnerStatus.BUSY, RunnerStatus.IDLE));

        this.jobQueue.add(becameAvailableJob);
        this.runnable.assign(assigned);

        Eventually.defaults().assertThat(() -> this.statusHistory, contains(RunnerStatus.BUSY, RunnerStatus.IDLE, RunnerStatus.BUSY, RunnerStatus.IDLE));

        assertThat(this.ranJobs, contains(assigned, becameAvailableJob));
    }

    @Test
    public void givenAvailableJob__whenUnexpectedExceptionRaised__thenStopsAndError() throws Exception {
        this.jobQueue.add(this.runningJob("available"));
        this.nextUnexpectedExeption.set(new RuntimeException("from test"));

        this.executor.execute(this.runnable);

        Eventually.defaults().assertThat(() -> this.runnable.running(), is(false));
        assertThat(this.unexpectedExeption.get(), is(this.nextUnexpectedExeption.get()));
        assertThat(this.runnable.error(), is(true));
    }

    @Test
    public void givenNoAvailableJob__whenUnexpectedExceptionRaisedWhenAssigned__thenStopsAndError() throws Exception {
        this.nextUnexpectedExeption.set(new RuntimeException("from test"));

        this.executor.execute(this.runnable);
        this.runnable.assign(this.runningJob("assign"));

        Eventually.defaults().assertThat(() -> this.runnable.running(), is(false));
        assertThat(this.unexpectedExeption.get(), is(this.nextUnexpectedExeption.get()));
        assertThat(this.runnable.error(), is(true));
    }

    @Test
    public void givenAvailableJob__whenJobProcessingExceptionRaised__thenStillRuns_andExceptionSignaled() throws Exception {
        this.jobQueue.add(this.runningJob("available"));
        this.nextJobProcessingException.set(new JobProcessingException("from test"));

        this.executor.execute(this.runnable);

        Eventually.defaults().assertThat(() -> this.jobProcessingException.get(), is(this.nextJobProcessingException.get()));
        assertThat(this.runnable.running(), is(true));
        assertThat(this.runnable.error(), is(false));
    }

    @Test
    public void givenNoAvailableJob__whenJobProcessingExceptionRaisedWhenAssigned__thenStillRuns_andExceptionSignaled() throws Exception {
        this.nextJobProcessingException.set(new JobProcessingException("from test"));

        this.executor.execute(this.runnable);
        this.runnable.assign(this.runningJob("assign"));

        Eventually.defaults().assertThat(() -> this.jobProcessingException.get(), is(this.nextJobProcessingException.get()));
        assertThat(this.runnable.running(), is(true));
        assertThat(this.runnable.error(), is(false));
    }

    private Job runningJob(String id) {
        return Job.builder().id(id).status(Status.builder().run(Status.Run.RUNNING).build()).build();
    }
}