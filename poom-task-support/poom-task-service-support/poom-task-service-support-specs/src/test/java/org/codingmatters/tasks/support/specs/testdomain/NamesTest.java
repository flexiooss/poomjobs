package org.codingmatters.tasks.support.specs.testdomain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class NamesTest {

    @Test
    void givenOneName__whenParsing__thenThatName() {
        assertThat(Names.parse("\"A\""), is(List.of("A")));
    }

    @Test
    void givenTwoNames__whenParsing__thenBothInOrder() {
        assertThat(Names.parse("\"fr\" et \"rapide\""), is(List.of("fr", "rapide")));
    }

    @Test
    void givenCommaAndEt__whenParsing__thenAllInOrder() {
        assertThat(Names.parse("\"B\", \"C\" et \"A\""), is(List.of("B", "C", "A")));
    }

    @Test
    void givenNamesWithSpaces__whenParsing__thenSpacesKept() {
        assertThat(Names.parse("\"journal de A\" et \"journal de B\""), is(List.of("journal de A", "journal de B")));
    }
}
