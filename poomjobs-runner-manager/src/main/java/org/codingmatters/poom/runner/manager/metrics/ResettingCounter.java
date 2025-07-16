package org.codingmatters.poom.runner.manager.metrics;

import com.codahale.metrics.Counter;

public class ResettingCounter extends Counter {

    @Override
    public long getCount() {
        long count = super.getCount();
        dec(count);
        return count;
    }
}
