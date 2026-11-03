package org.codingmatters.tasks.support.specs.testdomain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Moments du Gherkin : date-heure écrite « 2026-01-05 à 09:30 », et « à l'instant ». */
public final class Moments {
    /** « à » écrit \\u00e0 (échappement de regex, pas de source Java) : steps_catalog.py décode mal les regex non ASCII. */
    public static final String DATE_TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\u00e0 \\d{2}:\\d{2}";
    static final Duration JUST_NOW_TOLERANCE = Duration.ofMinutes(1);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd 'à' HH:mm");

    private Moments() {
    }

    public static LocalDateTime parse(String moment) {
        return LocalDateTime.parse(moment, FORMAT);
    }

    /**
     * Vrai si le moment vient de se produire au regard de {@code now} : au plus une minute avant, jamais après
     * (une seconde de marge pour la troncature à la milliseconde du stockage).
     */
    public static boolean isJustBefore(LocalDateTime moment, LocalDateTime now) {
        return moment != null
                && !moment.isAfter(now.plusSeconds(1))
                && !moment.isBefore(now.minus(JUST_NOW_TOLERANCE));
    }
}
