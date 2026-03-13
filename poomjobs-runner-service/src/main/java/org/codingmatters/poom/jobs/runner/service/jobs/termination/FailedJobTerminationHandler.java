package org.codingmatters.poom.jobs.runner.service.jobs.termination;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.AbortedJobTerminationPostRequest;
import org.codingmatters.poomjobs.api.AbortedJobTerminationPostResponse;
import org.codingmatters.poomjobs.api.abortedjobterminationpostresponse.Status204;
import org.codingmatters.poomjobs.api.abortedjobterminationpostresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;

import java.util.function.Function;

public class FailedJobTerminationHandler implements Function<AbortedJobTerminationPostRequest, AbortedJobTerminationPostResponse>{

    private static final CategorizedLogger log = CategorizedLogger.getLogger(FailedJobTerminationHandler.class);

    private final JobTerminator jobTerminator;

    public FailedJobTerminationHandler(JobTerminator jobTerminator) {
        this.jobTerminator = jobTerminator;
    }

    @Override
    public AbortedJobTerminationPostResponse apply(AbortedJobTerminationPostRequest request) {
        try {
            jobTerminator.terminateJob(request.payload());
            return AbortedJobTerminationPostResponse.builder()
                    .status204(Status204.builder().build())
                    .build();
        } catch (FailedJobTerminationException e) {
            log.error("Error terminating job", e);
            return AbortedJobTerminationPostResponse.builder()
                    .status500(Status500.builder()
                            .payload(Error.builder()
                                    .code(Error.Code.UNEXPECTED_ERROR)
                                    .description(e.getMessage())
                                    .build())
                            .build())
                    .build();
        }
    }

}
