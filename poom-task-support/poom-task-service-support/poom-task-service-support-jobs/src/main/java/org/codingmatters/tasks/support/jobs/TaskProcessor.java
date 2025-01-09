package org.codingmatters.tasks.support.jobs;

public interface TaskProcessor<Param, Result> {
    Result process(String taskId, Param param, TaskNotifier taskNotifier, String ... arguments) throws TaskFailure;

    class TaskFailure extends Exception {
        public TaskFailure(String message) {
            super(message);
        }

        public TaskFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
