package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poomjobs.api.types.JobUpdateData;

/**
 * Created by nelt on 6/20/17.
 */
public class JobValueMerger {

    static public JobValueMerger merge(JobValue jobValue) {
        return new JobValueMerger(jobValue);
    }

    private final JobValue currentValue;

    private JobValueMerger(JobValue currentValue) {
        this.currentValue = currentValue;
    }

    public JobValue with(JobUpdateData jobData) {
        return this.currentValue.changed(builder -> builder
                .category(jobData.category())
                .name(jobData.name())
                .arguments(jobData.arguments() != null ? jobData.arguments().toArray(new String[0]) : null)
                .result(jobData.result())
                .status(this.fromJobDataStatus(jobData.status()))
        );
    }

    private Status fromJobDataStatus(org.codingmatters.poomjobs.api.types.jobupdatedata.Status status) {
        if(status == null) return null;
        Status.Builder result = Status.Builder.builder();
        if(status.run() != null) {
            result.run(Status.Run.valueOf(status.run().name()));
        }
        if(status.exit() != null) {
            result.exit(Status.Exit.valueOf(status.exit().name()));
        }
        return result.build();
    }
}
