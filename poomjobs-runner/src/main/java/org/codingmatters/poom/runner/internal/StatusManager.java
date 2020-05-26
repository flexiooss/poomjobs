package org.codingmatters.poom.runner.internal;

import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private final AtomicReference<Boolean> scheduled = new AtomicReference<>( false );

    public StatusManager(String id, PoomjobsRunnerRegistryAPIClient runnerRegistryAPIClient, Long ttl, ScheduledExecutorService updateWorker) {
        this.id = id;
        this.runnerRegistryAPIClient = runnerRegistryAPIClient;
        this.ttl = ttl;
        this.updateWorker = updateWorker;
    }

    public synchronized void updateStatus(RunnerStatusData.Status status) {
        this.currentStatus.set(status);
        this.updateStatus();
    }

    private synchronized void updateStatus() {
        try {
            RunnerStatusData.Status status = this.currentStatus.get();
            RunnerPatchResponse response = this.runnerRegistryAPIClient.runnerCollection().runner().patch(request ->
                    request
                            .runnerId(this.id)
                            .payload(payload -> payload.status(status))
            );
            if (response.status200() != null) {
                log.debug("updated status for {} with status : {}", this.id, status);
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

    public synchronized void scheduleUpdates( ) {
        if( !scheduled.get() ){
            scheduled.set( true );
            updateWorker.scheduleAtFixedRate(
                    this::updateStatus,
                    ttl,
                    ttl,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public RunnerStatusData.Status status() {
        return this.currentStatus.get();
    }
}
