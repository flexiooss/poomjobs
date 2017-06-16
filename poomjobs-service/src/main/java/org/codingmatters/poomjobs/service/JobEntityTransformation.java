package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Accounting;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.types.Job;

/**
 * Created by nelt on 6/16/17.
 */
public class JobEntityTransformation {

    static public JobEntityTransformation transform(Entity<JobValue> entity) {
        return new JobEntityTransformation(entity);
    }

    private final Entity<JobValue> entity;

    public JobEntityTransformation(Entity<JobValue> entity) {
        this.entity = entity;
    }

    public Job asJob() {
        return Job.Builder.builder()
                .name(this.entity.value().name())
                .category(this.entity.value().category())
                .arguments(this.entity.value().arguments().toArray(new String[0]))
                .status(this.jobStatusFrom(this.entity.value().status()))
                .accounting(this.jobAccountingFrom(this.entity.value().accounting()))
                .processing(this.jobProcessingFrom(this.entity.value().processing()))
                .build();
    }

    private org.codingmatters.poomjobs.api.types.job.Status jobStatusFrom(Status status) {
        if(status == null) return null;

        org.codingmatters.poomjobs.api.types.job.Status.Builder builder = org.codingmatters.poomjobs.api.types.job.Status.Builder.builder();

        if(status.run() != null) {
            builder.run(org.codingmatters.poomjobs.api.types.job.Status.Run.valueOf(status.run().name()));
        }
        if(status.exit() != null) {
            builder.exit(org.codingmatters.poomjobs.api.types.job.Status.Exit.valueOf(status.exit().name()));
        }

        return builder.build();
    }

    private org.codingmatters.poomjobs.api.types.job.Accounting jobAccountingFrom(Accounting accounting) {
        if(accounting == null) return null;

        return org.codingmatters.poomjobs.api.types.job.Accounting.Builder.builder()
                .accountId(accounting.accountId())
                .build();
    }

    private org.codingmatters.poomjobs.api.types.job.Processing jobProcessingFrom(Processing processing) {
        if(processing == null) return null;

        return org.codingmatters.poomjobs.api.types.job.Processing.Builder.builder()
                .submitted(this.entity.value().processing().submitted())
                .started(this.entity.value().processing().started())
                .finished(this.entity.value().processing().finished())
                .build();
    }
}
