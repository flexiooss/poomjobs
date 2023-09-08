package org.codingmatters.poom.client;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.JobCollectionPostResponse;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poomjobs.service.PoomjobsJobRegistryAPI;
import org.codingmatters.poomjobs.service.api.PoomjobsJobRegistryAPIProcessor;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PoomjobsJobRegistryAPIRequesterClientTest {


    private final Repository<JobValue, PropertyQuery> jobRepository = JobRepository.createInMemory();
    private PoomjobsJobRegistryAPI serverApi = new PoomjobsJobRegistryAPI(jobRepository, new JsonFactory());
    private PoomjobsJobRegistryAPIProcessor processor = new PoomjobsJobRegistryAPIProcessor("/poom", new JsonFactory(), this.serverApi.handlers());
    private PoomjobsJobRegistryAPIClient apiClient;

    @Rule
    public UndertowResource undertow = new UndertowResource(new CdmHttpUndertowHandler(this.processor));

    @Before
    public void setUp() throws Exception {
        this.jobRepository.create(JobValue.builder()
                .name("test1")
                .category("categ")
                .build());
        this.jobRepository.create(JobValue.builder()
                .name("test2")
                .category("categ")
                .build());
        this.jobRepository.create(JobValue.builder()
                .name("test3")
                .category("categ")
                .build());
        this.jobRepository.create(JobValue.builder()
                .name("test4")
                .category("categ")
                .build());

        HttpClientWrapper client = OkHttpClientWrapper.build();
        JsonFactory jsonFactory = new JsonFactory();

        this.apiClient = new PoomjobsJobRegistryAPIRequesterClient(
                new OkHttpRequesterFactory(client, () -> this.undertow.baseUrl() + "/poom"),
                jsonFactory,
                this.undertow.baseUrl() + "/poom"
        );
    }


    @Test
    public void jobCollection_get() throws Exception {
        JobCollectionGetResponse resp = this.apiClient.jobCollection().get(JobCollectionGetRequest.builder()
                .range("1-2")
                .build());

        System.out.println(resp);

        assertThat(resp.status206().payload().size(), is(2));
        assertThat(resp.status206().acceptRange(), is("Job 100"));
        assertThat(resp.status206().contentRange(), is("Job 1-2/4"));

        resp = this.apiClient.jobCollection().get(JobCollectionGetRequest.builder()
                .build());
        assertThat(resp.status200().payload().size(), is(4));
        assertThat(resp.status200().acceptRange(), is("Job 100"));
        assertThat(resp.status200().contentRange(), is("Job 0-3/4"));
    }
    @Test
    public void jobCollection_post() throws Exception {
        JobCollectionPostResponse resp = this.apiClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("12")
                .payload(payload -> payload
                        .category("new")
                        .name("test"))
                .build());

        assertThat(resp.status201(), is(notNullValue()));
    }

    @Test
    public void jobCollection_postWithBuilderConsumer() throws Exception {
        JobCollectionPostResponse resp = this.apiClient.jobCollection().post(builder -> builder
                .accountId("12")
                .payload(payload -> payload
                        .category("new")
                        .name("test"))
        );

        assertThat(resp.status201(), is(notNullValue()));
    }

}