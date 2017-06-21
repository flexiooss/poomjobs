package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/21/17.
 */
public class JobValueChangeValidationTest {

    @Test
    public void runStatusIsDONE_whenAnyChangeOccures__thenIsNotValid() throws Exception {
        JobValue jobValue = JobValue.Builder.builder()
                .status(Status.Builder.builder()
                        .run(Status.Run.DONE)
                        .build())
                .build();

        assertThat(JobValueChangeValidation.from(jobValue)
                .to(jobValue.withStatus(Status.Builder.builder()
                                .run(Status.Run.RUNNING)
                                .build())),
                Matchers.is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.withStatus(Status.Builder.builder()
                                .run(Status.Run.PENDING)
                                .build())),
                Matchers.is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );

        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.name("changed"))),
                Matchers.is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.result("changed"))),
                Matchers.is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.arguments("changed"))),
                Matchers.is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.category("changed"))),
                Matchers.is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChangeValidation.from(jobValue)
                        .to(jobValue.changed(builder -> builder.processing(Processing.Builder.builder().finished(LocalDateTime.now()).build()))),
                Matchers.is(new JobValueChangeValidation(false, "cannot change a job when run status is DONE"))
        );

    }
}