package org.codingmatters.tasks.support.handlers.tasks;

import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.DateMatchers;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.PoomjobsJobRegistryAPIHandlers;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIHandlersClient;
import org.codingmatters.tasks.api.TaskCollectionPostRequest;
import org.codingmatters.tasks.api.TaskCollectionPostResponse;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class CreateTaskTest {

    private final Repository<Task, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Task.class);


    private HandlerResource<JobCollectionPostRequest, JobCollectionPostResponse> jobCreationRequest = new HandlerResource<JobCollectionPostRequest, JobCollectionPostResponse>() {
        @Override
        protected JobCollectionPostResponse defaultResponse(JobCollectionPostRequest jobCollectionPostRequest) {
            return JobCollectionPostResponse.builder().status201(st -> st.xEntityId("created-job")).build();
        }
    };
    private final PoomjobsJobRegistryAPIClient jobsClient = new PoomjobsJobRegistryAPIHandlersClient(new PoomjobsJobRegistryAPIHandlers.Builder()
            .jobCollectionPostHandler(this.jobCreationRequest)
            .build(), Executors.newSingleThreadExecutor());


    private final CreateTask createTask = new CreateTask(() -> new TaskEntryPointAdapter() {
        @Override
        public Repository<Task, PropertyQuery> tasks() {
            return repository;
        }

        @Override
        public Optional<Repository<TaskLog, PropertyQuery>> taskLogs() {
            return Optional.empty();
        }

        @Override
        public JobCreationData jobFor(Task task) {
            return JobCreationData.builder().category("test").name("job").arguments("arg1", "arg2").build();
        }

        @Override
        public String jobAccount() {
            return "test-account";
        }
    }, this.jobsClient);

    @Test
    public void whenCreatingTaskWithParams__thenTaskCreatedFromRequest() throws Exception {
        TaskCollectionPostResponse response = this.createTask.apply(TaskCollectionPostRequest.builder()
                        .callbackUrl("http://call.me/back")
                        .payload(ObjectValue.builder().property("submitted", v -> v.stringValue("value")).build())
                .build());

        assertThat(this.repository.all(0L, 0L).total(), is(1L));
        assertThat(this.repository.retrieve(response.status201().xEntityId()).value(), is(response.status201().payload()));

        Task task = this.repository.retrieve(response.status201().xEntityId()).value();
        assertThat(task.id(), is(response.status201().xEntityId()));
        assertThat(task.createdAt(), is(around(UTC.now())));
        assertThat(task.startedAt(), is(nullValue()));
        assertThat(task.finishedAt(), is(nullValue()));
        assertThat(task.status(), is(Status.builder().run(Status.Run.PENDING).build()));
        assertThat(task.callbackUrl(), is("http://call.me/back"));
        assertThat(task.params(), is(ObjectValue.builder().property("submitted", v -> v.stringValue("value")).build()));
        assertThat(task.results(), is(nullValue()));
    }

    @Test
    public void givenJobCreationSucceeds__whenCreatingTaskWithParams__thenJobSubmitted() throws Exception {
        this.createTask.apply(TaskCollectionPostRequest.builder()
                        .callbackUrl("http://call.me/back")
                        .payload(ObjectValue.builder().property("submitted", v -> v.stringValue("value")).build())
                .build());

        assertThat(this.jobCreationRequest.lastRequest().accountId(), is("test-account"));
        assertThat(this.jobCreationRequest.lastRequest().payload(), is(JobCreationData.builder().category("test").name("job").arguments("arg1", "arg2").build()));
    }

    @Test
    public void givenJobCreationFails__whenCreatingTaskWithParams__thenError500_andNoTaskStored() throws Exception {
        this.jobCreationRequest.nextResponse(req -> JobCollectionPostResponse.builder().status500(st -> st.payload(e -> e.token("tok"))).build());

        TaskCollectionPostResponse response = this.createTask.apply(TaskCollectionPostRequest.builder()
                        .callbackUrl("http://call.me/back")
                        .payload(ObjectValue.builder().property("submitted", v -> v.stringValue("value")).build())
                .build());

        assertThat(response.status500(), is(notNullValue()));

        assertThat(this.repository.all(0L, 0L).total(), is(1L));

        Task task = this.repository.all(0L, 0L).valueList().get(0);
        assertThat(task.status().run(), is(Status.Run.DONE));
        assertThat(task.status().exit(), is(Status.Exit.FAILURE));
        assertThat(task.createdAt(), is(around(UTC.now())));
        assertThat(task.startedAt(), is(nullValue()));
        assertThat(task.finishedAt(), is(around(UTC.now())));

        assertThat(task.callbackUrl(), is("http://call.me/back"));
        assertThat(task.params(), is(ObjectValue.builder().property("submitted", v -> v.stringValue("value")).build()));
        assertThat(task.results(), is(nullValue()));
    }
}