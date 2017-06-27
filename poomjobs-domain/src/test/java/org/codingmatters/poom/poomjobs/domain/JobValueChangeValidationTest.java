package org.codingmatters.poom.poomjobs.domain;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poom.services.domain.change.Validation;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/23/17.
 */
public class JobValueChangeValidationTest {


    @Test
    public void runStatusIsDONE__whenAnyChangeOccurs__thenInvalid() throws Exception {
        JobValue jobValue = JobValue.builder()
                .status(Status.builder()
                        .run(Status.Run.DONE)
                        .build())
                .build();

        assertThat(JobValueChange.from(jobValue)
                        .to(jobValue.withStatus(Status.builder()
                                .run(Status.Run.RUNNING)
                                .build()))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(jobValue)
                        .to(jobValue.withStatus(Status.builder()
                                .run(Status.Run.PENDING)
                                .build()))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );

        assertThat(JobValueChange.from(jobValue)
                        .to(jobValue.changed(builder -> builder.name("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(jobValue)
                        .to(jobValue.changed(builder -> builder.result("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(jobValue)
                        .to(jobValue.changed(builder -> builder.arguments("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(jobValue)
                        .to(jobValue.changed(builder -> builder.category("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(jobValue)
                        .to(jobValue.withProcessing(Processing.builder().finished(LocalDateTime.now()).build()))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
    }

    @Test
    public void runStatusIsRUNNING__whenRunStatusChangesToDONE_andExitStatusIsNotSetted__thenInvalid() throws Exception {
        JobValue jobValue = JobValue.builder()
                .status(Status.builder()
                        .run(Status.Run.RUNNING)
                        .exit(null)
                        .build())
                .build();

        assertThat(JobValueChange
                        .from(jobValue)
                        .to(jobValue.withStatus(jobValue.status().withRun(Status.Run.DONE)))
                        .validation(),
                is(new Validation(false, "when job run status changes to DONE, an exit status must be setted"))
        );
    }

}