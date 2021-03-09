package org.codingmatters.poom.jobs.runner.service.manager.status;

import org.codingmatters.poom.jobs.runner.service.manager.RunnerStatusProvider;
import org.codingmatters.poom.jobs.runner.service.exception.NotificationFailedException;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatus;
import org.codingmatters.poom.jobs.runner.service.manager.monitor.RunnerStatusChangedListener;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RunnerStatusManager implements RunnerStatusChangedListener {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(RunnerStatusManager.class);

    private final RunnerStatusNotifier notifier;
    private final RunnerStatusProvider statusProvider;
    private final ScheduledExecutorService scheduler;
    private final long tick;
    private ScheduledFuture<?> scheduledNotification;

    public RunnerStatusManager(RunnerStatusNotifier notifier, RunnerStatusProvider statusProvider, ScheduledExecutorService scheduler, long tick) {
        this.notifier = notifier;
        this.statusProvider = statusProvider;
        this.scheduler = scheduler;
        this.tick = tick;
    }

    public void start() {
        this.notifyAndSchedule();
    }

    public void stop() {
        this.scheduledNotification.cancel(false);
    }

    @Override
    public void onIdle(RunnerStatus was) {
        if(! RunnerStatus.IDLE.equals(was)) {
            this.changed();
        }
    }

    @Override
    public void onBusy(RunnerStatus was) {
        if(! RunnerStatus.BUSY.equals(was)) {
            this.changed();
        }
    }

    private synchronized void changed() {
        this.scheduledNotification.cancel(false);
        this.notifyAndSchedule();
    }

    private synchronized void notifyAndSchedule() {
        try {
            RunnerStatus status = this.statusProvider.status();
            this.notifier.notify(status);
            log.info("notified status {}", status);
        } catch (NotificationFailedException e) {
            log.error("runner failed to update status (will retry later in " + this.tick + "ms", e);
        }
        this.scheduledNotification = this.scheduler.schedule(this::notifyAndSchedule, this.tick, TimeUnit.MILLISECONDS);
    }

}
