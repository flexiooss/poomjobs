package org.codingmatters.tasks.support.handlers.tasks;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.DateMatchers;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.test.TestRequesterFactory;
import org.codingmatters.tasks.api.TaskLogsPostRequest;
import org.codingmatters.tasks.api.TaskLogsPostResponse;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.api.types.TaskLogCreation;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class CreateTaskLogTest {

    private final Repository<Task, PropertyQuery> taskRepository = InMemoryRepositoryWithPropertyQuery.validating(Task.class);

    private final AtomicReference<Repository<TaskLog, PropertyQuery>> taskLogRepository = new AtomicReference<>(InMemoryRepositoryWithPropertyQuery.validating(TaskLog.class));

    private final TestRequesterFactory callbackRequesterFactory = new TestRequesterFactory(() -> "http://call.me/back");

    private final CreateTaskLog createTaskLog = new CreateTaskLog(() -> new TaskEntryPointAdapter() {
        @Override
        public Repository<Task, PropertyQuery> tasks() {
            return taskRepository;
        }

        @Override
        public Optional<Repository<TaskLog, PropertyQuery>> taskLogs() {
            return Optional.ofNullable(taskLogRepository.get());
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
    public void givenHasTaskLogRepository__whenNoTask__then404_andNoLogCreated() throws Exception {
        TaskLogsPostResponse response = this.createTaskLog.apply(TaskLogsPostRequest.builder()
                        .taskId("no-task")
                        .payload(TaskLogCreation.builder().log("log").level(TaskLogCreation.Level.INFO).build())
                .build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));

        assertThat(this.taskLogRepository.get().all(0L, 0L).total(), is(0L));
    }

    @Test
    public void givenNoTaskLogRepository__whenNoTask__then404_andNoLogCreated() throws Exception {
        this.taskLogRepository.set(null);
        this.taskRepository.createWithId("no-task", Task.builder().id("task").build());

        TaskLogsPostResponse response = this.createTaskLog.apply(TaskLogsPostRequest.builder()
                .taskId("task")
                .payload(TaskLogCreation.builder().log("log").level(TaskLogCreation.Level.INFO).build())
                .build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void givenHasTaskLogRepository__whenTask__then201_andLogCreated() throws Exception {
        this.taskRepository.createWithId("task", Task.builder().id("task").build());

        TaskLogsPostResponse response = this.createTaskLog.apply(TaskLogsPostRequest.builder()
                .taskId("task")
                .payload(TaskLogCreation.builder().log("log").level(TaskLogCreation.Level.INFO).build())
                .build());

        response.opt().status201().orElseThrow(() -> new AssertionError("expected 201, got " + response));

        assertThat(this.taskLogRepository.get().all(0L, 0L).total(), is(1L));

        TaskLog log = this.taskLogRepository.get().all(0L, 0L).valueList().get(0);
        assertThat(log.at(), is(around(UTC.now())));
        assertThat(log.level(), is(TaskLog.Level.INFO));
        assertThat(log.log(), is("log"));
        assertThat(log.taskId(), is("task"));
    }

    @Test
    public void givenNoTaskLogRepository__whenTask__then201() throws Exception {
        this.taskLogRepository.set(null);
        this.taskRepository.createWithId("task", Task.builder().id("task").build());

        TaskLogsPostResponse response = this.createTaskLog.apply(TaskLogsPostRequest.builder()
                .taskId("task")
                .payload(TaskLogCreation.builder().log("log").level(TaskLogCreation.Level.INFO).build())
                .build());

        response.opt().status201().orElseThrow(() -> new AssertionError("expected 201, got " + response));
    }
}