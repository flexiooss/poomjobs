package org.codingmatters.tasks.support.jobs.notifier;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.tasks.api.*;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLogCreation;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.client.TaskApiClient;
import org.codingmatters.tasks.support.jobs.ExtendedTaskNotifier;
import org.codingmatters.value.objects.values.ObjectValue;

import java.io.IOException;

public class ClientTaskNotifier implements ExtendedTaskNotifier {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ClientTaskNotifier.class);

    private final TaskApiClient taskClient;
    private final Task task;

    public ClientTaskNotifier(TaskApiClient taskClient, Task task) {
        this.taskClient = taskClient;
        this.task = task;
    }

    @Override
    public void updateRunStatus(TaskStatusChange.Run status) {
        this.statusChange(status, null);
    }

    @Override
    public void success(ObjectValue result) {
        log.info("task succeeded wih result {} - {}", result, this.task);
        this.result(result);
        this.statusChange(TaskStatusChange.Run.DONE, TaskStatusChange.Exit.SUCCESS);
    }

    private void result(ObjectValue result) {
        try {
            TaskResultsPutResponse response = this.taskClient.taskCollection().taskEntity().taskResults().put(TaskResultsPutRequest.builder()
                    .taskId(this.task.id())
                    .payload(result)
                    .build());
            if (response.opt().status200().isEmpty()) {
                log.error("[GRAVE] while changing task result, unexpected response from api : {}", response);
            }
        } catch (IOException e) {
            log.error("[GRAVE] while changing task result, failed accessing task api");
        }
    }

    @Override
    public void failure() {
        this.statusChange(TaskStatusChange.Run.DONE, TaskStatusChange.Exit.FAILURE);
    }

    @Override
    public void partialResult(ObjectValue result) {
        log.info("task partial result {} - {}", result, this.task);
        this.result(result);
    }

    @Override
    public void info(String log, Object... args) {
        ClientTaskNotifier.log.info(String.format(log, args));
        this.log(TaskLogCreation.builder().level(TaskLogCreation.Level.INFO).log(log, args).build());
    }

    @Override
    public void error(String log, Object... args) {
        ClientTaskNotifier.log.error(log, args);
        this.log(TaskLogCreation.builder().level(TaskLogCreation.Level.ERROR).log(log, args).build());
    }

    private void statusChange(TaskStatusChange.Run run, TaskStatusChange.Exit exit) {
        try {
            log.info("changing task status to {}/{} - task : {}", run, exit, this.task);
            TaskStatusChangesPostResponse response = this.taskClient.taskCollection().taskEntity().taskStatusChanges().post(TaskStatusChangesPostRequest.builder()
                    .taskId(this.task.id())
                    .payload(TaskStatusChange.builder().run(run).exit(exit).build())
                    .build());
            if (response.opt().status201().isEmpty()) {
                log.error("[GRAVE] while changing task status to {}/{}, unexpected response from api : {}", run, exit, response);
            }
        } catch (IOException e) {
            log.error(String.format("[GRAVE] while changing task status to %s/%s, failed accessing task api", run, exit), e);
        }
    }

    private void log(TaskLogCreation logCreation) {
        try {
            switch (logCreation.level()) {
                case INFO -> log.info("{}", logCreation.log());
                case ERROR -> log.error("{}", logCreation.log());
                default -> log.info("[{}] {}", logCreation.level(), logCreation.log());
            }
            log.info("", this.task);
            TaskLogsPostResponse response = this.taskClient.taskCollection().taskEntity().taskLogs().post(TaskLogsPostRequest.builder()
                    .taskId(this.task.id())
                    .payload(logCreation)
                    .build());
            if (response.opt().status201().isEmpty()) {
                log.error("[GRAVE] while appending task log, unexpected response from api : {}", response);
            }
        } catch (IOException e) {
            log.error("[GRAVE] while appending task log, failed accessing task api");
        }
    }
}
