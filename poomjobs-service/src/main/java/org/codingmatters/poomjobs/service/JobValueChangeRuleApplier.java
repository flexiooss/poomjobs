package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;

import java.time.LocalDateTime;

/**
 * Created by nelt on 6/20/17.
 */
public class JobValueChangeRuleApplier {

    static public Builder from(JobValue currentValue) {
        return new Builder(currentValue);
    }

    static public class Builder {
        private final JobValue currentValue;

        private Builder(JobValue currentValue) {
            this.currentValue = currentValue;
        }

        public JobValueChangeRuleApplier to(JobValue newValue) {
            return new JobValueChangeRuleApplier(this.currentValue, newValue);
        }
    }

    private final JobValue currentValue;
    private final JobValue newValue;

    public JobValueChangeRuleApplier(JobValue currentValue, JobValue newValue) {
        this.currentValue = currentValue;
        this.newValue = newValue;
    }

    public JobValue apply() {
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
