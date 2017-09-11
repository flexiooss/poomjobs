package org.codingmatters.poom.client.explore;

import com.fasterxml.jackson.core.JsonFactory;
import okhttp3.OkHttpClient;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.api.PoomjobsAPIProcessor;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ClientTest {

    private final Repository<JobValue, JobQuery> jobRepository = JobRepository.createInMemory();
    private final Repository<RunnerValue, RunnerQuery> runnerRepository = RunnerRepository.createInMemory();
    private PoomjobsAPI serverApi = new PoomjobsAPI(jobRepository, runnerRepository);
    private PoomjobsAPIProcessor processor = new PoomjobsAPIProcessor("/poom", new JsonFactory(), this.serverApi.handlers());

    @Rule
    public UndertowResource undertow = new UndertowResource(new CdmHttpUndertowHandler(this.processor));

    @Test
    public void api() throws Exception {
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

        OkHttpClient client = new OkHttpClient();
        JsonFactory jsonFactory = new JsonFactory();

        PoomjobsAPIClient apiClient = new PoomjobsAPIRequesterClient(
                new OkHttpRequesterFactory(client),
                jsonFactory,
                this.undertow.baseUrl() + "/poom"
        );

        JobCollectionGetResponse resp = apiClient.jobCollection().get(JobCollectionGetRequest.builder()
                .range("1-2")
                .build());
        assertThat(resp.status206().payload().size(), is(2));

        resp = apiClient.jobCollection().get(JobCollectionGetRequest.builder()
                .build());
        System.out.println(resp);
        assertThat(resp.status200().payload().size(), is(4));
    }
}
