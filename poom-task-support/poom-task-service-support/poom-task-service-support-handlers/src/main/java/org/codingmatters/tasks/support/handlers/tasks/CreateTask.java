package org.codingmatters.tasks.support.handlers.tasks;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.tasks.api.TaskCollectionPostRequest;
import org.codingmatters.tasks.api.TaskCollectionPostResponse;
import org.codingmatters.tasks.api.taskcollectionpostresponse.Status201;
import org.codingmatters.tasks.api.types.Error;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.task.Status;
import org.codingmatters.tasks.support.api.TaskEntryPointAdapter;
import org.codingmatters.tasks.support.handlers.AbstractTaskHandler;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class CreateTask extends AbstractTaskHandler implements Function<TaskCollectionPostRequest, TaskCollectionPostResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(CreateTask.class);
    private final PoomjobsJobRegistryAPIClient jobsClient;

    public CreateTask(Supplier<TaskEntryPointAdapter> adapterProvider, PoomjobsJobRegistryAPIClient jobsClient) {
        super(adapterProvider);
        this.jobsClient = jobsClient;
    }

    @Override
    public TaskCollectionPostResponse apply(TaskCollectionPostRequest request) {
        TaskEntryPointAdapter adapter = this.adapter();
        Entity<Task> taskEntity;
        try {
            taskEntity = adapter.tasks().create(Task.builder()
                            .callbackUrl(request.callbackUrl())
                            .createdAt(UTC.now())
                            .status(Status.builder().run(Status.Run.PENDING).build())
                            .params(request.payload())
                    .build());
        } catch (RepositoryException e) {
            return TaskCollectionPostResponse.builder().status500(st -> st.payload(error -> error
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("while creating task, failed accessing task repository : " + request, e))
            )).build();
        }
        try {
            taskEntity = adapter.tasks().update(taskEntity, taskEntity.value().withId(taskEntity.id()));
        } catch (RepositoryException e) {
            return TaskCollectionPostResponse.builder().status500(st -> st.payload(error -> error
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("while updating task with id, failed accessing task repository : " + request, e))
            )).build();
        }

        try {
            JobCollectionPostResponse response = this.jobsClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .accountId(adapter.jobAccount())
                    .payload(adapter.jobFor(taskEntity.value()))
                    .build());
            if(response.opt().status201().isPresent()) {
                log.info("created job {} for task {}", response.status201().xEntityId(), taskEntity.id());
            } else {
                return TaskCollectionPostResponse.builder().status500(st -> st.payload(error -> error
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("while submitting job for task, job refused : {} - {}", request, response))
                    .description("job submission failed")
            )).build();
            }
        } catch (IOException e) {
            return TaskCollectionPostResponse.builder().status500(st -> st.payload(error -> error
                    .code(Error.Code.UNEXPECTED_ERROR)
                    .token(log.tokenized().error("while submitting job for task, failed accessing job registry : " + request, e))
                    .description("job submission failed")
            )).build();
        }

        return TaskCollectionPostResponse.builder()
                .status201(Status201.builder().xEntityId(taskEntity.id()).payload(taskEntity.value()).build())
                .build();
    }
}
