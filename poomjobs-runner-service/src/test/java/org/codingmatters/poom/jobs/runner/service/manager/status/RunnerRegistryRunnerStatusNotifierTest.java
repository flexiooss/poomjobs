package org.codingmatters.poom.jobs.runner.service.manager.status;

import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.jobs.runner.service.manager.exception.NotificationFailedException;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poomjobs.api.PoomjobsRunnerRegistryAPIHandlers;
import org.codingmatters.poomjobs.api.RunnerPatchRequest;
import org.codingmatters.poomjobs.api.RunnerPatchResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIHandlersClient;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RunnerRegistryRunnerStatusNotifierTest {

    private HandlerResource<RunnerPatchRequest, RunnerPatchResponse> patchRunner = new HandlerResource<RunnerPatchRequest, RunnerPatchResponse>() {
        @Override
        protected RunnerPatchResponse defaultResponse(RunnerPatchRequest runnerPatchRequest) {
            return RunnerPatchResponse.builder().status200(status -> status.payload(Runner.builder().build())).build();
        }
    };
    private PoomjobsRunnerRegistryAPIClient client = new PoomjobsRunnerRegistryAPIHandlersClient(
            new PoomjobsRunnerRegistryAPIHandlers.Builder()
                    .runnerPatchHandler(this.patchRunner)
                    .build(),
            Executors.newSingleThreadExecutor()
    );

    private final RunnerRegistryRunnerStatusNotifier notifier = new RunnerRegistryRunnerStatusNotifier("test-runner", this.client);

    @Test
    public void givenPatchReturns200__whenNotifyingIDLEStatus__thenPatchCalledWithRunnerIdAndIDLEStatus() throws Exception {
        this.notifier.notify(RunnerStatus.IDLE);

        assertThat(this.patchRunner.lastRequest().runnerId(), is("test-runner"));
        assertThat(this.patchRunner.lastRequest().payload(), is(RunnerStatusData.builder()
                .status(RunnerStatusData.Status.IDLE)
                .build()));
    }

    @Test
    public void givenPatchReturns200__whenNotifyingBUSYStatus__thenPatchCalledWithRunnerIdAndRUNNINGStatus() throws Exception {
        this.notifier.notify(RunnerStatus.BUSY);

        assertThat(this.patchRunner.lastRequest().runnerId(), is("test-runner"));
        assertThat(this.patchRunner.lastRequest().payload(), is(RunnerStatusData.builder()
                .status(RunnerStatusData.Status.RUNNING)
                .build()));
    }

    @Test
    public void givenPatchReturns200__whenNotifyingUNKNOWNStatus__thenPatchCalledWithRunnerIdAndDISCONNECTEDStatus() throws Exception {
        this.notifier.notify(RunnerStatus.UNKNOWN);

        assertThat(this.patchRunner.lastRequest().runnerId(), is("test-runner"));
        assertThat(this.patchRunner.lastRequest().payload(), is(RunnerStatusData.builder()
                .status(RunnerStatusData.Status.DISCONNECTED)
                .build()));
    }

    @Test(expected = NotificationFailedException.class)
    public void givenPatchDoesntReturn200__whenNotifying__thenNotificationFailedException() throws Exception {
        this.patchRunner.nextResponse(request -> RunnerPatchResponse.builder()
                .status500(status -> status.payload(error -> error.code(Error.Code.UNEXPECTED_ERROR)))
                .build());

        this.notifier.notify(RunnerStatus.BUSY);
    }
}