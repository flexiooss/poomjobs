package org.codingmatters.poom.jobs.collections.jobs;

import org.codingmatters.poom.generic.resource.domain.exceptions.MethodNotAllowedException;
import org.codingmatters.poom.generic.resource.domain.exceptions.NotFoundException;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.domain.repositories.inmemory.InMemoryRepositoryWithPropertyQuery;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.api.types.job.Processing;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

import static org.codingmatters.poom.services.tests.DateMatchers.around;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobRegistryCRUDTest {

    private Repository<JobValue, PropertyQuery> repository = InMemoryRepositoryWithPropertyQuery.validating(JobValue.class);

    private String accountId = UUID.randomUUID().toString();
    private String xExtension = UUID.randomUUID().toString();
    private ObjectValue context = ObjectValue.builder().property("test", v -> v.stringValue(UUID.randomUUID().toString())).build();

    private final List<Entity<JobValue>> created = Collections.synchronizedList(new LinkedList<>());
    private final List<Entity<JobValue>> updated = Collections.synchronizedList(new LinkedList<>());

    private final JobRegistryCRUD crud = new JobRegistryCRUD(this.repository, "https://some.where/here", this.accountId, this.xExtension, this.context, BigInteger.ONE, new PoomjobsJobRepositoryListener() {
        @Override
        public void jobCreated(Entity<JobValue> entity) {
            created.add(entity);
        }

        @Override
        public void jobUpdated(Entity<JobValue> entity) {
            updated.add(entity);
        }
    });

    @Test(expected = MethodNotAllowedException.class)
    public void whenReplacing__thenMethodNotAllowedException() throws Exception {
        this.crud.replaceEntityWith("42", null);
    }

    @Test
    public void givenValueExists__whenDeleting__thenDeleted() throws Exception {
        this.repository.createWithId("42", JobValue.builder().name("test").build());

        this.crud.deleteEntity("42");

        assertThat(this.repository.all(0, 0).total(), is(0L));
    }

    @Test(expected = NotFoundException.class)
    public void givenValueDoesntExist__whenDeleting__thenNotFoundException() throws Exception {
        this.crud.deleteEntity("42");
    }

    @Test
    public void givenJobHasMinimalData__whenCreating__thenJobValueCreatedInRepository_andJobComputedFromEntity() throws Exception {
        Entity<Job> created = this.crud.createEntityFrom(JobCreationData.builder()
                .name("name")
                .category("category")
                .build());

        assertThat(created, is(notNullValue()));

        Entity<JobValue> value = this.repository.retrieve(created.id());
        assertThat(value, is(notNullValue()));

        assertThat(
                created.value().withProcessing((Processing) null),
                is(Job.builder()
                        .name("name")
                        .category("category")
                        .id(value.id())
                        .version(value.version().toString())
                        .accounting(acc -> acc.accountId(this.accountId))
                        .context(this.context)
                        .status(Status.builder().run(Status.Run.PENDING).exit(null).build())
                    .build()
                ));

        assertThat(created.value().processing().submitted(), is(around(UTC.now())));
        assertThat(created.value().processing().started(), is(nullValue()));
        assertThat(created.value().processing().finished(), is(nullValue()));

        assertThat(this.created, contains(value));
    }

    @Test
    public void givenJobValueExists__whenRetrieving__thenRetrievedFromValue() throws Exception {
        LocalDateTime now = UTC.now();
        this.repository.createWithId("12", JobValue.builder()
                        .name("test").category("category")
                        .accounting(acc -> acc.accountId(this.accountId))
                        .processing(p -> p.submitted(now.minusHours(1)).started(now))
                        .status(s -> s.run(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Run.RUNNING).exit(null))
                .build());

        Optional<Entity<Job>> job = this.crud.retrieveEntity("12");
        assertThat(job.isPresent(), is(true));

        assertThat(job.get().value(), is(Job.builder()
                .id("12")
                .version("1")
                .status(s -> s.run(Status.Run.RUNNING).exit(null))
                .name("test").category("category")
                .accounting(acc -> acc.accountId(this.accountId))
                .processing(p -> p.submitted(now.minusHours(1)).started(now))
                .build()));
    }

    @Test
    public void givenJobValueExists__whenUpdating__thenBusinessRulesApplied_andUpdated() throws Exception {
        LocalDateTime now = UTC.now();
        this.repository.createWithId("12", JobValue.builder()
                .name("test").category("category")
                .accounting(acc -> acc.accountId(this.accountId))
                .processing(p -> p.submitted(now.minusHours(2)).started(now.minusHours(1)))
                .status(s -> s.run(org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status.Run.RUNNING).exit(null))
                .build());

        Entity<Job> job = this.crud.updateEntityWith("12", JobUpdateData.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.builder()
                                .run(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.Run.DONE)
                                .exit(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.Exit.SUCCESS)
                                .build())
                        .result("job result")
                .build());

        assertThat(job.value().status(), is(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build()));
        assertThat(job.value().result(), is("job result"));
        assertThat(job.value().processing().submitted(), is(now.minusHours(2)));
        assertThat(job.value().processing().started(), is(now.minusHours(1)));
        assertThat(job.value().processing().finished(), is(around(UTC.now())));

        assertThat(this.updated, contains(this.repository.retrieve("12")));
    }
}