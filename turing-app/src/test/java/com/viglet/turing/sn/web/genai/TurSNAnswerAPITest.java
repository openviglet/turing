/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.web.genai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.viglet.turing.sn.search.TurSNSiteSearchCachedAPI;
import com.viglet.turing.sn.search.TurSNSiteSearchService;
import com.viglet.turing.answer.TurAnswerShareService;
import com.viglet.turing.answer.TurAnswerSignalService;
import com.viglet.turing.answer.TurSNAnswerService;
import com.viglet.turing.rag.citation.TurCitationDriftService;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.TurSNSearchProcess;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSiteView;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshot;
import com.viglet.turing.sn.snapshot.TurSNSiteSearchSnapshotService;

import reactor.core.publisher.Flux;

/**
 * T1189 — {@code /answer} reads the locale as search does: {@code _setlocale},
 * {@code locale} as an alias, and a locale the site has no index for (or none)
 * resolved to the site's default indexed one.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
class TurSNAnswerAPITest {

    private final TurSNAnswerService answerService = mock(TurSNAnswerService.class);
    private final TurSNSearchProcess searchProcess = mock(TurSNSearchProcess.class);
    private final TurSNSiteSearchSnapshotService snapshots = mock(TurSNSiteSearchSnapshotService.class);
    private TurSNAnswerAPI api;

    @BeforeEach
    void setUp() {
        TurSNSiteSearchService searchService = new TurSNSiteSearchService(searchProcess,
                mock(TurSNSiteSearchCachedAPI.class), snapshots);
        api = new TurSNAnswerAPI(answerService, mock(TurCitationDriftService.class),
                mock(TurAnswerShareService.class), mock(TurAnswerSignalService.class), searchService);
        TurSNSite site = new TurSNSite();
        site.setId("site-id");
        site.setName("demo");
        when(snapshots.getSnapshot("demo", null)).thenReturn(Optional.of(new TurSNSiteSearchSnapshot(
                TurSNSiteSearchSiteView.from(site, "lucene"), null, "lucene",
                false, List.of(), Set.of(), Map.of(), Map.of(),
                List.of(new TurSNSiteSearchSnapshot.LocaleView(Locale.US, "demo_en_US", 0)), List.of())));
        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.US)).thenReturn(true);
        when(answerService.answer(any(), any(), any(), any())).thenReturn(Flux.empty());
    }

    @ParameterizedTest
    @CsvSource(value = { "en_US,null", "null,en_US", "en-US,null", "en,null", "null,en", "null,null" },
            nullValues = "null")
    void everySpellingAnswersFromAnIndexedLocale(String setLocale, String locale) {
        api.answer("demo", "light rain jacket", setLocale, locale, null);

        verify(answerService).answer(eq("demo"), eq("light rain jacket"), eq(Locale.US), any());
    }

    @Test
    void setLocaleWinsOverTheAlias() {
        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.CANADA)).thenReturn(true);

        api.answer("demo", "q", "en_US", "en_CA", null);

        verify(answerService).answer(eq("demo"), eq("q"), eq(Locale.US), any());
    }

    /** T1332 — a share is read under the site it was made on, not under any site's URL. */
    @Test
    void aShareIsReadOnlyUnderItsOwnSite() {
        TurAnswerShareService shares = mock(TurAnswerShareService.class);
        TurSNAnswerAPI withShares = new TurSNAnswerAPI(answerService, mock(TurCitationDriftService.class),
                shares, mock(TurAnswerSignalService.class), new TurSNSiteSearchService(searchProcess,
                        mock(TurSNSiteSearchCachedAPI.class), snapshots));
        com.viglet.turing.rag.persistence.model.answer.TurAnswerShare share =
                new com.viglet.turing.rag.persistence.model.answer.TurAnswerShare();
        share.setId("s1");
        share.setSiteName("demo");
        when(shares.find("s1")).thenReturn(Optional.of(share));
        when(shares.renderOgCard("s1", null)).thenReturn(Optional.of("<html/>"));

        org.assertj.core.api.Assertions.assertThat(withShares.readShare("demo", "s1").getStatusCode().value())
                .isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(withShares.readShare("other", "s1").getStatusCode().value())
                .isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(withShares.shareOg("other", "s1", null).getStatusCode().value())
                .isEqualTo(404);
    }
}
