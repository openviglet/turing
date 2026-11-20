package com.viglet.turing.searchengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class TurSEDateMathTest {

    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

    @Test
    void dateMathRoundsAndSteps() {
        assertEquals(Instant.parse("2026-10-04T00:00:00Z"), TurSEDateMath.parse("NOW/DAY-7DAYS", NOW));
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), TurSEDateMath.parse("NOW/MONTH", NOW));
        assertEquals(Instant.parse("2026-10-09T00:00:00Z"),
                TurSEDateMath.parse("2026-10-10T00:00:00Z-1DAY", NOW));
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), TurSEDateMath.parse("2026-05-20/YEAR", NOW));
    }

    @Test
    void bareNowIsTheAnchor() {
        assertEquals(NOW, TurSEDateMath.parse("NOW", NOW));
    }

    @Test
    void unknownUnitIsRefused() {
        assertThrows(DateTimeParseException.class, () -> TurSEDateMath.parse("NOW+1FORTNIGHT", NOW));
    }
}
