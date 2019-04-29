package org.codingmatters.poom.runner.manager;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poomjobs.client.PoomjobsRunnerAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerAPIRequesterClient;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

public class DefaultRunnerClientFactory implements RunnerClientFactory {

    private final JsonFactory jsonFactory;
    private final HttpClientWrapper client;

    public DefaultRunnerClientFactory(JsonFactory jsonFactory, HttpClientWrapper client) {
        this.jsonFactory = jsonFactory;
        this.client = client;
    }

    @Override
    public PoomjobsRunnerAPIClient runnerClient(Runner runner) {
        PoomjobsRunnerAPIClient result = new PoomjobsRunnerAPIRequesterClient(
                new OkHttpRequesterFactory(this.client, () -> runner.callback()),
                this.jsonFactory,
                runner.callback());
        return result;
    }
}
