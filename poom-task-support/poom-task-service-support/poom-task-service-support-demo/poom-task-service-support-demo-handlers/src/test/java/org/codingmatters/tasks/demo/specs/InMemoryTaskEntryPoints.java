package org.codingmatters.tasks.demo.specs;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.specs.TaskEntryPointsUnderTest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Points d'entrée de tâches en mémoire, éprouvés par les règles de poom-task-service-support-specs : un dépôt de
 * tâches et un dépôt de journaux par compte, un job "demo"/"task" par tâche, rappels postés en HTTP sur l'URL reçue.
 */
public class InMemoryTaskEntryPoints implements TaskEntryPointsUnderTest {
    private final Map<String, TaskEntryPointAdapter> adapters = new ConcurrentHashMap<>();
    private final HttpClientWrapper http = OkHttpClientWrapper.build();

    @Override
    public TaskEntryPointAdapter adapter(String account) {
        return this.adapters.computeIfAbsent(account, this::newAdapter);
    }

    @Override
    public void reset() {
        this.adapters.clear();
    }

    private TaskEntryPointAdapter newAdapter(String account) {
        Repository<Task, PropertyQuery> tasks = InMemoryRepositoryWithPropertyQuery.validating(Task.class);
        Repository<TaskLog, PropertyQuery> logs = InMemoryRepositoryWithPropertyQuery.validating(TaskLog.class);
        return new TaskEntryPointAdapter() {
            @Override
            public Repository<Task, PropertyQuery> tasks() {
                return tasks;
            }

            @Override
            public Optional<Repository<TaskLog, PropertyQuery>> taskLogs() {
                return Optional.of(logs);
            }

            @Override
            public JobSpec jobSpecFor(Task task) {
                return new JobSpec("demo", "task", "http://tasks.demo/" + account + "/tasks");
            }

            @Override
            public String jobAccount() {
                return account;
            }

            @Override
            public Requester callbackRequester(String callbackUrl) {
                return new OkHttpRequesterFactory(http, () -> callbackUrl).create();
            }
        };
    }
}
