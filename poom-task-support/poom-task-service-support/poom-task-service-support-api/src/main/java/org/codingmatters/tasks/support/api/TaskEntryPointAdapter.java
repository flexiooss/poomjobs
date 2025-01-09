package org.codingmatters.tasks.support.api;

import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;

import java.util.Optional;

public interface TaskEntryPointAdapter {
    Repository<Task, PropertyQuery> tasks();
    Optional<Repository<TaskLog, PropertyQuery>> taskLogs();
    JobSpec jobSpecFor(Task task);
    String jobAccount();
    Requester callbackRequester(String callbackUrl);

    class JobSpec {
        public final String category;
        public final String name;
        public final String tasksUrl;
        public final String[] additionalArgs;

        public JobSpec(String category, String name, String tasksUrl, String ... additionalArgs) {
            this.category = category;
            this.name = name;
            this.tasksUrl = tasksUrl;
            this.additionalArgs = additionalArgs;
        }
    }
}
