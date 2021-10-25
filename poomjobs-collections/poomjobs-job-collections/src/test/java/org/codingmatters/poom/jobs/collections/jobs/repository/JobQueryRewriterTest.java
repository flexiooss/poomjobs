package org.codingmatters.poom.jobs.collections.jobs.repository;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobCriteria;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobQueryRewriterTest {
    private Repository<JobValue, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(JobValue.class);

    @Test
    public void categoryFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create(JobValue.builder().category("CATEG-" + (i % 3)).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(
                new JobQueryRewriter().propertyQuery(JobQuery.builder().criteria(
                            JobCriteria.builder().category("CATEG-2").build()
                        ).build(),
                        null
                ),
                0, 100
        );

        assertThat(list, hasSize(3));
        assertThat(
                list.stream().map(e -> e.value().category()).collect(Collectors.toList()),
                everyItem(is("CATEG-2"))
        );
    }

    @Test
    public void nameFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create(JobValue.builder().name("NAME-" + (i % 3)).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(
                new JobQueryRewriter().propertyQuery(JobQuery.builder().criteria(
                                JobCriteria.builder().names("NAME-2").build()
                        ).build(),
                        null
                ),
                0, 100
        );

        assertThat(list, hasSize(3));
        assertThat(
                list.stream().map(e -> e.value().name()).collect(Collectors.toList()),
                everyItem(is("NAME-2"))
        );
    }

    @Test
    public void manyNamesFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.repository.create(JobValue.builder().name("NAME-" + (i % 3)).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(
                new JobQueryRewriter().propertyQuery(JobQuery.builder().criteria(
                                JobCriteria.builder().names("NAME-0", "NAME-1").build()
                        ).build(),
                        null
                ),
                0, 100
        );

        assertThat(list, hasSize(7));
        assertThat(
                list.stream().map(e -> e.value().name()).collect(Collectors.toList()),
                everyItem(oneOf("NAME-0", "NAME-1"))
        );
    }

    @Test
    public void runStatusFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            Status.Run status = Status.Run.values()[i % Status.Run.values().length];
            this.repository.create(JobValue.builder().status(Status.builder().run(status).build()).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(
                new JobQueryRewriter().propertyQuery(JobQuery.builder().criteria(
                                JobCriteria.builder().runStatus("RUNNING").build()
                        ).build(),
                        null
                ),
                0, 100
        );

        assertThat(list, hasSize(3));
        assertThat(
                list.stream().map(e -> e.value().status().run()).collect(Collectors.toList()),
                everyItem(is(Status.Run.RUNNING))
        );
    }

    @Test
    public void exitStatusFilter() throws Exception {
        for (int i = 0; i < 10; i++) {
            Status.Exit status = Status.Exit.values()[i % Status.Exit.values().length];
            this.repository.create(JobValue.builder().status(Status.builder().exit(status).build()).build());
        }

        PagedEntityList<JobValue> list = this.repository.search(
                new JobQueryRewriter().propertyQuery(JobQuery.builder().criteria(
                                JobCriteria.builder().exitStatus(Status.Exit.SUCCESS.name()).build()
                        ).build(),
                        null
                ),
                0, 100
        );

        assertThat(list, hasSize(5));
        assertThat(
                list.stream().map(e -> e.value().status().exit()).collect(Collectors.toList()),
                everyItem(is(Status.Exit.SUCCESS))
        );
    }

    @Test
    public void namesCategAndStatus() throws Exception {
        // name IN ('short', 'long') && category == 'c' && status.run == 'PENDING'
        for (int i = 0; i < 10; i++) {
            Status.Run status = Status.Run.values()[i % Status.Run.values().length];
            Entity<JobValue> e = this.repository.create(JobValue.builder()
                    .category("CATEG-" + (i % 4))
                    .name("NAME-" + (i % 3))
                            .status(s -> s.run(status))
                    .build());
            System.out.println(e.value());
        }

        PagedEntityList<JobValue> list = this.repository.search(
                new JobQueryRewriter().propertyQuery(JobQuery.builder().criteria(
                                JobCriteria.builder().names("NAME-0", "NAME-2").build(),
                                JobCriteria.builder().category("CATEG-0").build(),
                                JobCriteria.builder().runStatus("PENDING").build()
                        ).build(),
                        null
                ),
                0, 100
        );

        assertThat(list, hasSize(1));
        // JobValue{category=CATEG-0, name=NAME-0, arguments=null, result=null, status=Status{run=PENDING, exit=null}, processing=null, accounting=null, context=null}
        assertThat(list.valueList().get(0), is(JobValue.builder().category("CATEG-0").name("NAME-0").status(s -> s.run(Status.Run.PENDING)).build()));
    }
}