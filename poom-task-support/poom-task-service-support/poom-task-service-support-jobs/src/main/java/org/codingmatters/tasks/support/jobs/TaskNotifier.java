package org.codingmatters.tasks.support.jobs;

import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.context.TaskLogLine;
import org.codingmatters.tasks.context.TaskLogger;
import org.codingmatters.value.objects.values.ObjectValue;

public interface TaskNotifier extends TaskLogger {
    void updateRunStatus(TaskStatusChange.Run status);

    void partialResult(ObjectValue result);

    default TaskLogger withToken(String token) {
        return new TaskLogger() {
            @Override
            public void info(String log, Object... args) {
                TaskNotifier.this.info(log + " (support token : " + token + ")", args);
            }

            @Override
            public void warn(String log, Object... args) {
                TaskNotifier.this.warn(log + " (support token : " + token + ")", args);
            }

            @Override
            public void error(String log, Object... args) {
                TaskNotifier.this.error(log + " (support token : " + token + ")", args);
            }

            @Override
            public void info(TaskLogLine line) {
                TaskNotifier.this.info(withToken(line));
            }

            @Override
            public void warn(TaskLogLine line) {
                TaskNotifier.this.warn(withToken(line));
            }

            @Override
            public void error(TaskLogLine line) {
                TaskNotifier.this.error(withToken(line));
            }

            private TaskLogLine withToken(TaskLogLine line) {
                String suffix = "(support token : " + token + ")";
                return TaskLogLine.of(line.label(), line.hasDescription() ? line.description() + " " + suffix : suffix);
            }
        };
    }
}
