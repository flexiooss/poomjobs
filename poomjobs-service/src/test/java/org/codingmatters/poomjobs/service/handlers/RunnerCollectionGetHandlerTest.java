package org.codingmatters.poomjobs.service.handlers;

import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerCriteria;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.support.paging.Rfc7233Pager;
import org.codingmatters.poomjobs.api.RunnerCollectionGetRequest;
import org.codingmatters.poomjobs.api.RunnerCollectionGetResponse;
import org.codingmatters.poomjobs.api.types.Error;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.handlers.mocks.MockedJobRepository;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 7/12/17.
 */
public class RunnerCollectionGetHandlerTest {
    private PoomjobsAPI api = new PoomjobsAPI(new MockedJobRepository(), RunnerRepository.createInMemory());
    private RunnerCollectionGetHandler handler = (RunnerCollectionGetHandler) this.api.handlers().runnerCollectionGetHandler();

    @Test
    public void maxPageSize() throws Exception {
        assertThat(this.handler.maxPageSize(), is(100));
    }

    @Test
    public void rfc7233Unit() throws Exception {
        assertThat(this.handler.rfc7233Unit(), is("Runner"));
    }

    @Test
    public void rfc7233Range() throws Exception {
        assertThat(this.handler.rfc7233Range(RunnerCollectionGetRequest.builder().range("0-10").build()), is("0-10"));
    }

    @Test
    public void whenNameCategoryAndRuntimeStatusAreNonNull__thenJobQuery() throws Exception {
        assertThat(this.handler.parseQuery(RunnerCollectionGetRequest.builder()
                        .nameCompetency("name")
                        .categoryCompetency("category")
                        .runtimeStatus("status")
                        .build()).criteria(),
                containsInAnyOrder(
                        RunnerCriteria.builder().nameCompetency("name").build(),
                        RunnerCriteria.builder().categoryCompetency("category").build(),
                        RunnerCriteria.builder().runtimeStatus("status").build()
                )
        );
    }
    @Test
    public void whenAllNameCategoryAndRuntimeStatusAreNull__thenJobQueryIsNull() throws Exception {
        assertThat(this.handler.parseQuery(RunnerCollectionGetRequest.builder().build()),is(nullValue()));
    }

    @Test
    public void partialJobList() throws Exception {
        for(int i = 0 ; i < 15 ; i++) {
            this.handler.repository().create(RunnerValue.builder().build());
        }

        Rfc7233Pager.Page<RunnerValue> page = Rfc7233Pager.forRequestedRange("5-9")
                .unit("String")
                .maxPageSize(10)
                .pager(this.handler.repository())
                .page();

        RunnerCollectionGetResponse response = this.handler.partialList(page);
        assertThat(response.status206(), is(notNullValue()));
        assertThat(response.status206().payload().size(), is(5));
        assertThat(response.status206().acceptRange(), is(page.acceptRange()));
        assertThat(response.status206().contentRange(), is(page.contentRange()));
    }

    @Test
    public void completeList() throws Exception {
        for(int i = 0 ; i < 10 ; i++) {
            this.handler.repository().create(RunnerValue.builder().build());
        }

        Rfc7233Pager.Page<RunnerValue> page = Rfc7233Pager.forRequestedRange("0-9")
                .unit("String")
                .maxPageSize(10)
                .pager(this.handler.repository())
                .page();

        RunnerCollectionGetResponse response = this.handler.completeList(page);
        assertThat(response.status200(), is(notNullValue()));
        assertThat(response.status200().payload().size(), is(10));
        assertThat(response.status200().contentRange(), is(page.contentRange()));
        assertThat(response.status200().acceptRange(), is(page.acceptRange()));
    }

    @Test
    public void invalidRangeQuery() throws Exception {
        for(int i = 0 ; i < 10 ; i++) {
            this.handler.repository().create(RunnerValue.builder().build());
        }

        Rfc7233Pager.Page<RunnerValue> page = Rfc7233Pager.forRequestedRange("10-9")
                .unit("String")
                .maxPageSize(10)
                .pager(this.handler.repository())
                .page();

        RunnerCollectionGetResponse response = this.handler.invalidRangeQuery(page, "error-token");
        assertThat(response.status416(), is(notNullValue()));
        assertThat(
                response.status416().payload(),
                is(Error.builder()
                        .token("error-token")
                        .description("start must be before end of range")
                        .code(Error.Code.ILLEGAL_RANGE_SPEC)
                        .build())
        );
        assertThat(response.status416().acceptRange(), is(page.acceptRange()));
        assertThat(response.status416().contentRange(), is(page.contentRange()));
    }

    @Test
    public void unexpectedError() throws Exception {
        RunnerCollectionGetResponse response = this.handler.unexpectedError(new RepositoryException("repo exploded"), "error-token");

        assertThat(response.status500(), is(notNullValue()));
        assertThat(response.status500().payload().token(), is("error-token"));
        assertThat(response.status500().payload().description(), is("unexpected error, see logs"));
        assertThat(response.status500().payload().code(), is(Error.Code.UNEXPECTED_ERROR));
    }


}