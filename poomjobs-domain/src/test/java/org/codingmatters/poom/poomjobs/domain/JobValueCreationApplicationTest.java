package org.codingmatters.poom.poomjobs.domain;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/27/17.
 */
public class JobValueCreationApplicationTest {

    @Test
    public void providedNameCategoryAndArguments__valuesAreLeftUnchanged() throws Exception {
        JobValue jobValue = JobValueCreation.with(JobValue.Builder.builder()
                .name("job")
                .category("category")
                .arguments("a", "b", "c")
                .build())
                .applied();

        assertThat(jobValue.name(), is("job"));
        assertThat(jobValue.category(), is("category"));
        assertThat(jobValue.arguments(), contains("a", "b", "c"));
    }

    @Test
    public void whenSubmissionDateIsNull__thenSubmissionDateIsSetted() throws Exception {
        JobValue jobValue = JobValueCreation.with(this.base()
                .build())
                .applied();

        assertThat(jobValue.processing().submitted(), is(notNullValue()));
    }

    @Test
    public void whenSubmissionDateIsSetted__thenSubmissionDateIsUpdated() throws Exception {
        LocalDateTime currentDate = LocalDateTime.now().minus(10, ChronoUnit.MINUTES);
        JobValue jobValue = JobValueCreation.with(this.base()
                .processing(Processing.Builder.builder().submitted(currentDate).build())
                .build())
                .applied();

        assertThat(jobValue.processing().submitted(), is(not(currentDate)));
    }

    private JobValue.Builder base() {
        return JobValue.Builder.builder()
                .name("job")
                .category("category")
                .arguments("arg");
    }
}