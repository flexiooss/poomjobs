package org.codingmatters.poom.poomjobs.domain.runners.repositories;

import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerCriteria;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Competencies;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/10/17.
 */
public class RunnerRepositoryTest {

    private Repository<RunnerValue, RunnerQuery> repository = RunnerRepository.createInMemory();

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create(RunnerValue.builder()
                    .callback("URL-" + (i % 3))
                    .competencies(Competencies.builder()
                            .categories("CATEG-" + (i % 3))
                            .names("NAME-" + (i % 3))
                            .build())
                    .timeToLive(12L)
                    .runtime(Runtime.builder()
                            .created(LocalDateTime.now().minus((i % 3), ChronoUnit.HOURS))
                            .lastPing(LocalDateTime.now().minus((i % 3), ChronoUnit.MINUTES))
                            .status(Runtime.Status.values()[(i % 3)])
                            .build())
                    .build());
        }
    }

    @Test
    public void nameCompetencyFilter() throws Exception {
        PagedEntityList<RunnerValue> list = this.repository.search(RunnerQuery.builder()
                        .criteria(
                                RunnerCriteria.builder()
                                        .nameCompetency("NAME-1")
                                        .build()
                        )
                        .build(),
                0, 9
        );
        assertThat(list, hasSize(3));
    }

    @Test
    public void categoryCompetencyFilter() throws Exception {
        PagedEntityList<RunnerValue> list = this.repository.search(RunnerQuery.builder()
                        .criteria(
                                RunnerCriteria.builder()
                                        .categoryCompetency("CATEG-1")
                                        .build()
                        )
                        .build(),
                0, 9
        );
        assertThat(list, hasSize(3));
    }

    @Test
    public void runtimeStatusFilter() throws Exception {
        PagedEntityList<RunnerValue> list = this.repository.search(RunnerQuery.builder()
                        .criteria(
                                RunnerCriteria.builder()
                                        .runtimeStatus(Runtime.Status.IDLE.name())
                                        .build()
                        )
                        .build(),
                0, 9
        );
        assertThat(list, hasSize(4));
    }
}