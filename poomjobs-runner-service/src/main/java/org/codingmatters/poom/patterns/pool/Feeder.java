package org.codingmatters.poom.patterns.pool;

import org.codingmatters.poom.patterns.pool.exception.NotIdleException;
import org.codingmatters.poom.patterns.pool.exception.NotReservedException;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Feeder<In> {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(Feeder.class);

    enum Status {
        IDLE, RESERVED, RUNNING
    }

    private Status status = Status.IDLE;
    private final Monitor<In> monitor;

    private final AtomicReference<In> in = new AtomicReference<>();
    private final List<Listener> listeners = Collections.synchronizedList(new LinkedList<>());

    public Feeder() {
        this.monitor = new Monitor<>(this);
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public Handle<In> reserve() throws NotIdleException {
        if(this.status.equals(Status.IDLE)) {
            this.status = Status.RESERVED;
            return new Handle<>(this);
        } else {
            throw new NotIdleException("status was " + this.status);
        }
    }


    private synchronized void feed(In with) throws NotReservedException {
        if(this.status.equals(Status.RESERVED)) {
            this.status = Status.RUNNING;
            for (Listener listener : this.listeners) {
                try {
                    if(listener != null) {
                        listener.becameBusy();
                    }
                } catch (Exception e) {
                    log.error("exception in feeder listener" + listener.getClass().getName()+ ", ignored", e);
                }
            }
            this.in.set(with);
            synchronized (this.monitor) {
                this.monitor.notify();
            }
        } else {
            throw new NotReservedException("status was " + this.status);
        }
    }

    private synchronized void done() {
        this.status = Status.IDLE;
        this.in.set(null);
        for (Listener listener : this.listeners) {
            try {
                if(listener != null) {
                    listener.becameIdle();
                }
            } catch (Exception e) {
                log.error("exception in feeder listener" + listener.getClass().getName()+ ", ignored", e);
            }
        }
    }

    public synchronized boolean isIdle() {
        return this.status.equals(Status.IDLE);
    }

    public Monitor<In> monitor() {
        return this.monitor;
    }

    private In in() {
        return this.in.get();
    }



    static public class Handle<In> {
        private final Feeder<In> feeder;

        private Handle(Feeder<In> feeder) {
            this.feeder = feeder;
        }

        public void feed(In with) throws NotReservedException {
            this.feeder.feed(with);
        }
    }

    static public class Monitor<In> {
        private final Feeder<In> feeder;

        Monitor(Feeder<In> feeder) {
            this.feeder = feeder;
        }

        public In in() {
            return this.feeder.in();
        }

        public void done() {
            this.feeder.done();
        }
    }

    public interface Listener {
        void becameIdle();
        void becameBusy();
    }
}
