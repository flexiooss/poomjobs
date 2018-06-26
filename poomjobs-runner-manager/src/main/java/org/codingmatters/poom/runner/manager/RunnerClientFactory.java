package org.codingmatters.poom.runner.manager;

import org.codingmatters.poom.client.PoomjobsRunnerAPIClient;
import org.codingmatters.poomjobs.api.types.Runner;

public interface RunnerClientFactory {
    PoomjobsRunnerAPIClient runnerClient(Runner runner);
}
