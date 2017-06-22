package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/22/17.
 */
public class JobValueChangeRuleApplierTest {

    @Test
    public void runStatusIsPENDING__whenRunStatusChangedToRUNNING__willSetProcessingStartedDate() throws Exception {
        JobValue currentValue = JobValue.Builder.builder()
                .name("test")
                .status(Status.Builder.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .processing(Processing.Builder.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .started(null)
                        .build())
                .build();

        JobValue processed = JobValueChangeRuleApplier.from(currentValue)
                .to(currentValue.withStatus(currentValue.status().withRun(Status.Run.RUNNING)))
                .apply();

        assertThat(
                processed.processing().started(),
                is(notNullValue())
        );
    }
    @Test
    public void runStatusIsRUNNING__whenRunStatusChangedToDONE__willSetProcessingStartedDate() throws Exception {
        JobValue currentValue = JobValue.Builder.builder()
                .name("test")
                .status(Status.Builder.builder()
                        .run(Status.Run.RUNNING)
                        .build())
                .processing(Processing.Builder.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .started(LocalDateTime.now().minus(30, ChronoUnit.SECONDS))
                        .finished(null)
                        .build())
                .build();

        JobValue processed = JobValueChangeRuleApplier.from(currentValue)
                .to(currentValue.withStatus(currentValue.status().withRun(Status.Run.DONE)))
                .apply();

        assertThat(
                processed.processing().finished(),
                is(notNullValue())
        );
    }
}