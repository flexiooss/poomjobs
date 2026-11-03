package org.codingmatters.tasks.support.handlers.tasks;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.tasks.api.TaskLogsGetRequest;
import org.codingmatters.tasks.api.TaskLogsGetResponse;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BrowseTaskLogsTest {
    private final Repository<Task, PropertyQuery> taskRepository = InMemoryRepositoryWithPropertyQuery.validating(Task.class);
    private final Repository<TaskLog, PropertyQuery> taskLogRepository = InMemoryRepositoryWithPropertyQuery.validating(TaskLog.class);

    private final BrowseTaskLogs browseTaskLogs = new BrowseTaskLogs(() -> new TaskEntryPointAdapter() {
        @Override
        public Repository<Task, PropertyQuery> tasks() {
            return taskRepository;
        }

        @Override
        public Optional<Repository<TaskLog, PropertyQuery>> taskLogs() {
            return Optional.of(taskLogRepository);
        }

        @Override
        public JobSpec jobSpecFor(Task task) {
            return null;
        }

        @Override
        public String jobAccount() {
            return null;
        }

        @Override
        public Requester callbackRequester(String callbackUrl) {
            return null;
        }
    }, 100, new JsonFactory());

    @Test
    public void whenNoTask__then404() throws Exception {
        TaskLogsGetResponse response = this.browseTaskLogs.apply(TaskLogsGetRequest.builder().taskId("no-task").build());

        response.opt().status404().orElseThrow(() -> new AssertionError("expected 404, got " + response));
    }

    @Test
    public void whenTask__then200_withTaskLogs() throws Exception {
        this.taskRepository.createWithId("task", Task.builder().id("task").build());
        this.taskLogRepository.create(TaskLog.builder().taskId("task").at(UTC.now()).level(TaskLog.Level.INFO).log("mine").build());
        this.taskLogRepository.create(TaskLog.builder().taskId("other").at(UTC.now()).level(TaskLog.Level.INFO).log("other's").build());

        TaskLogsGetResponse response = this.browseTaskLogs.apply(TaskLogsGetRequest.builder().taskId("task").build());

        assertThat(response.opt().status200().payload().get().stream().map(TaskLog::log).toList(), contains("mine"));
    }
}
