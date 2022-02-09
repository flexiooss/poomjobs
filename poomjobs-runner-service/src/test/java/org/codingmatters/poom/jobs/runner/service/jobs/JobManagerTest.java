package org.codingmatters.poom.jobs.runner.service.jobs;

import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.jobs.runner.service.jobs.JobManager;
import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poomjobs.api.*;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.jobresourcepatchresponse.Status400;
import org.codingmatters.poomjobs.api.jobresourcepatchresponse.Status404;
import org.codingmatters.poomjobs.api.jobresourcepatchresponse.Status500;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIHandlersClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

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
            this.thrown.expectMessage(startsWith("Failed updating job, got response : JobResourcePatchResponse"));

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
}