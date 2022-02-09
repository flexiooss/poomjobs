package org.codingmatters.poom.patterns.pool;

import org.codingmatters.poom.patterns.pool.exception.NotIdleException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FeederPool<In> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(FeederPool.class);

    private final Feeder<In>[] feeders;
    private final Feeder.Monitor<In>[] monitors;

    private final List<FeederPool.Listener> listeners = Collections.synchronizedList(new LinkedList<>());

    public FeederPool(int poolSize) {
        this.feeders = new Feeder[poolSize];
        this.monitors = new Feeder.Monitor[poolSize];
        for (int i = 0; i < poolSize; i++) {
            this.feeders[i] = new Feeder<>();
            this.feeders[i].addListener(new Feeder.Listener() {
                @Override
                public void becameIdle() {
                    triggerStatus();
                }

                @Override
                public void becameBusy() {
                    triggerStatus();
                }
            });
            this.monitors[i] = this.feeders[i].monitor();
        }
    }

    private void triggerStatus() {
        if(this.isIdle()) {
            for (Listener listener : this.listeners) {
                try {
                    if(listener != null) {
                        listener.becameIdle();
                    }
                } catch (Exception e) {
                    log.error("exception in feeder listener" + listener.getClass().getName()+ ", ignored", e);
                }
            }
        } else {
            for (Listener listener : this.listeners) {
                try {
                    if(listener != null) {
                        listener.becameBusy();
                    }
                } catch (Exception e) {
                    log.error("exception in feeder listener" + listener.getClass().getName()+ ", ignored", e);
                }
            }
        }
    }

    public void addFeederListener(Feeder.Listener listener) {
        for (Feeder<In> feeder : this.feeders) {
            feeder.addListener(listener);
        }
    }

    public void addPoolListener(FeederPool.Listener listener) {
        this.listeners.add(listener);
    }

    public boolean isIdle() {
        for (Feeder<In> feeder : this.feeders) {
            if(feeder.isIdle()) return true;
        }
        return false;
    }

    public Feeder.Handle<In> reserve() throws NotIdleException {
        for (Feeder<In> feeder : this.feeders) {
            if(feeder.isIdle()) {
                try {
                    return feeder.reserve();
                } catch (NotIdleException e) {}
            }
        }
        throw new NotIdleException("no idle feeder available");
    }

    public Feeder.Monitor<In>[] monitors() {
        return this.monitors;
    }

    public interface Listener {
        void becameIdle();
        void becameBusy();
    }
}
