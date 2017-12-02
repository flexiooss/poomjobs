package org.codingmatters.poom.runner.manager;

import com.fasterxml.jackson.core.JsonFactory;
import okhttp3.OkHttpClient;
import org.codingmatters.poom.client.PoomjobsRunnerAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerAPIRequesterClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerCollectionGetResponse;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RunnerInvokerListener implements PoomjobsJobRepositoryListener {

    static private final Logger log = LoggerFactory.getLogger(RunnerInvokerListener.class);

    private final PoomjobsRunnerRegistryAPIClient runnerRegistry;

    public RunnerInvokerListener(PoomjobsRunnerRegistryAPIClient runnerRegistry) {
        this.runnerRegistry = runnerRegistry;
    }

    @Override
    public void jobCreated(Entity<JobValue> entity) {
        this.findRunnerAndDeleguateJob(entity);
    }

    @Override
    public void jobUpdated(Entity<JobValue> entity) {
        if(Status.Run.PENDING.equals(entity.value().opt().status().run().orElse(Status.Run.DONE))) {
            this.findRunnerAndDeleguateJob(entity);
        }
    }

    private void findRunnerAndDeleguateJob(Entity<JobValue> entity) {
        try {
            RunnerCollectionGetResponse response = this.runnerRegistry.runnerCollection().get(req -> req
                    .categoryCompetency(entity.value().category())
                    .nameCompetency(entity.value().category())
                    .runtimeStatus(Runtime.Status.IDLE.name())
                    .range("0-10")
            );
            ValueList<Runner> candidates = response.opt().status200().payload()
                    .orElse(response.opt().status206().payload()
                            .orElse(new ValueList.Builder<Runner>().build()))
                    ;
            log.debug("runner candidates: {}", candidates);
            if(! candidates.isEmpty()) {
                for (Runner candidate : candidates) {
                    log.debug("trying candidate: {}", candidate);
                    PoomjobsRunnerAPIClient runner = this.runnerClient(candidate);
                    RunningJobPutResponse resp = runner.runningJob().put(req -> req
                            .jobId(entity.id())
                            .payload(this.createJobRequest(entity)));
                    if(resp.opt().status201().isPresent()) {
                        log.info("delegated job to runner {} at {}", candidate.id(), candidate.callback());
                        return;
                    } else {
                        log.info("runner refused the job with response: {}", resp);
                    }
                }
            } else {
                log.info("no runner ready for job {}", entity.id());
            }
        } catch (IOException e) {
            log.error("problem occurred while looking up runner for job " + entity.id(), e);
        }
    }

    private PoomjobsRunnerAPIClient runnerClient(Runner runner) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        JsonFactory jsonFactory = new JsonFactory();
        PoomjobsRunnerAPIClient result = new PoomjobsRunnerAPIRequesterClient(
                new OkHttpRequesterFactory(client),
                jsonFactory,
                runner.callback());
        return result;
    }

    private Job createJobRequest(Entity<JobValue> entity) {
        return JobEntityTransformation.transform(entity).asJob();
    }
}
