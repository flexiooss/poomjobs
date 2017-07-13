package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerCollectionGetRequest;
import org.codingmatters.poomjobs.api.RunnerCollectionGetResponse;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedJobRepository;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/12/17.
 */
public class RunnerCollectionGetHandlerTest {
    private Repository<RunnerValue, RunnerQuery> repository = RunnerRepository.createInMemory();
    private PoomjobsAPI api = new PoomjobsAPI(new MockedJobRepository(), this.repository);


    @Test
    public void whenNoRangeRequested__ifRepositoryIsEmpty__thenReturnStatus200_andEmptyJobList() throws Exception {
        RunnerCollectionGetResponse response = this.api.handlers().runnerCollectionGetHandler().apply(RunnerCollectionGetRequest.builder()
                .build());
        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Runner 0-0/0"));
        assertThat(response.status200().acceptRange(), is("Runner 100"));

        assertThat(response.status200().payload().size(), is(0));
    }


    @Test
    public void whenNoRangeRequested__ifRepositorySmallerThanDefaultRange__thenReturnStatus200_andCompleteJobList() throws Exception {
        Entity<RunnerValue> stored = this.repository.create(this.createRunnerBuilder().build());
        RunnerCollectionGetResponse response = this.api.handlers().runnerCollectionGetHandler().apply(RunnerCollectionGetRequest.builder()
                .build());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Runner 0-0/1"));
        assertThat(response.status200().acceptRange(), is("Runner 100"));

        assertThat(response.status200().payload().size(), is(1));
        assertThat(response.status200().payload().get(0).id(), is(stored.id()));
    }

    @Test
    public void whenNoRangeRequested__ifRepositoryLargerThanDefaultRange__thenReturnStatus206_andPartialJobList() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create(this.createRunnerBuilder().build());
        }
        RunnerCollectionGetResponse response = this.api.handlers().runnerCollectionGetHandler().apply(RunnerCollectionGetRequest.builder()
                .build());

        assertThat(response.status206(), is(notNullValue()));
        assertThat(response.status206().contentRange(), is("Runner 0-99/150"));
        assertThat(response.status206().acceptRange(), is("Runner 100"));

        assertThat(response.status206().payload().size(), is(100));
    }


    @Test
    public void whenRangeRequested__ifRepositoryIsEmpty__thenReturnStatus200_andEmptyJobList() throws Exception {
        RunnerCollectionGetResponse response = this.api.handlers().runnerCollectionGetHandler().apply(RunnerCollectionGetRequest.builder()
                .build());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Runner 0-0/0"));
        assertThat(response.status200().acceptRange(), is("Runner 100"));

        assertThat(response.status200().payload().size(), is(0));
    }

    @Test
    public void whenRangeRequested__ifRangeIsLargerRepository__thenReturnStatus200_andCompleteJobList() throws Exception {
        Entity<RunnerValue> stored = this.repository.create(this.createRunnerBuilder().build());
        RunnerCollectionGetResponse response = this.api.handlers().runnerCollectionGetHandler().apply(RunnerCollectionGetRequest.builder()
                .range("0-10")
                .build());

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().contentRange(), is("Runner 0-0/1"));
        assertThat(response.status200().acceptRange(), is("Runner 100"));

        assertThat(response.status200().payload().size(), is(1));
        assertThat(response.status200().payload().get(0).id(), is(stored.id()));
    }

    @Test
    public void whenRangeRequested__ifRepositoryLargerThanRange__thenReturnStatus206_andPartialJobList() throws Exception {
        for(int i = 0 ; i < 150 ; i++) {
            this.repository.create(this.createRunnerBuilder().build());
        }

        RunnerCollectionGetResponse response = this.api.handlers().runnerCollectionGetHandler().apply(RunnerCollectionGetRequest.builder()
                .range("0-9")
                .build());

        assertThat(response.status206(), is(notNullValue()));
        assertThat(response.status206().contentRange(), is("Runner 0-9/150"));
        assertThat(response.status206().acceptRange(), is("Runner 100"));

        assertThat(response.status206().payload().size(), is(10));
    }



    private RunnerValue.Builder createRunnerBuilder() {
        return RunnerValue.builder()
                .callback("http://call.me/up")
                .timeToLive(1200L)
                .competencies(Competencies.builder()
                        .categories("test").names("test")
                        .build())
                .runtime(Runtime.builder()
                        .status(Runtime.Status.IDLE)
                        .created(LocalDateTime.now().minus(1, ChronoUnit.HOURS))
                        .lastPing(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .build());
    }

}