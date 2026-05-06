package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobRunnerMetaData;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Accounting;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.status.AbortionStatus;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.status.TerminationStatus;
import org.codingmatters.poom.services.domain.entities.Entity;
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
                .id(this.entity.id())
                .name(value.name())
                .version(this.entity.version().toString())
                .category(value.category())
                .arguments(value.arguments() != null ? value.arguments().toArray(new String[0]) : null)
                .status(this.jobStatusFrom(value.status()))
                .accounting(this.jobAccountingFrom(value.accounting()))
                .processing(this.jobProcessingFrom(value.processing()))
                .runner(this.jobRunnerMetaDataFrom(value.runner()))
                .result(value.result())
                .context(value.context())
                .attemptCount(value.attemptCount())
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
        builder.retriedByJob(status.retriedByJob());
        if (status.abortionStatus() != null) {
            builder.abortionStatus(this.abortionStatusFrom(status.abortionStatus()));
        }
        if (status.terminationStatus() != null) {
            builder.terminationStatus(this.terminationStatusFrom(status.terminationStatus()));
        }

        return builder.build();
    }

    private org.codingmatters.poomjobs.api.types.job.status.AbortionStatus abortionStatusFrom(AbortionStatus abortionStatus) {
        if (abortionStatus == null) return null;
        return org.codingmatters.poomjobs.api.types.job.status.AbortionStatus.builder()
                .cause(abortionStatus.cause() != null ? org.codingmatters.poomjobs.api.types.job.status.AbortionStatus.Cause.valueOf(abortionStatus.cause().name()) : null)
                .recuperationAttempt(abortionStatus.recuperationAttempt())
                .build();
    }

    private org.codingmatters.poomjobs.api.types.job.status.TerminationStatus terminationStatusFrom(TerminationStatus terminationStatus) {
        if (terminationStatus == null) return null;
        return org.codingmatters.poomjobs.api.types.job.status.TerminationStatus.builder()
                .status(terminationStatus.status() != null ? org.codingmatters.poomjobs.api.types.job.status.TerminationStatus.Status.valueOf(terminationStatus.status().name()) : null)
                .terminationAttempt(terminationStatus.terminationAttempt())
                .build();
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

    private org.codingmatters.poomjobs.api.types.JobRunnerMetaData jobRunnerMetaDataFrom(JobRunnerMetaData runner) {
        if(runner == null) return null;

        return org.codingmatters.poomjobs.api.types.JobRunnerMetaData.builder()
                .runnerId(runner.runnerId())
                .idempotent(runner.idempotent())
                .build();
    }
}
