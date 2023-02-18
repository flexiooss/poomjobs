package org.codingmatters.tasks.support.api;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.tasks.api.types.Task;

public interface TaskEntryPointAdapter {
    Repository<Task, PropertyQuery> tasks();
    JobCreationData jobFor(Task task);
    String jobAccount();
}
