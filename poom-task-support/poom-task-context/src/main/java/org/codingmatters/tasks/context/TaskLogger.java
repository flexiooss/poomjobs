package org.codingmatters.tasks.context;

public interface TaskLogger {
    void info(String log, Object... args);
    void warn(String log, Object... args);
    void error(String log, Object... args);

    /**
     * A line with a label and a description. Loggers that do not store descriptions log the line
     * as a single text ({@link TaskLogLine#text()}), passed as the log with no args : its % are
     * escaped for the loggers that format it, the others keep it readable.
     */
    default void info(TaskLogLine line) {
        this.info(TaskLogLine.asFormat(line.text()));
    }

    default void warn(TaskLogLine line) {
        this.warn(TaskLogLine.asFormat(line.text()));
    }

    default void error(TaskLogLine line) {
        this.error(TaskLogLine.asFormat(line.text()));
    }
}
