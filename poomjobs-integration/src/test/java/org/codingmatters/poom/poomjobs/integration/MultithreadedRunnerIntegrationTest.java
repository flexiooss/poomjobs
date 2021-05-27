package org.codingmatters.poom.poomjobs.integration;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.jobs.runner.service.RunnerService;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerServiceInitializationException;
import org.codingmatters.poom.runner.JobProcessor;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.JobCollectionPostRequest;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIRequesterClient;
import org.codingmatters.poomjobs.registries.service.PoomjobRegistriesService;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;

public class MultithreadedRunnerIntegrationTest {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(MultithreadedRunnerIntegrationTest.class);

    private PoomjobsJobRegistryAPIRequesterClient jobRegistryClient;
    private PoomjobsRunnerRegistryAPIRequesterClient runnerRegistryClient;

    static int freePort() throws IOException {
        int port;
        try(ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        return port;
    }

    private PoomjobRegistriesService registries;
    private RunnerService runnerService;

    @Before
    public void setUp() throws Exception {
        int registriesPort = freePort();
        this.registries = new PoomjobRegistriesService("localhost", registriesPort, Executors.newFixedThreadPool(10));
        this.registries.start();

        HttpClientWrapper httpClientWrapper = OkHttpClientWrapper.build();
        JsonFactory jsonFactory = new JsonFactory();

        String jobRegistryUrl = String.format("http://localhost:%s/poomjobs-jobs/v1/", registriesPort);
        String runnerRegistryUrl = String.format("http://localhost:%s/poomjobs-runners/v1", registriesPort);

        this.runnerRegistryClient = new PoomjobsRunnerRegistryAPIRequesterClient(new OkHttpRequesterFactory(httpClientWrapper, () -> runnerRegistryUrl), jsonFactory, () -> runnerRegistryUrl);
        this.jobRegistryClient = new PoomjobsJobRegistryAPIRequesterClient(new OkHttpRequesterFactory(httpClientWrapper, () -> jobRegistryUrl), jsonFactory, () -> jobRegistryUrl);

    }

    private void createAndStartRunnerServiceWithConcurrency(int concurrency) throws IOException, InterruptedException {
        int port = freePort();
        String backup = System.getProperty("service.url");
        System.setProperty("service.url", "http://localhost:" + port);
        try {
            this.runnerService = RunnerService.setup()
                    .jobs(
                            "c", new String[]{"short", "long"},
                            new JobProcessor.Factory() {
                                @Override
                                public JobProcessor createFor(Job job) {
                                    return () -> {
                                        log.info("PROCESSING TEST JOB {}", job);
                                        try {
                                            if (job.name().equals("short")) {
                                                Thread.sleep(500L);
                                            } else {
                                                Thread.sleep(5000L);
                                            }
                                        } catch (InterruptedException e) {
                                        }

                                        log.info("DONE PROCESSING TEST JOB {}", job);
                                        return job.withStatus(Status.builder().run(Status.Run.DONE).exit(Status.Exit.SUCCESS).build());
                                    };
                                }
                            }
                    )
                    .clients(
                            this.runnerRegistryClient,
                            this.jobRegistryClient
                    )
                    .concurrency(concurrency)
                    .endpoint("localhost", port)
                    .ttl(1000L)
                    .build()
            ;
        } finally {
            if(backup == null) {
                System.clearProperty("service.url");
            } else {
                System.setProperty("service.url", backup);
            }
        }

        new Thread(() -> {
            try {
                this.runnerService.run();
            } catch (RunnerServiceInitializationException e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(1000L);
    }

    @After
    public void tearDown() throws Exception {
        this.runnerService.stop();
        this.registries.stop();
    }

    @Test
    public void oneJob_noConcurrency() throws Exception {
        this.createAndStartRunnerServiceWithConcurrency(1);

        this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("blurp")
                .payload(JobCreationData.builder()
                        .category("c").name("short")
                        .build())
                .build());

        Eventually.timeout(2, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-0/1")
        );
    }

    @Test
    public void manyJob_noConcurrency() throws Exception {
        this.createAndStartRunnerServiceWithConcurrency(1);
        for (int i = 0; i < 10; i++) {
            this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .accountId("blurp")
                    .payload(JobCreationData.builder()
                            .category("c").name("short")
                            .build())
                    .build());
        }

        Eventually.timeout(20, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-9/10")
        );
    }

    @Test
    public void oneJob_concurrency() throws Exception {
        this.createAndStartRunnerServiceWithConcurrency(10);

        this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("blurp")
                .payload(JobCreationData.builder()
                        .category("c").name("short")
                        .build())
                .build());


        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-0/1")
        );
    }

    @Test
    public void manyJob_concurrency() throws Exception {
        this.createAndStartRunnerServiceWithConcurrency(10);
        for (int i = 0; i < 50; i++) {
            this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .accountId("blurp")
                    .payload(JobCreationData.builder()
                            .category("c").name("short")
                            .build())
                    .build());
        }

        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-49/50")
        );
    }
}
