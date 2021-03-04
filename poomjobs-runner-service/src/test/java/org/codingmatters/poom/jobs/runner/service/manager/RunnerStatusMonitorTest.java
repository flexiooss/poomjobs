package org.codingmatters.poom.jobs.runner.service.manager;

import org.codingmatters.poom.jobs.runner.service.exception.UnregisteredTokenException;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatusChangedListener;
import org.codingmatters.poomjobs.runner.domain.RunnerToken;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RunnerStatusMonitorTest {

    private final List<String> stateChanges = Collections.synchronizedList(new LinkedList<>());

    RunnerStatusMonitor monitor = new RunnerStatusMonitor("test", new RunnerStatusChangedListener() {
        @Override
        public void onIdle(RunnerStatus was) {
            stateChanges.add(String.format("%s to %s", was.toString(), "IDLE"));
        }

        @Override
        public void onBusy(RunnerStatus was) {
            stateChanges.add(String.format("%s to %s", was.toString(), "BUSY"));
        }
    });

    @Test
    public void givenMonitorEmpty__whenAddingTokens__thenTokensAreLabeled_andInitialStatusIsDISCONNECTED() throws Exception {
        for (int i = 0; i < 10; i++) {
            RunnerToken runnerToken = this.monitor.addToken();
            assertThat("runner " + i + " label", runnerToken.label(), is(String.format("test-runner-%03d", i)));
            assertThat("runner " + i + " initial status", this.monitor.status(runnerToken), is(RunnerStatus.UNKNOWN));
        }
    }

    @Test(expected = UnregisteredTokenException.class)
    public void givenTokenNotCreatedByMonitor__whenGettingStatus__thenUnregisteredTokenException() throws Exception {
        this.monitor.status(RunnerToken.builder().build());
    }

    @Test(expected = UnregisteredTokenException.class)
    public void givenTokenIsNull__whenGettingStatus__thenUnregisteredTokenException() throws Exception {
        this.monitor.status(null);
    }

    @Test
    public void givenMonitorHasTwoTokens__whenSettingStatusForOne__thenStatusForThisTokenIsChanged_andTheOtherIsUnchanged() throws Exception {
        RunnerToken token = this.monitor.addToken();
        RunnerToken another = this.monitor.addToken();

        for (RunnerStatus status : RunnerStatus.values()) {
            if(status != RunnerStatus.UNKNOWN) {
                this.monitor.statusFor(token, status);
                assertThat(this.monitor.status(token), is(status));
                assertThat(this.monitor.status(another), is(RunnerStatus.UNKNOWN));
            }
        }
    }

    @Test(expected = UnregisteredTokenException.class)
    public void givenTokenNotCreatedByMonitor__whenSettingStatus__thenUnregisteredTokenException() throws Exception {
        this.monitor.statusFor(RunnerToken.builder().build(), RunnerStatus.IDLE);
    }

    @Test(expected = UnregisteredTokenException.class)
    public void givenTokenIsNull__whenSettingStatus__thenUnregisteredTokenException() throws Exception {
        this.monitor.statusFor(null, RunnerStatus.IDLE);
    }

    @Test
    public void whenMonitorIsEmpty__thenMonitorBUSY() throws Exception {
        assertThat(this.monitor.status(), is(RunnerStatus.BUSY));
    }

    @Test
    public void givenMonitorHasOneToken__whenTokenIsBUSY__thenMonitorIsBUSY() throws Exception {
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);

        assertThat(this.monitor.status(), is(RunnerStatus.BUSY));
    }

    @Test
    public void givenMonitorHasOneToken__whenTokenIsUNKNOWN__thenMonitorIsBUSY() throws Exception {
        this.monitor.addToken();

        assertThat(this.monitor.status(), is(RunnerStatus.BUSY));
    }

    @Test
    public void givenMonitorHasOneToken__whenTokenIsIDLE__thenMonitorIsIDLE() throws Exception {
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.IDLE);

        assertThat(this.monitor.status(), is(RunnerStatus.IDLE));
    }

    @Test
    public void givenMonitorHasManyToken__whenTokensAreUNKNOWN__thenMonitorIsBUSY() throws Exception {
        this.monitor.addToken();
        this.monitor.addToken();
        this.monitor.addToken();

        assertThat(this.monitor.status(), is(RunnerStatus.BUSY));
    }

    @Test
    public void givenMonitorHasManyToken__whenTokensAreIDLE__thenMonitorIsIDLE() throws Exception {
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.IDLE);
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.IDLE);
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.IDLE);

        assertThat(this.monitor.status(), is(RunnerStatus.IDLE));
    }

    @Test
    public void givenMonitorHasManyToken__whenTokensAreBUSY__thenMonitorIsBUSY() throws Exception {
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);

        assertThat(this.monitor.status(), is(RunnerStatus.BUSY));
    }

    @Test
    public void givenMonitorHasManyToken__whenSomeAreUNKNOWN_andSomeAreIDLE_andSomeAreBusy__thenMonitorIsIDLE() throws Exception {
        this.monitor.addToken();
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.IDLE);
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);

        assertThat(this.monitor.status(), is(RunnerStatus.IDLE));
    }

    @Test
    public void givenMonitorHasManyToken__whenSomeAreUNKNOWN_andOneIsIDLE__thenMonitorIsIDLE() throws Exception {
        this.monitor.addToken();
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.IDLE);
        this.monitor.addToken();

        assertThat(this.monitor.status(), is(RunnerStatus.IDLE));
    }

    @Test
    public void givenMonitorHasManyToken__whenSomeAreBusy_andOneIsIDLE__thenMonitorIsIDLE() throws Exception {
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.IDLE);
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);

        assertThat(this.monitor.status(), is(RunnerStatus.IDLE));
    }

    @Test
    public void givenMonitorHasManyToken__whenSomeAreUNKNOWN_andOneIsBusy__thenMonitorIsBUSY() throws Exception {
        this.monitor.addToken();
        this.monitor.addToken();
        this.monitor.statusFor(this.monitor.addToken(), RunnerStatus.BUSY);

        assertThat(this.monitor.status(), is(RunnerStatus.BUSY));
    }

    @Test
    public void givenJustCreated__whenStatusSetToIDLE__thenStateChangesFromBUSYToIDLE() throws Exception {
        RunnerToken token = this.monitor.addToken();

        this.monitor.statusFor(token, RunnerStatus.IDLE);

        assertThat(this.stateChanges, contains("BUSY to IDLE"));
    }

    @Test
    public void givenJustCreated__whenStatusSetToBUSY__thenStateDoesNotChange() throws Exception {
        RunnerToken token = this.monitor.addToken();

        this.monitor.statusFor(token, RunnerStatus.BUSY);

        assertThat(this.stateChanges, is(empty()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenJustCreated__whenStatusSetToUNKNOWN__thenIllegalArgumentException() throws Exception {
        RunnerToken token = this.monitor.addToken();
        this.monitor.statusFor(token, RunnerStatus.UNKNOWN);
    }

    @Test
    public void givenStatusIsBUSY__whenStatusSetToIDLE__thenStateChangedFromBUSYToIDLE() throws Exception {
        RunnerToken token = this.monitor.addToken();
        this.monitor.statusFor(token, RunnerStatus.BUSY);
        this.stateChanges.clear();

        this.monitor.statusFor(token, RunnerStatus.IDLE);

        assertThat(this.stateChanges, contains("BUSY to IDLE"));
    }

    @Test
    public void givenStatusIsIDLE__whenStatusSetToBUSY__thenStateChangedFromIDLEToBUSY() throws Exception {
        RunnerToken token = this.monitor.addToken();
        this.monitor.statusFor(token, RunnerStatus.IDLE);
        this.stateChanges.clear();

        this.monitor.statusFor(token, RunnerStatus.BUSY);

        assertThat(this.stateChanges, contains("IDLE to BUSY"));
    }

    @Test
    public void givenStatusIsIDLE__whenStatusSetToIDLE__thenStateDoesNotChange() throws Exception {
        RunnerToken token = this.monitor.addToken();
        this.monitor.statusFor(token, RunnerStatus.IDLE);
        this.stateChanges.clear();

        this.monitor.statusFor(token, RunnerStatus.IDLE);

        assertThat(this.stateChanges, is(empty()));
    }

    @Test
    public void givenStatusIsBUSY__whenStatusSetToBUSY__thenStateDoesNotChange() throws Exception {
        RunnerToken token = this.monitor.addToken();
        this.monitor.statusFor(token, RunnerStatus.BUSY);
        this.stateChanges.clear();

        this.monitor.statusFor(token, RunnerStatus.BUSY);

        assertThat(this.stateChanges, is(empty()));
    }
}