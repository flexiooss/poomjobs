package org.codingmatters.poom.runner.manager;

import com.codahale.metrics.Counter;
import org.codingmatters.poom.poomjobs.domain.values.jobs.JobValue;
import org.codingmatters.poom.poomjobs.domain.values.jobs.jobvalue.Status;
import org.codingmatters.poom.runner.manager.metrics.AverageTimeCounter;
import org.codingmatters.poom.runner.manager.metrics.ResettingCounter;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.codingmatters.poomjobs.service.PoomjobsJobRepositoryListener;
import org.codingmatters.value.objects.values.ObjectValue;

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
     de ceux qui sont créés en charge pendant la periode
      créés = created
     */
    private final Map<String, Map<String, Counter>> count;

    /*
     de ceux qui sont pris en charge pendant la periode
     pris en charge = updated = PENDING -> RUNNING
     */
    private final Map<String, Map<String, AverageTimeCounter>> averageWaitTime;


    /*
     de ceux qui ce sont temrinés pendant la periode
     temrinés = updated = RUNNING -> DONE
     */
    private final Map<String, Map<String, AverageTimeCounter>> averageRunTime;

    public JobMetricsListener() {
        this.count = new HashMap<>();
        this.averageWaitTime = new HashMap<>();
        this.averageRunTime = new HashMap<>();
    }

    @Override
    public void jobCreated(Entity<JobValue> entity) {
        String category = entity.value().category();
        String name = entity.value().name();
        count.putIfAbsent(category, new HashMap<>());
        count.get(category).putIfAbsent(name, new ResettingCounter());
        count.get(category).get(name).inc();
    }

    @Override
    public void jobUpdated(Entity<JobValue> newValue, JobValue oldValue) {
        if (oldValue.opt().status().run().orElse(Status.Run.DONE) == Status.Run.PENDING && newValue.value().status().run() == Status.Run.RUNNING) {
            String category = newValue.value().category();
            String name = newValue.value().name();
            averageWaitTime.putIfAbsent(category, new HashMap<>());
            averageWaitTime.get(category).putIfAbsent(name, new AverageTimeCounter());
            long submitted = newValue.value().processing().submitted().toEpochSecond(ZoneOffset.UTC);
            long started = newValue.value().processing().started().toEpochSecond(ZoneOffset.UTC);
            long waitTimeSec = started - submitted;
            averageWaitTime.get(category).get(name).newWaitTime(waitTimeSec);
        } else if (oldValue.opt().status().run().orElse(Status.Run.DONE) == Status.Run.RUNNING && newValue.value().status().run() == Status.Run.DONE) {
            String category = newValue.value().category();
            String name = newValue.value().name();
            averageRunTime.putIfAbsent(category, new HashMap<>());
            averageRunTime.get(category).putIfAbsent(name, new AverageTimeCounter());
            long started = newValue.value().processing().started().toEpochSecond(ZoneOffset.UTC);
            long finished = newValue.value().processing().finished().toEpochSecond(ZoneOffset.UTC);
            long runTimeSec = finished - started;
            averageRunTime.get(category).get(name).newWaitTime(runTimeSec);
        }
    }

    @Override
    public ObjectValue get() {
        ObjectValue.Builder builder = ObjectValue.builder();
        for (Map.Entry<String, Map<String, Counter>> entry : count.entrySet()) {
            String category = entry.getKey();
            Map<String, Counter> byJobName = entry.getValue();
            for (Map.Entry<String, Counter> categoryEntry : byJobName.entrySet()) {
                String name = categoryEntry.getKey();
                builder.property(String.format("%s/%s/count", category, name), val -> val.longValue(categoryEntry.getValue().getCount()));
            }
        }

        for (Map.Entry<String, Map<String, AverageTimeCounter>> entry : averageWaitTime.entrySet()) {
            String category = entry.getKey();
            Map<String, AverageTimeCounter> byJobName = entry.getValue();
            for (Map.Entry<String, AverageTimeCounter> categoryEntry : byJobName.entrySet()) {
                String name = categoryEntry.getKey();
                builder.property(String.format("%s/%s/averageWaitTime", category, name), val -> val.doubleValue(categoryEntry.getValue().getValue()));
            }
        }

        for (Map.Entry<String, Map<String, AverageTimeCounter>> entry : averageRunTime.entrySet()) {
            String category = entry.getKey();
            Map<String, AverageTimeCounter> byJobName = entry.getValue();
            for (Map.Entry<String, AverageTimeCounter> categoryEntry : byJobName.entrySet()) {
                String name = categoryEntry.getKey();
                builder.property(String.format("%s/%s/averageRunTime", category, name), val -> val.doubleValue(categoryEntry.getValue().getValue()));
            }
        }
        return builder.build();
    }

}
