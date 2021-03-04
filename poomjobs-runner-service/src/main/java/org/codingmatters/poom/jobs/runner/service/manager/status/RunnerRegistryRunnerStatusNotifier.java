package org.codingmatters.poom.jobs.runner.service.manager.status;

import org.codingmatters.poom.jobs.runner.service.manager.exception.NotificationFailedException;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.RunnerPatchRequest;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;

import java.io.IOException;

public class RunnerRegistryRunnerStatusNotifier implements RunnerStatusNotifier {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerRegistryRunnerStatusNotifier.class);

    private final String runnerId;
    private final PoomjobsRunnerRegistryAPIClient client;

    public RunnerRegistryRunnerStatusNotifier(String runnerId, PoomjobsRunnerRegistryAPIClient client) {
        this.runnerId = runnerId;
        this.client = client;
    }

    @Override
    public void notify(RunnerStatus status) throws NotificationFailedException {
        try {
            RunnerPatchResponse response = this.client.runnerCollection().runner().patch(RunnerPatchRequest.builder()
                    .runnerId(this.runnerId)
                    .payload(RunnerStatusData.builder()
                            .status(this.registryStatusFrom(status))
                            .build())
                    .build());
            if(response.opt().status200().isEmpty()) {
                String token = log.tokenized().error("while notifying status to runner registry, expected 200, got : {}", response);
                throw new NotificationFailedException("failed notifying status, see logs with token : " + token);
            }
        } catch (IOException e) {
            throw new NotificationFailedException("failed reaching runner registry", e);
        }
    }

    private RunnerStatusData.Status registryStatusFrom(RunnerStatus status) {
        switch (status) {
            case IDLE:
                return RunnerStatusData.Status.IDLE;
            case BUSY:
                return RunnerStatusData.Status.RUNNING;
            default:
                return RunnerStatusData.Status.DISCONNECTED;
        }
    }
}
