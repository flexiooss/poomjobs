package org.codingmatters.poom.runner.manager;

import com.codahale.metrics.Counter;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.runner.manager.metrics.MaxTimeCounter;
import org.codingmatters.poom.runner.manager.metrics.ResettingCounter;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/*
flexio.services.jobs.<category>.<name>.count
flexio.services.jobs.<category>.<name>.averageWaitTime
flexio.services.jobs.<category>.<name>.averageRunTime
*/
public class JobMetricsListener implements PoomjobsJobRepositoryListener, Supplier<ObjectValue> {

    /*
     nb de jobs créés pendant la periode
      créés = created
     */
    private final Map<String, Map<String, Counter>> count;

    /*
     temps d'attente de ceux qui sont pris en charge pendant la periode
     pris en charge = updated = PENDING -> RUNNING
     */
    private final Map<String, MaxTimeCounter> categoryMaxWaitTime;

    public JobMetricsListener() {
        this.count = new HashMap<>();
        this.categoryMaxWaitTime = new HashMap<>();
    }

    @Override
    public void jobCreated(Entity<JobValue> entity) {
        String category = entity.value().category();
        String account = entity.value().opt().accounting().accountId().orElse("undefined");
        count.putIfAbsent(category, new HashMap<>());
        count.get(category).putIfAbsent(account, new ResettingCounter());
        count.get(category).get(account).inc();
    }

    @Override
    public void jobUpdated(Entity<JobValue> newValue, JobValue oldValue) {
        if (oldValue.opt().status().run().orElse(Status.Run.DONE) == Status.Run.PENDING && newValue.value().status().run() == Status.Run.RUNNING) {
            String category = newValue.value().category();
            String name = newValue.value().name();
            long submitted = newValue.value().processing().submitted().toEpochSecond(ZoneOffset.UTC);
            long started = newValue.value().processing().started().toEpochSecond(ZoneOffset.UTC);
            long waitTimeSec = started - submitted;
            newWaitTime(category, name, waitTimeSec);
        } else if (oldValue.opt().status().run().orElse(Status.Run.DONE) == Status.Run.RUNNING && newValue.value().status().run() == Status.Run.DONE) {
            /*
            String category = newValue.value().category();
            String name = newValue.value().name();
            long started = newValue.value().processing().started().toEpochSecond(ZoneOffset.UTC);
            long finished = newValue.value().processing().finished().toEpochSecond(ZoneOffset.UTC);
            long runTimeSec = finished - started;
            newRunTime(category, name, runTimeSec);
             */
        }
    }

    private void newRunTime(String category, String name, long runTimeSec) {
    }

    private void newWaitTime(String category, String name, long waitTimeSec) {
        categoryMaxWaitTime.putIfAbsent(category, new MaxTimeCounter());
        categoryMaxWaitTime.get(category).newTime(waitTimeSec);
    }

    @Override
    public ObjectValue get() {
        ObjectValue.Builder builder = ObjectValue.builder();

        for (Map.Entry<String, Map<String, Counter>> entry : count.entrySet()) {
            String category = entry.getKey();
            Map<String, Counter> byJobName = entry.getValue();
            long totalAccount = 0;
            for (Map.Entry<String, Counter> categoryEntry : byJobName.entrySet()) {
                String account = categoryEntry.getKey();
                long accountCount = categoryEntry.getValue().getCount();
                totalAccount += accountCount;
                builder.property(String.format("total/account/count#category=%s,account=%s#", category, account), val -> val.longValue(accountCount));
            }
            builder.property(String.format("total/count#category=%s#", category), PropertyValue.builder().longValue(totalAccount));
        }

        for (Map.Entry<String, MaxTimeCounter> entry : categoryMaxWaitTime.entrySet()) {
            String category = entry.getKey();
            MaxTimeCounter counter = entry.getValue();
            builder.property(String.format("category/max/wait/time#category=%s#", category), val -> val.longValue(counter.getValue()));
        }

        return builder.build();
    }

}
