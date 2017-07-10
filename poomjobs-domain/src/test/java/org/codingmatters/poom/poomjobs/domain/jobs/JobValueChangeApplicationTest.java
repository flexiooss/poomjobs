package org.codingmatters.poom.poomjobs.domain.jobs;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/23/17.
 */
public class JobValueChangeApplicationTest {

    @Test
    public void runStatusIsPENDING__whenRunStatusChangedToRUNNING__willSetProcessingStartedDate() throws Exception {
        JobValue currentValue = JobValue.builder()
                .name("test")
                .status(Status.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .processing(Processing.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .started(null)
                        .build())
                .build();

        JobValue processed = JobValueChange.from(currentValue)
                .to(currentValue.withStatus(currentValue.status().withRun(Status.Run.RUNNING)))
                .applied();

        assertThat(
                processed.processing().started(),
                is(notNullValue())
        );
    }

    @Test
    public void runStatusIsRUNNING__whenRunStatusChangedToDONE__willSetProcessingSFinishedDate() throws Exception {
        JobValue currentValue = JobValue.builder()
                .name("test")
                .status(Status.builder()
                        .run(Status.Run.RUNNING)
                        .build())
                .processing(Processing.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .started(LocalDateTime.now().minus(30, ChronoUnit.SECONDS))
                        .finished(null)
                        .build())
                .build();

        JobValue processed = JobValueChange.from(currentValue)
                .to(currentValue.withStatus(currentValue.status().withRun(Status.Run.DONE)))
                .applied();

        assertThat(
                processed.processing().finished(),
                is(notNullValue())
        );
    }
}