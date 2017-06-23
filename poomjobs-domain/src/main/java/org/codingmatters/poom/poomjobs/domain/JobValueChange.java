package org.codingmatters.poom.poomjobs.domain;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.ChangeBuilder;
import org.codingmatters.poom.services.domain.change.Validation;

import java.time.LocalDateTime;

/**
 * Created by nelt on 6/23/17.
 */
public class JobValueChange extends Change<JobValue> {

    static public ChangeBuilder<JobValue, JobValueChange> from(JobValue current) {
        return new ChangeBuilder<>(current, (currentValue, newValue) -> new JobValueChange(currentValue, newValue));
    }

    private JobValueChange(JobValue currentValue, JobValue newValue) {
        super(currentValue, newValue);
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

}
