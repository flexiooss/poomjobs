package org.codingmatters.poom.jobs.runner.service.jobs;

import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobProcessorRunnerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final AtomicReference<Job> processedJob = new AtomicReference<>();
    private final AtomicReference<JobProcessingException> nextJobProcessingException = new AtomicReference<>();
    private final AtomicReference<Status> nextJobStatus = new AtomicReference<>();

    private final AtomicReference<Job> updatedJob = new AtomicReference<>();

    private final JobProcessorRunner flow = new JobProcessorRunner(
            job -> {
                updatedJob.set(job);
                return job;
            },
            (job, monitor) -> () -> {
                processedJob.set(job);
                JobProcessingException exception = nextJobProcessingException.get();
                if (exception != null) {throw exception;}
                return job.withStatus(nextJobStatus.get());
            },
            JobContextSetup.NOOP
    );

    @Test
    public void givenJobRunning__whenNoException_andStatusSetByProcessor__thenJobProcessed_andStatusSetFromProcessor() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build();

        this.nextJobProcessingException.set(null);
        this.nextJobStatus.set(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build());

        this.flow.runWith(job);

        assertThat(
                this.updatedJob.get(),
                is(job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build()))
        );
    }

    @Test
    public void givenJobRunning__whenNoException_andRunStatusSetByProcessor_andExitStatusNotSet__thenJobProcessed_andDefaultExitStatusSet() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build();

        this.nextJobProcessingException.set(null);
        this.nextJobStatus.set(Status.builder().run(Status.Run.DONE).build());

        this.flow.runWith(job);

        assertThat(
                this.updatedJob.get(),
                is(job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build()))
        );
    }

    @Test
    public void givenJobRunning__whenNoException_andRunStatusNotSetByProcessor_andExitStatusSet__thenJobProcessed_andDefaultExitStatusSet() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build();

        this.nextJobProcessingException.set(null);
        this.nextJobStatus.set(Status.builder().exit(Status.Exit.FAILURE).build());

        this.flow.runWith(job);

        assertThat(
                this.updatedJob.get(),
                is(job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build()))
        );
    }

    @Test
    public void givenJobRunning__whenNoException_andStatusNotSetByProcessor__thenJobProcessed_andDefaultStatusSet() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build();

        this.nextJobProcessingException.set(null);
        this.nextJobStatus.set(null);

        this.flow.runWith(job);

        assertThat(
                this.updatedJob.get(),
                is(job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build()))
        );
    }

    @Test
    public void givenJobRunning__whenNoException_andStatusLeftUntouchedByProcessor__thenJobProcessed_andDefaultStatusSet() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build();

        this.nextJobProcessingException.set(null);
        this.nextJobStatus.set(Status.builder().run(Status.Run.RUNNING).build());

        this.flow.runWith(job);

        assertThat(
                this.updatedJob.get(),
                is(job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build()))
        );
    }

    @Test
    public void givenJobRunning__whenJobProcessingExceptionThrown__thenExceptionPassedBy_andJobNotMarkedProcessed() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build();

        this.nextJobProcessingException.set(new JobProcessingException("test exception"));

        thrown.expect(JobProcessingException.class);
        this.flow.runWith(job);

        assertThat(this.updatedJob.get(), is(nullValue()));
    }

    @Test
    public void whenJobNotRunning__thenJobProcessingException_andJobNotMarkedProcessed() throws Exception {
        Job job = Job.builder().status(Status.builder().run(Status.Run.PENDING).build()).build();

        thrown.expect(JobProcessingException.class);
        this.flow.runWith(job);

        assertThat(this.updatedJob.get(), is(nullValue()));
    }

    @Test
    public void testRunnerShutdown() throws JobProcessorRunner.JobUpdateFailure, JobProcessingException, InterruptedException {
        JobProcessorRunner runner = new JobProcessorRunner(
                job -> {
                    updatedJob.set(job);
                    return job;
                },
                (job, monitor) -> new JobProcessor() {
                    @Override
                    public Job process() throws JobProcessingException {
                        do {
                            try {
                                System.out.println("Running...");
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new JobProcessingException("Error ", e);
                            }
                        } while (!monitor.isShutdownRequested());
                        System.out.println("Job end");
                        return job.withStatus(Status.builder().exit(Status.Exit.SUCCESS).run(Status.Run.DONE).build());
                    }
                },
                JobContextSetup.NOOP
        );
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                runner.runWith(Job.builder().status(Status.builder().run(Status.Run.RUNNING).build()).build());
            } catch (Exception e) {
                throw new AssertionError("should execute", e);
            }
        });

        boolean terminated = executor.awaitTermination(2, TimeUnit.SECONDS);
        assertThat(terminated, is(false));
        assertThat(runner.runningJobs().size(), is(1));

        runner.shutdownProperlyAllProcessors();
        executor.shutdown();
        terminated = executor.awaitTermination(3, TimeUnit.SECONDS);
        assertThat(terminated, is(true));
        assertThat(runner.runningJobs().size(), is(0));
    }

}