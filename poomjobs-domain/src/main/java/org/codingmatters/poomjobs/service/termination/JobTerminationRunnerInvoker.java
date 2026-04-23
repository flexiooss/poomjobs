package org.codingmatters.poomjobs.service.termination;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.entities.Entity;

public interface JobTerminationRunnerInvoker {

    public void notifyRunnerJobAborted(Entity<JobValue> entity) throws NoRunnerCandidateFoundException, AbortionImpossibleException;
}
