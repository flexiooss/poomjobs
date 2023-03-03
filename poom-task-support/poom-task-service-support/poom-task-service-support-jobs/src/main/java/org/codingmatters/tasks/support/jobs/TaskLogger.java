package org.codingmatters.tasks.support.jobs;

public interface TaskLogger {
    void info(String log, Object... args);
    void error(String log, Object... args);
}
