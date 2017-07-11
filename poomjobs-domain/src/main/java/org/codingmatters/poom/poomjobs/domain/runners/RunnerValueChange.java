package org.codingmatters.poom.poomjobs.domain.runners;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.ChangeBuilder;
import org.codingmatters.poom.services.domain.change.Validation;

/**
 * Created by nelt on 7/10/17.
 */
public class RunnerValueChange extends Change<RunnerValue> {

    static public ChangeBuilder<RunnerValue, RunnerValueChange> from(RunnerValue current) {
        return new ChangeBuilder<>(current, RunnerValueChange::new);
    }

    private RunnerValueChange(RunnerValue currentValue, RunnerValue newValue) {
        super(currentValue, newValue);
    }

    @Override
    protected Validation validate() {
        return new Validation(true, "");
    }

    @Override
    public RunnerValue applied() {
        return this.newValue();
    }
}
