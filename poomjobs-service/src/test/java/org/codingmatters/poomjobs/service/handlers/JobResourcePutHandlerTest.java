package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourcePutRequest;
import org.codingmatters.poomjobs.api.JobResourcePutResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobData;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedJobRepository;
import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/20/17.
 */
public class JobResourcePutHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();
    private PoomjobsAPI api = new PoomjobsAPI(this.repository);

    @Test
    public void whenJobInRepository__willUpdateJob_andReturnStatus200() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.Builder.builder()
                .name("test")
                .status(Status.Builder.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .build());

        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.Builder.builder()
                .accountId("121212")
                .currentVersion(entity.version().toString())
                .jobId(entity.id())
                .payload(JobData.Builder.builder()
                        .name("changed")
                        .build())
                .build());

        assertThat(response.status404(), is(nullValue()));
        assertThat(response.status500(), is(nullValue()));

        Job job = response.status200().payload();
        assertThat(job.version(), is(entity.version().add(BigInteger.ONE).toString()));
        assertThat(job.name(), is("changed"));

        assertThat(this.repository.retrieve(entity.id()).version(), is(BigInteger.valueOf(2)));
        assertThat(this.repository.retrieve(entity.id()).value().name(), is("changed"));
    }

    @Test
    public void whenJobNotInRepository__willReturnStatus404() throws Exception {
        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.Builder.builder()
                .accountId("121212")
                .currentVersion("2")
                .jobId("123456789")
                .payload(JobData.Builder.builder()
                        .name("changed")
                        .build())
                .build());

        assertThat(response.status200(), is(nullValue()));
        assertThat(response.status500(), is(nullValue()));

        assertThat(response.status404().payload().code(), is(Error.Code.JOB_NOT_FOUND));
        assertThat(response.status404().payload().description(), is("no job found with the given jobId"));
        assertThat(response.status404().payload().token(), is(notNullValue()));
    }

    @Test
    public void whenUnexpectedRepositoryException__willReturnStatus500() throws Exception {
        JobResourcePutResponse response = new PoomjobsAPI(new MockedJobRepository()).handlers().jobResourcePutHandler().apply(JobResourcePutRequest.Builder.builder()
                .accountId("121212")
                .currentVersion("2")
                .jobId("123456789")
                .payload(JobData.Builder.builder()
                        .name("changed")
                        .build())
                .build());

        assertThat(response.status200(), is(nullValue()));
        assertThat(response.status404(), is(nullValue()));

        assertThat(response.status500().payload().code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(response.status500().payload().description(), is("unexpected error, see logs"));
        assertThat(response.status500().payload().token(), is(notNullValue()));
    }

    @Test
    public void whenRunStatusChangesToRUNNING__jobProcessingStartedDateIsUpdated() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.Builder.builder()
                .name("test")
                .status(Status.Builder.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .processing(Processing.Builder.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .build())
                .build());
        assertThat(entity.value().processing().started(), is(nullValue()));

        this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.Builder.builder()
                .accountId("121212")
                .currentVersion(entity.version().toString())
                .jobId(entity.id())
                .payload(JobData.Builder.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobdata.Status.Builder.builder()
                                .run(org.codingmatters.poomjobs.api.types.jobdata.Status.Run.RUNNING)
                                .build())
                        .build())
                .build());

        entity = this.repository.retrieve(entity.id());

        assertThat(entity.value().status().run(), is(Status.Run.RUNNING));
        assertThat(entity.value().processing().started(), is(notNullValue()));
    }

    @Test
    public void whenRunStatusChangesToDONE__jobProcessingFinishedDateIsUpdated() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.Builder.builder()
                .name("test")
                .status(Status.Builder.builder()
                        .run(Status.Run.RUNNING)
                        .build())
                .processing(Processing.Builder.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .started(LocalDateTime.now().minus(30, ChronoUnit.SECONDS))
                        .build())
                .build());
        assertThat(entity.value().processing().finished(), is(nullValue()));

        this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.Builder.builder()
                .accountId("121212")
                .currentVersion(entity.version().toString())
                .jobId(entity.id())
                .payload(JobData.Builder.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobdata.Status.Builder.builder()
                                .run(org.codingmatters.poomjobs.api.types.jobdata.Status.Run.DONE)
                                .build())
                        .build())
                .build());

        entity = this.repository.retrieve(entity.id());

        assertThat(entity.value().status().run(), is(Status.Run.DONE));
        assertThat(entity.value().processing().finished(), is(notNullValue()));
    }

    @Test
    public void whenRunStatusChangesFromDONEToRUNNING__returnsStatus400_withILLEGAL_JOB_CHANGEErrorCode() throws Exception {
        Entity<JobValue> job = this.repository.create(JobValue.Builder.builder()
                .name("test")
                .status(Status.Builder.builder()
                        .run(Status.Run.DONE)
                        .build())
                .build());

        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.Builder.builder()
                .accountId("121212")
                .currentVersion(job.version().toString())
                .jobId(job.id())
                .payload(JobData.Builder.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobdata.Status.Builder.builder()
                                .run(org.codingmatters.poomjobs.api.types.jobdata.Status.Run.RUNNING)
                                .build())
                        .build())
                .build());

        assertThat(response.status400().payload().code(), is(Error.Code.ILLEGAL_JOB_CHANGE));
        assertThat(response.status400().payload().description(), is("cannot change run status from DONE to RUNNING"));
        assertThat(response.status400().payload().token(), is(notNullValue()));
    }

    @Test
    public void whenRunStatusChangesFromDONEToPENDING__returnsStatus400_withILLEGAL_JOB_CHANGEErrorCode() throws Exception {
        Entity<JobValue> job = this.repository.create(JobValue.Builder.builder()
                .name("test")
                .status(Status.Builder.builder()
                        .run(Status.Run.DONE)
                        .build())
                .build());

        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.Builder.builder()
                .accountId("121212")
                .currentVersion(job.version().toString())
                .jobId(job.id())
                .payload(JobData.Builder.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobdata.Status.Builder.builder()
                                .run(org.codingmatters.poomjobs.api.types.jobdata.Status.Run.PENDING)
                                .build())
                        .build())
                .build());

        assertThat(response.status400().payload().code(), is(Error.Code.ILLEGAL_JOB_CHANGE));
        assertThat(response.status400().payload().description(), is("cannot change run status from DONE to PENDING"));
        assertThat(response.status400().payload().token(), is(notNullValue()));
    }
}