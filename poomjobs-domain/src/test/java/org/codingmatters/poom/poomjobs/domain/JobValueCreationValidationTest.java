package org.codingmatters.poom.poomjobs.domain;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
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
                JobValueCreation.with(JobValue.Builder.builder()
                    .category("category")
                    .build())
                    .validation(),
                is(new Validation(false, "cannot create a job with no name"))
        );
    }

    @Test
    public void whenCategoryIsNull__thenCreationIsInvalid() throws Exception {
        assertThat(
                JobValueCreation.with(JobValue.Builder.builder()
                        .name("name")
                        .build())
                        .validation(),
                is(new Validation(false, "cannot create a job with no category"))
        );
    }
}