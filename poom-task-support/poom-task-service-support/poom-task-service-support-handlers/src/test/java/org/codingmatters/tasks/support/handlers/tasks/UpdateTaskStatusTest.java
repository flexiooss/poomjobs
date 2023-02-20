package org.codingmatters.tasks.support.handlers.tasks;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.DateMatchers;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.test.TestRequesterFactory;
import org.codingmatters.tasks.api.TaskStatusChangesPostRequest;
import org.codingmatters.tasks.api.TaskStatusChangesPostResponse;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.junit.Test;

import java.util.Optional;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class UpdateTaskStatusTest {
    private final Repository<Task, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(Task.class);

    private final TestRequesterFactory callbackRequesterFactory = new TestRequesterFactory(() -> "http://call.me/back");

    private final UpdateTaskStatus retrieveTask = new UpdateTaskStatus(() -> new TaskEntryPointAdapter() {
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
    }, new JsonFactory());

    @Test
    public void whenTaskDoesntExists__then404() throws Exception {
        TaskStatusChangesPostResponse response = this.retrieveTask.apply(TaskStatusChangesPostRequest.builder()
                        .taskId("no-task").payload(TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(TaskStatusChange.Exit.FAILURE).build())
                .build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, git : " + response));
    }

    @Test
    public void givenTaskExist__whenTaskWasPending_andChangeToDoneFailure__thenTaskUpdatedWithNewStatus_andFinishedAtTimestamped() throws Exception {
        this.repository.createWithId("task", Task.builder().status(Status.builder().run(Status.Run.PENDING).build()).build());

        TaskStatusChangesPostResponse response = this.retrieveTask.apply(TaskStatusChangesPostRequest.builder()
                .taskId("task").payload(TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(TaskStatusChange.Exit.FAILURE).build())
                .build());

        assertThat(response.status201().payload(), is(TaskStatusChange.builder().run(TaskStatusChange.Run.DONE).exit(TaskStatusChange.Exit.FAILURE).build()));
        Task task = this.repository.retrieve("task").value();
        assertThat(task.status(), is(Status.builder().run(Status.Run.DONE).exit(Status.Exit.FAILURE).build()));
        assertThat(task.finishedAt(), is(around(UTC.now())));
    }

    @Test
    public void givenTaskExist__whenTaskWasPending_andChangeToRunning__thenTaskUpdatedWithNewStatus_andStartedAtTimestamped() throws Exception {
        this.repository.createWithId("task", Task.builder().status(Status.builder().run(Status.Run.PENDING).build()).build());

        TaskStatusChangesPostResponse response = this.retrieveTask.apply(TaskStatusChangesPostRequest.builder()
                .taskId("task").payload(TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build())
                .build());

        assertThat(response.status201().payload(), is(TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).exit(null).build()));
        Task task = this.repository.retrieve("task").value();
        assertThat(task.status(), is(Status.builder().run(Status.Run.RUNNING).exit(null).build()));
        assertThat(task.startedAt(), is(around(UTC.now())));
    }

    @Test
    public void givenTaskExist__whenTaskWasDone__thenBadRequest() throws Exception {
        this.repository.createWithId("task", Task.builder().status(Status.builder().run(Status.Run.DONE).build()).build());

        TaskStatusChangesPostResponse response = this.retrieveTask.apply(TaskStatusChangesPostRequest.builder()
                .taskId("task").payload(TaskStatusChange.builder().run(TaskStatusChange.Run.RUNNING).build())
                .build());

        response.opt().status400().orElseThrow(() -> new AssertionError("expected 400, git : " + response));
    }
}