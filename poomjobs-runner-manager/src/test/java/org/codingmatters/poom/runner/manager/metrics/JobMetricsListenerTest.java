package org.codingmatters.poom.runner.manager.metrics;

import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Accounting;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Processing;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.runner.manager.JobMetricsListener;
import org.codingmatters.poom.services.domain.entities.ImmutableEntity;
import org.codingmatters.poom.services.support.date.UTC;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobMetricsListenerTest {

    private JobMetricsListener listener;

    @Before
    public void setUp() throws Exception {
        this.listener = new JobMetricsListener();
    }

    @Test
    public void testEmptyMetrics() {
        ObjectValue metric = listener.get();
        assertThat(metric, is(ObjectValue.builder().build()));
    }

    @Test
    public void testMetricsAtCreatedJob() {
        jobCreated("a1", "c1", "j1");
        jobCreated("a1", "c1", "j2");
        jobCreated("a1", "c2", "j3");

        jobCreated("a2", "c1", "j1");
        jobCreated("a2", "c1", "j2");
        jobCreated("a2", "c2", "j3");

        ObjectValue metric = listener.get();

        assertThat(metric, is(ObjectValue.builder()
                .property("total/count#category=c1#", c -> c.longValue(4L))
                .property("total/count#category=c2#", c -> c.longValue(2L))
                .property("total/account/count#category=c1,account=a2#", c -> c.longValue(2L))
                .property("total/account/count#category=c1,account=a1#", c -> c.longValue(2L))
                .property("total/account/count#category=c2,account=a1#", c -> c.longValue(1L))
                .property("total/account/count#category=c2,account=a2#", c -> c.longValue(1L))
                .build()));
    }

    @Test
    public void testMetricsAtUpdatedJob() {
        LocalDateTime date = UTC.now();

        jobUpdated("c1", "j1", date, date.plusSeconds(10));
        jobUpdated("c1", "j1", date, date.plusSeconds(15));
        jobUpdated("c1", "j1", date, date.plusSeconds(20));

        jobUpdated("c1", "j2", date, date.plusSeconds(25));

        jobUpdated("c2", "j3", date, date.plusSeconds(5));

        ObjectValue metric = listener.get();
        assertThat(metric, is(ObjectValue.builder()
                .property("category/max/wait/time#category=c1#", v -> v.longValue(25L))
                .property("category/max/wait/time#category=c2#", v -> v.longValue(5L))
                .build()));

        jobUpdated("c1", "j2", date, date.plusSeconds(2));
        jobUpdated("c2", "j3", date, date.plusSeconds(1));

        metric = listener.get();
        assertThat(metric, is(ObjectValue.builder()
                .property("category/max/wait/time#category=c1#", v -> v.longValue(2L))
                .property("category/max/wait/time#category=c2#", v -> v.longValue(1L))
                .build()));
    }

    private void jobUpdated(String category, String name, LocalDateTime submittedAt, LocalDateTime startedAt) {
        listener.jobUpdated(new ImmutableEntity<>("id", BigInteger.ONE, JobValue.builder()
                        .category(category)
                        .name(name)
                        .status(Status.builder()
                                .run(Status.Run.RUNNING)
                                .build())
                        .processing(Processing.builder()
                                .submitted(submittedAt)
                                .started(startedAt)
                                .build())
                        .build()),
                JobValue.builder()
                        .category(category)
                        .name(name)
                        .status(Status.builder()
                                .run(Status.Run.PENDING)
                                .build())
                        .processing(Processing.builder()
                                .submitted(submittedAt)
                                .build())
                        .build());
    }

    private void jobCreated(String account, String category, String name) {
        listener.jobCreated(new ImmutableEntity<>("id", BigInteger.ONE, JobValue.builder()
                .accounting(Accounting.builder().accountId(account).build())
                .category(category)
                .name(name)
                .build()));
    }
}
