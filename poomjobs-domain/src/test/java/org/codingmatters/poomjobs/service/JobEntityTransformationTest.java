package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobRunnerMetaData;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.codingmatters.poomjobs.api.types.Job;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JobEntityTransformationTest {

    @Test
    public void whenAllFields_thenAllFieldsTransformed() {
        JobValue jobValue = JobValue.builder()
                .name("name")
                .arguments("argument", "list")
                .category("category")
                .result("result")
                .status(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.builder()
                        .run(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Run.PENDING)
                        .exit(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Exit.FAILURE)
                        .retriedByJob("new-job")
                        .build())
                .runner(JobRunnerMetaData.builder().runnerId("runner-1").idempotent(true).build())
                .build();

        Job job = JobEntityTransformation.transform(new ImmutableEntity<>("12", BigInteger.ONE, jobValue)).asJob();

        assertThat(job.id(), is("12"));
        assertThat(job.version(), is("1"));
        assertThat(job.name(), is(jobValue.name()));
        assertThat(job.arguments().toArray(), is(jobValue.arguments().toArray()));
        assertThat(job.category(), is(jobValue.category()));
        assertThat(job.result(), is(jobValue.result()));
        assertThat(job.status().run().name(), is(jobValue.status().run().name()));
        assertThat(job.status().exit().name(), is(jobValue.status().exit().name()));
        assertThat(job.status().retriedByJob(), is(jobValue.status().retriedByJob()));
        assertThat(job.runner().runnerId(), is(jobValue.runner().runnerId()));
        assertThat(job.runner().idempotent(), is(jobValue.runner().idempotent()));
    }
}
