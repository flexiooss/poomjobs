package org.codingmatters.poom.jobs.runner.service.manager.flow;

import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobRunnerRunnableTest {

    private JobProcessorRunner.JobUpdater jobUpdater = job -> job;

    private final List<Job> processed = Collections.synchronizedList(new LinkedList<>());
    private final List<RunnerStatus> runnerStatuses = Collections.synchronizedList(new LinkedList<>());
    private final List<Job> contextSetup = Collections.synchronizedList(new LinkedList<>());
    private final List<Exception> unrecoverableExceptions = Collections.synchronizedList(new LinkedList<>());
    private final List<JobProcessingException> jobProcessingExceptions = Collections.synchronizedList(new LinkedList<>());

    private final RunnerToken testToken = RunnerToken.builder().label("test-runnable").build();

    private final JobRunnerRunnable runnable = new JobRunnerRunnable(
            this.testToken,
            this.jobUpdater,
            job -> () -> {
                processed.add(job);
                return job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build());
            },
            (token, status) -> {
                assertThat(token, is(testToken));
                runnerStatuses.add(status);
            },
            new JobRunnerRunnable.JobRunnerRunnableErrorListener() {
                @Override
                public void unrecoverableExceptionThrown(Exception e) {
                    unrecoverableExceptions.add(e);
                }

                @Override
                public void processingExceptionThrown(RunnerToken token, JobProcessingException e) {
                    assertThat(token, is(testToken));
                    jobProcessingExceptions.add(e);
                }
            },
            (job, processor) -> contextSetup.add(job)
    );

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    @Before
    public void setUp() throws Exception {
        this.pool.submit(this.runnable);
        Eventually.defaults().assertThat(() -> this.runnable.running(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        this.pool.shutdownNow();
    }

    @Test
    public void whenSubmittedJobIsPending__thenJobProcessingException() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.PENDING).build()).build();
        this.runnable.assign(job);

        Eventually.defaults().assertThat(() -> this.jobProcessingExceptions, hasSize(1));
        assertThat(this.jobProcessingExceptions.get(0).getMessage(), is("Job has not been reserved, will not execute. Run status should be RUNNING, was : PENDING"));

        assertThat(this.processed, is(empty()));
        assertThat(this.contextSetup, is(empty()));
        assertThat(this.unrecoverableExceptions, is(empty()));

        System.out.println(this.runnerStatuses);
        assertThat(this.runnerStatuses, contains(RunnerStatus.IDLE, RunnerStatus.BUSY, RunnerStatus.IDLE));
    }

    @Test
    public void whenSubmittedJobIsDone__thenJobProcessingException() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.DONE).build()).build();
        this.runnable.assign(job);

        Eventually.defaults().assertThat(() -> this.jobProcessingExceptions, hasSize(1));
        assertThat(this.jobProcessingExceptions.get(0).getMessage(), is("Job has not been reserved, will not execute. Run status should be RUNNING, was : DONE"));

        assertThat(this.processed, is(empty()));
        assertThat(this.contextSetup, is(empty()));
        assertThat(this.unrecoverableExceptions, is(empty()));
        System.out.println(this.runnerStatuses);
        assertThat(this.runnerStatuses, contains(RunnerStatus.BUSY, RunnerStatus.IDLE));
    }

    @Test
    public void whenSubmittedJobIsRunning__thenProcessed() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build();
        this.runnable.assign(job);

        Eventually.defaults().assertThat(() -> this.processed, contains(job));
        assertThat(this.contextSetup, contains(job));

        assertThat(this.unrecoverableExceptions, is(empty()));
        assertThat(this.jobProcessingExceptions, is(empty()));
        System.out.println(this.runnerStatuses);
        assertThat(this.runnerStatuses, contains(RunnerStatus.BUSY, RunnerStatus.IDLE));
    }
}