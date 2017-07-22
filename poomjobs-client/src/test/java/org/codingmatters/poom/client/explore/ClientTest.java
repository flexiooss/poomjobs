package org.codingmatters.poom.client.explore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.JobCollectionGetRequest;
import org.codingmatters.poomjobs.api.JobCollectionGetResponse;
import org.codingmatters.poomjobs.api.PoomjobsAPIHandlers;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status206;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.json.JobReader;
import org.codingmatters.poomjobs.service.PoomjobsAPI;
import org.codingmatters.poomjobs.service.api.PoomjobsAPIProcessor;
import org.codingmatters.rest.api.client.Requester;
import org.codingmatters.rest.api.client.ResponseDelegate;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequester;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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
    public void direct() throws Exception {
        this.jobRepository.create(JobValue.builder()
                .name("test")
                .category("categ")
                .build());

        OkHttpClient client = new OkHttpClient();

        System.out.println(this.undertow.baseUrl());

        Request request = new Request.Builder()
                .url(this.undertow.baseUrl() + "/poom/jobs")
                .get().build();

        Response response = client.newCall(request).execute();
        assertThat(response.code(), is(200));
        System.out.println(response.headers());
        System.out.println(response.body().contentType());
        System.out.println(response.body().string());
    }

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

        PoomjobsAPIHandlers api = new PoomjobsAPIHandlers.Builder()
                .jobCollectionGetHandler(req -> {
                    String url = this.undertow.baseUrl() + "/poom";
                    Requester requester = new OkHttpRequester(client, url).path("/jobs");
                    if(req.range() != null) {
                        requester.queryParameter("range", req.range());
                    }

                    try {
                        ResponseDelegate response = requester.get();

                        System.out.println(response);
                        if(response.code() == 200) {
                            return JobCollectionGetResponse.builder()
                                    .status200(Status200.builder()
                                            .payload(readListValue(jsonFactory.createParser(response.body())))
                                            .acceptRange(response.header("accept-range"))
                                            .contentRange(response.header("content-range"))
                                            .build())
                                    .build();
                        }
                        if(response.code() == 206) {
                            return JobCollectionGetResponse.builder()
                                    .status206(Status206.builder()
                                            .payload(readListValue(jsonFactory.createParser(response.body())))
                                            .acceptRange(response.header("accept-range"))
                                            .contentRange(response.header("content-range"))
                                            .build())
                                    .build();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .build();

        JobCollectionGetResponse resp = api.jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .range("1-2")
                .build());
        System.out.println(resp);
        assertThat(resp.status206().payload().size(), is(2));

        resp = api.jobCollectionGetHandler().apply(JobCollectionGetRequest.builder()
                .build());
        System.out.println(resp);
        assertThat(resp.status200().payload().size(), is(4));
    }



    private List<Job> readListValue(JsonParser parser) throws IOException {
        parser.nextToken();
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            LinkedList<Job> listValue = new LinkedList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if(parser.currentToken() == JsonToken.VALUE_NULL) {
                    listValue.add(null);
                } else {
                    listValue.add(new JobReader().read(parser));
                }
            }
            return listValue;
        }
        throw new IOException("failed reading Job list");
    }
}
