package org.codingmatters.poom.runner;

import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIHandlersClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIHandlersClient;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.runner.configuration.RunnerConfiguration;
import org.codingmatters.poom.runner.tests.Eventually;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poomjobs.api.RunnerCollectionGetRequest;
import org.codingmatters.poomjobs.api.types.Runner;
import org.codingmatters.poomjobs.service.PoomjobsJobRegistryAPI;
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.is;
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

    @Test
    public void runnerInitialization() throws Exception {
        this.runner.start();
        Eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        Runner registered = this.runnerRegistry.runnerCollection().get(RunnerCollectionGetRequest.builder().build()).status200().payload().get(0);

        assertThat(registered.id(), is(this.runner.id()));
    }

    @Test
    public void whenNoJobToProcess_thenStatusUpdatedToPending() throws Exception {
        this.runner.start();
        Eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        assertThat(this.runnerRepository.retrieve(this.runner.id()).value().runtime().status(), is(Runtime.Status.IDLE));
    }

    @Test
    public void statusUpdate() throws Exception {
        this.runner.start();
        Eventually.assertThat(() -> this.runnerRepository.all(0, 0).total(), is(1L));

        LocalDateTime ttlExpiration = LocalDateTime.now().plus(TTL, ChronoUnit.MILLIS);
        LocalDateTime nextTtlExpiration = LocalDateTime.now().plus(2 * TTL, ChronoUnit.MILLIS);
        Thread.sleep(2 * TTL);

        Runtime actualRuntime = this.runnerRepository.retrieve(this.runner.id()).value().runtime();

        assertThat(actualRuntime.status(), is(Runtime.Status.IDLE));
        assertThat(actualRuntime.lastPing().isAfter(ttlExpiration), is(true));
        assertThat(actualRuntime.lastPing().isBefore(nextTtlExpiration), is(true));
    }

    @Before
    public void setUp() throws Exception {
        this.setUpJobRegistry();
        this.setUpRunnerRegistry();
        this.setUpRunner();
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
        this.runnerRegistryPool.shutdownNow();
    }

    public void setUpRunner() throws Exception {
        this.jobWorker = Executors.newFixedThreadPool(5);
        this.runner = new GenericRunner(RunnerConfiguration.builder()
                .jobWorker(this.jobWorker)
                .jobRegistryAPIClient(this.jobRegistry)
                .runnerRegistryAPIClient(this.runnerRegistry)
                .callbackBaseUrl("http://base.runner.url")
                .ttl(TTL)
                .jobCategories("TEST")
                .jobNames("*")
                .build());
    }

    public void tearDownRunner() throws Exception {
        this.jobWorker.shutdownNow();
    }
}