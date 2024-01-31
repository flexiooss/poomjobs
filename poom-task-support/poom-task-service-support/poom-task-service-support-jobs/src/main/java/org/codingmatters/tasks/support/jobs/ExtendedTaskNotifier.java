package org.codingmatters.tasks.support.jobs;

import org.codingmatters.value.objects.values.ObjectValue;

public interface ExtendedTaskNotifier extends TaskNotifier {
    void success(ObjectValue result);
    void failure();
}
