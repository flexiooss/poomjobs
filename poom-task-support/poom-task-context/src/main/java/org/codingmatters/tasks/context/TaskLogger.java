package org.codingmatters.tasks.context;

public interface TaskLogger {
    void info(String log, Object... args);
    void warn(String log, Object... args);
    void error(String log, Object... args);
}
