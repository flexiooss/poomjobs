package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.jobs.runner.service.jobs.JobProcessorRunner;
import org.codingmatters.poom.patterns.pool.FeederPool;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poomjobs.api.ValueList;
import org.codingmatters.poomjobs.api.types.Job;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

public class IdleManagerTest {
    static private CategorizedLogger log = CategorizedLogger.getLogger(IdleManagerTest.class);

    private final LinkedList<List<Job>> nextPendingJobsResponses = new LinkedList<>();
    private final LinkedList<Job> lastReservationRequests = new LinkedList<>();
    private final LinkedList<Job> nextReservationResponses = new LinkedList<>();

    private JobProcessorRunner.PendingJobManager jobManager = new JobProcessorRunner.PendingJobManager() {
        @Override
        public ValueList<Job> pendingJobs() {
            log.debug("pendingJobs invoked");
            return ValueList.from(nextPendingJobsResponses.pop()).build();
        }

        @Override
        public Job reserve(Job job) throws JobProcessorRunner.JobUpdateFailure {
            log.debug("reserve invoked with {}", job);
            lastReservationRequests.add(job);
            Job result = nextReservationResponses.pop();
            if(result == null) {
                throw new JobProcessorRunner.JobUpdateFailure("cannot reserve");
            }
            return result;
        }
    };
    private FeederPool<Job> feeders = new FeederPool<>(3);
    private final IdleManager manager = new IdleManager(this.jobManager, this.feeders);

    @Test
    public void givenFeedersAreIdle__whenNoPendingJob__thenPendingJobsRequested_andNoJobReserved() throws Exception {
        this.nextPendingJobsResponses.add(Collections.emptyList());

        manager.becameIdle();

        assertThat(this.nextPendingJobsResponses, is(empty()));
        assertThat(this.lastReservationRequests, is(empty()));
    }

    @Test
    public void givenFeedersAreIdle__whenSomePendingJobsOnFirstCall_thenNoPendingJobs__thenFirstJobReserved_andPendingJobsRequestedAgain() throws Exception {
        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("a").build(), Job.builder().id("b").build(), Job.builder().id("c").build()
        ));
        this.nextPendingJobsResponses.add(Collections.emptyList());
        this.nextReservationResponses.add(Job.builder().id("a").build());

        manager.becameIdle();


        assertThat(this.nextPendingJobsResponses, is(empty()));
        assertThat(this.lastReservationRequests, contains(Job.builder().id("a").build()));
        assertThat(this.nextReservationResponses, is(empty()));
    }

    @Test
    public void givenFeedersAreIdle__whenSomePendingJobsOnFirstCall_andSomePendingJobsOnSecondCall_thenNoPendingJobs__thenTwoJobsReserved_andPendingJobsRequestedAgain() throws Exception {
        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("a").build(), Job.builder().id("b").build(), Job.builder().id("c").build()
        ));this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("b").build(), Job.builder().id("c").build()
        ));
        this.nextPendingJobsResponses.add(Collections.emptyList());
        this.nextReservationResponses.add(Job.builder().id("a").build());
        this.nextReservationResponses.add(Job.builder().id("b").build());

        manager.becameIdle();

        assertThat(this.nextPendingJobsResponses, is(empty()));
        assertThat(this.lastReservationRequests, contains(Job.builder().id("a").build(), Job.builder().id("b").build()));
        assertThat(this.nextReservationResponses, is(empty()));
    }

    @Test
    public void givenFeedersAreIdle__whenSomePendingJobsOnAllCalls__thenThreeJobsReserved_andPendingJobsNotRequestedAgain() throws Exception {
        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("a").build(), Job.builder().id("b").build(), Job.builder().id("c").build(), Job.builder().id("d").build()
        ));
        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("b").build(), Job.builder().id("c").build(), Job.builder().id("d").build()
        ));
        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("c").build(), Job.builder().id("d").build()
        ));
        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("d").build()
        ));

        this.nextReservationResponses.add(Job.builder().id("a").build());
        this.nextReservationResponses.add(Job.builder().id("b").build());
        this.nextReservationResponses.add(Job.builder().id("c").build());

        manager.becameIdle();

        assertThat(this.nextPendingJobsResponses, hasSize(1));
        assertThat(this.lastReservationRequests, contains(Job.builder().id("a").build(), Job.builder().id("b").build(), Job.builder().id("c").build()));
        assertThat(this.nextReservationResponses, is(empty()));
    }

    @Test
    public void givenFeedersAreIdle__whenSomePendingJobsOnFirstCall_thenNoPendingJobs_andJobAReservationFails__thenJobBReserved() throws Exception {
        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("a").build(), Job.builder().id("b").build(), Job.builder().id("c").build()
        ));
        this.nextPendingJobsResponses.add(Collections.emptyList());
        this.nextReservationResponses.add(null);
        this.nextReservationResponses.add(Job.builder().id("b").build());

        manager.becameIdle();

        assertThat(this.nextPendingJobsResponses, is(empty()));
        assertThat(this.lastReservationRequests, contains(Job.builder().id("a").build(), Job.builder().id("b").build()));
        assertThat(this.nextReservationResponses, is(empty()));
    }

    @Test
    public void givenOneFeederIdle__whenSomePendingJobsOnFirstCall_andSomePendingJobsOnSecondCall_thenNoPendingJobs__thenOneJobsReserved_andPendingJobsNotRequestedAgain() throws Exception {
        this.feeders.reserve().feed(Job.builder().id("working-1").build());
        this.feeders.reserve().feed(Job.builder().id("working-2").build());

        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("a").build(), Job.builder().id("b").build(), Job.builder().id("c").build()
        ));this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("b").build(), Job.builder().id("c").build()
        ));
        this.nextPendingJobsResponses.add(Collections.emptyList());
        this.nextReservationResponses.add(Job.builder().id("a").build());

        manager.becameIdle();

        assertThat(this.nextPendingJobsResponses, hasSize(2));
        assertThat(this.lastReservationRequests, contains(Job.builder().id("a").build()));
        assertThat(this.nextReservationResponses, is(empty()));
    }

    @Test
    public void givenFeedersBusy__whenSomePendingJobsOnFirstCall__thenPendingJobsNotRequested_andNoJobReserved() throws Exception {
        this.feeders.reserve().feed(Job.builder().id("working-1").build());
        this.feeders.reserve().feed(Job.builder().id("working-2").build());
        this.feeders.reserve().feed(Job.builder().id("working-3").build());

        this.nextPendingJobsResponses.add(Arrays.asList(
                Job.builder().id("a").build(), Job.builder().id("b").build(), Job.builder().id("c").build()
        ));

        manager.becameIdle();

        assertThat(this.nextPendingJobsResponses, hasSize(1));
        assertThat(this.lastReservationRequests, is(empty()));
        assertThat(this.nextReservationResponses, is(empty()));
    }
}