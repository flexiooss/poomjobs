package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerGetRequest;
import org.codingmatters.poomjobs.api.RunnerGetResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedJobRepository;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedRunnerRepository;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/18/17.
 */
public class RunnerGetHandlerTest {

    private Repository<RunnerValue, RunnerQuery> repository = RunnerRepository.createInMemory();
    private PoomjobsAPI api = new PoomjobsAPI(new MockedJobRepository(), this.repository);

    @Test
    public void whenInRepository__willReturnAStatus200() throws Exception {
        Entity<RunnerValue> entity = this.repository.create(RunnerValue.builder()
                .competencies(Competencies.builder()
                        .names("n1", "n2")
                        .categories("c1", "c2")
                        .build())
                .timeToLive(12L)
                .callback("http://call.me/up")
                .runtime(Runtime.builder()
                        .created(LocalDateTime.now().minus(1, ChronoUnit.DAYS))
                        .lastPing(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .status(Runtime.Status.RUNNING)
                        .build())
                .build());

        RunnerGetResponse response = this.api.handlers().runnerGetHandler().apply(RunnerGetRequest.builder()
                .runnerId(entity.id())
                .build());


        assertThat(response.status404(), is(nullValue()));
        assertThat(response.status500(), is(nullValue()));

        Runner runner = response.status200().payload();

        assertThat(runner.id(), is(entity.id()));
        assertThat(runner.callback(), is("http://call.me/up"));
        assertThat(runner.ttl(), is(12L));
        assertThat(runner.competencies().names().toArray(), arrayContaining("n1", "n2"));
        assertThat(runner.competencies().categories().toArray(), arrayContaining("c1", "c2"));
        assertThat(runner.runtime().status(), is(org.codingmatters.poomjobs.api.types.runner.Runtime.Status.RUNNING));
        assertThat(runner.runtime().created(), is(notNullValue()));
        assertThat(runner.runtime().lastPing(), is(notNullValue()));

    }

    @Test
    public void whenNotInRepository__willReturnAStatus404() throws Exception {
        RunnerGetResponse response = this.api.handlers().runnerGetHandler().apply(RunnerGetRequest.builder()
                .runnerId("not in repo")
                .build());

        assertThat(response.status200(), is(nullValue()));
        assertThat(response.status500(), is(nullValue()));

        assertThat(response.status404().payload().code(), is(Error.Code.RUNNER_NOT_FOUND));
        assertThat(response.status404().payload().description(), is("no runner found with the given runner id"));
        assertThat(response.status404().payload().token(), is(notNullValue()));
    }

    @Test
    public void whenUnexpectedRepositoryException__willReturnAStatus500() throws Exception {
        RunnerGetResponse response = new PoomjobsAPI(new MockedJobRepository(), new MockedRunnerRepository()).handlers().runnerGetHandler().apply(RunnerGetRequest.builder()
                .runnerId("12")
                .build());

        assertThat(response.status200(), is(nullValue()));
        assertThat(response.status404(), is(nullValue()));

        assertThat(response.status500().payload().code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(response.status500().payload().description(), is("unexpected error, see logs"));
        assertThat(response.status500().payload().token(), is(notNullValue()));
    }
}