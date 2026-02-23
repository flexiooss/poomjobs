package org.codingmatters.poom.poomjobs.integration;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.jobs.runner.service.RunnerService;
import org.codingmatters.poom.jobs.runner.service.exception.RunnerServiceInitializationException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.EnvProvider;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.services.tests.DateMatchers;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.*;
import org.codingmatters.poomjobs.api.jobcollectiongetresponse.Status200;
import org.codingmatters.poomjobs.api.types.JobCreationData;
import org.codingmatters.poomjobs.api.types.runner.Runtime;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIRequesterClient;
import org.codingmatters.poomjobs.client.PoomjobsRunnerRegistryAPIRequesterClient;
import org.codingmatters.poomjobs.registries.service.PoomjobRegistriesService;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(Parameterized.class)
public class MultithreadedRunnerIntegrationTest {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(MultithreadedRunnerIntegrationTest.class);
    public static final long RUNNER_TTL = 1000L;


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

    @Parameterized.Parameters(name = "jop pool : {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "experimental" }, { "legacy" }
        });
    }
    private final boolean experimentalPool;

    public MultithreadedRunnerIntegrationTest(String experimentalPool) {
        this.experimentalPool = experimentalPool.equals("experimental");
    }

    @Before
    public void setUp() throws Exception {
        Map<String, String> env = Collections.synchronizedMap(new HashMap<>());
        env.put(RunnerService.USE_EXPERIMENTAL_POOL, this.experimentalPool ? "true" : "false");
        EnvProvider.provider(s -> env.get(s));

        int registriesPort = freePort();
        this.registries = new PoomjobRegistriesService("localhost", registriesPort, Executors.newFixedThreadPool(30), Executors.newSingleThreadExecutor());
        this.registries.start();

        HttpClientWrapper httpClientWrapper = OkHttpClientWrapper.build();
        JsonFactory jsonFactory = new JsonFactory();

        String jobRegistryUrl = String.format("http://localhost:%s/poomjobs-jobs/v1/", registriesPort);
        String runnerRegistryUrl = String.format("http://localhost:%s/poomjobs-runners/v1", registriesPort);

        this.runnerRegistryClient = new PoomjobsRunnerRegistryAPIRequesterClient(new OkHttpRequesterFactory(httpClientWrapper, () -> runnerRegistryUrl), jsonFactory, () -> runnerRegistryUrl);
        this.jobRegistryClient = new PoomjobsJobRegistryAPIRequesterClient(new OkHttpRequesterFactory(httpClientWrapper, () -> jobRegistryUrl), jsonFactory, () -> jobRegistryUrl);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EnvProvider.reset();
    }

    private void createAndStartRunnerServiceWithConcurrency(int concurrency) throws IOException, InterruptedException {
        int port = freePort();
        String backup = System.getProperty("service.url");
        System.setProperty("service.url", "http://localhost:" + port);
        try {
            this.runnerService = RunnerService.setup()
                    .jobs(
                            "c", new String[]{"short", "long"},
                            new TestJobFactory()
                    )
                    .clients(
                            this.runnerRegistryClient,
                            this.jobRegistryClient
                    )
                    .concurrency(concurrency)
                    .endpoint("localhost", port)
                    .ttl(RUNNER_TTL)
                    .exitOnUnrecoverableError(false)
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
        try {
            this.runnerService.stop();
        } catch (Exception e) {
            log.error("error stopping runner", e);
        }
        this.registries.stop();
    }

    @Test
    public void oneJob_noConcurrency() throws Exception {
        log.info("\n\n\noneJob_noConcurrency\n\n\n");
        this.createAndStartRunnerServiceWithConcurrency(1);

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
    public void twoJobs_noConcurrency() throws Exception {
        log.info("\n\n\ntwoJobs_noConcurrency\n\n\n");
        this.createAndStartRunnerServiceWithConcurrency(1);

        this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("blurp1")
                .payload(JobCreationData.builder()
                        .category("c").name("short").arguments("job-1")
                        .build())
                .build());
        this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("blurp2")
                .payload(JobCreationData.builder()
                        .category("c").name("short").arguments("job-2")
                        .build())
                .build());

        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-1/2")
        );
    }

    @Test
    public void manyJob_noConcurrency() throws Exception {
        log.info("\n\n\nmanyJob_noConcurrency\n\n\n");
        this.createAndStartRunnerServiceWithConcurrency(1);
        for (int i = 0; i < 10; i++) {
            JobCollectionPostResponse e = this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .accountId("blurp")
                    .payload(JobCreationData.builder()
                            .category("c").name("short")
                            .build())
                    .build());
            System.out.println("######## posted " + i + " : " + e);
        }

        Eventually.timeout(20, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-9/10")
        );
    }

    @Test
    public void oneJob_concurrency() throws Exception {
        log.info("\n\n\noneJob_concurrency\n\n\n");
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
    public void oneJobBeforeStart_concurrency() throws Exception {
        log.info("\n\n\noneJobBeforeStart_concurrency\n\n\n");
        this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("blurp")
                .payload(JobCreationData.builder()
                        .category("c").name("short")
                        .build())
                .build());

        this.createAndStartRunnerServiceWithConcurrency(10);

        Eventually.timeout(5, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-0/1")
        );
    }

    @Test
    public void manyJob_concurrency() throws Exception {
        log.info("\n\n\nmanyJob_concurrency\n\n\n");
        this.createAndStartRunnerServiceWithConcurrency(10);
        for (int i = 0; i < 50; i++) {
            this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .accountId("blurp")
                    .payload(JobCreationData.builder()
                            .category("c").name("short").arguments("job-" + i)
                            .build())
                    .build());
            log.info("submitted job-" + i);
        }

        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> {
                    String contentRange = this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange();
                    log.info("done jobs range : {}", contentRange);
                    Status200 pending = this.jobRegistryClient.jobCollection().get(get -> get.runStatus("PENDING")).status200();
                    log.info("pending jobs range : {}", pending.contentRange());
                    log.info("\t pending jobs : {}", pending.payload().stream().map(job -> job.arguments().get(0)).collect(Collectors.joining(", ")));
                    Status200 running = this.jobRegistryClient.jobCollection().get(get -> get.runStatus("RUNNING")).status200();
                    log.info("running jobs range : {}", running.contentRange());
                    log.info("\t running jobs : {}", running.payload().stream().map(job -> job.arguments().get(0)).collect(Collectors.joining(", ")));
                    return contentRange;
                },
                is("Job 0-49/50")
        );
    }

    @Test
    public void manyJobBeforeStart_concurrency() throws Exception {
        log.info("\n\n\nmanyJobBeforeStart_concurrency\n\n\n");
        for (int i = 0; i < 50; i++) {
            this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .accountId("blurp")
                    .payload(JobCreationData.builder()
                            .category("c").name("short")
                            .build())
                    .build());
        }

        assertThat(
                this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-0/0")
        );
        assertThat(
                this.jobRegistryClient.jobCollection().get(JobCollectionGetRequest.builder().build()).status200().contentRange(),
                is("Job 0-49/50")
        );

        this.createAndStartRunnerServiceWithConcurrency(10);

        Eventually.timeout(20, TimeUnit.SECONDS).assertThat(
                () -> this.jobRegistryClient.jobCollection().get(get -> get.runStatus("DONE")).status200().contentRange(),
                is("Job 0-49/50")
        );
    }

    @Test
    public void givenNoConcurrency__whenNoJob__thenRunnerRegistered_andIdle() throws Exception {
        log.info("\n\n\ngivenNoConcurrency__whenNoJob__thenRunnerRegistered_andIdle\n\n\n");
        LocalDateTime start = UTC.now();
        this.createAndStartRunnerServiceWithConcurrency(1);

        RunnerCollectionGetResponse response = this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build());
        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200 got : " + response));

        System.out.println(response.status200());

        assertThat(response.status200().payload().get(0).runtime().created(), is(DateMatchers.around(start)));
        assertThat(response.status200().payload().get(0).runtime().lastPing(), is(DateMatchers.around(start)));
        assertThat(response.status200().payload().get(0).runtime().status(), is(Runtime.Status.IDLE));
    }

    @Test
    public void givenNoConcurrency__whenNoJob_andWaitingForAWhile__thenPinged() throws Exception {
        log.info("\n\n\ngivenNoConcurrency__whenNoJob_andWaitingForAWhile__thenPinged\n\n\n");
        LocalDateTime start = UTC.now();
        this.createAndStartRunnerServiceWithConcurrency(1);

        Thread.sleep(RUNNER_TTL * 2);
        LocalDateTime now = UTC.now();

        RunnerCollectionGetResponse response = this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build());
        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200 got : " + response));

        System.out.println(response.status200());

        assertThat(response.status200().payload().get(0).runtime().lastPing(), is(not(DateMatchers.around(start))));
        assertThat(response.status200().payload().get(0).runtime().lastPing(), is(DateMatchers.after(start.plus((long) (RUNNER_TTL * 0.66), ChronoUnit.MILLIS))));
    }

    @Test
    public void givenNoConcurrency__whenOneLongJob__thenRunnerStatusChangesToBusy_thanChangesBackToIdle() throws Exception {
        log.info("\n\n\ngivenNoConcurrency__whenOneLongJob__thenRunnerStatusChangesToBusy_thanChangesBackToIdle\n\n\n");
        this.createAndStartRunnerServiceWithConcurrency(1);

        this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("blurp")
                .payload(JobCreationData.builder()
                        .category("c").name("long")
                        .build())
                .build());


        Eventually.timeout(2, TimeUnit.SECONDS).assertThat(
                () -> this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0).runtime().status(),
                is(Runtime.Status.RUNNING)
        );

        Eventually.timeout(10, TimeUnit.SECONDS).assertThat(
                () -> this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0).runtime().status(),
                is(Runtime.Status.IDLE)
        );
    }

    @Test
    public void givenConcurrency__whenOneLongJob__thenRunnerStatusStaysIdle() throws Exception {
        log.info("\n\n\ngivenConcurrency__whenOneLongJob__thenRunnerStatusStaysIdle\n\n\n");
        this.createAndStartRunnerServiceWithConcurrency(2);

        this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                .accountId("blurp")
                .payload(JobCreationData.builder()
                        .category("c").name("long")
                        .build())
                .build());

        Eventually.timeout(2, TimeUnit.SECONDS).assertThat(
                () -> this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0).runtime().status(),
                is(Runtime.Status.IDLE)
        );

        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
            assertThat("still idle at " + i,
                    this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0).runtime().status(),
                    is(Runtime.Status.IDLE)
            );
        }

        assertThat(
                this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0).runtime().status(),
                is(Runtime.Status.IDLE)
        );
    }

    @Test
    public void givenConcurrency__whenMoreJobsThanConcurrency__thenRunnerStatusChangesToBusy_thanChangesBackToIdle() throws Exception {
        log.info("\n\n\ngivenConcurrency__whenMoreJobsThanConcurrency__thenRunnerStatusChangesToBusy_thanChangesBackToIdle\n\n\n");
        this.createAndStartRunnerServiceWithConcurrency(2);

        for (int i = 0; i < 6; i++) {
            this.jobRegistryClient.jobCollection().post(JobCollectionPostRequest.builder()
                    .accountId("blurp")
                    .payload(JobCreationData.builder()
                            .category("c").name("short")
                            .build())
                    .build());
        }

        Thread.sleep(1000);
        Eventually.timeout(15, TimeUnit.SECONDS).assertThat(
                () -> this.runnerRegistryClient.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0).runtime().status(),
                is(Runtime.Status.IDLE)
        );

        log.info("\n\n\ntest end\n\n\n");
    }
}
