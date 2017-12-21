package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.runners.RunnerValueCreation;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerCollectionPostRequest;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.RunnerData;
import org.codingmatters.poomjobs.api.types.runnerdata.Competencies;
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class RunnerCollectionPostHandlerTest {

    private Repository<RunnerValue, RunnerQuery> repository = RunnerRepository.createInMemory();
    private RunnerCollectionPostHandler handler = (RunnerCollectionPostHandler) new PoomjobsRunnerRegistryAPI(this.repository).handlers().runnerCollectionPostHandler();


    @Test
    public void log() throws Exception {
        assertThat(this.handler.log().getName(), is(RunnerCollectionPostHandler.class.getName()));
    }

    @Test
    public void repository() throws Exception {
        assertThat(this.handler.repository(null), is(this.repository));
    }

    @Test
    public void valueCreation() throws Exception {
        Change<RunnerValue> creation = this.handler.valueCreation(RunnerCollectionPostRequest.builder()
                .payload(RunnerData.builder()
                        .callback("http://call.me/up")
                        .ttl(12L)
                        .competencies(Competencies.builder()
                                .names("n")
                                .categories("c")
                                .build())
                        .build())
                .build());

        assertThat(creation, is(instanceOf(RunnerValueCreation.class)));
        assertThat(creation.currentValue(), is(nullValue()));
        assertThat(creation.newValue().callback(), is("http://call.me/up"));
        assertThat(creation.newValue().timeToLive(), is(12L));
        assertThat(creation.newValue().competencies().names(), contains("n"));
        assertThat(creation.newValue().competencies().categories(), contains("c"));
    }

    @Test
    public void whenEntityCreatedCalled__thenStatus201Returned() throws Exception {
        RunnerValueCreation creation = RunnerValueCreation.with(RunnerValue.builder().build());
        Entity<RunnerValue> entity = this.repository.create(creation.applied());

        RunnerCollectionPostResponse response = this.handler.entityCreated(creation, entity);

        assertThat(response.status201(), is(notNullValue()));
        assertThat(response.status201().location(), is("%API_PATH%/runners/" + entity.id()));
    }

    @Test
    public void whenInvalidCreationCalled__thenStatus400Returned() throws Exception {
        RunnerValueCreation creation = RunnerValueCreation.with(RunnerValue.builder().build());

        RunnerCollectionPostResponse response = this.handler.invalidCreation(creation, "error-token");

        assertThat(response.status400(), is(notNullValue()));
        assertThat(
                response.status400().payload(),
                is(Error.builder()
                        .token("error-token")
                        .code(Error.Code.ILLEGAL_RUNNER_SPEC)
                        .description(creation.validation().message())
                        .build())
        );
    }

    @Test
    public void whenUnexpectedErrorCalled__thenStatus500Returned() throws Exception {
        RunnerValueCreation creation = RunnerValueCreation.with(RunnerValue.builder().build());

        RunnerCollectionPostResponse response = this.handler.unexpectedError(creation, new RepositoryException("error"), "error-token");

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