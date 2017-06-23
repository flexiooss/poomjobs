package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.api.types.jobupdatedata.Status;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/20/17.
 */
public class JobValueMergerTest {

    @Test
    public void mergeEmptyFields_withEmptyFields__returnEmptyFields() throws Exception {
        assertThat(
                JobValueMerger.merge(JobValue.Builder.builder().build())
                        .with(JobUpdateData.Builder.builder().build()),
                is(
                        JobValue.Builder.builder().build()
                )
                );
    }

    @Test
    public void mergeAllFields_withNewValues__returnNewValues() throws Exception {
        JobValue value = JobValue.Builder.builder()
                .name("name")
                .arguments("argument", "list")
                .category("category")
                .result("result")
                .status(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Builder.builder()
                        .run(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Run.PENDING)
                        .exit(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Exit.FAILURE)
                        .build())
                .build();
        assertThat(
                JobValueMerger
                        .merge(value)
                        .with(JobUpdateData.Builder.builder()
                                .result("changed result")
                                .status(Status.Builder.builder()
                                        .run(Status.Run.DONE)
                                        .exit(Status.Exit.SUCCESS)
                                        .build())
                                .build()),
                is(
                        value.withResult("changed result")
                            .withStatus(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Builder.builder()
                                        .run(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Run.DONE)
                                        .exit(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Exit.SUCCESS)
                                        .build())
                )
                );
    }
}