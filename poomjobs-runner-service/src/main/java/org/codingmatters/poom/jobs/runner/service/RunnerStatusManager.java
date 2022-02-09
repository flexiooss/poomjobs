package org.codingmatters.poom.jobs.runner.service;

import org.codingmatters.poom.patterns.pool.FeederPool;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.RunnerPatchRequest;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RunnerStatusManager implements Runnable, FeederPool.Listener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerStatusManager.class);

    private final PoomjobsRunnerRegistryAPIClient runnerRegistryClient;
    private final String runnerId;

    private final Random random = new Random();
    private final long maxTimeout;
    private final long minTimeout;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<RunnerStatusData> nextStatus = new AtomicReference<>(RunnerStatusData.builder().status(RunnerStatusData.Status.IDLE).build());

    public RunnerStatusManager(PoomjobsRunnerRegistryAPIClient runnerRegistryClient, String runnerId, long maxTimeout, long minTimeout) {
        this.runnerRegistryClient = runnerRegistryClient;
        this.runnerId = runnerId;
        this.maxTimeout = maxTimeout;
        this.minTimeout = minTimeout;
    }

    @Override
    public void becameIdle() {
        synchronized (this.nextStatus) {
            this.nextStatus.set(RunnerStatusData.builder().status(RunnerStatusData.Status.IDLE).build());
            this.nextStatus.notify();
        }
    }

    @Override
    public void becameBusy() {
        synchronized (this.nextStatus) {
            this.nextStatus.set(RunnerStatusData.builder().status(RunnerStatusData.Status.RUNNING).build());
            this.nextStatus.notify();
        }
    }


    public void start() {
        this.running.set(true);
    }

    public void stop() {
        this.running.set(false);
        synchronized (this.nextStatus) {
            this.nextStatus.notify();
        }
    }

    @Override
    public void run() {
        while(this.running.get()) {
            synchronized (this.nextStatus) {
                try {
                    this.nextStatus.wait(this.nextTimeout());
                } catch (InterruptedException e) {}
                if(this.running.get()) {
                    RunnerStatusData statusData = this.nextStatus.get();
                    if (statusData != null) {
                        this.patchRunnerStatus(statusData);
                    }
                }
            }
        }
    }

    private long nextTimeout() {
        if(this.maxTimeout > this.minTimeout) {
            return this.minTimeout + this.random.nextInt((int) (this.maxTimeout - this.minTimeout));
        } else {
            return this.minTimeout;
        }
    }

    private void patchRunnerStatus(RunnerStatusData statusData) {
        try {
            RunnerPatchResponse response = this.runnerRegistryClient.runnerCollection().runner().patch(RunnerPatchRequest.builder()
                    .runnerId(this.runnerId)
                    .payload(statusData)
                    .build());
            if(response.opt().status200().isEmpty()) {
                log.error("[GRAVE] unexpected response from runner registry while patching runner status");
            } else {
                log.debug("runner status patched to {}", statusData);
            }
        } catch (IOException e) {
            log.error("[GRAVE] while patching runner status, failed reaching runner registry");
        }
    }
}
