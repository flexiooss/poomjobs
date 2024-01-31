package org.codingmatters.tasks.support.jobs;

import org.codingmatters.tasks.api.types.TaskLogCreation;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.value.objects.values.ObjectValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
public class TestTaskNotifier implements TaskNotifier {

    private final List<TaskLogCreation> logs = Collections.synchronizedList(new LinkedList<>());
    private final List<Status.Run> statusChanges = Collections.synchronizedList(new LinkedList<>());
    private final List<ObjectValue> resultChanges = Collections.synchronizedList(new LinkedList<>());

    @Override
    public void info(String s, Object ... args) {
        logs.add(TaskLogCreation.builder().level(TaskLogCreation.Level.INFO).log(s, args).build());
    }

    @Override
    public void error(String s, Object ... args) {
        logs.add(TaskLogCreation.builder().level(TaskLogCreation.Level.ERROR).log(s, args).build());
    }

    @Override
    public void updateRunStatus(TaskStatusChange.Run run) {
        System.out.println("STATUS CHANGED: " + run);
        this.statusChanges.add(Status.Run.valueOf(run.name()));
    }

    @Override
    public void partialResult(ObjectValue result) {
        this.resultChanges.add(result);
    }

    public List<TaskLogCreation> logs() {
        return new ArrayList<>(this.logs);
    }

    public List<Status.Run> statusChangeHistory() {
        return new ArrayList<>(this.statusChanges);
    }

    public List<ObjectValue> resultChangeHistory() {
        return new ArrayList<>(this.resultChanges);
    }

    public Status.Run lastStatus() {
        return this.statusChanges.isEmpty() ? this.statusChanges.get(this.statusChanges.size()) : null;
    }
}
