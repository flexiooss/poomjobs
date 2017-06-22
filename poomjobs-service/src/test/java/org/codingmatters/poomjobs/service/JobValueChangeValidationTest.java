package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/21/17.
 */
public class JobValueChangeValidationTest {

    @Test
    public void runStatusIsDONE__whenAnyChangeOccurs__thenInvalid() throws Exception {
        JobValue jobValue = JobValue.Builder.builder()
                .status(Status.Builder.builder()
                        .run(Status.Run.DONE)
                        .build())
                .build();

        assertThat(JobValueChangeValidation.from(jobValue)
                .to(jobValue.withStatus(Status.Builder.builder()
                                .run(Status.Run.RUNNING)
                                .build())),
                is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.withStatus(Status.Builder.builder()
                                .run(Status.Run.PENDING)
                                .build())),
                is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );

        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.name("changed"))),
                is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.result("changed"))),
                is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.arguments("changed"))),
                is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.category("changed"))),
                is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.processing(Processing.Builder.builder().finished(LocalDateTime.now()).build()))),
                is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
    }

    @Test
    public void runStatusIsRUNNING__whenRunStatusChangesToDONE_andExitStatusIsNotSetted__thenInvalid() throws Exception {
        JobValue jobValue = JobValue.Builder.builder()
                .status(Status.Builder.builder()
                        .run(Status.Run.RUNNING)
                        .exit(null)
                        .build())
                .build();

        assertThat(JobValueChangeValidation
                        .from(jobValue)
                        .to(jobValue.withStatus(jobValue.status().withRun(Status.Run.DONE))),
                is(new JobValueChangeValidation(false, "when job run status changes to DONE, an exit status must be setted"))
        );
    }

}