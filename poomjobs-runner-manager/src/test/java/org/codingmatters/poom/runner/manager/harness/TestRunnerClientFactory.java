package org.codingmatters.poom.runner.manager.harness;

import org.codingmatters.poom.client.PoomjobsRunnerAPIClient;
import org.codingmatters.poom.runner.manager.DefaultRunnerClientFactory;
import org.codingmatters.poom.runner.manager.RunnerClientFactory;
import org.codingmatters.poomjobs.api.RunningJobPutRequest;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.types.Runner;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TestRunnerClientFactory implements RunnerClientFactory {
    private final DefaultRunnerClientFactory deleguate;

    private final AtomicBoolean nextCallFails = new AtomicBoolean(false);

    public TestRunnerClientFactory(DefaultRunnerClientFactory deleguate) {
        this.deleguate = deleguate;
    }

    public TestRunnerClientFactory nextCallWillFail(boolean fails) {
        this.nextCallFails.set(fails);
        return this;
    }

    @Override
    public PoomjobsRunnerAPIClient runnerClient(Runner runner) {
        return new TestPoomjobsRunnerAPIClient(deleguate.runnerClient(runner));
    }

    private class TestPoomjobsRunnerAPIClient implements PoomjobsRunnerAPIClient {
        private final PoomjobsRunnerAPIClient deleguate;

        public TestPoomjobsRunnerAPIClient(PoomjobsRunnerAPIClient deleguate) {
            this.deleguate = deleguate;
        }

        @Override
        public RunningJob runningJob() {
            return new TestRunningJob(deleguate.runningJob());
        }

        private class TestRunningJob implements RunningJob {
            private final RunningJob deleguate;

            public TestRunningJob(RunningJob deleguate) {
                this.deleguate = deleguate;
            }

            @Override
            public RunningJobPutResponse put(RunningJobPutRequest request) throws IOException {
                if(nextCallFails.get()) {
                    throw new IOException("injected failure");
                }
                return deleguate.put(request);
            }

            @Override
            public RunningJobPutResponse put(Consumer<RunningJobPutRequest.Builder> request) throws IOException {
                if(nextCallFails.get()) {
                    throw new IOException("injected failure");
                }
                return deleguate.put(request);
            }
        }
    }
}
