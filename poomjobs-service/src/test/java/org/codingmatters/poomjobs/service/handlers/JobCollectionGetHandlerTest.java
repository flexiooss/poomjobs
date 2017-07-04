package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/29/17.
 */
public class JobCollectionGetHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();
    private PoomjobsAPI api = new PoomjobsAPI(this.repository);

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
}
