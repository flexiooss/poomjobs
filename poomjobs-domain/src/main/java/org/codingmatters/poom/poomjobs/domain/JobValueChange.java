package org.codingmatters.poom.poomjobs.domain;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poom.services.domain.change.Validation;

import java.time.LocalDateTime;

/**
 * Created by nelt on 6/23/17.
 */
public class JobValueChange {

    static public Builder from(JobValue currentValue) {
        return new Builder(currentValue);
    }

    static public class Builder {
        private final JobValue currentValue;

        private Builder(JobValue currentValue) {
            this.currentValue = currentValue;
        }

        public JobValueChange to(JobValue newValue) {
            return new JobValueChange(this.currentValue, newValue);
        }
    }

    private final JobValue currentValue;
    private final JobValue newValue;
    private final Validation validation;

    private JobValueChange(JobValue currentValue, JobValue newValue) {
        this.currentValue = currentValue;
        this.newValue = newValue;
        this.validation = this.validate();
    }

    public Validation validation() {
        return this.validation;
    }

    private Validation validate() {
        if(currentValue.status().run().equals(Status.Run.DONE)) {
            return new Validation(
                    false,
                    String.format("cannot change a job when run status is DONE")
            );
        }
        if(currentValue.status().run().equals(Status.Run.RUNNING)
                && newValue.status().run().equals(Status.Run.DONE)
                && newValue.status().exit() == null) {
            return new Validation(
                    false,
                    String.format("when job run status changes to DONE, an exit status must be setted")
            );
        }
        return new Validation(true, "");
    }

    public JobValue applied() {
        JobValue result = newValue;

        if(this.runStatusChanges(Status.Run.PENDING, Status.Run.RUNNING)) {
            result = result.withProcessing(result.processing().withStarted(LocalDateTime.now()));
        }
        if(this.runStatusChanges(Status.Run.RUNNING, Status.Run.DONE)) {
            result = result.withProcessing(result.processing().withFinished(LocalDateTime.now()));
        }

        return result;
    }

    private boolean runStatusChanges(Status.Run from, Status.Run to) {
        if(this.currentValue.status() != null && from == this.currentValue.status().run()) {
            if(this.newValue.status() != null && to == this.newValue.status().run()) {
                return true;
            }
        }
        return false;
    }

}
