package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourcePutRequest;
import org.codingmatters.poomjobs.api.JobResourcePutResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedJobRepository;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedRunnerRepository;
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
    private PoomjobsAPI api = new PoomjobsAPI(this.repository, new MockedRunnerRepository());

    @Test
    public void whenJobInRepository__willUpdateJob_andReturnStatus200() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.builder()
                .name("test")
                .status(Status.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .build());

        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.builder()
                .accountId("121212")
                .currentVersion(entity.version().toString())
                .jobId(entity.id())
                .payload(JobUpdateData.builder()
                        .build())
                .build());

        assertThat(response.status404(), is(nullValue()));
        assertThat(response.status500(), is(nullValue()));

        Job job = response.status200().payload();
        assertThat(job.version(), is(entity.version().add(BigInteger.ONE).toString()));

        assertThat(this.repository.retrieve(entity.id()).version(), is(BigInteger.valueOf(2)));
    }

    @Test
    public void whenJobNotInRepository__willReturnStatus404() throws Exception {
        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.builder()
                .accountId("121212")
                .currentVersion("2")
                .jobId("123456789")
                .payload(JobUpdateData.builder()
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
        JobResourcePutResponse response = new PoomjobsAPI(new MockedJobRepository(), new MockedRunnerRepository()).handlers().jobResourcePutHandler().apply(JobResourcePutRequest.builder()
                .accountId("121212")
                .currentVersion("2")
                .jobId("123456789")
                .payload(JobUpdateData.builder()
                        .build())
                .build());

        assertThat(response.status200(), is(nullValue()));
        assertThat(response.status404(), is(nullValue()));

        assertThat(response.status500().payload().code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(response.status500().payload().description(), is("unexpected error, see logs"));
        assertThat(response.status500().payload().token(), is(notNullValue()));
    }

    @Test
    public void whenChangeIsValidated__returnStatus200_andChangeRulesAreApplied() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.builder()
                .name("test")
                .status(Status.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .processing(Processing.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .build())
                .build());
        assertThat(entity.value().processing().started(), is(nullValue()));

        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.builder()
                .accountId("121212")
                .currentVersion(entity.version().toString())
                .jobId(entity.id())
                .payload(JobUpdateData.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.builder()
                                .run(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.Run.RUNNING)
                                .build())
                        .build())
                .build());

        entity = this.repository.retrieve(entity.id());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(entity.value().status().run(), is(Status.Run.RUNNING));
        assertThat(entity.value().processing().started(), is(notNullValue()));
    }

    @Test
    public void whenChangeIsNotValidated__returnsStatus400_withILLEGAL_JOB_CHANGEErrorCode() throws Exception {
        Entity<JobValue> job = this.repository.create(JobValue.builder()
                .name("test")
                .status(Status.builder()
                        .run(Status.Run.DONE)
                        .build())
                .build());

        JobResourcePutResponse response = this.api.handlers().jobResourcePutHandler().apply(JobResourcePutRequest.builder()
                .accountId("121212")
                .currentVersion(job.version().toString())
                .jobId(job.id())
                .payload(JobUpdateData.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.builder()
                                .run(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.Run.RUNNING)
                                .build())
                        .build())
                .build());

        assertThat(response.status400().payload().code(), is(Error.Code.ILLEGAL_JOB_CHANGE));
        assertThat(response.status400().payload().description(), is("cannot change a job when run status is DONE"));
        assertThat(response.status400().payload().token(), is(notNullValue()));
    }

}