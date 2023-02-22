package org.codingmatters.tasks.support.jobs;

import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.api.types.task.Status;

public interface TaskNotifier {
    void info(String log);
    void error(String log);
    void updateRunStatus(TaskStatusChange.Run status);
}
