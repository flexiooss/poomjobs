package org.codingmatters.poom.runner.manager;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.client.PoomjobsRunnerAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerAPIRequesterClient;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;

public class DefaultRunnerClientFactory implements RunnerClientFactory {

    private final JsonFactory jsonFactory;
    private final OkHttpClientWrapper client;

    public DefaultRunnerClientFactory(JsonFactory jsonFactory, OkHttpClientWrapper client) {
        this.jsonFactory = jsonFactory;
        this.client = client;
    }

    @Override
    public PoomjobsRunnerAPIClient runnerClient(Runner runner) {
        PoomjobsRunnerAPIClient result = new PoomjobsRunnerAPIRequesterClient(
                new OkHttpRequesterFactory(this.client),
                this.jsonFactory,
                runner.callback());
        return result;
    }
}
