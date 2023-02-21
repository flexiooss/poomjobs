package org.codingmatters.tasks.support.handlers.tasks;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.tasks.api.TaskLogsPostRequest;
import org.codingmatters.tasks.api.TaskLogsPostResponse;
import org.codingmatters.tasks.api.TaskResultsPutRequest;
import org.codingmatters.tasks.api.TaskResultsPutResponse;
import org.codingmatters.tasks.api.taskresultsputresponse.Status200;
import org.codingmatters.tasks.api.taskresultsputresponse.Status404;
import org.codingmatters.tasks.api.taskresultsputresponse.Status500;
import org.codingmatters.tasks.api.types.Error;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.AbstractTaskHandler;
import org.codingmatters.tasks.support.handlers.tasks.adapter.ReflectHandlerAdapter;
import org.codingmatters.tasks.support.handlers.tasks.adapter.UnadatableHandlerException;

import java.util.function.Function;
import java.util.function.Supplier;

public class ReplaceTaskResult extends AbstractTaskHandler implements Function<TaskResultsPutRequest, TaskResultsPutResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ReplaceTaskResult.class);

    public ReplaceTaskResult(Supplier<TaskEntryPointAdapter> adapterProvider, JsonFactory jsonFactory) {
        super(adapterProvider, jsonFactory);
    }

    public <Req, Resp> Function<Req, Resp> adapted(Class<Req> requestClass, Class<Resp> responseClass) {
        try {
            return new ReflectHandlerAdapter<>(this, requestClass, responseClass, TaskResultsPutRequest.class, TaskResultsPutResponse.class);
        } catch (UnadatableHandlerException e) {
            log.error("[GRAVE] error adapting adapter", e);
            throw new RuntimeException("error adapting handler", e);
        }
    }

    @Override
    public TaskResultsPutResponse apply(TaskResultsPutRequest request) {
        Repository<Task, PropertyQuery> repository = this.adapter().tasks();

        Entity<Task> taskEntity;
        try {
            taskEntity = repository.retrieve(request.taskId());
        } catch (RepositoryException e) {
            return TaskResultsPutResponse.builder().status500(Status500.builder().payload(Error.builder()
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .token(log.tokenized().error("while getting task to treplace task result, failed reaching task repository", e))
                    .build()).build()).build();
        }

        if(taskEntity == null) {
            return TaskResultsPutResponse.builder().status404(Status404.builder().payload(Error.builder()
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .token(log.tokenized().info("task not found {}", request))
                            .description("task not found")
                    .build()).build()).build();
        }

        try {
            taskEntity = repository.update(taskEntity, taskEntity.value().withResults(request.payload()));
        } catch (RepositoryException e) {
            return TaskResultsPutResponse.builder().status500(Status500.builder().payload(Error.builder()
                    .code(Error.Code.RESOURCE_NOT_FOUND)
                    .token(log.tokenized().error("while replacing task result, failed reaching task repository", e))
                    .build()).build()).build();
        }

        return TaskResultsPutResponse.builder().status200(Status200.builder().xEntityId(taskEntity.id()).payload(taskEntity.value().results()).build()).build();
    }
}
