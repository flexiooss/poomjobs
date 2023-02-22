package org.codingmatters.tasks.support.jobs;

import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.runner.exception.JobProcessingException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.tasks.api.TaskEntityGetRequest;
import org.codingmatters.tasks.api.TaskEntityGetResponse;
import org.codingmatters.tasks.api.types.Task;
import org.codingmatters.tasks.api.types.TaskStatusChange;
import org.codingmatters.tasks.client.TaskApiClient;
import org.codingmatters.tasks.support.jobs.notifier.ClientTaskNotifier;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.casts.ValueObjectCaster;
import org.codingmatters.value.objects.values.casts.reflect.ValueObjectReflectCaster;

import java.io.IOException;

public abstract class TaskJobProcessor<Param, Result> implements JobProcessor {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(TaskJobProcessor.class);

    private final ValueObjectReflectCaster<ObjectValue, Param> paramCaster;
    private final ValueObjectReflectCaster<Result, ObjectValue> resultCaster;

    protected final Job job;
    private final TaskApiClient taskClient;

    public TaskJobProcessor(Job job, TaskApiClient taskClient, Class<Param> paramClass, Class<Result> resultClass) throws ValueObjectCaster.ValueObjectUncastableException {
        this.job = job;
        this.taskClient = taskClient;
        this.paramCaster = new ValueObjectReflectCaster<ObjectValue, Param>(ObjectValue.class, paramClass);
        this.resultCaster = new ValueObjectReflectCaster<Result, ObjectValue>(resultClass, ObjectValue.class);
    }

    protected abstract TaskProcessor<Param, Result> taskProcessor() throws JobProcessingException;

    @Override
    public Job process() throws JobProcessingException {
        Task task = this.task(job);
        ExtendedTaskNotifier notifier = this.taskNotifier(task);
        TaskProcessor<Param, Result> processor = this.taskProcessor();

        notifier.updateRunStatus(TaskStatusChange.Run.RUNNING);
        Param param;
        try {
            param = this.paramCaster.cast(task.params());
        } catch (ValueObjectCaster.ValueObjectCastException e) {
            log.error("[GRAVE] error casting task param", e);
            return this.failure(notifier);
        }

        try {
            Result result = processor.process(param, notifier);
            ObjectValue resultObject;
            try {
                resultObject = this.resultCaster.cast(result);
            } catch (ValueObjectCaster.ValueObjectCastException e) {
                log.error("[GRAVE] error casting task result", e);
                return this.failure(notifier);
            }
            return this.success(notifier, resultObject);
        } catch (TaskProcessor.TaskFailure e) {
            return this.failure(notifier);
        }
    }

    private Task task(Job job) throws JobProcessingException {
        try {
            TaskEntityGetResponse response = this.taskClient.taskCollection().taskEntity().get(TaskEntityGetRequest.builder()
                    .taskId(job.arguments().get(0))
                    .build());
            if(response.opt().status200().isEmpty()) {
                throw new JobProcessingException("no task for job : " + job);
            }
            return response.status200().payload();
        } catch (IOException e) {
            throw new JobProcessingException("failed reaching task api for job : " + job);
        }
    }


    private ExtendedTaskNotifier taskNotifier(Task task) {
        return new ClientTaskNotifier(this.taskClient, task);
    }

    protected Job success(ExtendedTaskNotifier notifier, ObjectValue result) {
        notifier.success(result);
        return this.job.withStatus(org.codingmatters.poomjobs.api.types.job.Status.builder().run(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE).exit(org.codingmatters.poomjobs.api.types.job.Status.Exit.SUCCESS).build());
    }

    private Job failure(ExtendedTaskNotifier notifier) {
        notifier.failure();
        return this.job.withStatus(org.codingmatters.poomjobs.api.types.job.Status.builder().run(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE).exit(org.codingmatters.poomjobs.api.types.job.Status.Exit.FAILURE).build());
    }
}
