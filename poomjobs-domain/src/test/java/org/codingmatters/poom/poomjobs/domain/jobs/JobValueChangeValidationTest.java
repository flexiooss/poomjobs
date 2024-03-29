package org.codingmatters.poom.poomjobs.domain.jobs;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.change.Validation;
import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

        assertThat(JobValueChange.from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.withStatus(Status.builder()
                                .run(Status.Run.RUNNING)
                                .build()))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.withStatus(Status.builder()
                                .run(Status.Run.PENDING)
                                .build()))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );

        assertThat(JobValueChange.from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.changed(builder -> builder.name("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.changed(builder -> builder.result("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.changed(builder -> builder.arguments("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.changed(builder -> builder.category("changed")))
                        .validation(),
                is(new Validation(false, "cannot change a job when run status is DONE"))
        );
        assertThat(JobValueChange.from(BigInteger.ONE, BigInteger.ONE, jobValue)
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
                        .from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.withStatus(jobValue.status().withRun(Status.Run.DONE)))
                        .validation(),
                is(new Validation(false, "when job run status changes to DONE, an exit status must be setted"))
        );
    }

    @Test
    public void givenRunStatusIsRUNNING__whenRunStatusChangedToRUNNING__thenInvalid() throws Exception {
        JobValue jobValue = JobValue.builder()
                .status(Status.builder()
                        .run(Status.Run.RUNNING)
                        .exit(null)
                        .build())
                .build();

        assertThat(JobValueChange
                        .from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.withStatus(jobValue.status().withRun(Status.Run.RUNNING)))
                        .validation(),
                is(new Validation(false, "job already RUNNING, cannont change running statuys to RUNNING again"))
        );
    }

    @Test
    public void givenPENDING__whenChangingToRUNNING__thenValid() throws Exception {
        JobValue jobValue = JobValue.builder()
                .name("test")
                .status(Status.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .processing(Processing.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .started(null)
                        .build())
                .build();
        assertThat(JobValueChange
                        .from(BigInteger.ONE, BigInteger.ONE, jobValue)
                        .to(jobValue.withStatus(jobValue.status().withRun(Status.Run.RUNNING)))
                        .validation(),
                is(new Validation(true, ""))
        );
    }

    @Test
    public void givenPENDING__whenChangingToRUNNING_andDifferentVersiuons__thenInvalid() throws Exception {
        JobValue jobValue = JobValue.builder()
                .name("test")
                .status(Status.builder()
                        .run(Status.Run.PENDING)
                        .build())
                .processing(Processing.builder()
                        .submitted(LocalDateTime.now().minus(1, ChronoUnit.MINUTES))
                        .started(null)
                        .build())
                .build();
        assertThat(JobValueChange
                        .from(BigInteger.TWO, BigInteger.ONE, jobValue)
                        .to(jobValue.withStatus(jobValue.status().withRun(Status.Run.RUNNING)))
                        .validation(),
                is(new Validation(false, "version differs, cannot validate change (current is 2, changing from 1)"))
        );
    }
}