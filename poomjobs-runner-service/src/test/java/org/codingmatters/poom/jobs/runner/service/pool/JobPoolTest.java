package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.pattern.execution.pool.exceptions.PoolBusyException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.tests.Eventually;
import org.codingmatters.poomjobs.api.types.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class JobPoolTest {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(JobPoolTest.class);

    private final List<Job> doneJobs = Collections.synchronizedList(new LinkedList<>());
    private final List<String> poolStatusChanges = Collections.synchronizedList(new LinkedList<>());

    private final JobPoolListener jobPoolListener = new JobPoolListener() {
        @Override
        public void poolIsFull() {
            log.info("pool is full");
            poolStatusChanges.add("FULL");
        }

        @Override
        public void poolIsAcceptingJobs() {
            log.info("pool is accepting jobs");
            poolStatusChanges.add("ACCEPTING");
        }
    };

    private JobPool pool = new JobPool(3, this::executeJob);

    private Eventually eventually = Eventually.timeout(2, TimeUnit.SECONDS);

    @BeforeEach
    void setUp() {
        this.pool.addJobPoolListener(this.jobPoolListener);
    }

    @AfterEach
    void tearDown() {
        this.pool.stop();
    }

    @Test
    void whenSubmittingOneJob__thenJobExecuted_andPoolIsAlwaysAccepting() throws Exception {
        Job job = Job.builder().name("SHORT").build();
        this.pool.feed(job);

        this.eventually.assertThat("job executed", () -> this.doneJobs, hasItem(job));

        assertThat(this.poolStatusChanges, is(not(empty())));
        assertFalse(
                this.poolStatusChanges.stream().anyMatch(s -> ! s.equals("ACCEPTING")),
                "status is always accepting"
        );
    }

    @Test
    void whenSubmittedCapacityJobs__thenAllAreExecuted_andPoolBecameFull() throws Exception {
        Job job = Job.builder().name("SHORT").build();
        for (int i = 0; i < 3; i++) {
            this.pool.feed(job);
        }
        assertThat(this.poolStatusChanges.getLast(), is("FULL"));

        this.eventually.assertThat("all job executed", () -> this.doneJobs, hasSize(3));

        assertThat(this.poolStatusChanges, is(not(empty())));
        System.out.println(this.poolStatusChanges);
        assertTrue(
                this.poolStatusChanges.stream().anyMatch(s -> s.equals("FULL")),
                "status became full"
        );
    }

    @Test
    void givenSubmittedCapacityJobs__whenSumittingOneMore__thenPoolBusyException() throws Exception {
        Job job = Job.builder().name("SHORT").build();
        for (int i = 0; i < 3; i++) {
            this.pool.feed(job);
        }

        assertThat(this.poolStatusChanges.getLast(), is("FULL"));
        assertThrows(PoolBusyException.class, () -> this.pool.feed(job));
    }

    private void executeJob(Job job) {
        log.info("executing job " + job.name());
        String name = job.opt().name().orElse("SHORT");
        try {
            switch (name) {
                case "SHORT":
                    Thread.sleep(100);
                    break;
                case "MEDIUM":
                    Thread.sleep(500);
                    break;
                case "LONG":
                    Thread.sleep(1000);
                    break;
                case "VERY_LONG":
                    Thread.sleep(2000);
                    break;
                default:
                    Thread.sleep(100);
                    break;
            }
        } catch (InterruptedException e) {
            log.error("interrupted while waiting for job " + job.name(), e);
        }
        this.doneJobs.add(job);
        log.info("done running job " + name);
    }
}