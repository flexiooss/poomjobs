package org.codingmatters.tasks.support.jobs;

import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

public class TaskNotifierTest {
    private final List<String> info = new LinkedList<>();
    private final List<String> error = new LinkedList<>();

    private final TaskNotifier notifier = new TaskNotifier() {
        @Override
        public void updateRunStatus(TaskStatusChange.Run status) {}

        @Override
        public void partialResult(ObjectValue result) {}

        @Override
        public void info(String log, Object... args) {
            info.add(String.format(log, args));
        }

        @Override
        public void error(String log, Object... args) {
            error.add(String.format(log, args));
        }
    };

    @Test
    public void logsWithToken() throws Exception {
        this.notifier.withToken("12").info("hello %s", "world");
        this.notifier.withToken("12").error("goodby %s", "world");

        assertThat(this.info, contains("hello world (support token : 12)"));
        assertThat(this.error, contains("goodby world (support token : 12)"));
    }
}