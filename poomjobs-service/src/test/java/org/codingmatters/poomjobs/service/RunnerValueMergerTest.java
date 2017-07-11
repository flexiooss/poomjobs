package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poomjobs.api.types.RunnerData;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;
import org.codingmatters.poomjobs.api.types.runnerdata.Competencies;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/11/17.
 */
public class RunnerValueMergerTest {

    @Test
    public void whenCreation__withRunnerData__setsCallback_setsCompetencies() throws Exception {
        RunnerValue value = RunnerValueMerger.create().with(RunnerData.builder()
                .callback("http://call.me/up")
                .competencies(Competencies.builder()
                        .categories("CATEG")
                        .names("NAME")
                        .build())
                .build());

        assertThat(
                value,
                is(RunnerValue.builder()
                        .callback("http://call.me/up")
                        .competencies(org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies.builder()
                                .categories("CATEG")
                                .names("NAME")
                                .build())
                        .build())
        );
    }

    @Test
    public void whenUpdate__withRunnerData__updatesCallback_updatesCompetencies() throws Exception {
        RunnerValue value = RunnerValueMerger
                .merge(RunnerValue.builder()
                        .callback("http://dont.call/me")
                        .competencies(org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies.builder()
                                .categories("NOT")
                                .names("NEITHER")
                                .build())
                        .build())
                .with(RunnerData.builder()
                        .callback("http://call.me/up")
                        .competencies(Competencies.builder()
                                .categories("CATEG")
                                .names("NAME")
                                .build())
                        .build());

        assertThat(
                value,
                is(RunnerValue.builder()
                        .callback("http://call.me/up")
                        .competencies(org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies.builder()
                                .categories("CATEG")
                                .names("NAME")
                                .build())
                        .build())
        );
    }

    @Test
    public void whenCreation__withRunnerStatusData__statusIsSetted() throws Exception {
        RunnerValue value = RunnerValueMerger.create().with(RunnerStatusData.builder()
                .status(RunnerStatusData.Status.RUNNING)
                .build());

        assertThat(
                value,
                is(RunnerValue.builder()
                        .runtime(Runtime.builder()
                                .status(Runtime.Status.RUNNING)
                                .build())
                        .build())
        );
    }

    @Test
    public void whenUpdate__withRunnerStatusData__statusIsUpdated() throws Exception {
        RunnerValue value = RunnerValueMerger
                .merge(RunnerValue.builder()
                        .runtime(Runtime.builder()
                                .status(Runtime.Status.IDLE)
                                .build())
                        .build())
                .with(RunnerStatusData.builder()
                        .status(RunnerStatusData.Status.RUNNING)
                        .build());

        assertThat(
                value,
                is(RunnerValue.builder()
                        .runtime(Runtime.builder()
                                .status(Runtime.Status.RUNNING)
                                .build())
                        .build())
        );
    }
}
