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

        if(this.currentValue.status() != null && Status.Run.PENDIND == this.currentValue.status().run()) {
            if(result.status() != null && Status.Run.RUNNING == result.status().run()) {
                result = result.withProcessing(result.processing().changed(builder -> builder.started(LocalDateTime.now())));
            }
        }
        if(this.currentValue.status() != null && Status.Run.RUNNING == this.currentValue.status().run()) {
            if(result.status() != null && Status.Run.DONE == result.status().run()) {
                result = result.withProcessing(result.processing().changed(builder -> builder.finished(LocalDateTime.now())));
            }
        }

        return result;
    }
}
