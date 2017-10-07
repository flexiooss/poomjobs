package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Accounting;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourceGetRequest;
import org.codingmatters.poomjobs.api.JobResourceGetResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.service.PoomjobsJobRegistryAPI;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/19/17.
 */
public class JobResourceGetHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();
    private JobResourceGetHandler handler = (JobResourceGetHandler) new PoomjobsJobRegistryAPI(this.repository).handlers().jobResourceGetHandler();

    @Test
    public void log() throws Exception {
        assertThat(this.handler.log().getName(), is(JobResourceGetHandler.class.getName()));
    }

    @Test
    public void repository() throws Exception {
        assertThat(this.handler.repository(), is(this.repository));
    }

    @Test
    public void entityId() throws Exception {
        assertThat(this.handler.entityId(JobResourceGetRequest.builder().jobId("12").build()), is("12"));
    }

    @Test
    public void whenEntityFoundCalled__thenStatus200Returned() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.builder()
                .category("jobs/for/test")
                .name("test-job")
                .arguments("a", "b", "c")
                .status(Status.builder().run(Status.Run.PENDING).build())
                .accounting(Accounting.builder().accountId("121212").build())
                .processing(Processing.builder()
                        .submitted(LocalDateTime.now().minus(10, ChronoUnit.MINUTES))
                        .build())
                .build());

        JobResourceGetResponse response = this.handler.entityFound(entity);

        assertThat(response.status200(), is(notNullValue()));
        assertThat(
                response.status200().payload(),
                is(Job.builder()
                        .id(entity.id())
                        .version(entity.version().toString())
                        .category("jobs/for/test")
                        .name("test-job")
                        .arguments("a", "b", "c")
                        .status(org.codingmatters.poomjobs.api.types.job.Status.builder()
                                .run(org.codingmatters.poomjobs.api.types.job.Status.Run.PENDING)
                                .build())
                        .accounting(org.codingmatters.poomjobs.api.types.job.Accounting.builder()
                                .accountId("121212")
                                .build())
                        .processing(org.codingmatters.poomjobs.api.types.job.Processing.builder()
                                .submitted(entity.value().processing().submitted())
                                .build())
                        .build())
        );
    }

    @Test
    public void whenEntityNotFoundCalled__thenStatus404Returned() throws Exception {
        JobResourceGetResponse response = this.handler.entityNotFound("error-token");

        assertThat(response.status404(), is(notNullValue()));
        assertThat(
                response.status404().payload(),
                is(Error.builder()
                        .token("error-token")
                        .code(Error.Code.JOB_NOT_FOUND)
                        .description("no job found with the given jobId")
                        .build())
        );
    }

    @Test
    public void whenUnexpectedError__thenStatus500Returned() throws Exception {
        JobResourceGetResponse response = this.handler.unexpectedError("error-token");

        assertThat(response.status500(), is(notNullValue()));
        assertThat(
                response.status500().payload(),
                is(Error.builder()
                        .token("error-token")
                        .code(Error.Code.UNEXPECTED_ERROR)
                        .description("unexpected error, see logs")
                        .build())
        );
    }
}