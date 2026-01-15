package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.RunningJobPutRequest;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.Error;

import java.util.function.Function;

public class JobFeederHandler implements Function<RunningJobPutRequest, RunningJobPutResponse> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobFeederHandler.class);
    private final JobPool jobPool;
    private final String jobRequestEndpointUrl;

    public JobFeederHandler(JobPool jobPool, String jobRequestEndpointUrl) {
        this.jobPool = jobPool;
        this.jobRequestEndpointUrl = jobRequestEndpointUrl;
    }

    @Override
    public RunningJobPutResponse apply(RunningJobPutRequest request) {
        log.debug("job execution requested : {}", request);
        Job job = request.payload();
        try {
            this.jobPool.feed(job);
            log.debug("job execution accepted : {}", request);
        } catch (PoolBusyException e) {
            return RunningJobPutResponse.builder().status409(status -> status.payload(error -> error
                    .code(Error.Code.OVERLOADED)
                    .token(log.tokenized().info("runner became busy for job " + job, e))
                    .description("runner busy, come back later")
            )).build();
        }
        return RunningJobPutResponse.builder().status201(status -> status
                .location("%s/%s", this.jobRequestEndpointUrl, job.id())
        ).build();
    }
}
