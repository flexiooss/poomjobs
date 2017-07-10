package org.codingmatters.poom.poomjobs.domain.jobs.repositories;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.junit.Test;

import static org.codingmatters.poom.poomjobs.domain.jobs.repositories.EntityValueMatcher.valueMatches;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/5/17.
 */
public class JobRepositoryTest {
    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();

    @Test
    public void categoryFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create(JobValue.builder().category("CATEG-" + (i % 3)).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(JobQuery.builder().criteria(
                JobCriteria.builder().category("CATEG-2").build()
        ).build(), 0, 100);

        assertThat(list, hasSize(3));
        assertThat(list, everyItem(valueMatches(o -> o.category().equals("CATEG-2"))));
    }

    @Test
    public void nameFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create(JobValue.builder().name("NAME-" + (i % 3)).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(JobQuery.builder().criteria(
                JobCriteria.builder().name("NAME-2").build()
        ).build(), 0, 100);

        assertThat(list, hasSize(3));
        assertThat(list, everyItem(valueMatches(o -> o.name().equals("NAME-2"))));
    }

    @Test
    public void runStatusFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            Status.Run status = Status.Run.values()[i % Status.Run.values().length];
            this.repository.create(JobValue.builder().status(Status.builder().run(status).build()).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(JobQuery.builder().criteria(
                JobCriteria.builder().runStatus("RUNNING").build()
        ).build(), 0, 100);

        assertThat(list, hasSize(3));
        assertThat(list, everyItem(valueMatches(o -> o.status() != null && o.status().run().equals(Status.Run.RUNNING))));
    }

    @Test
    public void exitStatusFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            Status.Exit status = Status.Exit.values()[i % Status.Exit.values().length];
            this.repository.create(JobValue.builder().status(Status.builder().exit(status).build()).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(JobQuery.builder().criteria(
                JobCriteria.builder().exitStatus(Status.Exit.SUCCESS.name()).build()
        ).build(), 0, 100);

        assertThat(list, hasSize(5));
        assertThat(list, everyItem(valueMatches(o -> o.status() != null && o.status().exit().equals(Status.Exit.SUCCESS))));
    }
}