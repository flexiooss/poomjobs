package org.codingmatters.poom.runner.internal;

import com.fasterxml.jackson.core.JsonFactory;
import io.undertow.Undertow;
import org.codingmatters.poomjobs.api.PoomjobsRunnerAPIHandlers;
import org.codingmatters.poomjobs.api.RunningJobPutRequest;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.service.api.PoomjobsRunnerAPIProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

import java.util.UUID;

public class RunnerEndpoint {

    private final StatusManager statusManager;
    private final JobManager jobManager;
    private final String jobRegistryUrl;
    private final String host;
    private final int port;

    private final PoomjobsRunnerAPIHandlers handlers;

    private Undertow server;

    public RunnerEndpoint(StatusManager statusManager, JobManager jobManager, String jobRegistryUrl, String endpointHost, int endpointPort) {
        this.statusManager = statusManager;
        this.jobManager = jobManager;
        this.jobRegistryUrl = jobRegistryUrl;
        this.host = endpointHost;
        this.port = endpointPort;

        this.handlers = new PoomjobsRunnerAPIHandlers.Builder()
                .runningJobPutHandler(this::handleRunningJobPut)
                .build();
    }

    public void start() {
        this.server = Undertow.builder()
                .addHttpListener(this.port, this.host)
                .setHandler(new CdmHttpUndertowHandler(new PoomjobsRunnerAPIProcessor(
                        "/",
                        new JsonFactory(),
                        this.handlers
                )))
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }

    private RunningJobPutResponse handleRunningJobPut(RunningJobPutRequest runningJobPutRequest) {
        synchronized (this.statusManager) {
            if(this.statusManager.status() == RunnerStatusData.Status.RUNNING) {
                String errorToken = UUID.randomUUID().toString();
                return RunningJobPutResponse.builder()
                        .status409(status ->
                            status.payload(error ->
                                error.code(Error.Code.RUNNER_IS_BUSY)
                                    .token(errorToken)
                                    .description("runner is busy, come back later.")
                        ))
                        .build();
            }
            this.statusManager.updateStatus(RunnerStatusData.Status.RUNNING);
        }
        Job job = runningJobPutRequest.payload();
        this.jobManager.processJob(job);

        return RunningJobPutResponse.builder().status201(status ->
                status.location(this.jobRegistryUrl + "/jobs/" + job.id()))
                .build();
    }
}
