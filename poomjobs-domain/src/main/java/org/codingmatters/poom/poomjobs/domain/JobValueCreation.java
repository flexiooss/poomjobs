package org.codingmatters.poom.poomjobs.domain;

import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.Validation;

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
                Processing.Builder.from(this.newValue().processing()) :
                Processing.Builder.builder();
        return this.newValue()
                .withProcessing(processing.submitted(LocalDateTime.now()).build());
    }
}
