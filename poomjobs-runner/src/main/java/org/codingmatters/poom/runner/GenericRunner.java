package org.codingmatters.poom.runner;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.exception.RunnerInitializationException;
import org.codingmatters.poomjobs.api.RunnerCollectionPostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class GenericRunner {

    static private Logger log = LoggerFactory.getLogger(GenericRunner.class);

    private final PoomjobsJobRegistryAPIClient jobRegistryAPIClient;
    private final PoomjobsRunnerRegistryAPIClient runnerRegistryAPIClient;
    private final ExecutorService jobWorker;
    private final String callbackBaseUrl;
    private final Long ttl;
    private final String[] jobCategories;
    private final String[] jobNames;

    private String id;

    public GenericRunner(RunnerConfiguration configuration) {
        this.jobRegistryAPIClient = configuration.jobRegistryAPIClient();
        this.runnerRegistryAPIClient = configuration.runnerRegistryAPIClient();
        this.jobWorker = configuration.jobWorker();
        this.callbackBaseUrl = configuration.callbackBaseUrl();
        this.ttl = configuration.ttl();
        this.jobCategories = configuration.jobCategories().toArray(new String[configuration.jobCategories().size()]);
        this.jobNames = configuration.jobNames().toArray(new String[configuration.jobNames().size()]);

    }

    public void start() throws RunnerInitializationException {
        RunnerCollectionPostResponse response = null;
        try {
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
}
