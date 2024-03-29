package org.codingmatters.tasks.support.handlers.tasks;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.tasks.api.TaskEntityGetRequest;
import org.codingmatters.tasks.api.TaskEntityGetResponse;
import org.codingmatters.tasks.api.taskentitygetresponse.Status200;
import org.codingmatters.tasks.api.taskentitygetresponse.Status404;
import org.codingmatters.tasks.api.taskentitygetresponse.Status500;
import org.codingmatters.tasks.api.types.Error;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.AbstractTaskHandler;
import org.codingmatters.tasks.support.handlers.tasks.adapter.ReflectHandlerAdapter;
import org.codingmatters.tasks.support.handlers.tasks.adapter.UnadatableHandlerException;

import java.util.function.Function;
import java.util.function.Supplier;

public class RetrieveTask extends AbstractTaskHandler implements Function<TaskEntityGetRequest, TaskEntityGetResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RetrieveTask.class);

    public RetrieveTask(Supplier<TaskEntryPointAdapter> adapterProvider, JsonFactory jsonFactory) {
        super(adapterProvider, jsonFactory);
    }

    public <Req, Resp> Function<Req, Resp> adapted(Class<Req> requestClass, Class<Resp> responseClass) {
        try {
            return new ReflectHandlerAdapter<>(this, requestClass, responseClass, TaskEntityGetRequest.class, TaskEntityGetResponse.class);
        } catch (UnadatableHandlerException e) {
            log.error("[GRAVE] error adapting adapter", e);
            throw new RuntimeException("error adapting handler", e);
        }
    }

    @Override
    public TaskEntityGetResponse apply(TaskEntityGetRequest request) {
        TaskEntryPointAdapter adapter = this.adapter();
        Entity<Task> taskEntity;
        try {
            taskEntity = adapter.tasks().retrieve(request.taskId());
        } catch (RepositoryException e) {
            return TaskEntityGetResponse.builder()
                    .status500(Status500.builder().payload(Error.builder()
                            .code(Error.Code.UNEXPECTED_ERROR)
                            .token(log.tokenized().error("while getting task, failed accessing repository", e))
                            .build()).build())
                    .build();
        }
        if(taskEntity == null) {
            return TaskEntityGetResponse.builder()
                    .status404(Status404.builder().payload(Error.builder()
                            .code(Error.Code.RESOURCE_NOT_FOUND)
                            .token(log.tokenized().info("while getting task, no task with requested id : {}", request))
                            .build()).build())
                    .build();
        }
        return TaskEntityGetResponse.builder()
                .status200(Status200.builder()
                        .xEntityId(taskEntity.id())
                        .payload(taskEntity.value())
                        .build())
                .build();
    }
}
