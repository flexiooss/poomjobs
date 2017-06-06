package org.codingmatters.poom.poomjobs.domain.repositories;

import org.codingmatters.poom.poomjobs.domain.values.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.junit.Test;

import static org.codingmatters.poom.poomjobs.domain.repositories.EntityValueMatcher.valueMatches;
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
            this.repository.create(JobValue.Builder.builder().category("CATEG-" + (i % 3)).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(JobQuery.Builder.builder().criteria(
                JobCriteria.Builder.builder().category("CATEG-2").build()
        ).build(), 0, 100);

        assertThat(list, hasSize(3));
        assertThat(list, everyItem(valueMatches(o -> o.category().equals("CATEG-2"))));
        System.out.println(list);
    }

}