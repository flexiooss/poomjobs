package org.codingmatters.poom.runner;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.exception.RunnerInitializationException;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GenericRunner {

    static private Logger log = LoggerFactory.getLogger(GenericRunner.class);

    static private long MIN_TTL = 1000L;
    static private long NOTIFY_BEFORE_TTL = 500L;

    private final PoomjobsJobRegistryAPIClient jobRegistryAPIClient;
    private final PoomjobsRunnerRegistryAPIClient runnerRegistryAPIClient;
    private final ExecutorService jobWorker;

    private final String callbackBaseUrl;
    private final Long ttl;
    private final String[] jobCategories;
    private final String[] jobNames;

    private final ScheduledExecutorService updateWorker = Executors.newSingleThreadScheduledExecutor();

    private String id;
    private final AtomicReference<RunnerStatusData.Status> currentStatus = new AtomicReference<>(RunnerStatusData.Status.IDLE);


    public GenericRunner(RunnerConfiguration configuration) {
        this.jobRegistryAPIClient = configuration.jobRegistryAPIClient();
        this.runnerRegistryAPIClient = configuration.runnerRegistryAPIClient();
        this.jobWorker = configuration.jobWorker();
        this.callbackBaseUrl = configuration.callbackBaseUrl();
        this.ttl = Math.max(MIN_TTL, configuration.ttl());
        this.jobCategories = configuration.jobCategories().toArray(new String[configuration.jobCategories().size()]);
        this.jobNames = configuration.jobNames().toArray(new String[configuration.jobNames().size()]);
    }

    public void start() throws RunnerInitializationException {
        RunnerCollectionPostResponse response = null;
        try {
            LocalDateTime firstPing = LocalDateTime.now();
            response = this.runnerRegistryAPIClient.runnerCollection().post(request ->
                    request
                        .payload(payload ->
                                payload
                                        .callback(this.callbackBaseUrl)
                                        .ttl(this.ttl)
                                        .competencies(competencies ->
                                                competencies
                                                        .categories(this.jobCategories)
                                                        .names(this.jobNames)
                                        )
                        )
                        .build());
            this.scheduleNextStatusUpdate(firstPing);
        } catch (IOException e) {
            log.error("cannot connect to runner registry", e);
            throw new RunnerInitializationException("cannot connect to runner registry", e);
        }
        if(response.status201() != null) {
            String[] splitted = response.status201().location().split("/");
            this.id = splitted[splitted.length - 1];
        } else {
            log.error("registry refused to register runner : {}", response);
            throw new RunnerInitializationException("registry refused to register runner : " + response.toString());
        }
    }

    public String id() {
        return this.id;
    }

    private void updateStatus() {
        try {
            RunnerStatusData.Status status = this.currentStatus.get();
            RunnerPatchResponse response = this.runnerRegistryAPIClient.runnerCollection().runner().patch(request ->
                    request
                            .runnerId(this.id())
                            .payload(payload -> payload.status(status))
            );
            if(response.status200() != null) {
                log.debug("updated status for {} with status : {}", this.id(), status);
                this.scheduleNextStatusUpdate(response.status200().payload().runtime().lastPing());
            } else {
                log.error("runner registry refused our status notification for runner {} with response : {}",
                        this.id(),
                        response
                );
                return;
            }
        } catch (IOException e) {
            log.error("error notifying status to runner repository for runner " + this.id(), e);
        }
    }

    private void scheduleNextStatusUpdate(LocalDateTime lastPing) {
        LocalDateTime nextNotification = lastPing.plus(this.ttl, ChronoUnit.MILLIS);
        this.updateWorker.schedule(
                this::updateStatus,
                Duration.between(LocalDateTime.now(), nextNotification).toMillis(), TimeUnit.MILLISECONDS
        );
    }
}
