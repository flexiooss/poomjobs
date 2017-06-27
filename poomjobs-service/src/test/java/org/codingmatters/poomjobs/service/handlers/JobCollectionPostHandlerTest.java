package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.JobValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/23/17.
 */
public class JobCollectionPostHandlerTest {

    private Repository<JobValue, JobQuery> repository = JobRepository.createInMemory();
    private PoomjobsAPI api = new PoomjobsAPI(this.repository);

    @Test
    public void whenJobCreationDataIsValid__thenReturnsStatus201_andJobCreated() throws Exception {
        JobCollectionPostResponse response = this.api.handlers().jobCollectionPostHandler().apply(JobCollectionPostRequest.builder()
                .accountId("1212")
                .payload(JobCreationData.builder()
                        .category("category")
                        .name("name")
                        .arguments("one", "two")
                        .build())
                .build());

        assertThat(response.status201(), is(notNullValue()));

        Entity<JobValue> entity = this.repository.all(0, 1).get(0);
        assertThat(entity, is(notNullValue()));
        assertThat(response.status201().location(), is("%API_PATH%/jobs/" + entity.id()));

        assertThat(entity.value().category(), is("category"));
        assertThat(entity.value().name(), is("name"));
        assertThat(entity.value().arguments(), contains("one", "two"));
    }

    @Test
    public void whenJobCreationDataIsInvalid__thenReturnsStatus400_andJobNotCreated() throws Exception {
        JobCollectionPostResponse response = this.api.handlers().jobCollectionPostHandler().apply(JobCollectionPostRequest.builder()
                .accountId("1212")
                .payload(JobCreationData.builder()
                        .category("category")
                        .build())
                .build());

        assertThat(response.status400(), is(notNullValue()));
        assertThat(this.repository.all(0, 1), is(empty()));

        assertThat(response.status400().payload().token(), is(notNullValue()));
        assertThat(response.status400().payload().code(), is(Error.Code.ILLEGAL_JOB_SPEC));
        assertThat(response.status400().payload().description(), is("cannot create a job with no name"));
    }
}
