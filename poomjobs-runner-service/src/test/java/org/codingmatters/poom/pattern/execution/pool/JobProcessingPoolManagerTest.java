package org.codingmatters.poom.pattern.execution.pool;

import org.codingmatters.poom.handler.HandlerResource;
import org.codingmatters.poom.jobs.runner.service.execution.pool.JobProcessingPoolManager;
import org.codingmatters.poom.jobs.runner.service.jobs.JobManager;
import org.codingmatters.poom.pattern.execution.pool.utils.TestFactory;
import org.codingmatters.poom.pattern.execution.pool.utils.TestJobCollection;
import org.codingmatters.poom.pattern.execution.pool.utils.TestStatusManager;
import org.codingmatters.poom.runner.JobContextSetup;
import org.codingmatters.poomjobs.api.*;
import org.codingmatters.poomjobs.api.jobresourcepatchresponse.Status200;
import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.codingmatters.poomjobs.client.PoomjobsJobRegistryAPIHandlersClient;
import org.junit.Before;
import org.junit.Test;

import static org.codingmatters.poomjobs.api.types.RunnerStatusData.Status.IDLE;
import static org.codingmatters.poomjobs.api.types.RunnerStatusData.Status.RUNNING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobProcessingPoolManagerTest {

    private TestStatusManager statusManager;
    private JobProcessingPoolManager poolManager;

    private HandlerResource<JobCollectionGetRequest, JobCollectionGetResponse> jobCollectionGet;
    private TestFactory processorFactory;

    @Before
    public void setUp() throws Exception {
        jobCollectionGet = new TestJobCollection();
        PoomjobsJobRegistryAPIHandlersClient jobsClient = new PoomjobsJobRegistryAPIHandlersClient(
                new PoomjobsJobRegistryAPIHandlers.Builder()
                        .jobCollectionGetHandler(jobCollectionGet)
                        .jobResourcePatchHandler(new HandlerResource<JobResourcePatchRequest, JobResourcePatchResponse>() {
                            @Override
                            protected JobResourcePatchResponse defaultResponse(JobResourcePatchRequest jobResourcePatchRequest) {
                                return JobResourcePatchResponse.builder()
                                        .status200(Status200.builder()
                                                .payload(Job.builder().name(jobResourcePatchRequest.jobId())
                                                        .status(Status.fromMap(jobResourcePatchRequest.payload().status().toMap()).build())
                                                        .build())
                                                .build())
                                        .build();
                            }
                        })
                        .build()
        );
        JobManager jobManager = new JobManager(jobsClient, "dev", "test", new String[]{"test"});
        processorFactory = new TestFactory();
        statusManager = new TestStatusManager();
        poolManager = new JobProcessingPoolManager(3, jobManager, processorFactory, JobContextSetup.NOOP, "job/endpoint", statusManager);
    }

    @Test
    public void addMaxJobsThenTerminateOneByOne() throws InterruptedException {
        poolManager.start();

        assertThat(statusManager.status(), is(IDLE));

        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job1")
                .payload(Job.builder().name("job1").build())
                .build());
        Thread.sleep(1000);

        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(1));
        assertThat(poolManager.pool().poolSize(), is(3));

        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job2")
                .payload(Job.builder().name("job2").build())
                .build());
        Thread.sleep(1000);

        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(2));
        assertThat(poolManager.pool().poolSize(), is(3));

        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job3")
                .payload(Job.builder().name("job3").build())
                .build());
        Thread.sleep(1000);

        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.FULL));
        assertThat(poolManager.pool().workingCount().get(), is(3));
        assertThat(poolManager.pool().poolSize(), is(3));


        assertThat(processorFactory.processors().size(), is(3));

        processorFactory.processors().get(0).terminate();
        Thread.sleep(1000);
        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(2));
        assertThat(poolManager.pool().poolSize(), is(3));

        processorFactory.processors().get(1).terminate();
        Thread.sleep(1000);
        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(1));
        assertThat(poolManager.pool().poolSize(), is(3));

        processorFactory.processors().get(2).terminate();
        Thread.sleep(1000);
        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(0));
        assertThat(poolManager.pool().poolSize(), is(3));
    }

    @Test
    public void addTwoOn3JobsThenTerminateOneByOne() throws InterruptedException {
        poolManager.start();

        assertThat(statusManager.status(), is(IDLE));

        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job1")
                .payload(Job.builder().name("job1").build())
                .build());
        Thread.sleep(1000);

        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(1));
        assertThat(poolManager.pool().poolSize(), is(3));

        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job2")
                .payload(Job.builder().name("job2").build())
                .build());
        Thread.sleep(1000);

        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(2));
        assertThat(poolManager.pool().poolSize(), is(3));


        assertThat(processorFactory.processors().size(), is(2));

        processorFactory.processors().get(0).terminate();
        Thread.sleep(1000);
        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(1));
        assertThat(poolManager.pool().poolSize(), is(3));

        processorFactory.processors().get(1).terminate();
        Thread.sleep(1000);
        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
        assertThat(poolManager.pool().workingCount().get(), is(0));
        assertThat(poolManager.pool().poolSize(), is(3));
    }

    /**
     * Test reproduisant le bug de status bloqué à RUNNING
     * <p>
     * Scénario:
     * 1. Remplir le pool complètement (3/3 workers occupés) -> status = RUNNING
     * 2. Ajouter des pending jobs en attente
     * 3. Terminer UN job -> déclenche accepting()
     * 4. Dans accepting(), processPendingJobs() remplit à nouveau le pool -> full() -> becameBusy()
     * 5. La condition dans accepting() est FALSE, becameIdle() n'est PAS appelé
     * 6. Terminer tous les jobs rapidement
     * 7. Le pool est vide mais le status reste bloqué à RUNNING
     */
    @Test
    public void bugStatusStuckAtRunningWhenPendingJobsRefillPool() throws InterruptedException {
        poolManager.start();
        assertThat(statusManager.status(), is(IDLE));

        System.out.println("Remplir le pool (1/3)");
        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job1")
                .payload(Job.builder().name("job1").build())
                .build());
        Thread.sleep(1000);

        System.out.println("Remplir le pool (2/3)");
        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job2")
                .payload(Job.builder().name("job2").build())
                .build());
        Thread.sleep(1000);

        System.out.println("Remplir le pool (3/3)");
        poolManager.jobExecutionRequested(RunningJobPutRequest.builder()
                .jobId("job3")
                .payload(Job.builder().name("job3").build())
                .build());
        Thread.sleep(1000);

        System.out.println("pool plein, status = RUNNING");
        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.FULL));
        assertThat(poolManager.pool().workingCount().get(), is(3));
        assertThat(statusManager.status(), is(RUNNING));
        Thread.sleep(1000);

        System.out.println("2. Ajouter des pending jobs");
        ((TestJobCollection) jobCollectionGet).currentJobs().add(Job.builder().name("pending1").build());
        ((TestJobCollection) jobCollectionGet).currentJobs().add(Job.builder().name("pending2").build());
        ((TestJobCollection) jobCollectionGet).currentJobs().add(Job.builder().name("pending3").build());
        Thread.sleep(1000);

        System.out.println("Terminer le premier job => le pool se re-remplit");
        processorFactory.processors().get(0).terminate();

        System.out.println("Terminer tous les jobs");
        for (int i = 1; i < processorFactory.processors().size(); i++) {
            processorFactory.processors().get(i).terminate();
        }
        Thread.sleep(2000);

        if (poolManager.pool().status() == ProcessingPool.Status.FULL) {
            System.out.println("Bug reproduced ??");
            Thread.sleep(2000);
        }
        assertThat(poolManager.pool().status(), is(ProcessingPool.Status.ACCEPTING));
//        assertThat(poolManager.pool().workingCount().get(), is(0));

        assertThat(statusManager.status(), is(IDLE));
    }

}
