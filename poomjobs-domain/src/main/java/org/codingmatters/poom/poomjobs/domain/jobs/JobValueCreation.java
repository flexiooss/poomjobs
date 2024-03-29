package org.codingmatters.poom.poomjobs.domain.jobs;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.Validation;
import org.codingmatters.poom.services.support.date.UTC;

import java.time.LocalDateTime;

/**
 * Created by nelt on 6/24/17.
 */
public class JobValueCreation extends Change<JobValue> {

    static public JobValueCreation with(JobValue initialValue) {
        return new JobValueCreation(initialValue);
    }

    private JobValueCreation(JobValue initialValue) {
        super(null, initialValue);
    }

    @Override
    protected Validation validate() {
        if(this.newValue().name() == null) {
            return new Validation(false, "cannot create a job with no name");
        }
        if(this.newValue().category() == null) {
            return new Validation(false, "cannot create a job with no category");
        }
        if(this.newValue().accounting() == null || this.newValue().accounting().accountId() == null) {
            return new Validation(false, "cannot create a job with no account id");
        }
        return new Validation(true, "");
    }

    @Override
    public JobValue applied() {
        Processing.Builder processing = this.newValue().processing() != null ?
                Processing.from(this.newValue().processing()) :
                Processing.builder();
        return this.newValue()
                .withProcessing(processing.submitted(UTC.now()).build())
                .withStatus(Status.builder().run(Status.Run.PENDING).build());
    }

    @Override
    public String toString() {
        return String.format("JobValueCreation{newValue=%s}", this.newValue());
    }
}
