package org.codingmatters.poom.poomjobs.domain.runners;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.Validation;

import java.time.LocalDateTime;

/**
 * Created by nelt on 7/11/17.
 */
public class RunnerValueCreation extends Change<RunnerValue> {

    static public RunnerValueCreation with(RunnerValue initialValue) {
        return new RunnerValueCreation(initialValue);
    }

    private RunnerValueCreation(RunnerValue initialValue) {
        super(null, initialValue);
    }

    @Override
    protected Validation validate() {
        if(this.newValue().callback() == null) {
            return new Validation(false, "a runner must have a callback");
        }
        if(this.newValue().timeToLive() == null) {
            return new Validation(false, "a runner must have a time to live");
        }
        if(this.newValue().timeToLive() <= 0) {
            return new Validation(false, "a runner must have a positive time to live");
        }
        if(this.newValue().competencies() == null) {
            return new Validation(false, "a runner must have some competencies explicitly setted");
        }
        if(this.newValue().competencies().names() == null) {
            return new Validation(false, "a runner must have some name competency explicitly setted");
        }
        if(this.newValue().competencies().categories() == null) {
            return new Validation(false, "a runner must have some category competency explicitly setted");
        }
        return new Validation(true, "");
    }

    @Override
    public RunnerValue applied() {
        RunnerValue result = this.newValue();
        if(result.runtime() == null) {
            result = result.withRuntime(Runtime.builder().build());
        }
        result = result.withRuntime(result.runtime()
                .withStatus(result.runtime().status() != null ? result.runtime().status() : Runtime.Status.IDLE)
                .withCreated(LocalDateTime.now())
                .withLastPing(LocalDateTime.now())
        );
        return result;
    }
}
