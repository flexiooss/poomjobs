package org.codingmatters.poom.runner.manager;

import org.codingmatters.poomjobs.client.PoomjobsRunnerAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerCollectionGetResponse;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class RunnerInvokerListener implements PoomjobsJobRepositoryListener {

    static private final Logger log = LoggerFactory.getLogger(RunnerInvokerListener.class);

    private final PoomjobsRunnerRegistryAPIClient runnerRegistry;
    private final RunnerClientFactory runnerClientFactory;
    private final ExecutorService listenerPool;

    public RunnerInvokerListener(PoomjobsRunnerRegistryAPIClient runnerRegistry, RunnerClientFactory runnerClientFactory, ExecutorService listenerPool) {
        this.runnerRegistry = runnerRegistry;
        this.runnerClientFactory = runnerClientFactory;
        this.listenerPool = listenerPool;
    }

    @Override
    public void jobCreated(Entity<JobValue> entity) {
        this.listenerPool.submit(() -> this.findRunnerAndDeleguateJob(entity));
    }

    @Override
    public void jobUpdated(Entity<JobValue> entity) {
        if(Status.Run.PENDING.equals(entity.value().opt().status().run().orElse(Status.Run.DONE))) {
            this.listenerPool.submit(() -> this.findRunnerAndDeleguateJob(entity));
        }
    }

    private void findRunnerAndDeleguateJob(Entity<JobValue> entity) {
        try {
            int start = 0;
            int step = 10;
            RunnerCollectionGetResponse response;
            do {
                String range = String.format("%s-%s", start, start + step - 1);
                start = start + step;

                response = this.runnerRegistry.runnerCollection().get(req -> req
                        .categoryCompetency(entity.value().category())
                        .nameCompetency(entity.value().name())
                        .runtimeStatus(Runtime.Status.IDLE.name())
                        .range(range)
                );
                ValueList<Runner> candidates = response.opt().status200().payload()
                        .orElse(response.opt().status206().payload()
                                .orElse(new ValueList.Builder<Runner>().build()))
                        ;
                log.debug("runner candidates: {}", candidates);
                if(! candidates.isEmpty()) {
                    boolean someDisconnected = false;
                    for (Runner candidate : candidates) {
                        log.debug("trying candidate: {}", candidate);
                        PoomjobsRunnerAPIClient runner = this.runnerClient(candidate);
                        try {
                            RunningJobPutResponse resp = runner.runningJob().put(req -> req
                                    .jobId(entity.id())
                                    .payload(this.createJobRequest(entity)));
                            if (resp.opt().status201().isPresent()) {
                                log.info("delegated job {}/{} to runner {} at {}",
                                        entity.value().category(),
                                        entity.value().name(),
                                        candidate.id(),
                                        candidate.callback());
                                return;
                            } else {
                                log.info("runner refused the job ; runner {} job {}/{} with response: {} (runner : {})",
                                        candidate.id(),
                                        entity.value().category(),
                                        entity.value().name(),
                                        resp,
                                        candidate);
                            }
                        } catch(IOException e) {
                            this.disconnectRunner(candidate, e);
                            someDisconnected = true;
                        }
                    }
                    if(someDisconnected) {
                        start = 0;
                    }
                } else {
                    log.info("no runner ready for job {}", entity.id());
                }
            } while (! response.opt().status200().isPresent());
        } catch (IOException e) {
            log.error("problem occurred while looking up runner for job " + entity.id(), e);
        }
    }

    private void disconnectRunner(Runner candidate, IOException e) {
        log.info(
                String.format("runner with id %s at %s seem to be down, setting as disconnected",
                        candidate.id(),
                        candidate.callback()),
                e);
        try {
            RunnerPatchResponse response = this.runnerRegistry.runnerCollection().runner().patch(req -> req
                    .runnerId(candidate.id())
                    .payload(status -> status.status(RunnerStatusData.Status.DISCONNECTED))
            );
            if(! response.opt().status200().isPresent()) {
                log.error("runner {} update refused with response : {}", candidate.id(), response);
            }
        } catch (IOException e1) {
            log.error(String.format("error updating runner %s status", candidate.id()), e1);
        }
    }

    private PoomjobsRunnerAPIClient runnerClient(Runner runner) {
        return this.runnerClientFactory.runnerClient(runner);
    }

    private Job createJobRequest(Entity<JobValue> entity) {
        return JobEntityTransformation.transform(entity).asJob();
    }
}
