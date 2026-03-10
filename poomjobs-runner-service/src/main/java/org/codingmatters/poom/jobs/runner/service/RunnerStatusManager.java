package org.codingmatters.poom.jobs.runner.service;

import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poomjobs.api.RunnerPatchRequest;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunnerStatusManager implements Runnable, StatusManager {

    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerStatusManager.class);
    public static final int MAX_STATUS_FAIL_COUNT = Env.optional("MAX_STATUS_FAIL_COUNT").orElse(Env.Var.value("3")).asInteger();

    private int failCount;

    private final PoomjobsRunnerRegistryAPIClient runnerRegistryClient;
    private final String runnerId;

    private final Random random = new Random();
    private final long maxTimeout;
    private final long minTimeout;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final StatusManager poolStatusManager;
    private final Runnable stopRunner;
    private final ScheduledExecutorService executor;

    public RunnerStatusManager(PoomjobsRunnerRegistryAPIClient runnerRegistryClient, String runnerId, long maxTimeout, long minTimeout, StatusManager poolStatusManager, Runnable stopRunner) {
        this.runnerRegistryClient = runnerRegistryClient;
        this.runnerId = runnerId;
        this.maxTimeout = maxTimeout;
        this.minTimeout = minTimeout;
        this.poolStatusManager = poolStatusManager;
        this.stopRunner = stopRunner;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(new ThreadGroup("runner-status-manager"), runnable));
        this.failCount = 0;
    }

    public void start() {
        this.running.set(true);
        this.executor.schedule(this, nextTimeout(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        try {
            this.running.set(false);
            this.executor.shutdownNow();
        } finally {
            this.patchRunnerStatus(RunnerStatusData.builder().status(RunnerStatusData.Status.DISCONNECTED).build());
        }
    }

    @Override
    public void run() {
        this.patchRunnerStatus(RunnerStatusData.builder().status(status()).build());
        this.executor.schedule(this, nextTimeout(), TimeUnit.MILLISECONDS);
    }

    private long nextTimeout() {
        if (this.maxTimeout > this.minTimeout) {
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
            log.debug("status manager - status updated to {}", statusData);
            response.opt().status200().orElseThrow(() -> new IOException("Bad response code " + response));
            log.debug("runner status patched to {}", statusData);
            failCount = 0;
        } catch (IOException e) {
            log.error("[GRAVE] while patching runner status, failed reaching runner registry", e);
            failCount++;
            if (failCount >= MAX_STATUS_FAIL_COUNT) {
                log.error("Failed patching runner status for " + MAX_STATUS_FAIL_COUNT + " times. ");
                running.set(false);
                stopRunner.run();
            }
        }
    }

    @Override
    public RunnerStatusData.Status status() {
        if (running.get()) {
            return poolStatusManager.status();
        } else {
            return RunnerStatusData.Status.DISCONNECTED;
        }
    }
}
