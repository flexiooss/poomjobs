package org.codingmatters.poom.runner.manager;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.client.PoomjobsJobRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIClient;
import org.codingmatters.poom.client.PoomjobsRunnerRegistryAPIHandlersClient;
import org.codingmatters.poom.poomjobs.domain.jobs.repositories.JobRepository;
import org.codingmatters.poom.poomjobs.domain.runners.repositories.RunnerRepository;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobQuery;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerQuery;
import org.codingmatters.poom.poomjobs.domain.values.runners.RunnerValue;
import org.codingmatters.poom.poomjobs.domain.values.runners.runnervalue.Runtime;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poomjobs.api.PoomjobsRunnerAPIHandlers;
import org.codingmatters.poomjobs.api.RunningJobPutRequest;
import org.codingmatters.poomjobs.api.RunningJobPutResponse;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.service.PoomjobsRunnerRegistryAPI;
import org.codingmatters.poomjobs.service.api.PoomjobsRunnerAPIProcessor;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;
import org.codingmatters.rest.undertow.support.UndertowResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RunnerInvokerListenerTest {

    private RunnerInvokerListener runnerInvokerListener;

    private final Repository<JobValue, JobQuery> jobRepository = JobRepository.createInMemory();
    private PoomjobsJobRegistryAPIClient jobRegistry;

    private Repository<RunnerValue, RunnerQuery> runnerRepository = RunnerRepository.createInMemory();
    private ExecutorService runnerRegistryPool;
    private PoomjobsRunnerRegistryAPIClient runnerRegistry;

    private Function<RunningJobPutRequest, RunningJobPutResponse> runnerPutResponder;

    @Rule
    public UndertowResource undertow = new UndertowResource(new CdmHttpUndertowHandler(new PoomjobsRunnerAPIProcessor(
            "",
            new JsonFactory(),
            new PoomjobsRunnerAPIHandlers.Builder()
                .runningJobPutHandler(req -> this.runnerPutResponder.apply(req))
                .build()
    )));

    @Before
    public void setUp() throws Exception {
        this.setUpRunnerRegistry();

        this.runnerInvokerListener = new RunnerInvokerListener(this.runnerRegistry);
    }

    @After
    public void tearDown() throws Exception {
        this.tearDownRunnerRegistry();
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

    @Test
    public void givenRunnerExists__whenJobCreatedForRunnerCompetencies__thenRunnerGetsDelegatedTheJob() throws Exception {
        this.runnerRepository.create(RunnerValue.builder()
                .callback(this.undertow.baseUrl())
                .competencies(competencies -> competencies.categories("TEST").names("TEST"))
                .timeToLive(20000L)
                .runtime(runtime -> runtime
                        .status(Runtime.Status.IDLE)
                        .created(LocalDateTime.now())
                        .lastPing(LocalDateTime.now())
                )
                .build());

        AtomicReference<Job> runnerRequestJob = new AtomicReference<>();

        this.runnerPutResponder = request -> {
            Job job = request.payload();
            runnerRequestJob.set(job);
            return RunningJobPutResponse.builder()
                    .status201(status -> status.location("http://fake.job.repo/jobs/" + job.id())).build();
        };

        Entity<JobValue> jobEntity = this.jobRepository.create(JobValue.builder()
                .category("TEST")
                .name("TEST")
                .build());
        this.runnerInvokerListener.jobCreated(jobEntity);

        assertThat(runnerRequestJob.get().id(), is(jobEntity.id()));
    }
}