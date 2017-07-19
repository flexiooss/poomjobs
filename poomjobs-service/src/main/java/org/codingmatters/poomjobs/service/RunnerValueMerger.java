package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poomjobs.api.types.RunnerData;
import org.codingmatters.poomjobs.api.types.RunnerStatusData;

/**
 * Created by nelt on 7/11/17.
 */
public class RunnerValueMerger {

    static public RunnerValueMerger create() {
        return new RunnerValueMerger(RunnerValue.builder().build());
    }

    static public RunnerValueMerger merge(RunnerValue value) {
        return new RunnerValueMerger(value);
    }

    private final RunnerValue currentValue;

    private RunnerValueMerger(RunnerValue currentValue) {
        this.currentValue = currentValue;
    }

    public RunnerValue with(RunnerData runnerData) {
        RunnerValue result = this.currentValue;

        result = result
                .withCallback(runnerData.callback())
                .withTimeToLive(runnerData.ttl())
                ;

        if(runnerData.competencies() != null) {
            result = result.withCompetencies(Competencies.builder()
                    .categories(runnerData.competencies().categories().toArray(new String[0]))
                    .names(runnerData.competencies().names().toArray(new String[0]))
                    .build());
        } else {
            result = result.withCompetencies(null);
        }

        return result;
    }

    public RunnerValue with(RunnerStatusData runnerStatusData) {
        RunnerValue result = this.currentValue;
        Runtime.Builder runtime = Runtime.from(result.runtime());
        if(runtime == null) {
            runtime = Runtime.builder();
        }

        if(runnerStatusData.status() != null) {
            runtime.status(Runtime.Status.valueOf(runnerStatusData.status().name()));
        } else {
            runtime.status(null);
        }

        return result.withRuntime(runtime.build());
    }
}
