package org.codingmatters.tasks.context;

/**
 * A task log line made of a label and a description shown under it. The label is the log itself
 * (TaskLog.log), the description its detail (TaskLog.description) ; a null or blank description
 * makes a line with a label only.
 *
 * <p>Neither is a format : they are logged as is.
 */
public record TaskLogLine(String label, String description) {

    public static TaskLogLine of(String label, String description) {
        return new TaskLogLine(label, description);
    }

    public boolean hasDescription() {
        return this.description != null && !this.description.isBlank();
    }

    /** A text to pass as a log format with no args : String.format would fail on a lone %. */
    static String asFormat(String text) {
        return text == null ? null : text.replace("%", "%%");
    }

    /** The line as a single text, for loggers that have no description : the label, then the description. */
    public String text() {
        return this.hasDescription() ? this.label + " — " + this.description : this.label;
    }
}
