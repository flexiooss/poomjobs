package org.codingmatters.poom.runner.tests;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class Eventually {

    static private AtomicLong timeout = new AtomicLong(2 * 000L);

    public static void timeout(long t) {
        timeout.set(t);
    }

    public static <T> void assertThat(CheckedSupplier<T> actual, Matcher<? super T> matcher) {
        assertThat("", actual, matcher);
    }

    public static <T> void assertThat(String reason, CheckedSupplier<T> actual,
                                      Matcher<? super T> matcher) {
        long start = System.currentTimeMillis();
        AssertionError last = null;
        do {
            try {
                MatcherAssert.assertThat(reason, actual.get(), matcher);
                return;
            } catch (AssertionError ae) {
                last = ae;
            } catch (Exception e) {
                last = new AssertionError("failed invoking actual value supplier", e);
            }
        } while (System.currentTimeMillis() - start < timeout.get());

        throw last;
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
