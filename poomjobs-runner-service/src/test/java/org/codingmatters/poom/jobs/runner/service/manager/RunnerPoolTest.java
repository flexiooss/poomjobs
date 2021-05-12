package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.jobs.runner.service.exception.JobNotSubmitableException;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobRunnerRunnable;
import org.codingmatters.poom.jobs.runner.service.manager.jobs.JobManager;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatusChangedListener;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.*;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIHandlersClient;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RunnerPoolTest {

    private final List<Job> jobs = Collections.synchronizedList(new LinkedList<>());
    private final List<String> getResponsesContent = Collections.synchronizedList(new LinkedList<>());

    private HandlerResource<JobCollectionGetRequest, JobCollectionGetResponse> getJobs = new HandlerResource<JobCollectionGetRequest, JobCollectionGetResponse>() {
        @Override
        protected JobCollectionGetResponse defaultResponse(JobCollectionGetRequest request) {
            List<Job> page = jobs.subList(0, Math.min(10, jobs.size()));
            getResponsesContent.add(page.stream().map(job -> job.id()).collect(Collectors.joining(", ")));
            return JobCollectionGetResponse.builder().status200(status -> status
                            .acceptRange("Job 100")
                            .contentRange("Job %s-%s/%s", 0, page.size() - 1, page.size())
                            .payload(page)
                    ).build();
        }
    };
    private final List<String> reserved = Collections.synchronizedList(new LinkedList<>());
    private final HandlerResource<JobResourcePatchRequest, JobResourcePatchResponse> patchJob = new HandlerResource<JobResourcePatchRequest, JobResourcePatchResponse>() {
        @Override
        protected JobResourcePatchResponse defaultResponse(JobResourcePatchRequest request) {
            if("DONE".equals(request.payload().status().run().name())) {
                return JobResourcePatchResponse.builder().status200(status -> status.payload(Job.builder().id(request.jobId()).build())).build();
            }
            synchronized (reserved) {
                if(reserved.contains(request.jobId())) {
                    return JobResourcePatchResponse.builder()
                            .status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_JOB_CHANGE)))
                            .build();
                } else {
                    Job job;
                    if(request.jobId().startsWith("submitted")) {
                        job = Job.builder().id("submitted").status(Status.builder().run(Status.Run.RUNNING).build()).build();
                    } else {
                        Job found = jobs.stream().filter(j -> j.id().equals(request.jobId())).findAny().get();
                        if (found != null) {
                            jobs.remove(found);
                        }
                        job = found.withStatus(Status.builder().run(Status.Run.RUNNING).build());
                    }
                    if(job == null) {
                        return JobResourcePatchResponse.builder().status404(status -> status.payload(error -> error.code(Error.Code.RESOURCE_NOT_FOUND))).build();
                    } else {
                        reserved.add(job.id());
                        return JobResourcePatchResponse.builder().status200(status -> status.payload(job)).build();
                    }
                }
            }

        }
    };
    private PoomjobsJobRegistryAPIClient client = new PoomjobsJobRegistryAPIHandlersClient(
            new PoomjobsJobRegistryAPIHandlers.Builder()
                    .jobCollectionGetHandler(this.getJobs)
                    .jobResourcePatchHandler(this.patchJob)
                    .build(),
            Executors.newSingleThreadExecutor()
    );
    private JobManager jobManager = new JobManager(
            this.client,
            "test-runner",
            "category",
            new String[]{ "j1", "j2" }
    );

    private final List<String> statusChanges = Collections.synchronizedList(new LinkedList<>());

    private final List<Job> processedJobs = Collections.synchronizedList(new LinkedList<>());

    private final int concurrentJobCount = 10;

    private final RunnerPool pool = new RunnerPool(
            this.concurrentJobCount,
            this.jobManager,
            job -> () -> {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {}
                Job changed = job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build());
                processedJobs.add(changed);
                return changed;
            },
            JobContextSetup.NOOP,
            new JobRunnerRunnable.JobRunnerRunnableErrorListener() {
                @Override
                public void unrecoverableExceptionThrown(Exception e) {}

                @Override
                public void processingExceptionThrown(RunnerToken token, JobProcessingException e) {}
            },
            new RunnerStatusMonitor(
                    "test-job-pool",
                    new RunnerStatusChangedListener() {
                        @Override
                        public void onIdle(RunnerStatus was) {
                            statusChanges.add(String.format("%s to %s", was, "IDLE"));
                        }

                        @Override
                        public void onBusy(RunnerStatus was) {
                            statusChanges.add(String.format("%s to %s", was, "BUSY"));
                        }
                    }
            )

    );

    @After
    public void tearDown() throws Exception {
        this.pool.shutdownNow();
    }

    @Test
    public void givenNoJobSubmitted__whenNoPendingJob__thenBecomesIDLE() throws Exception {
        this.pool.start();
        Eventually.defaults().assertThat(() -> this.statusChanges, contains("BUSY to IDLE"));
    }

    @Test
    public void givenNoPendingJob__whenOneJobSubmitted_andJobStatusIsPending__thenJobExecuted() throws Exception {
        this.pool.start();
        this.pool.awaitReady(1L, TimeUnit.SECONDS);

        this.pool.submit(Job.builder().id("submitted").status(status -> status.run(Status.Run.PENDING)).build());

        Eventually.timeout(1, TimeUnit.SECONDS).assertThat(
                () -> this.processedJobs.stream().map(Job::id).collect(Collectors.toList()),
                contains("submitted")
        );
    }

    @Test(expected = JobNotSubmitableException.class)
    public void givenNoPendingJob__whenOneJobSubmitted_andJobStatusIsRunning__thenJobIsNotExecuted() throws Exception {
        this.pool.start();
        this.pool.awaitReady(1L, TimeUnit.SECONDS);

        this.pool.submit(Job.builder().id("submitted").status(status -> status.run(Status.Run.RUNNING)).build());

        Eventually.timeout(1, TimeUnit.SECONDS).assertThat(
                () -> this.processedJobs.stream().map(Job::id).collect(Collectors.toList()),
                contains("submitted")
        );
    }

    @Test
    public void givenNoPendingJob__whenOneJobSubmitted_andJobStatusIsPending__thenJobIsExecuted() throws Exception {
        this.pool.start();
        this.pool.awaitReady(1L, TimeUnit.SECONDS);

        this.pool.submit(Job.builder().id("submitted").status(status -> status.run(Status.Run.PENDING)).build());

        Eventually.timeout(1, TimeUnit.SECONDS).assertThat(
                () -> this.processedJobs.stream().map(Job::id).collect(Collectors.toList()),
                contains("submitted")
        );
    }

    @Test(expected = JobNotSubmitableException.class)
    public void givenNoPendingJob__whenOneJobSubmitted_andJobStatusIsDone__thenJobIsNotExecuted() throws Exception {
        this.pool.start();
        this.pool.awaitReady(1L, TimeUnit.SECONDS);

        this.pool.submit(Job.builder().id("submitted").status(status -> status.run(Status.Run.DONE)).build());
    }

    @Test(expected = JobNotSubmitableException.class)
    public void givenNoPendingJob__whenOneJobSubmitted_andJobStatusIsNull__thenJobIsNotExecuted() throws Exception {
        this.pool.start();
        this.pool.awaitReady(1L, TimeUnit.SECONDS);

        this.pool.submit(Job.builder().id("submitted").status(status -> status.run(null)).build());
    }

    @Test
    public void givenOnePendingJob__whenPoolStarted__thenJobProcessed() throws Exception {
        this.jobs.add(Job.builder().id("pending").status(status -> status.run(Status.Run.PENDING)).build());

        this.pool.start();

        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> this.processedJobs.stream().map(Job::id).collect(Collectors.toList()),
                contains("pending")
        );
    }

    @Test
    public void givenManyPendingJobs__whenPoolStarted__thenAllJobProcessed() throws Exception {
        String[] expectedProcessedIds = new String[this.concurrentJobCount * 2];
        for (int i = 0; i < this.concurrentJobCount * 2; i++) {
            String id = "job-" + i;
            this.jobs.add(Job.builder().id(id).status(Status.builder().run(Status.Run.PENDING).build()).build());
            expectedProcessedIds[i] = id;
        }
        this.pool.start();

        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> this.processedJobs.stream().map(Job::id).collect(Collectors.toList()),
                containsInAnyOrder(expectedProcessedIds)
        );
    }

    @Test
    public void givenNoPendingJobsInitially__whenJobSubmitted_andJobBecamePending__thenBothAreExecuted() throws Exception {
        AtomicBoolean noPending = new AtomicBoolean(true);
        AtomicBoolean pendingReturned = new AtomicBoolean(false);
        this.getJobs.nextResponse(request -> {
            if(noPending.get()) {
                return JobCollectionGetResponse.builder().status200(status -> status.contentRange("Job 0-0/0").payload(Collections.emptyList())).build();
            } else {
                if(! pendingReturned.getAndSet(true)) {
                    return JobCollectionGetResponse.builder().status200(status -> status.contentRange("Job 0-0/1").payload(
                            Job.builder().id("pending").build()
                    )).build();
                } else {
                    return JobCollectionGetResponse.builder().status200(status -> status.contentRange("Job 0-0/0").payload(Collections.emptyList())).build();
                }
            }
        });
        this.patchJob.nextResponse(request -> JobResourcePatchResponse.builder()
                .status200(status -> status.payload(Job.builder().id(request.jobId()).status(st -> st
                        .run(request.payload().opt().status().run().isPresent() ? Status.Run.valueOf(request.payload().status().run().name()) : null)
                        .exit(request.payload().opt().status().exit().isPresent() ? Status.Exit.valueOf(request.payload().status().exit().name()) : null)
                ).build()))
                .build());

        this.pool.start();
        this.pool.awaitReady(1L, TimeUnit.SECONDS);

        noPending.set(false);

        this.pool.submit(Job.builder().id("submitted").status(status -> status.run(Status.Run.PENDING)).build());

        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> this.processedJobs.stream().map(Job::id).collect(Collectors.toList()),
                containsInAnyOrder("submitted", "pending")
        );
    }
}