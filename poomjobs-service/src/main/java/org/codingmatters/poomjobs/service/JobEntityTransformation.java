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

    private JobEntityTransformation(Entity<JobValue> entity) {
        this.entity = entity;
    }

    public Job asJob() {
        JobValue value = this.entity.value();
        return Job.builder()
                .name(value.name())
                .version(this.entity.version().toString())
                .category(value.category())
                .arguments(value.arguments() != null ? value.arguments().toArray(new String[0]) : null)
                .status(this.jobStatusFrom(value.status()))
                .accounting(this.jobAccountingFrom(value.accounting()))
                .processing(this.jobProcessingFrom(value.processing()))
                .build();
    }

    private org.codingmatters.poomjobs.api.types.job.Status jobStatusFrom(Status status) {
        if(status == null) return null;

        org.codingmatters.poomjobs.api.types.job.Status.Builder builder = org.codingmatters.poomjobs.api.types.job.Status.builder();

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

        return org.codingmatters.poomjobs.api.types.job.Accounting.builder()
                .accountId(accounting.accountId())
                .build();
    }

    private org.codingmatters.poomjobs.api.types.job.Processing jobProcessingFrom(Processing processing) {
        if(processing == null) return null;

        return org.codingmatters.poomjobs.api.types.job.Processing.builder()
                .submitted(this.entity.value().processing().submitted())
                .started(this.entity.value().processing().started())
                .finished(this.entity.value().processing().finished())
                .build();
    }
}
