package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.JobValueCreation;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.service.PoomjobsJobRegistryAPI;
import org.codingmatters.poomjobs.service.handlers.test.TestJobRepositoryListener;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;
import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by nelt on 6/23/17.
 */
public class JobCollectionPostHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();

    @Rule
    public TestJobRepositoryListener listener = new TestJobRepositoryListener();

    private JobCollectionPostHandler handler = (JobCollectionPostHandler) new PoomjobsJobRegistryAPI(this.repository, this.listener, null).handlers().jobCollectionPostHandler();

    @Test
    public void givenContextualizerNotNull__whenCreatingJob__thenContextualizerCalled_andContextSet() throws Exception {
        ObjectValue contextValue = ObjectValue.builder().property("test", PropertyValue.builder().stringValue(UUID.randomUUID().toString()).build()).build();

        JobCollectionPostResponse response = new PoomjobsJobRegistryAPI(this.repository, this.listener, request -> contextValue).handlers().jobCollectionPostHandler()
                .apply(JobCollectionPostRequest.builder()
                        .accountId("1212")
                        .payload(JobCreationData.builder()
                                .category("category")
                                .name("name")
                                .arguments("one", "two")
                                .build())
                        .build());


        String jobId = response.status201().location().substring("%API_PATH%/jobs/".length());

        assertThat(this.repository.retrieve(jobId).value().context(), is(contextValue));
    }

    @Test
    public void log() throws Exception {
        assertThat(this.handler.log().getName(), is(JobCollectionPostHandler.class.getName()));
    }

    @Test
    public void repository() throws Exception {
        assertThat(this.handler.repository(null), is(this.repository));
    }

    @Test
    public void valueCreation() throws Exception {
        Change<JobValue> creation = this.handler.valueCreation(JobCollectionPostRequest.builder()
                .accountId("1212")
                .payload(JobCreationData.builder()
                        .category("category")
                        .name("name")
                        .arguments("one", "two")
                        .build())
                .build());

        assertThat(creation, is(instanceOf(JobValueCreation.class)));
        assertThat(creation.currentValue(), is(nullValue()));
        assertThat(creation.newValue().name(), is("name"));
        assertThat(creation.newValue().category(), is("category"));
        assertThat(creation.newValue().arguments(), contains("one", "two"));
        assertThat(creation.newValue().accounting().accountId(), is("1212"));
    }

    @Test
    public void whenEntityCreatedCalled__thenStatus201Returned() throws Exception {
        JobValueCreation creation = JobValueCreation.with(JobValue.builder().build());
        Entity<JobValue> entity = this.repository.create(creation.applied());

        JobCollectionPostResponse response = this.handler.entityCreated(null, creation, entity);

        assertThat(response.status201(), is(notNullValue()));
        assertThat(response.status201().location(), is("%API_PATH%/jobs/" + entity.id()));
    }



    @Test
    public void whenEntityCreated__thenListenerIsNotified() throws Exception {
        JobValueCreation creation = JobValueCreation.with(JobValue.builder().build());
        Entity<JobValue> entity = this.repository.create(creation.applied());

        this.handler.entityCreated(null, creation, entity);

        assertThat(this.listener.created(), is(entity));
        assertThat(this.listener.updated(), is(nullValue()));
    }

    @Test
    public void whenInvalidCreationCalled__thenStatus400Returned() throws Exception {
        JobValueCreation creation = JobValueCreation.with(JobValue.builder().build());

        JobCollectionPostResponse response = this.handler.invalidCreation(creation, "error-token");

        assertThat(response.status400(), is(notNullValue()));
        assertThat(
                response.status400().payload(),
                is(Error.builder()
                        .token("error-token")
                        .code(Error.Code.ILLEGAL_JOB_SPEC)
                        .description(creation.validation().message())
                        .build())
        );
    }

    @Test
    public void whenUnexpectedErrorCalled__thenStatus500Returned() throws Exception {
        JobValueCreation creation = JobValueCreation.with(JobValue.builder().build());

        JobCollectionPostResponse response = this.handler.unexpectedError(creation, new RepositoryException("error"), "error-token");

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
