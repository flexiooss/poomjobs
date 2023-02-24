package org.codingmatters.tasks.support.jobs;

public interface TaskProcessor<Param, Result> {
    Result process(Param param, TaskNotifier taskNotifier) throws TaskFailure;

    class TaskFailure extends Exception {
        public TaskFailure(String message) {
            super(message);
        }

        public TaskFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
