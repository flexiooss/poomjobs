package org.codingmatters.poom.runner;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poomjobs.client.*;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.RunnerCollectionGetRequest;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.service.PoomjobsJobRegistryAPI;
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
import org.codingmatters.rest.api.client.okhttp.HttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpClientWrapper;
import org.codingmatters.rest.api.client.okhttp.OkHttpRequesterFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GenericRunnerTest {

    public static final long TTL = 1000L;

    private GenericRunner runner;
    private PoomjobsJobRegistryAPIClient jobRegistry;
    private PoomjobsRunnerRegistryAPIClient runnerRegistry;
    private ExecutorService jobWorker;

    private final Repository<JobValue, JobQuery> jobRepository = JobRepository.createInMemory();
    private ExecutorService jobRegistryPool;

    private Repository<RunnerValue, RunnerQuery> runnerRepository = RunnerRepository.createInMemory();
    private ExecutorService runnerRegistryPool;

    private RunnerConfiguration runnerConfiguration;
    private PoomjobsRunnerAPIClient runnerEndpointClient;

    private AtomicLong runnerSleepTime = new AtomicLong(1 * 1000L);

    private Eventually eventually = Eventually.defaults();

    @Test
    public void runnerInitialization() throws Exception {
        this.runner.start();
        eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        Runner registered = this.runnerRegistry.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0);

        assertThat(registered.id(), is(this.runner.id()));
    }

    @Test
    public void whenNoJobToProcess_thenStatusUpdatedToPending() throws Exception {
        this.runner.start();
        eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        assertThat(this.runnerRepository.retrieve(this.runner.id()).value().runtime().status(), is(Runtime.Status.IDLE));
    }

    @Test
    public void statusUpdate() throws Exception {
        this.runner.start();
        eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        LocalDateTime ttlExpiration = UTC.now().plus(TTL, ChronoUnit.MILLIS);
        LocalDateTime nextTtlExpiration = UTC.now().plus(2 * TTL, ChronoUnit.MILLIS);
        Thread.sleep(2 * TTL);

        Runtime actualRuntime = this.runnerRepository.retrieve(this.runner.id()).value().runtime();

        assertThat(actualRuntime.status(), is(Runtime.Status.IDLE));
        assertThat(actualRuntime.lastPing().isAfter(ttlExpiration), is(true));
        System.out.println(actualRuntime.lastPing());
        System.out.println(nextTtlExpiration);
        assertThat(actualRuntime.lastPing().isBefore(nextTtlExpiration), is(true));
    }

    @Test
    public void whenJobToProcess_thenRunnerStatusIsRunning() throws Exception {
        Entity<JobValue> entity = this.createPendingJob();

        this.runner.start();

        eventually.assertThat(
                () -> this.runnerRepository.retrieve(this.runner.id()).value().runtime().status(),
                is(Runtime.Status.RUNNING)
        );
    }

    @Test
    public void whenJobIsBeingProcessed__thenJobStatusIsRunning() throws Exception {
        Entity<JobValue> entity = this.createPendingJob();

        this.runner.start();
        eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        eventually.assertThat(
                () -> this.jobRepository.retrieve(entity.id()).value().status().run(),
                is(Status.Run.RUNNING)
        );
    }

    @Test
    public void whenJobHasBeenProcessed__thenJobStatusIsDone_AndRunnerStatusIsIdle() throws Exception {
        Entity<JobValue> entity = this.createPendingJob();

        this.runner.start();
        eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        Thread.sleep(4 * 1000L);

        eventually.assertThat(
                () -> this.jobRepository.retrieve(entity.id()).value().status(),
                is(Status.builder()
                        .run(Status.Run.DONE)
                        .exit(Status.Exit.SUCCESS)
                        .build()
                )
        );

        eventually.assertThat(
                () -> this.runnerRepository.retrieve(this.runner.id()).value().runtime().status(),
                is(Runtime.Status.IDLE)
        );

    }

    @Test
    public void whenJobIsSubmitted_andStatusIsRunning__thenReturnsAStatus409() throws Exception {
        this.createPendingJob();
        this.runner.start();

        eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));
        eventually.assertThat(() -> this.runnerRepository.all(0, 0).get(0).value().runtime().status(), is(Runtime.Status.RUNNING));

        Entity<JobValue> entity = this.createPendingJob();
        Job job = this.jobRegistry.jobCollection().jobResource().get(req -> req.jobId(entity.id())).status200().payload();

        RunningJobPutResponse resp = this.runnerEndpointClient.runningJob().put(req ->
                req
                        .jobId(entity.id())
                        .payload(job)
        );

        assertThat(resp.status409(), is(notNullValue()));
    }

    @Test
    public void whenJobIsSubmitted_andStatusIsIdle__thenReturnsAStatus201_andJobIsProcessed() throws Exception {
        this.runner.start();

        eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));
        eventually.assertThat(() -> this.runnerRepository.all(0, 0).get(0).value().runtime().status(), is(Runtime.Status.IDLE));


        Entity<JobValue> entity = this.createPendingJob();
        Job job = this.jobRegistry.jobCollection().jobResource().get(req -> req.jobId(entity.id())).status200().payload();

        Thread.sleep(this.runnerSleepTime.get());

        eventually.assertThat(
                () -> this.runnerEndpointClient.runningJob().put(req -> req.jobId(entity.id()).payload(job)).status201(),
                is(notNullValue())
        );

        eventually.assertThat(
                () -> this.jobRepository.retrieve(entity.id()).value().status(),
                is(Status.builder()
                        .run(Status.Run.DONE)
                        .exit(Status.Exit.SUCCESS)
                        .build()
                )
        );
    }

    @Before
    public void setUp() throws Exception {
        this.setUpJobRegistry();
        this.setUpRunnerRegistry();
        this.setUpRunner();
        this.setupRunnerEndpointClient();
    }

    @After
    public void tearDown() throws Exception {
        this.tearDownRunner();
        this.tearDownRunnerRegistry();
        this.tearDownJobRegistry();
    }

    public void setUpJobRegistry() throws Exception {
        this.jobRegistryPool = Executors.newFixedThreadPool(5);

        this.jobRegistry = new PoomjobsJobRegistryAPIHandlersClient(
                new PoomjobsJobRegistryAPI(this.jobRepository).handlers(),
                this.jobRegistryPool
        );
    }

    public void tearDownJobRegistry() throws Exception {
        this.jobRegistryPool.shutdownNow();
    }

    public void setUpRunnerRegistry() throws Exception {
        this.runnerRegistryPool = Executors.newFixedThreadPool(5);

        this.runnerRegistry = new PoomjobsRunnerRegistryAPIHandlersClient(
                new PoomjobsRunnerRegistryAPI(this.runnerRepository).handlers(),
                this.runnerRegistryPool
        );
    }

    public void tearDownRunnerRegistry() throws Exception {
        this.stopExecutorService(this.runnerRegistryPool);
    }

    public void setUpRunner() throws Exception {

        int port;
        try(ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        this.jobWorker = Executors.newFixedThreadPool(5);
        this.runnerConfiguration = RunnerConfiguration.builder()
                .jobWorker(this.jobWorker)
                .jobRegistryAPIClient(this.jobRegistry)
                .runnerRegistryAPIClient(this.runnerRegistry)
                .callbackBaseUrl("http://base.runner.url")
                .ttl(TTL)
                .jobCategory("TEST")
                .jobName("TEST")
                .processorFactory(job -> () -> {
                    System.out.println("job started : " + job.id());
                    try {
                        Thread.sleep(this.runnerSleepTime.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("job done : " + job.id());
                    return job.changed(builder -> builder.status(job.status()
                            .withRun(org.codingmatters.poomjobs.api.types.job.Status.Run.DONE)
                            .withExit(org.codingmatters.poomjobs.api.types.job.Status.Exit.SUCCESS)
                    ));
                })
                .jobRegistryUrl("http://localhost/fake/root")
                .endpointHost("localhost")
                .endpointPort(port)
                .build();
        this.runner = new GenericRunner(this.runnerConfiguration);
    }

    private void setupRunnerEndpointClient() {

        HttpClientWrapper client = OkHttpClientWrapper.build();
        JsonFactory jsonFactory = new JsonFactory();

        this.runnerEndpointClient = new PoomjobsRunnerAPIRequesterClient(
                new OkHttpRequesterFactory(client, () ->String.format("http://%s:%s",
                        this.runnerConfiguration.endpointHost(),
                        this.runnerConfiguration.endpointPort()
                )),
                jsonFactory,
                String.format("http://%s:%s",
                        this.runnerConfiguration.endpointHost(),
                        this.runnerConfiguration.endpointPort()
                )
        );
    }

    public void tearDownRunner() throws Exception {
        this.runner.stop();
        this.stopExecutorService(this.jobWorker);
    }

    private void stopExecutorService(ExecutorService executorService) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(! executorService.isTerminated()) {
            executorService.shutdownNow();
        }
    }

    private Entity<JobValue> createPendingJob() throws org.codingmatters.poom.services.domain.exceptions.RepositoryException {
        return this.jobRepository.create(JobValue.builder()
                .name("TEST")
                .category("TEST")
                .status(status -> status.run(Status.Run.PENDING))
                .processing(processing -> processing.submitted(LocalDateTime.now()))
                .build());
    }
}