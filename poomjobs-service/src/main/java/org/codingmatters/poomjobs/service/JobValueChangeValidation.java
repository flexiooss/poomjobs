package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;

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
            return process(this.currentValue, newValue);
        }
    }

    static private JobValueChangeValidation process(JobValue currentValue, JobValue newValue) {
        if(currentValue.status().run().equals(Status.Run.DONE)) {
            return new JobValueChangeValidation(
                    false,
                    String.format("cannot change run status from %s to %s", currentValue.status().run(), newValue.status().run())
            );
        }
        return new JobValueChangeValidation(true, "");
    }

    private final boolean valid;
    private final String message;

    private JobValueChangeValidation(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String message() {
        return message;
    }
}
