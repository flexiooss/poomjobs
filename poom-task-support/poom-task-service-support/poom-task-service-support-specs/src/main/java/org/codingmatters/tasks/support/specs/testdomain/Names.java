package org.codingmatters.tasks.support.specs.testdomain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Énumération de noms écrite dans le Gherkin : {@code "A"}, {@code "A" et "B"}, {@code "A", "B" et "C"}. */
public final class Names {
    public static final String PATTERN = "\"[^\"]+\"(?:(?:, | et )\"[^\"]+\")*";
    private static final Pattern NAME = Pattern.compile("\"([^\"]+)\"");

    private Names() {
    }

    public static List<String> parse(String names) {
        List<String> result = new ArrayList<>();
        Matcher matcher = NAME.matcher(names);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
