package org.codingmatters.poom.jobs.runner.service.manager.flow;

import org.codingmatters.poomjobs.api.types.Job;
import org.codingmatters.poomjobs.api.types.job.Status;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JobConsumerTest {

    private final Queue<Job> jobQueue = new ConcurrentLinkedQueue<>();
    private final List<Job> executed = Collections.synchronizedList(new LinkedList<>());
    private final List<Job> setupJobs = Collections.synchronizedList(new LinkedList<>());

    private final JobConsumer flow = new JobConsumer(
            new JobProcessorRunner(
                    job -> {},
                    job -> () -> {
                        executed.add(job);
                        return job;
                    },
                    (job, processor) -> {
                        setupJobs.add(job);
                    }
                ),
            () -> jobQueue.poll()
    );

    @Test
    public void givenNoNextJob__whenRunningOneJob__thenThisJobIsRan_andProcessEnds() throws Exception {
        Job firstJob = this.runningJob();
        this.flow.runWith(firstJob);

        assertThat(this.executed, contains(firstJob));
        assertThat(this.setupJobs, contains(firstJob));
    }

    @Test
    public void givenSomeNextJob__whenRunningOneJob__thenThisJobIsRan_thenAllNextAreRan_andProcessEnds() throws Exception {
        Job firstJob = this.runningJob();
        for (int i = 0; i < 10; i++) {
            this.jobQueue.add(this.runningJob("q" + i));
        }

        this.flow.runWith(firstJob);

        assertThat(this.executed, hasSize(11));
        assertThat(this.setupJobs, hasSize(11));
        assertThat(this.executed.get(0), is(firstJob));
        assertThat(this.setupJobs.get(0), is(firstJob));
        for (int i = 0; i < 10; i++) {
            assertThat("exec job " + i, this.executed.get(i + 1).id(), is("q" + i));
            assertThat("setup job " + i, this.setupJobs.get(i + 1).id(), is("q" + i));
        }
    }

    private Job runningJob() {
        String id = UUID.randomUUID().toString();
        return this.runningJob(id);
    }

    private Job runningJob(String id) {
        return Job.builder().id(id).status(Status.builder().run(Status.Run.RUNNING).build()).build();
    }
}