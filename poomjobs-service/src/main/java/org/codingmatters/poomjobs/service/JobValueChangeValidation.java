package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;

import java.util.Objects;

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
                    String.format("cannot change a job when run status is DONE")
            );
        }
        return new JobValueChangeValidation(true, "");
    }

    private final boolean valid;
    private final String message;

    public JobValueChangeValidation(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "JobValueChangeValidation{" +
                "valid=" + valid +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobValueChangeValidation that = (JobValueChangeValidation) o;
        return valid == that.valid &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, message);
    }
}
