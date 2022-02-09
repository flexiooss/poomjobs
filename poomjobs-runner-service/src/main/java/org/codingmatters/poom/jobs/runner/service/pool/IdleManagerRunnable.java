package org.codingmatters.poom.jobs.runner.service.pool;

import org.codingmatters.poom.patterns.pool.Feeder;

import java.util.concurrent.atomic.AtomicBoolean;

public class IdleManagerRunnable implements Runnable, Feeder.Listener {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Object monitor = new Object();
    private final IdleManager manager;

    public IdleManagerRunnable(IdleManager manager) {
        this.manager = manager;
    }

    @Override
    public void becameIdle() {
        this.trig();
    }

    @Override
    public void becameBusy() {}

    private void trig() {
        synchronized (this.monitor) {
            this.monitor.notify();
        }
    }

    public void start() {
        this.running.set(true);
    }

    public void stop() {
        this.running.set(false);
    }

    @Override
    public void run() {
        while(this.running.get()) {
            synchronized (this.monitor) {
                try {
                    this.monitor.wait(200);
                } catch (InterruptedException e) {}
            }
            this.manager.becameIdle();
        }
    }
}
