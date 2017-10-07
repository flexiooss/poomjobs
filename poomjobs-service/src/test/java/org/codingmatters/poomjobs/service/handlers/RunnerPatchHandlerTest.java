package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.Validation;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerPatchRequest;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
import org.codingmatters.poomjobs.service.RunnerEntityTransformation;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class RunnerPatchHandlerTest {

    private Repository<RunnerValue, RunnerQuery> repository = RunnerRepository.createInMemory();
    private RunnerPatchHandler handler = (RunnerPatchHandler) new PoomjobsRunnerRegistryAPI(this.repository).handlers().runnerPatchHandler();

    @Test
    public void log() throws Exception {
        assertThat(this.handler.log().getName(), is(RunnerPatchHandler.class.getName()));
    }

    @Test
    public void repository() throws Exception {
        assertThat(this.handler.repository(), is(this.repository));
    }

    @Test
    public void entityId() throws Exception {
        assertThat(this.handler.entityId(RunnerPatchRequest.builder().runnerId("12").build()), is("12"));
    }

    @Test
    public void valueUpdate() throws Exception {
        Entity<RunnerValue> entity = this.repository.create(RunnerValue.builder()
                .callback("http://call.me/up")
                .runtime(Runtime.builder()
                        .created(LocalDateTime.now().minus(1, ChronoUnit.HOURS))
                        .lastPing(LocalDateTime.now().minus(1, ChronoUnit.HOURS))
                        .status(Runtime.Status.IDLE)
                        .build())
                .build());
        Change<RunnerValue> update = this.handler.valueUpdate(RunnerPatchRequest.builder()
                        .runnerId("12")
                        .payload(RunnerStatusData.builder()
                                .status(RunnerStatusData.Status.RUNNING)
                                .build())
                        .build(),
                entity);

        assertThat(update.currentValue(), is(entity.value()));
        assertThat(update.newValue().runtime().status(), is(Runtime.Status.RUNNING));
        assertThat(update.newValue().runtime().lastPing(), is(greaterThan(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))));
    }

    @Test
    public void whenEntityUpdatedCalled__thenStatus200Returned() throws Exception {
        Entity<RunnerValue> entity = this.repository.create(RunnerValue.builder().build());
        RunnerPatchResponse response = this.handler.entityUpdated(entity);

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().payload(), is(RunnerEntityTransformation.transform(entity).asRunner()));
    }

    @Test
    public void whenInvalidUpdateCalled__thenStatus400Returned() throws Exception {
        RunnerPatchResponse response = this.handler.invalidUpdate(this.createChange(false, "invalid update", null, null), "error-token");

        assertThat(response.status400(), is(notNullValue()));
        assertThat(response.status400().payload().code(), is(Error.Code.ILLEGAL_RUNNER_STATUS_CHANGE));
        assertThat(response.status400().payload().token(), is("error-token"));
        assertThat(response.status400().payload().description(), is("invalid update"));
    }

    @Test
    public void whenEntityNotFoundCalled__thenStatus404Returned() throws Exception {
        RunnerPatchResponse response = this.handler.entityNotFound("error-token");

        assertThat(response.status404(), is(notNullValue()));
        assertThat(response.status404().payload().code(), is(Error.Code.RUNNER_NOT_FOUND));
        assertThat(response.status404().payload().token(), is("error-token"));
        assertThat(response.status404().payload().description(), is("no runner found with the given runner-id"));
    }

    @Test
    public void whenUnexpectedErrorCalled__theStatus500Returned() throws Exception {
        RunnerPatchResponse response = this.handler.unexpectedError(new RepositoryException("erro"), "error-token");

        assertThat(response.status500(), is(notNullValue()));
        assertThat(response.status500().payload().code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(response.status500().payload().token(), is("error-token"));
        assertThat(response.status500().payload().description(), is("unexpected error, see logs"));
    }

    private Change<RunnerValue> createChange(boolean valid, String message, RunnerValue currentValue, RunnerValue newValue) {
        return new Change<RunnerValue>(currentValue, newValue) {
            @Override
            protected Validation validate() {
                return new Validation(valid, message);
            }

            @Override
            public RunnerValue applied() {
                return null;
            }
        };
    }
}