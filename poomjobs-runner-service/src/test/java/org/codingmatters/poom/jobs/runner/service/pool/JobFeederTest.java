package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.junit.jupiter.api.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;

class JobFeederTest {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobFeederTest.class);

    private final ConcurrentLinkedQueue<ValueList<Job>> nextPendingJobs = new ConcurrentLinkedQueue<>();

    private JobProcessorRunner.PendingJobManager pendingJobManager = new JobProcessorRunner.PendingJobManager() {
        @Override
        public ValueList<Job> pendingJobs() {
            ValueList<Job> result = nextPendingJobs.poll();
            if(result == null) {
                log.info("nothing pending");
                return ValueList.<Job>builder().build();
            } else {
                log.info("pending " + result);
                return result;
            }
        }

        @Override
        public Job reserve(Job job) throws JobProcessorRunner.JobUpdateFailure {
            throw new RuntimeException("NYIMPL");
        }

        @Override
        public Job release(Job reserved) throws JobProcessorRunner.JobUpdateFailure {
            throw new RuntimeException("NYIMPL");
        }
    };

    private Eventually eventually = Eventually.timeout(2, TimeUnit.SECONDS);

    private final TestJobRunner jobRunner = new TestJobRunner();
    private JobPool pool = new JobPool(3, jobRunner, new NOOPJobLocker());
    private JobFeeder feeder = new JobFeeder(this.pool, this.pendingJobManager);

    @AfterEach
    void tearDown() throws Exception{
        this.feeder.stop();
        this.pool.stop();
        while(! this.feeder.stopped()){
            Thread.sleep(100);
        }
    }

    @Test
    @Timeout(5)
    void whenStartingAndStopping__thenStopsCorrectly() throws Exception {
        log.info("test starts");
        Thread.sleep(2000);
        log.info("test ends");
    }

    @RepeatedTest(3)
    @Timeout(5)
    void givenStarted__whenPendingJobs__thenPendingJobsAreEventuallyExecuted() throws Exception {
        log.info("test starts");
        this.nextPendingJobs.offer(ValueList.<Job>builder().with(Job.builder().name("SHORT").arguments("from pending jobs").build()).build());

        this.eventually.assertThat("", () -> this.jobRunner.doneJobs, containsInAnyOrder(
                Job.builder().name("SHORT").arguments("from pending jobs").build()
        ));
    }

    @RepeatedTest(3)
    @Timeout(5)
    void givenStarted__whenPendingJobs_andJobExternallyFeed__thenPendingJobsAreExecutedAfter() throws Exception {
        log.info("test starts");
        this.nextPendingJobs.offer(ValueList.<Job>builder().with(Job.builder().name("SHORT").arguments("from pending jobs").build()).build());
        this.pool.feed(Job.builder().name("SHORT").arguments("from feeding").build());

        this.eventually.assertThat("", () -> this.jobRunner.doneJobs, containsInAnyOrder(
                Job.builder().name("SHORT").arguments("from pending jobs").build(),
                Job.builder().name("SHORT").arguments("from feeding").build()
        ));
    }


}