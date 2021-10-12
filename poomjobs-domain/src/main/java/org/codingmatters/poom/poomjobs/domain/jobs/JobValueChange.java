package org.codingmatters.poom.poomjobs.domain.jobs;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.ChangeBuilder;
import org.codingmatters.poom.services.domain.change.Validation;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Created by nelt on 6/23/17.
 */
public class JobValueChange extends Change<JobValue> {

    private final BigInteger currentVersion;
    private final BigInteger fromVersion;

    static public ChangeBuilder<JobValue, JobValueChange> from(BigInteger currentVersion, BigInteger fromVersion, JobValue current) {
        return new ChangeBuilder<>(current, (currentValue, newValue) -> new JobValueChange(currentVersion, fromVersion, currentValue, newValue));
    }

    private JobValueChange(BigInteger currentVersion, BigInteger fromVersion, JobValue currentValue, JobValue newValue) {
        super(currentValue, newValue);
        this.currentVersion = currentVersion;
        this.fromVersion = fromVersion;
    }

    @Override
    public Validation validation() {
        if(this.currentVersion.compareTo(this.fromVersion) != 0) {
            return new Validation(
                    false,
                    String.format("version differs, cannot validate change (current is %s, changing from %s)", this.currentVersion, this.fromVersion)
            );
        }
        return super.validation();
    }

    @Override
    protected Validation validate() {
        if(this.currentValue().status().run().equals(Status.Run.DONE)) {
            return new Validation(
                    false,
                    String.format("cannot change a job when run status is DONE")
            );
        }
        if(this.currentValue().status().run().equals(Status.Run.RUNNING)
                && this.newValue().status().run().equals(Status.Run.DONE)
                && this.newValue().status().exit() == null) {
            return new Validation(
                    false,
                    String.format("when job run status changes to DONE, an exit status must be setted")
            );
        }
        if(this.currentValue().status().run().equals(Status.Run.RUNNING)
                && this.newValue().status().run().equals(Status.Run.RUNNING)) {
            return new Validation(
                    false,
                    String.format("job already RUNNING, cannont change running statuys to RUNNING again")
            );
        }
        return new Validation(true, "");
    }

    @Override
    public JobValue applied() {
        JobValue result = this.newValue();

        if(this.runStatusChanges(Status.Run.PENDING, Status.Run.RUNNING)) {
            result = result.withProcessing(result.processing().withStarted(LocalDateTime.now()));
        }
        if(this.runStatusChanges(Status.Run.RUNNING, Status.Run.DONE)) {
            result = result.withProcessing(result.processing().withFinished(LocalDateTime.now()));
        }

        return result;
    }

    private boolean runStatusChanges(Status.Run from, Status.Run to) {
        if(this.currentValue().status() != null && from == this.currentValue().status().run()) {
            if(this.newValue().status() != null && to == this.newValue().status().run()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("JobValueChange{currentValue=%s newValue=%s}", this.currentValue(), this.newValue());
    }
}
