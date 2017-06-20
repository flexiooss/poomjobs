package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;

/**
 * Created by nelt on 6/20/17.
 */
public class JobValueChangeValidation {

    static public Builder from(JobValue currentValue) {
        return new Builder(currentValue);
    }

    static public class Builder {
        private final JobValue currentValue;

        private Builder(JobValue currentValue) {
            this.currentValue = currentValue;
        }

        public JobValueChangeValidation to(JobValue newValue) {
            JobValueChangeValidation result = new JobValueChangeValidation(this.currentValue, newValue);
            result.process();
            return result;
        }
    }

    private final JobValue currentValue;
    private final JobValue newValue;

    private boolean valid;
    private String message;

    private JobValueChangeValidation(JobValue currentValue, JobValue newValue) {
        this.currentValue = currentValue;
        this.newValue = newValue;
    }

    private void process() {

    }

    public boolean isValid() {
        return valid;
    }

    public String message() {
        return message;
    }
}
