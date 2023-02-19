package org.codingmatters.tasks.support.handlers.tasks;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.tasks.api.TaskEntityGetRequest;
import org.codingmatters.tasks.api.TaskEntityGetResponse;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class RetrieveTaskTest {
    private final Repository<Task, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Task.class);

    private final RetrieveTask retrieveTask = new RetrieveTask(() -> new TaskEntryPointAdapter() {
        @Override
        public Repository<Task, PropertyQuery> tasks() {
            return repository;
        }

        @Override
        public JobCreationData jobFor(Task task) {
            return null;
        }

        @Override
        public String jobAccount() {
            return null;
        }
    });

    @Test
    public void givenRepositoryEmpty__whenRetrivingTask__then404() throws Exception {
        TaskEntityGetResponse response = this.retrieveTask.apply(TaskEntityGetRequest.builder().taskId("notask").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void givenRepositoryNotEmpty__whenRetrivingTaskWithWrongId__then404() throws Exception {
        this.repository.createWithId("task", Task.builder().id("task").build());

        TaskEntityGetResponse response = this.retrieveTask.apply(TaskEntityGetRequest.builder().taskId("notask").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void givenRepositoryNotEmpty__whenRetrivingTask__then200WithTask() throws Exception {
        this.repository.createWithId("task", Task.builder().id("task").build());

        TaskEntityGetResponse response = this.retrieveTask.apply(TaskEntityGetRequest.builder().taskId("task").build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));
        assertThat(response.status200().xEntityId(), is("task"));
        assertThat(response.status200().payload(), is(Task.builder().id("task").build()));
    }
}