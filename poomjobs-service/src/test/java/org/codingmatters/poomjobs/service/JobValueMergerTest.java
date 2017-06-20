package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poomjobs.api.types.JobData;
import org.codingmatters.poomjobs.api.types.jobdata.Status;
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
                        .with(JobData.Builder.builder().build()),
                is(
                        JobValue.Builder.builder().build()
                )
                );
    }

    @Test
    public void mergeAllFields_withNewValues__returnNewValues() throws Exception {
        assertThat(
                JobValueMerger
                        .merge(JobValue.Builder.builder()
                                .name("name")
                                .arguments("argument", "list")
                                .category("category")
                                .result("result")
                                .status(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Builder.builder()
                                        .run(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Run.PENDIND)
                                        .exit(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Exit.FAILURE)
                                        .build())
                                .build())
                        .with(JobData.Builder.builder()
                                .name("changed name")
                                .arguments("changed", "arguments")
                                .category("changed category")
                                .result("changed result")
                                .status(Status.Builder.builder()
                                        .run(Status.Run.DONE)
                                        .exit(Status.Exit.SUCCESS)
                                        .build())
                                .build()),
                is(
                        JobValue.Builder.builder()
                                .name("changed name")
                                .arguments("changed", "arguments")
                                .category("changed category")
                                .result("changed result")
                                .status(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Builder.builder()
                                        .run(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Run.DONE)
                                        .exit(org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status.Exit.SUCCESS)
                                        .build())
                                .build()
                )
                );
    }
}