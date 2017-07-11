package org.codingmatters.poom.poomjobs.domain.runners;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.change.Validation;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/11/17.
 */
public class RunnerValueCreationTest {

    @Test
    public void whenCallbackIsNull__validationFails() throws Exception {
        assertThat(
                RunnerValueCreation.with(this.validValue()
                        .withCallback(null)
                ).validation(),
                is(new Validation(false, "a runner must have a callback"))
        );
    }

    @Test
    public void whenTimeToLiveIsNull__validationFails() throws Exception {
        assertThat(
                RunnerValueCreation.with(this.validValue()
                        .withTimeToLive(null)
                ).validation(),
                is(new Validation(false, "a runner must have a time to live"))
        );
    }

    @Test
    public void whenCompetenciesIsNull__validationFails() throws Exception {
        assertThat(
                RunnerValueCreation.with(this.validValue()
                        .withCompetencies(null)
                ).validation(),
                is(new Validation(false, "a runner must have some competencies explicitly setted"))
        );
    }

    @Test
    public void whenCategoryCompetenciesIsNull__validationFails() throws Exception {
        assertThat(
                RunnerValueCreation.with(this.validValue()
                        .withCompetencies(this.validValue().competencies()
                                .withCategories(null))
                ).validation(),
                is(new Validation(false, "a runner must have some category competency explicitly setted"))
        );
    }

    @Test
    public void whenNameCompetenciesIsNull__validationFails() throws Exception {
        assertThat(
                RunnerValueCreation.with(this.validValue()
                        .withCompetencies(this.validValue().competencies()
                                .withNames(null))
                ).validation(),
                is(new Validation(false, "a runner must have some name competency explicitly setted"))
        );
    }

    @Test
    public void whenValueIsValid__runtimeIsInitialized() throws Exception {
        RunnerValue initial = this.validValue()
                .withRuntime(Runtime.builder()
                        .status(Runtime.Status.DISCONNECTED)
                        .created(null)
                        .lastPing(null)
                        .build());

        RunnerValue actual = RunnerValueCreation.with(initial).applied();

        assertThat(actual.runtime().created(), is(notNullValue()));
        assertThat(actual.runtime().lastPing(), is(notNullValue()));
    }

    @Test
    public void whenValueIsValid__statusIsInitialized() throws Exception {
        RunnerValue initial = this.validValue()
                .withRuntime(Runtime.builder()
                        .status(null)
                        .build());

        RunnerValue actual = RunnerValueCreation.with(initial).applied();

        assertThat(actual.runtime().status(), is(Runtime.Status.IDLE));
    }

    private RunnerValue validValue() {
        return RunnerValue.builder()
                .callback("http://call.me/up")
                .competencies(Competencies.builder()
                        .categories("ALL")
                        .names("ANY")
                        .build())
                .timeToLive(12L)
                .build();
    }
}