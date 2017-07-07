package org.codingmatters.poom.poomjobs.domain;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Accounting;
import org.codingmatters.poom.services.domain.change.Validation;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/24/17.
 */
public class JobValueCreationValidationTest {
    @Test
    public void whenNameIsNull__thenCreationIsInvalid() throws Exception {
        assertThat(
                JobValueCreation.with(this.valid()
                        .name(null)
                    .build())
                    .validation(),
                is(new Validation(false, "cannot create a job with no name"))
        );
    }

    @Test
    public void whenCategoryIsNull__thenCreationIsInvalid() throws Exception {
        assertThat(
                JobValueCreation.with(this.valid()
                        .category(null)
                        .build())
                        .validation(),
                is(new Validation(false, "cannot create a job with no category"))
        );
    }

    @Test
    public void whenAccountingAccountIdIsNotSetted__theCreationIsInvalid() throws Exception {
        assertThat(
                JobValueCreation.with(this.valid()
                        .accounting(null)
                        .build())
                        .validation(),
                is(new Validation(false, "cannot create a job with no account id"))
        );

        assertThat(
                JobValueCreation.with(this.valid()
                        .accounting(Accounting.builder().build())
                        .build())
                        .validation(),
                is(new Validation(false, "cannot create a job with no account id"))
        );
    }

    private JobValue.Builder valid() {
        return JobValue.builder()
                .name("name")
                .category("category")
                .accounting(Accounting.builder()
                        .accountId("121212")
                        .build())
                ;
    }
}