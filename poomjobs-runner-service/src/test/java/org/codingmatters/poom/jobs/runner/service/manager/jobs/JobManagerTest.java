package org.codingmatters.poom.jobs.runner.service.manager.jobs;

import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.jobs.runner.service.manager.flow.JobProcessorRunner;
import org.codingmatters.poomjobs.api.*;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status206;
import org.codingmatters.poomjobs.api.jobresourcepatchresponse.Status400;
import org.codingmatters.poomjobs.api.jobresourcepatchresponse.Status404;
import org.codingmatters.poomjobs.api.jobresourcepatchresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIHandlersClient;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private HandlerResource<JobResourcePatchRequest, JobResourcePatchResponse> jobPatch = new HandlerResource<JobResourcePatchRequest, JobResourcePatchResponse>() {
        @Override
        protected JobResourcePatchResponse defaultResponse(JobResourcePatchRequest request) {
            return JobResourcePatchResponse.builder()
                    .status200(builder -> builder.payload(Job.builder().id("patched-job").build()))
                    .build();
        }
    };
    private HandlerResource<JobCollectionGetRequest, JobCollectionGetResponse> jobsGet = new HandlerResource<JobCollectionGetRequest, JobCollectionGetResponse>() {
        @Override
        protected JobCollectionGetResponse defaultResponse(JobCollectionGetRequest jobCollectionGetRequest) {
            return JobCollectionGetResponse.builder().status200(Status200.builder().contentRange("Job 0-0/0").acceptRange("Job 100").payload(new Job[0]).build()).build();
        }
    };
    private PoomjobsJobRegistryAPIClient apiClient = new PoomjobsJobRegistryAPIHandlersClient(
            new PoomjobsJobRegistryAPIHandlers.Builder()
                    .jobResourcePatchHandler(this.jobPatch)
                    .jobCollectionGetHandler(this.jobsGet)
                    .build(),
            Executors.newSingleThreadExecutor()
    );
    private final JobManager manager = new JobManager(this.apiClient, "account", "test-category", new String [] {"job1", "job2"});

    @Test
    public void givenUpdatingJob__whenJobPatchSucceeds__thenJobPatchCalled_andJobUpdateDataTakenFromJob() throws Exception {
        this.manager.update(Job.builder()
                .id("job-id")
                .version("version")
                .result("job result")
                .status(builder -> builder
                        .run(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE)
                        .exit(org.codingmatters.poomjobs.api.types.job.Status.Exit.SUCCESS))
                .build());

        assertThat(this.jobPatch.lastRequest().jobId(), is("job-id"));
        assertThat(this.jobPatch.lastRequest().accountId(), is("account"));
        assertThat(this.jobPatch.lastRequest().currentVersion(), is("version"));
        assertThat(this.jobPatch.lastRequest().payload(), is(JobUpdateData.builder()
                .status(Status.builder()
                        .run(Status.Run.DONE)
                        .exit(Status.Exit.SUCCESS)
                        .build())
                .result("job result")
                .build()));
    }

    @Test
    public void givenUpdatingJob__whenPatchFails__thenRuntimeExceptionWithLogTokenMessage() throws Exception {
        List<JobResourcePatchResponse> responses = Arrays.asList(
                JobResourcePatchResponse.builder().status400(Status400.builder().build()).build(),
                JobResourcePatchResponse.builder().status404(Status404.builder().build()).build(),
                JobResourcePatchResponse.builder().status500(Status500.builder().build()).build()
        );
        for (JobResourcePatchResponse response : responses) {
            this.jobPatch.nextResponse(request -> response);

            this.thrown.expect(JobProcessorRunner.JobUpdateFailure.class);
            this.thrown.expectMessage(startsWith("Unrecoverable error updating job status. See logs with token : "));

            this.manager.update(Job.builder()
                    .id("job-id")
                    .accounting(builder -> builder.accountId("account"))
                    .version("version")
                    .result("job result")
                    .status(builder -> builder
                            .run(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE)
                            .exit(org.codingmatters.poomjobs.api.types.job.Status.Exit.SUCCESS))
                    .build());
        }
    }

    @Test
    public void whenGettingNextJob__thenJobListRequested_andNoJobPatched() throws Exception {
        this.manager.nextJob();

        assertThat(this.jobsGet.lastRequest().accountId(), is("account"));
        assertThat(this.jobsGet.lastRequest().category(), is("test-category"));
        assertThat(this.jobsGet.lastRequest().names(), contains("job1", "job2"));
        assertThat(this.jobsGet.lastRequest().runStatus(), is(org.codingmatters.poomjobs.api.types.job.Status.Run.PENDING.name()));
        assertThat(this.jobsGet.lastRequest().range(), is("0-9"));
        assertThat(this.jobPatch.lastRequest(), is(nullValue()));
    }

    @Test
    public void givenGettingNextJob__whenJobListEmpty__thenReturnsNull_andNoJobPatched() throws Exception {
        assertThat(this.manager.nextJob(), is(nullValue()));
        assertThat(this.jobPatch.lastRequest(), is(nullValue()));
    }

    @Test
    public void givenGettingNextJob__whenJobListError__thenReturnsNull_andNoJobPatched() throws Exception {
        this.jobsGet.nextResponse(request -> JobCollectionGetResponse.builder()
                .status500(status -> status.payload(error -> error
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token("12")
                        .description("failed getting jobs from test")
                )).build()
        );

        assertThat(this.manager.nextJob(), is(nullValue()));
        assertThat(this.jobPatch.lastRequest(), is(nullValue()));
    }

    @Test
    public void givenGettingNextJob__whenJobReturnedAs200__thenFirstJobsChangesStatusToRunning_andPatchedJobReturned() throws Exception {
        this.jobsGet.nextResponse(request -> JobCollectionGetResponse.builder()
                .status200(Status200.builder().contentRange("Job 0-0/1").acceptRange("Job 100").payload(
                        Job.builder().id("pending-job").build()
                ).build()).build()
        );

        assertThat(this.manager.nextJob().id(), is("patched-job"));

        assertThat(this.jobPatch.lastRequest().accountId(), is("account"));
        assertThat(this.jobPatch.lastRequest().accountId(), is("account"));
        assertThat(this.jobPatch.lastRequest().jobId(), is("pending-job"));
        assertThat(this.jobPatch.lastRequest().payload(), is(JobUpdateData.builder()
                .status(Status.builder().run(Status.Run.RUNNING).build())
                .build()));
    }

    @Test
    public void givenGettingNextJob__whenJobReturnedAs206__thenFirstJobsChangesStatusToRunning_andPatchedJobReturned() throws Exception {
        this.jobsGet.nextResponse(request -> JobCollectionGetResponse.builder()
                .status206(Status206.builder().contentRange("Job 0-0/1").acceptRange("Job 100").payload(
                        Job.builder().id("pending-job").build()
                ).build()).build()
        );

        assertThat(this.manager.nextJob().id(), is("patched-job"));

        assertThat(this.jobPatch.lastRequest().accountId(), is("account"));
        assertThat(this.jobPatch.lastRequest().accountId(), is("account"));
        assertThat(this.jobPatch.lastRequest().jobId(), is("pending-job"));
        assertThat(this.jobPatch.lastRequest().payload(), is(JobUpdateData.builder()
                .status(Status.builder().run(Status.Run.RUNNING).build())
                .build()));
    }

    @Test
    public void givenGettingNextJob__whenJobListQueryFails__thenReturnsNull_andNoJobPatched() throws Exception {
        AtomicBoolean firstPage = new AtomicBoolean(true);
        this.jobsGet.nextResponse(request -> {
                    if(firstPage.getAndSet(false)) {
                        return JobCollectionGetResponse.builder()
                                .status200(Status200.builder().contentRange("Job 0-0/1").acceptRange("Job 100").payload(
                                        Job.builder().id("pending-job").build()
                                ).build()).build();
                    } else {
                        return JobCollectionGetResponse.builder()
                                .status200(Status200.builder().contentRange("Job 0-0/0").acceptRange("Job 100").payload().build()).build();
                    }
                }
        );
        this.jobPatch.nextResponse(request -> JobResourcePatchResponse.builder().status500(status -> status.payload(error -> error
                .code(Error.Code.UNEXPECTED_ERROR)
                .token("12")
                .description("failed in test")
        )).build());

        assertThat(this.manager.nextJob(), is(nullValue()));
        assertThat(this.jobPatch.lastRequest(), is(notNullValue()));
    }

    @Test
    public void givenGettingNextJob__whenJobsOnFirstPageFailsToPatch_andJobOnSecondPageSucceedToPatch__thenJobOnSecondPageReturned() throws Exception {
        AtomicInteger page = new AtomicInteger(0);
        this.jobsGet.nextResponse(request -> {
            if(page.incrementAndGet() == 1) {
                return JobCollectionGetResponse.builder()
                        .status200(status -> status.contentRange("Job 0-0/1").payload(
                                Job.builder().id("unreservable").build()
                        ))
                        .build();
            } else {
                return JobCollectionGetResponse.builder()
                        .status200(status -> status.contentRange("Job 0-0/1").payload(
                                Job.builder().id("reservable").build()
                        ))
                        .build();
            }
        });
        this.jobPatch.nextResponse(request -> {
            if(request.jobId().equals("reservable")) {
                return JobResourcePatchResponse.builder().status200(status -> status.payload(Job.builder().id(request.jobId()).build())).build();
            } else {
                return JobResourcePatchResponse.builder().status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_JOB_CHANGE))).build();
            }
        });

        Job actual = this.manager.nextJob();

        assertThat(page.get(), is(2));
        assertThat(actual.id(), is("reservable"));
    }


    @Test
    public void givenGettingNextJob__whenJobsOnTwoFirstPagesFailsToPatch_andThirdPageIsEmpty__thenNoJobReturned() throws Exception {
        AtomicInteger page = new AtomicInteger(0);
        this.jobsGet.nextResponse(request -> {
            if(page.incrementAndGet() <= 2) {
                return JobCollectionGetResponse.builder()
                        .status200(status -> status.contentRange("Job 0-0/1").payload(
                                Job.builder().id("unreservable").build()
                        ))
                        .build();
            } else {
                return JobCollectionGetResponse.builder()
                        .status200(status -> status.contentRange("Job 0-0/0").payload())
                        .build();
            }
        });
        this.jobPatch.nextResponse(request -> {
            if(request.jobId().equals("reservable")) {
                return JobResourcePatchResponse.builder().status200(status -> status.payload(Job.builder().id(request.jobId()).build())).build();
            } else {
                return JobResourcePatchResponse.builder().status400(status -> status.payload(error -> error.code(Error.Code.ILLEGAL_JOB_CHANGE))).build();
            }
        });

        Job actual = this.manager.nextJob();

        assertThat(page.get(), is(3));
        assertThat(actual, is(nullValue()));
    }
}