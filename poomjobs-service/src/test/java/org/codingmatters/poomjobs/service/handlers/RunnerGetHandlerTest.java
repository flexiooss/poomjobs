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
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
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
    private RunnerGetHandler handler = (RunnerGetHandler) new PoomjobsRunnerRegistryAPI(this.repository).handlers().runnerGetHandler();

    @Test
    public void log() throws Exception {
        assertThat(this.handler.log().getName(), is(RunnerGetHandler.class.getName()));
    }

    @Test
    public void repository() throws Exception {
        assertThat(this.handler.repository(), is(this.repository));
    }

    @Test
    public void entityId() throws Exception {
        assertThat(this.handler.entityId(RunnerGetRequest.builder().runnerId("12").build()), is("12"));
    }

    @Test
    public void whenEntityFoundCalled__thenReturnsStatus200() throws Exception {
        LocalDateTime created = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
        LocalDateTime lastPing = LocalDateTime.now().minus(1, ChronoUnit.MINUTES);

        Entity<RunnerValue> entity = this.repository.create(RunnerValue.builder()
                .competencies(Competencies.builder()
                        .names("n1", "n2")
                        .categories("c1", "c2")
                        .build())
                .timeToLive(12L)
                .callback("http://call.me/up")
                .runtime(Runtime.builder()
                        .created(created)
                        .lastPing(lastPing)
                        .status(Runtime.Status.RUNNING)
                        .build())
                .build());
        RunnerGetResponse response = this.handler.entityFound(entity);

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().payload().competencies().names(), contains("n1", "n2"));
        assertThat(response.status200().payload().competencies().categories(), contains("c1", "c2"));
        assertThat(response.status200().payload().ttl(), is(12L));
        assertThat(response.status200().payload().callback(), is("http://call.me/up"));
        assertThat(response.status200().payload().runtime().created(), is(created));
        assertThat(response.status200().payload().runtime().lastPing(), is(lastPing));
        assertThat(response.status200().payload().runtime().status(), is(org.codingmatters.poomjobs.api.types.runner.Runtime.Status.RUNNING));
    }

    @Test
    public void whenEntityNotFoundCalled__thenReturnsStatus404() throws Exception {
        RunnerGetResponse response = this.handler.entityNotFound("error-token");

        assertThat(response.status404(), is(notNullValue()));
        assertThat(
                response.status404().payload(),
                is(Error.builder()
                        .token("error-token")
                        .code(Error.Code.RUNNER_NOT_FOUND)
                        .description("no runner found with the given runner id")
                        .build())
        );
    }

    @Test
    public void whenUnexpectedErrorCalled__thenReturnsStatus500() throws Exception {
        RunnerGetResponse response = this.handler.unexpectedError("error-token");

        assertThat(response.status500(), is(notNullValue()));
        assertThat(
                response.status500().payload(),
                is(Error.builder()
                        .token("error-token")
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .description("unexpected error, see logs")
                        .build())
        );
    }
}