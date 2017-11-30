package org.codingmatters.poom.runner.internal;

import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class StatusManager {
    static private final Logger log = LoggerFactory.getLogger(StatusManager.class);

    private final AtomicReference<RunnerStatusData.Status> currentStatus = new AtomicReference<>(RunnerStatusData.Status.IDLE);
    private final String id;
    private final PoomjobsRunnerRegistryAPIClient runnerRegistryAPIClient;
    private final Long ttl;
    private final ScheduledExecutorService updateWorker;

    public StatusManager(String id, PoomjobsRunnerRegistryAPIClient runnerRegistryAPIClient, Long ttl, ScheduledExecutorService updateWorker) {
        this.id = id;
        this.runnerRegistryAPIClient = runnerRegistryAPIClient;
        this.ttl = ttl;
        this.updateWorker = updateWorker;
    }


    public void updateStatus(RunnerStatusData.Status status) {
        this.currentStatus.set(status);
        this.updateStatus();
    }


    private void updateStatus() {
        try {
            RunnerStatusData.Status status = this.currentStatus.get();
            RunnerPatchResponse response = this.runnerRegistryAPIClient.runnerCollection().runner().patch(request ->
                    request
                            .runnerId(this.id)
                            .payload(payload -> payload.status(status))
            );
            if (response.status200() != null) {
                log.debug("updated status for {} with status : {}", this.id, status);
                this.scheduleNextStatusUpdate(response.status200().payload().runtime().lastPing());
            } else {
                log.error("runner registry refused our status notification for runner {} with response : {}",
                        this.id,
                        response
                );
                return;
            }
        } catch (IOException e) {
            log.error("error notifying status to runner repository for runner " + this.id, e);
        }
    }


    public void scheduleNextStatusUpdate(LocalDateTime lastPing) {
        LocalDateTime nextNotification = lastPing.plus(this.ttl, ChronoUnit.MILLIS);
        this.updateWorker.schedule(
                () -> this.updateStatus(),
                Duration.between(LocalDateTime.now(), nextNotification).toMillis(), TimeUnit.MILLISECONDS
        );
    }
}
