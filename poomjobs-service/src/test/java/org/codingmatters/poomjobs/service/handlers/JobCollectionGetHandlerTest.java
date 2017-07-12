package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedJobRepository;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedRunnerRepository;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/29/17.
 */
public class JobCollectionGetHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();
    private PoomjobsAPI api = new PoomjobsAPI(this.repository, new MockedRunnerRepository());

    @Test
    public void whenNoRangeRequested__ifRepositoryIsEmpty__thenReturnStatus200_andEmptyJobList() throws Exception {
        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .build());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Job 0-0/0"));
        assertThat(response.status200().acceptRange(), is("Job 100"));

        assertThat(response.status200().payload().size(), is(0));
    }

    @Test
    public void whenNoRangeRequested__ifRepositorySmallerThanDefaultRange__thenReturnStatus200_andCompleteJobList() throws Exception {
        Entity<JobValue> storedJob = this.repository.create(JobValue.builder()
                .category("test").name("test").status(Status.builder().run(Status.Run.PENDING).build())
                .build());
        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .build());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Job 0-0/1"));
        assertThat(response.status200().acceptRange(), is("Job 100"));

        assertThat(response.status200().payload().size(), is(1));
        assertThat(response.status200().payload().get(0).id(), is(storedJob.id()));
    }

    @Test
    public void whenNoRangeRequested__ifRepositoryLargerThanDefaultRange__thenReturnStatus206_andPartialJobList() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create(JobValue.builder()
                    .category("test").name("test").status(Status.builder().run(Status.Run.PENDING).build())
                    .build());
        }
        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .build());

        assertThat(response.status206(), is(notNullValue()));
        assertThat(response.status206().contentRange(), is("Job 0-99/150"));
        assertThat(response.status206().acceptRange(), is("Job 100"));

        assertThat(response.status206().payload().size(), is(100));
    }


    @Test
    public void whenRangeRequested__ifRepositoryIsEmpty__thenReturnStatus200_andEmptyJobList() throws Exception {
        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .build());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Job 0-0/0"));
        assertThat(response.status200().acceptRange(), is("Job 100"));

        assertThat(response.status200().payload().size(), is(0));
    }

    @Test
    public void whenRangeRequested__ifRangeIsLargerRepository__thenReturnStatus200_andCompleteJobList() throws Exception {
        Entity<JobValue> storedJob = this.repository.create(JobValue.builder()
                .category("test").name("test").status(Status.builder().run(Status.Run.PENDING).build())
                .build());
        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .range("0-10")
                .build());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Job 0-0/1"));
        assertThat(response.status200().acceptRange(), is("Job 100"));

        assertThat(response.status200().payload().size(), is(1));
        assertThat(response.status200().payload().get(0).id(), is(storedJob.id()));
    }

    @Test
    public void whenRangeRequested__ifRepositoryLargerThanRange__thenReturnStatus206_andPartialJobList() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create(JobValue.builder()
                    .category("test").name("test").status(Status.builder().run(Status.Run.PENDING).build())
                    .build());
        }

        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .range("0-9")
                .build());

        assertThat(response.status206(), is(notNullValue()));
        assertThat(response.status206().contentRange(), is("Job 0-9/150"));
        assertThat(response.status206().acceptRange(), is("Job 100"));

        assertThat(response.status206().payload().size(), is(10));
    }

    @Test
    public void whenRangeRequested__ifRangeStartsAfter0__thenReturnStatus206_andOffsettedPartialJobList() throws Exception {
        Entity<JobValue> startEntity = null;
        for(int i = 0 ; i < 150 ; i++) {
            Entity<JobValue> entity = this.repository.create(JobValue.builder()
                    .category("test").name("test").status(Status.builder().run(Status.Run.PENDING).build())
                    .build());
            if(i == 50) {
                startEntity = entity;
            }
        }

        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .range("50-99")
                .build());

        assertThat(response.status206(), is(notNullValue()));
        assertThat(response.status206().contentRange(), is("Job 50-99/150"));
        assertThat(response.status206().acceptRange(), is("Job 100"));

        assertThat(response.status206().payload().size(), is(50));
        assertThat(response.status206().payload().get(0).id(), is(startEntity.id()));
    }

    @Test
    public void whenRangeRequested__ifRangeIsInvalid__thenResturnStatus406() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create(JobValue.builder()
                    .category("test").name("test").status(Status.builder().run(Status.Run.PENDING).build())
                    .build());
        }

        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .range("9-0")
                .build());

        assertThat(response.status416(), is(notNullValue()));
        assertThat(response.status416().acceptRange(), is("Job 100"));
        assertThat(response.status416().contentRange(), is("Job */150"));
        assertThat(response.status416().payload().code(), is(Error.Code.ILLEGAL_RANGE_SPEC));
        assertThat(response.status416().payload().description(), is("start must be before end of range"));
        assertThat(response.status416().payload().token(), is(notNullValue()));
    }



    @Test
    public void whenUnexpectedRepositoryException__willReturnAStatus500() throws Exception {
        JobCollectionGetResponse response = new PoomjobsAPI(new MockedJobRepository(), new MockedRunnerRepository()).handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .build());

        assertThat(response.status500().payload().code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(response.status500().payload().description(), is("unexpected error, see logs"));
        assertThat(response.status500().payload().token(), is(notNullValue()));
    }

    @Test
    public void whenNameParameter__thenOnlyJobWithSuchNameAreReturned() throws Exception {
        this.repository.create(JobValue.builder()
                .name("J1").category("C1").status(Status.builder().run(Status.Run.PENDING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J2").category("C2").status(Status.builder().run(Status.Run.RUNNING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J3").category("C3").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J4").category("C4").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build())
                .build());

        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .name("J2")
                .build());

        assertThat(response.status200().payload().size(), is(1));
        assertThat(response.status200().payload().get(0).name(), is("J2"));
    }

    @Test
    public void whenCategoryParameter__thenOnlyJobWithSuchCategoryAreReturned() throws Exception {
        this.repository.create(JobValue.builder()
                .name("J1").category("C1").status(Status.builder().run(Status.Run.PENDING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J2").category("C2").status(Status.builder().run(Status.Run.RUNNING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J3").category("C3").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J4").category("C4").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build())
                .build());

        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .category("C3")
                .build());

        assertThat(response.status200().payload().size(), is(1));
        assertThat(response.status200().payload().get(0).category(), is("C3"));
    }

    @Test
    public void whenRunStatusParameter__thenOnlyJobWithSuchRunStatusAreReturned() throws Exception {
        this.repository.create(JobValue.builder()
                .name("J1").category("C1").status(Status.builder().run(Status.Run.PENDING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J2").category("C2").status(Status.builder().run(Status.Run.RUNNING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J3").category("C3").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J4").category("C4").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build())
                .build());

        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .runStatus("DONE")
                .build());

        assertThat(response.status200().payload().size(), is(2));
        assertThat(response.status200().payload().get(0).status().run(), is(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE));
        assertThat(response.status200().payload().get(1).status().run(), is(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE));
    }

    @Test
    public void whenRunExitParameter__thenOnlyJobWithSuchExitStatusAreReturned() throws Exception {
        this.repository.create(JobValue.builder()
                .name("J1").category("C1").status(Status.builder().run(Status.Run.PENDING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J2").category("C2").status(Status.builder().run(Status.Run.RUNNING).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J3").category("C3").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build())
                .build());
        this.repository.create(JobValue.builder()
                .name("J4").category("C4").status(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build())
                .build());

        JobCollectionGetResponse response = this.api.handlers().jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .exitStatus("SUCCESS")
                .build());

        assertThat(response.status200().payload().size(), is(1));
        assertThat(response.status200().payload().get(0).status().exit(), is(org.codingmatters.poomjobs.api.types.job.Status.Exit.SUCCESS));
    }
}
