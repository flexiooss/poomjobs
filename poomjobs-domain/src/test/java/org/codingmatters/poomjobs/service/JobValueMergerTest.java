package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobRunnerMetaData;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/20/17.
 */
public class JobValueMergerTest {

    @Test
    public void mergeEmptyFields_withEmptyFields__returnEmptyFields() throws Exception {
        assertThat(
                JobValueMerger.merge(JobValue.builder().build())
                        .with(JobUpdateData.builder().build()),
                is(
                        JobValue.builder().build()
                )
                );
    }

    @Test
    public void mergeAllFields_withNewValues__returnNewValues() throws Exception {
        JobValue value = JobValue.builder()
                .name("name")
                .arguments("argument", "list")
                .category("category")
                .result("result")
                .status(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.builder()
                        .run(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Run.PENDING)
                        .exit(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Exit.FAILURE)
                        .build())
                .runner(JobRunnerMetaData.builder().runnerId("runner-1").idempotent(false).build())
                .build();
        assertThat(
                JobValueMerger
                        .merge(value)
                        .with(JobUpdateData.builder()
                                .result("changed result")
                                .status(Status.builder()
                                        .run(Status.Run.DONE)
                                        .exit(Status.Exit.SUCCESS)
                                        .retriedByJob("new-job")
                                        .build())
                                .runner(org.codingmatters.poomjobs.api.types.JobRunnerMetaData.builder().runnerId("runner-2").idempotent(true).build())
                                .build()),
                is(
                        value.withResult("changed result")
                            .withStatus(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.builder()
                                        .run(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Run.DONE)
                                        .exit(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Exit.SUCCESS)
                                        .retriedByJob("new-job")
                                        .build())
                            .withRunner(JobRunnerMetaData.builder().runnerId("runner-2").idempotent(true).build())
                )
                );
    }

    @Test
    public void createWithAllFields() {
        JobValue value = JobValueMerger.create()
                .with(JobCreationData.builder()
                        .name("name")
                        .category("category")
                        .arguments("arg1", "arg2")
                        .attemptCount(1L)
                        .build());

        assertThat(value.name(), is("name"));
        assertThat(value.category(), is("category"));
        assertThat(value.arguments(), contains("arg1", "arg2"));
        assertThat(value.attemptCount(), is(1L));
    }
}
