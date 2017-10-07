package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.services.domain.change.Change;
import org.codingmatters.poom.services.domain.change.Validation;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobResourcePatchRequest;
import org.codingmatters.poomjobs.api.JobResourcePatchResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.JobUpdateData;
import org.codingmatters.poomjobs.service.JobEntityTransformation;
import org.codingmatters.poomjobs.service.PoomjobsJobsAPI;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/20/17.
 */
public class JobResourcePutHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();
    private JobResourcePutHandler handler = (JobResourcePutHandler) new PoomjobsJobsAPI(this.repository).handlers().jobResourcePatchHandler();


    @Test
    public void log() throws Exception {
        assertThat(this.handler.log().getName(), is(JobResourcePutHandler.class.getName()));
    }

    @Test
    public void repository() throws Exception {
        assertThat(this.handler.repository(), is(this.repository));
    }

    @Test
    public void entityId() throws Exception {
        assertThat(this.handler.entityId(JobResourcePatchRequest.builder().jobId("12").build()), is("12"));
    }

    @Test
    public void valueUpdate() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.builder()
                .name("name")
                .status(Status.builder()
                        .run(Status.Run.RUNNING)
                        .build())
                .build());
        Change<JobValue> update = this.handler.valueUpdate(JobResourcePatchRequest.builder()
                .accountId("12")
                .jobId(entity.id())
                .currentVersion(entity.version().toString())
                .payload(JobUpdateData.builder()
                        .status(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.builder()
                                .exit(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.Exit.SUCCESS)
                                .run(org.codingmatters.poomjobs.api.types.jobupdatedata.Status.Run.DONE)
                                .build())
                        .build())
                .build(),
                entity);

        assertThat(update.currentValue(), is(entity.value()));
        assertThat(
                update.newValue(),
                is(entity.value().withStatus(Status.builder()
                        .exit(Status.Exit.SUCCESS)
                        .run(Status.Run.DONE)
                        .build())
                )
        );

    }

    @Test
    public void whenEntityUpdatedCalled__thenStatus200Returned() throws Exception {
        Entity<JobValue> entity = this.repository.create(JobValue.builder().build());
        JobResourcePatchResponse response = this.handler.entityUpdated(entity);

        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().payload(), is(JobEntityTransformation.transform(entity).asJob()));
    }

    @Test
    public void whenInvalidUpdateCalled__thenStatus400Returned() throws Exception {
        JobResourcePatchResponse response = this.handler.invalidUpdate(this.createChange(false, "invalid update", null, null), "error-token");

        assertThat(response.status400(), is(notNullValue()));
        assertThat(response.status400().payload().code(), is(Error.Code.ILLEGAL_JOB_CHANGE));
        assertThat(response.status400().payload().token(), is("error-token"));
        assertThat(response.status400().payload().description(), is("invalid update"));
    }

    @Test
    public void whenEntityNotFoundCalled__thenStatus404Returned() throws Exception {
        JobResourcePatchResponse response = this.handler.entityNotFound("error-token");

        assertThat(response.status404(), is(notNullValue()));
        assertThat(response.status404().payload().code(), is(Error.Code.JOB_NOT_FOUND));
        assertThat(response.status404().payload().token(), is("error-token"));
        assertThat(response.status404().payload().description(), is("no job found with the given jobId"));
    }

    @Test
    public void whenUnexpectedErrorCalled__theStatus500Returned() throws Exception {
        JobResourcePatchResponse response = this.handler.unexpectedError(new RepositoryException("erro"), "error-token");

        assertThat(response.status500(), is(notNullValue()));
        assertThat(response.status500().payload().code(), is(Error.Code.UNEXPECTED_ERROR));
        assertThat(response.status500().payload().token(), is("error-token"));
        assertThat(response.status500().payload().description(), is("unexpected error, see logs"));
    }

    private Change<JobValue> createChange(boolean valid, String message, JobValue currentValue, JobValue newValue) {
        return new Change<JobValue>(currentValue, newValue) {
            @Override
            protected Validation validate() {
                return new Validation(valid, message);
            }

            @Override
            public JobValue applied() {
                return null;
            }
        };
    }
}