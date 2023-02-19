package org.codingmatters.tasks.support.handlers.tasks;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.tasks.api.TaskStatusChangesPostRequest;
import org.codingmatters.tasks.api.TaskStatusChangesPostResponse;
import org.codingmatters.tasks.api.taskstatuschangespostresponse.Status201;
import org.codingmatters.tasks.api.taskstatuschangespostresponse.Status400;
import org.codingmatters.tasks.api.taskstatuschangespostresponse.Status404;
import org.codingmatters.tasks.api.taskstatuschangespostresponse.Status500;
import org.codingmatters.tasks.api.types.Error;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.AbstractTaskHandler;

import java.util.function.Function;
import java.util.function.Supplier;

public class UpdateTaskStatus extends AbstractTaskHandler implements Function<TaskStatusChangesPostRequest, TaskStatusChangesPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(UpdateTaskStatus.class);

    public UpdateTaskStatus(Supplier<TaskEntryPointAdapter> adapterProvider) {
        super(adapterProvider);
    }

    @Override
    public TaskStatusChangesPostResponse apply(TaskStatusChangesPostRequest request) {
        Repository<Task, PropertyQuery> repository = this.adapter().tasks();

        Entity<Task> taskEntry;
        try {
            taskEntry = repository.retrieve(request.taskId());
        } catch (RepositoryException e) {
            return TaskStatusChangesPostResponse.builder().status500(Status500.builder().payload(Error.builder()
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("while changing task status, failed accessing task repository", e))
                    .build()).build()).build();
        }

        if(taskEntry == null) {
            return TaskStatusChangesPostResponse.builder().status404(Status404.builder().payload(Error.builder()
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .token(log.tokenized().info("while changing task status, task not found : {}", request))
                    .build()).build()).build();
        } else if(taskEntry.value().opt().status().run().orElse(null).equals(Status.Run.DONE)) {
            return TaskStatusChangesPostResponse.builder().status400(Status400.builder().payload(Error.builder()
                            .code(Error.Code.BAD_REQUEST)
                            .token(log.tokenized().info("cannot change task status when status is DONE : {}", taskEntry))
                    .build()).build()).build();
        } else {
            try {
                if (request.payload().run().equals(TaskStatusChange.Run.RUNNING)) {
                    taskEntry = repository.update(taskEntry, taskEntry.value()
                            .withStartedAt(UTC.now())
                            .withStatus(Status.builder().run(Status.Run.RUNNING).build())
                    );
                } else if (request.payload().run().equals(TaskStatusChange.Run.DONE)) {
                    taskEntry = repository.update(taskEntry, taskEntry.value()
                            .withFinishedAt(UTC.now())
                            .withStatus(Status.builder().run(Status.Run.DONE).exit(
                                    request.opt().payload().exit().isPresent() ? Status.Exit.valueOf(request.payload().exit().name()) : Status.Exit.SUCCESS
                            ).build())
                    );
                }
                log.info("task status updated {}", taskEntry);
            } catch (RepositoryException e) {
                return TaskStatusChangesPostResponse.builder().status500(Status500.builder().payload(Error.builder()
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .token(log.tokenized().error("while changing task status, failed accessing task repository to update task", e))
                        .build()).build()).build();
            }
        }
        return TaskStatusChangesPostResponse.builder().status201(Status201.builder().xEntityId(taskEntry.id()).payload(request.payload()).build()).build();
    }
}
