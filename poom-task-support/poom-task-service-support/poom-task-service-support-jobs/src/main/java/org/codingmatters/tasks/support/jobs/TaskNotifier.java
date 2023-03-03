package org.codingmatters.tasks.support.jobs;

import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.api.types.task.Status;

public interface TaskNotifier extends TaskLogger {
    void updateRunStatus(TaskStatusChange.Run status);
    default TaskLogger withToken(String token) {
        return new TaskLogger() {
            @Override
            public void info(String log, Object... args) {
                TaskNotifier.this.info(log + " (support token : " + token + ")", args);
            }

            @Override
            public void error(String log, Object... args) {
                TaskNotifier.this.error(log + " (support token : " + token + ")", args);
            }
        };
    }
}
