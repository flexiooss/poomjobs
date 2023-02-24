package org.codingmatters.tasks.support.jobs;

import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.tasks.client.TaskApiClient;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.casts.ValueObjectCaster;

public abstract class TaskFragmentJobProcessor<Param, Result> extends TaskJobProcessor<Param, Result> {
    public TaskFragmentJobProcessor(Job job, TaskApiClient taskClient, Class<Param> paramClass, Class<Result> resultClass) throws ValueObjectCaster.ValueObjectUncastableException {
        super(job, taskClient, paramClass, resultClass);
    }

    @Override
    protected Job success(ExtendedTaskNotifier notifier, ObjectValue result) {
        notifier.partialResult(result);
        return this.job.withStatus(org.codingmatters.poomjobs.api.types.job.Status.builder().run(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE).exit(org.codingmatters.poomjobs.api.types.job.Status.Exit.SUCCESS).build());
    }
}
