package org.codingmatters.poom.poomjobs.domain.runners;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.change.Validation;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/11/17.
 */
public class RunnerValueChangeTest {

    @Test
    public void changeAlwaysValid() throws Exception {
        assertThat(
                RunnerValueChange.from(RunnerValue.builder().build()).to(RunnerValue.builder().build()).validation(),
                is(new Validation(true, ""))
        );
    }

    @Test
    public void changeAsNoSideEffect() throws Exception {
        RunnerValue from = RunnerValue.builder()
                .timeToLive(18L)
                .callback("http://dont.call.me/up")
                .build();
        RunnerValue to = RunnerValue.builder()
                .timeToLive(12L)
                .callback("http://call.me/up")
                .build();
        assertThat(RunnerValueChange.from(from).to(to).applied(), is(to));

    }
}