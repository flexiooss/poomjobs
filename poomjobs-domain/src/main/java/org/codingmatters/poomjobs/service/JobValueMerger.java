package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.api.types.JobUpdateData;

/**
 * Created by nelt on 6/20/17.
 */
public class JobValueMerger {

    static public JobValueMerger create() {
        return new JobValueMerger(JobValue.builder().build());
    }

    static public JobValueMerger merge(JobValue jobValue) {
        return new JobValueMerger(jobValue);
    }

    private final JobValue currentValue;

    private JobValueMerger(JobValue currentValue) {
        this.currentValue = currentValue;
    }

    public JobValue with(JobUpdateData jobData) {
        return this.currentValue.changed(builder -> builder
                .result(jobData.result())
                .status(this.fromJobDataStatus(jobData.status()))
        );
    }

    public JobValue with(JobCreationData creationData) {
        return this.currentValue
                .changed(builder -> builder
                        .category(creationData.category())
                        .name(creationData.name())
                        .arguments(creationData.arguments() != null ?
                                 creationData.arguments().toArray(new String[creationData.arguments().size()]) :
                                (String[]) null)
                )
                ;
    }

    private Status fromJobDataStatus(org.codingmatters.poomjobs.api.types.jobupdatedata.Status status) {
        if(status == null) return null;
        Status.Builder result = Status.builder();
        if(status.run() != null) {
            result.run(Status.Run.valueOf(status.run().name()));
        }
        if(status.exit() != null) {
            result.exit(Status.Exit.valueOf(status.exit().name()));
        }
        return result.build();
    }
}
