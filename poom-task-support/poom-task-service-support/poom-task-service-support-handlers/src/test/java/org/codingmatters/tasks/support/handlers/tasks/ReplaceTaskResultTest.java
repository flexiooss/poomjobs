package org.codingmatters.tasks.support.handlers.tasks;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.test.TestRequesterFactory;
import org.codingmatters.tasks.api.TaskResultsPutRequest;
import org.codingmatters.tasks.api.TaskResultsPutResponse;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class ReplaceTaskResultTest {
    private final Repository<Task, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Task.class);

    private final TestRequesterFactory callbackRequesterFactory = new TestRequesterFactory(() -> "http://call.me/back");

    private final ReplaceTaskResult replaceTaskResult = new ReplaceTaskResult(() -> new TaskEntryPointAdapter() {
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
            return null;
        }

        @Override
        public String jobAccount() {
            return null;
        }

        @Override
        public Requester callbackRequester(String callbackUrl) {
            return callbackRequesterFactory.create();
        }
    });

    @Test
    public void whenNoTask__then404() throws Exception {
        TaskResultsPutResponse response = this.replaceTaskResult.apply(TaskResultsPutRequest.builder().taskId("no-task").payload(ObjectValue.builder().property("the", v -> v.stringValue("result")).build()).build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expoected 404, got " + response));
    }

    @Test
    public void whenTask__then200_andResultUpdated() throws Exception {
        this.repository.createWithId("task", Task.builder().id("task").build());
        TaskResultsPutResponse response = this.replaceTaskResult.apply(TaskResultsPutRequest.builder().taskId("task").payload(ObjectValue.builder().property("the", v -> v.stringValue("result")).build()).build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expoected 200, got " + response));

        assertThat(this.repository.retrieve("task").value(), is(Task.builder()
                .id("task")
                .results(ObjectValue.builder().property("the", v -> v.stringValue("result")).build())
                .build()));
    }
}