/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.nl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

/**
 * T1363 — chunk times in the site's wall clock.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSiteWallClockTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void anEveningEventOnAUtcMinusThreeSiteReadsAtItsOwnHour() {
        assertEquals("startDate: 2026-11-24 19:00",
                TurSiteWallClock.localize("startDate: 2026-11-24T22:00:00Z", SAO_PAULO));
    }

    @Test
    void anInstantPastMidnightUtcFallsBackOnTheLocalDay() {
        assertEquals("ends 2026-11-24 22:30.",
                TurSiteWallClock.localize("ends 2026-11-25T01:30:00.000Z.", SAO_PAULO));
    }

    @Test
    void aDateOnlyValueStaysItsOwnDay() {
        assertEquals("publicationDate: 2026-11-24",
                TurSiteWallClock.localize("publicationDate: 2026-11-24T00:00:00Z", SAO_PAULO));
    }

    @Test
    void everyInstantInTheTextIsConverted() {
        assertEquals("from 2026-11-24 19:00 to 2026-11-24 21:00",
                TurSiteWallClock.localize("from 2026-11-24T22:00:00Z to 2026-11-25T00:00:01Z", SAO_PAULO));
    }

    @Test
    void noZoneLeavesTheTextAsItIs() {
        String text = "startDate: 2026-11-24T22:00:00Z";
        assertSame(text, TurSiteWallClock.localize(text, null));
    }

    @Test
    void textWithoutAnInstantIsUnchanged() {
        assertEquals("Zebra 2026-11-24 and 12:00Z", TurSiteWallClock.localize("Zebra 2026-11-24 and 12:00Z", SAO_PAULO));
    }

    @Test
    void aDocumentKeepsItsIdAndMetadata() {
        Document document = new Document("doc-1", "at 2026-11-24T22:00:00Z", Map.of("sites", "fespsp"));
        Document localized = TurSiteWallClock.localize(document, SAO_PAULO);
        assertEquals("at 2026-11-24 19:00", localized.getText());
        assertEquals("doc-1", localized.getId());
        assertEquals("fespsp", localized.getMetadata().get("sites"));
        Document untouched = new Document("no time here");
        assertSame(untouched, TurSiteWallClock.localize(untouched, SAO_PAULO));
        assertEquals(List.of(localized.getText()),
                TurSiteWallClock.localize(List.of(document), SAO_PAULO).stream().map(Document::getText).toList());
    }
}
