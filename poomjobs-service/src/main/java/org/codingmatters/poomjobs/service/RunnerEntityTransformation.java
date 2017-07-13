package org.codingmatters.poomjobs.service;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.api.types.runner.Competencies;
import org.codingmatters.poomjobs.api.types.runner.Runtime;

/**
 * Created by nelt on 7/13/17.
 */
public class RunnerEntityTransformation {

    public static RunnerEntityTransformation transform(Entity<RunnerValue> entity) {
        return new RunnerEntityTransformation(entity);
    }

    private final Entity<RunnerValue> entity;

    public RunnerEntityTransformation(Entity<RunnerValue> entity) {
        this.entity = entity;
    }

    public Runner asRunner() {
        Runner.Builder result = Runner.builder()
                .id(this.entity.id())
                .callback(this.entity.value().callback())
                .ttl(this.entity.value().timeToLive());

        if(this.entity.value().competencies() != null) {
            result.competencies(Competencies.builder()
                    .categories(this.entity.value().competencies().categories() != null ?
                            this.entity.value().competencies().categories().toArray(new String[0]) : null)
                    .names(this.entity.value().competencies().names() != null ?
                            this.entity.value().competencies().names().toArray(new String[0]) : null)
                    .build()
            );
        }

        if(this.entity.value().runtime() != null) {
            result.runtime(Runtime.builder()
                    .created(this.entity.value().runtime().created())
                    .lastPing(this.entity.value().runtime().lastPing())
                    .status(this.runnerStatus(this.entity.value().runtime().status()))
                    .build());
        }

        return result.build();
    }

    private Runtime.Status runnerStatus(org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime.Status status) {
        switch (status) {
            case IDLE:
                return Runtime.Status.IDLE;
            case RUNNING:
                return Runtime.Status.RUNNING;
            case DISCONNECTED:
                return Runtime.Status.DISCONNECTED;
            default:
                return null;
        }
    }
}
