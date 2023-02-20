package org.codingmatters.tasks.support.handlers.tasks;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.tasks.api.TaskLogsPostRequest;
import org.codingmatters.tasks.api.TaskLogsPostResponse;
import org.codingmatters.tasks.api.tasklogspostresponse.Status201;
import org.codingmatters.tasks.api.tasklogspostresponse.Status404;
import org.codingmatters.tasks.api.tasklogspostresponse.Status500;
import org.codingmatters.tasks.api.types.Error;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskLog;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.AbstractTaskHandler;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class CreateTaskLog extends AbstractTaskHandler implements Function<TaskLogsPostRequest, TaskLogsPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(CreateTaskLog.class);

    public CreateTaskLog(Supplier<TaskEntryPointAdapter> adapterProvider, JsonFactory jsonFactory) {
        super(adapterProvider, jsonFactory);
    }

    @Override
    public TaskLogsPostResponse apply(TaskLogsPostRequest request) {
        TaskEntryPointAdapter adapter = this.adapter();
        Entity<Task> task;
        try {
            task = adapter.tasks().retrieve(request.taskId());
        } catch (RepositoryException e) {
            return TaskLogsPostResponse.builder().status500(Status500.builder().payload(Error.builder()
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("while creating task log, failed reaching task repository", e))
                    .build()).build()).build();
        }
        if(task == null) {
            return TaskLogsPostResponse.builder().status404(Status404.builder().payload(Error.builder()
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .token(log.tokenized().info("request to add log to unexistent task : {}", request))
                            .description("task not found")
                    .build()).build()).build();
        }

        TaskLog taskLog = TaskLog.builder()
                .taskId(task.id())
                .at(UTC.now())
                .level(TaskLog.Level.valueOf(request.payload().level().name()))
                .log(request.payload().log())
                .build();

        Optional<Repository<TaskLog, PropertyQuery>> logs = adapter.taskLogs();
        if(logs.isPresent()) {
            try {
                logs.get().create(taskLog);
            } catch (RepositoryException e) {
                return TaskLogsPostResponse.builder().status500(Status500.builder().payload(Error.builder()
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token(log.tokenized().error("while creating task log, failed reaching task log repository", e))
                        .build()).build()).build();
            }
        }
        return TaskLogsPostResponse.builder().status201(Status201.builder().payload(taskLog).build()).build();
    }
}
