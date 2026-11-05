package org.codingmatters.tasks.support.specs.testdomain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MomentsTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 17, 0, 0);

    @Test
    void givenGherkinMoment__whenParsing__thenLocalDateTime() {
        assertThat(Moments.parse("2026-01-05 à 09:30"), is(LocalDateTime.of(2026, 1, 5, 9, 30)));
    }

    @Test
    void givenMomentWithinLastMinute__whenCheckingJustBefore__thenTrue() {
        assertThat(Moments.isJustBefore(NOW.minusSeconds(59), NOW), is(true));
        assertThat(Moments.isJustBefore(NOW, NOW), is(true));
    }

    @Test
    void givenMomentOlderThanAMinute__whenCheckingJustBefore__thenFalse() {
        assertThat(Moments.isJustBefore(NOW.minusMinutes(2), NOW), is(false));
    }

    @Test
    void givenMomentInTheFuture__whenCheckingJustBefore__thenFalse() {
        assertThat(Moments.isJustBefore(NOW.plusSeconds(5), NOW), is(false));
    }

    @Test
    void givenNoMoment__whenCheckingJustBefore__thenFalse() {
        assertThat(Moments.isJustBefore(null, NOW), is(false));
    }
}
