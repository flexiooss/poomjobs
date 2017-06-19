package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Accounting;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobvalue.Status;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourceGetRequest;
import org.codingmatters.poomjobs.api.JobResourceGetResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/19/17.
 */
public class JobResourceGetHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();
    private PoomjobsAPI api = new PoomjobsAPI(this.repository);

    @Test
    public void getJob_whenInRepository__willReturnAStatus200() throws Exception {
        Entity<JobValue> jobEntity = this.repository.create(JobValue.Builder.builder()
                .category("jobs/for/test")
                .name("test-job")
                .arguments("a", "b", "c")
                .status(Status.Builder.builder().run(Status.Run.PENDIND).build())
                .accounting(Accounting.Builder.builder().accountId("121212").build())
                .processing(Processing.Builder.builder().submitted(LocalDateTime.now().minus(10, ChronoUnit.MINUTES)).build())
                .build());

        JobResourceGetResponse response = this.api.handlers().jobResourceGetHandler().apply(JobResourceGetRequest.Builder.builder()
                .jobId(jobEntity.id())
                .build());

        Job job = response.status200().payload();

        assertThat(job.name(), is(jobEntity.value().name()));
        assertThat(job.version(), is(jobEntity.version().toString()));
        assertThat(job.category(), is(jobEntity.value().category()));
        assertThat(job.arguments().toArray(), is(jobEntity.value().arguments().toArray()));
        assertThat(job.status().run().name(), is(jobEntity.value().status().run().name()));
        assertThat(job.accounting().accountId(), is(jobEntity.value().accounting().accountId()));
        assertThat(job.processing().started(), is(jobEntity.value().processing().started()));
    }

    @Test
    public void getJob_whenNotInRepository__willReturnAStatus404() throws Exception {
        JobResourceGetResponse response = this.api.handlers().jobResourceGetHandler().apply(JobResourceGetRequest.Builder.builder()
                .jobId("not in repo")
                .build());

        assertThat(response.status200(), is(nullValue()));

        assertThat(response.status404().payload().code(), is(Error.Code.JOB_NOT_FOUND));
        assertThat(response.status404().payload().description(), is("no job found with the given jobId"));
        assertThat(response.status404().payload().token(), is(notNullValue()));
    }
}