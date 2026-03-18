package org.codingmatters.poom.jobs.runner.service.jobs.termination;

import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.AbortedJobTerminationPostRequest;
import org.codingmatters.poomjobs.api.AbortedJobTerminationPostResponse;
import org.codingmatters.poomjobs.api.abortedjobterminationpostresponse.Status204;
import org.codingmatters.poomjobs.api.abortedjobterminationpostresponse.Status500;
import org.codingmatters.poomjobs.api.types.Error;

import java.io.IOException;
import java.util.function.Function;

public class FailedJobTerminationHandler implements Function<AbortedJobTerminationPostRequest, AbortedJobTerminationPostResponse>, JobProcessor.JobMonitor {

    private static final CategorizedLogger log = CategorizedLogger.getLogger(FailedJobTerminationHandler.class);

    private final JobProcessor.Factory jobProcessorFactory;

    public FailedJobTerminationHandler(JobProcessor.Factory jobProcessorFactory) {
        this.jobProcessorFactory = jobProcessorFactory;
    }

    @Override
    public AbortedJobTerminationPostResponse apply(AbortedJobTerminationPostRequest request) {
        try {
            JobProcessor processor = jobProcessorFactory.createFor(request.payload(), this);
            processor.terminateFailedJob(request.payload());
            return AbortedJobTerminationPostResponse.builder()
                    .status204(Status204.builder().build())
                    .build();
        } catch (Exception e) {
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

    @Override
    public void doNotRestartThisJobAtThisPoint() throws IOException {
        throw new IOException("Cannot monitor a failed job");
    }

    @Override
    public void canRestartThisJobFromTheBeginning() throws IOException {
        throw new IOException("Cannot monitor a failed job");
    }

    @Override
    public boolean isShutdownRequested() {
        return false;
    }
}
