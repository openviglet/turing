/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.viglet.turing.rag.TurGenAiContext;
import com.viglet.turing.rag.TurSNFullContextService;
import com.viglet.turing.searchengine.dsl.TurDslQuery;
import com.viglet.turing.searchengine.dsl.TurDslQueryRequest;
import com.viglet.turing.searchengine.dsl.TurDslSearchResponse;
import com.viglet.turing.sn.dsl.TurDslSearchService;
import com.viglet.turing.sn.nl.TurSNNLFilters;

/**
 * T1360 — a dated question narrows chat and {@code /answer} retrieval through the
 * site's own search: the vector store has no form for a range, so the documents of
 * the period are read there and retrieval is confined to them.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNGenAiDatedRetrievalTest {

    private static final Locale PT = Locale.of("pt", "BR");
    private static final TurSNNLFilters.Filter THIS_WEEK_START = new TurSNNLFilters.Filter("data_inicio",
            List.of(), null, "2026-11-29T23:59:59Z", List.of("esta semana"), 1.0, true);
    private static final TurSNNLFilters.Filter THIS_WEEK_END = new TurSNNLFilters.Filter("data_fim",
            List.of(), "2026-11-23T00:00:00Z", null, List.of("esta semana"), 1.0, true);

    private TurDslSearchService dslSearchService;
    private VectorStore vectorStore;
    private TurSNGenAi genAi;

    @BeforeEach
    void setUp() {
        dslSearchService = mock(TurDslSearchService.class);
        vectorStore = mock(VectorStore.class);
        genAi = new TurSNGenAi(null, null, null, null, null, null, null, null, null, null,
                new TurSNFullContextService(), null, null, null, dslSearchService);
    }

    private static TurSNNLFilters thisWeek(String q, TurSNNLFilters.Filter... more) {
        List<TurSNNLFilters.Filter> filters = new ArrayList<>(List.of(THIS_WEEK_START, THIS_WEEK_END));
        filters.addAll(List.of(more));
        return new TurSNNLFilters(true, "o que tem esta semana", q, filters, List.of(), List.of());
    }

    @Test
    @DisplayName("A range confines retrieval to the documents the site's search finds in the period")
    void aRangeConfinesRetrievalToThePeriod() {
        ArgumentCaptor<TurDslQueryRequest> body = ArgumentCaptor.forClass(TurDslQueryRequest.class);
        when(dslSearchService.search(eq("fespsp"), eq("pt_BR"), body.capture()))
                .thenReturn(Optional.of(hits("expo", "talk")));
        TurSNNLFilters.Filter palestra = new TurSNNLFilters.Filter("tipo_evento", List.of("Palestra"), null, null,
                List.of("palestras"), 0.6, true);

        Map<String, List<String>> filters = genAi.retrievalFilters("fespsp", PT, thisWeek("o que tem", palestra));

        assertThat(filters).containsExactly(Map.entry("tipo_evento", List.of("Palestra")),
                Map.entry(TurSNGenAi.SOURCE_ID, List.of("expo", "talk")));
        // "o que tem" is only stop words: the period alone is searched, once.
        verify(dslSearchService, times(1)).search(any(), any(), any());
        TurDslQuery.Bool query = (TurDslQuery.Bool) body.getValue().query();
        assertThat(query.must()).isEmpty();
        assertThat(query.filter()).hasSize(3).anySatisfy(clause -> assertThat(clause)
                .isInstanceOfSatisfying(TurDslQuery.Range.class,
                        range -> assertThat(range.field()).isEqualTo("data_fim")));
        assertThat(body.getValue().size()).isEqualTo(TurSNGenAi.DATED_HITS);
    }

    @Test
    @DisplayName("Words that find nothing in the period are dropped, and the period is searched alone")
    void wordsThatFindNothingAreDropped() {
        when(dslSearchService.search(eq("fespsp"), eq("pt_BR"), any()))
                .thenReturn(Optional.of(hits()))
                .thenReturn(Optional.of(hits("expo")));

        Map<String, List<String>> filters = genAi.retrievalFilters("fespsp", PT, thisWeek("democracia"));

        assertThat(filters).containsExactly(Map.entry(TurSNGenAi.SOURCE_ID, List.of("expo")));
        verify(dslSearchService, times(2)).search(any(), any(), any());
    }

    @Test
    @DisplayName("Nothing in the period, a failing search, or no range leaves the value filters alone")
    void nothingDatedLeavesTheValueFilters() {
        when(dslSearchService.search(any(), any(), any())).thenReturn(Optional.of(hits()));
        assertThat(genAi.retrievalFilters("fespsp", PT, thisWeek(""))).isEmpty();

        when(dslSearchService.search(any(), any(), any())).thenThrow(new IllegalStateException("engine down"));
        assertThat(genAi.retrievalFilters("fespsp", PT, thisWeek(""))).isEmpty();

        TurDslSearchService untouched = mock(TurDslSearchService.class);
        TurSNGenAi plain = new TurSNGenAi(null, null, null, null, null, null, null, null, null, null,
                new TurSNFullContextService(), null, null, null, untouched);
        TurSNNLFilters values = new TurSNNLFilters(true, "palestras", "", List.of(new TurSNNLFilters.Filter(
                "tipo_evento", List.of("Palestra"), null, null, List.of("palestras"), 1.0, true)), List.of(),
                List.of());
        assertThat(plain.retrievalFilters("fespsp", PT, values))
                .containsExactly(Map.entry("tipo_evento", List.of("Palestra")));
        verifyNoInteractions(untouched);
    }

    @Test
    @DisplayName("The lexical top-up stays inside the period's documents")
    void theTopUpStaysInsideThePeriod() {
        when(dslSearchService.search(eq("fespsp"), eq("pt_BR"), any()))
                .thenReturn(Optional.of(hits("old-talk", "expo")));
        ArgumentCaptor<SearchRequest> fetched = ArgumentCaptor.forClass(SearchRequest.class);
        when(vectorStore.similaritySearch(fetched.capture())).thenReturn(List.of(chunk("e-1", "expo")));
        TurGenAiContext context = TurGenAiContext.builder().enabled(true).vectorStore(vectorStore)
                .siteName("fespsp").build();

        genAi.withLexicalRecall(context, "o que tem esta semana", PT,
                Map.of(TurSNGenAi.SOURCE_ID, List.of("expo", "talk")), List.of());

        assertThat(fetched.getValue().getFilterExpression().toString()).contains("expo")
                .doesNotContain("old-talk");
    }

    @Test
    @DisplayName("A question that names a period runs the extractor, as a list-style one does")
    void aDatedQuestionNamesADate() {
        assertThat(TurSNGenAi.namesADate("o que tem esta semana?")).isTrue();
        assertThat(TurSNGenAi.namesADate("what is on tonight")).isTrue();
        assertThat(TurSNGenAi.namesADate("Tell me about the Bali surf camp")).isFalse();
        assertThat(TurSNGenAi.namesADate(null)).isFalse();
    }

    @Test
    @DisplayName("T1362: the period note states today and the period an overlap pair spans, with its words")
    void thePeriodNoteStatesTodayAndThePeriod() {
        String note = TurSNGenAi.periodNote(thisWeek(""), java.time.LocalDate.of(2026, 11, 27));

        assertThat(note).contains("today is 2026-11-27")
                .contains("the period from 2026-11-23 to 2026-11-29")
                .contains("(\"esta semana\")");
        TurSNNLFilters values = new TurSNNLFilters(true, "palestras", "", List.of(new TurSNNLFilters.Filter(
                "tipo_evento", List.of("Palestra"), null, null, List.of("palestras"), 1.0, true),
                new TurSNNLFilters.Filter("price", List.of(), "100", "200", List.of(), 1.0, false)),
                List.of(), List.of());
        assertThat(TurSNGenAi.periodNote(values, java.time.LocalDate.of(2026, 11, 27))).isEmpty();
        assertThat(TurSNGenAi.periodNote(null, java.time.LocalDate.of(2026, 11, 27))).isEmpty();
    }

    @Test
    @DisplayName("T1362: the STRICT_RAG judge is asked with the period, and its NO is still the refusal gate")
    void theJudgeIsToldThePeriod() {
        TurGenAiContext context = mock(TurGenAiContext.class);
        org.springframework.ai.chat.model.ChatModel chatModel = mock(org.springframework.ai.chat.model.ChatModel.class);
        when(context.getChatModel()).thenReturn(chatModel);
        ArgumentCaptor<org.springframework.ai.chat.prompt.Prompt> asked =
                ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        when(chatModel.call(asked.capture())).thenReturn(new org.springframework.ai.chat.model.ChatResponse(List.of(
                new org.springframework.ai.chat.model.Generation(
                        new org.springframework.ai.chat.messages.AssistantMessage("NO")))));
        String note = TurSNGenAi.periodNote(thisWeek(""), java.time.LocalDate.of(2026, 11, 27));

        boolean answerable = genAi.canAnswerFromRetrievedContent(context, "o que tem esta semana na FESPSP?",
                List.of(new Document("Sarau no Casarão, 29 de novembro")), note);

        assertThat(answerable).isFalse();
        assertThat(asked.getValue().getContents()).contains(note)
                .containsSubsequence("DATE CONTEXT", "QUESTION: o que tem esta semana", "CONTEXT:");
    }

    @Test
    @DisplayName("T1362: the answer prompt states the period ahead of the website content")
    void theAnswerPromptStatesThePeriod() {
        String note = TurSNGenAi.periodNote(thisWeek(""), java.time.LocalDate.of(2026, 11, 27));

        String prompt = genAi.buildSystemPrompt(null, "[1] Sarau", true, true, note);

        assertThat(prompt).containsSubsequence("STRICT GROUNDING", note, "<untrusted_website_content>");
        assertThat(genAi.buildSystemPrompt(null, "[1] Sarau", true, true)).doesNotContain("DATE CONTEXT");
    }

    @Test
    @DisplayName("T1362: an empty pass confined to a period is not widened to the whole site")
    void aPeriodPassIsNotWidened() {
        assertThat(TurSNGenAi.widensOnEmpty(true, Map.of("tipo_evento", List.of("Palestra")))).isTrue();
        assertThat(TurSNGenAi.widensOnEmpty(true, Map.of(TurSNGenAi.SOURCE_ID, List.of("expo")))).isFalse();
        assertThat(TurSNGenAi.widensOnEmpty(false, Map.of("city", List.of("Santos")))).isFalse();
    }

    private static Document chunk(String id, String source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(TurSNGenAi.SOURCE_ID, source);
        return Document.builder().id(id).text("text of " + id).metadata(metadata).build();
    }

    private static TurDslSearchResponse hits(String... ids) {
        List<TurDslSearchResponse.Hit> list = new ArrayList<>();
        for (String id : ids) {
            list.add(new TurDslSearchResponse.Hit(id, 1.0, Map.of("title", id), null));
        }
        return new TurDslSearchResponse(1L, false,
                new TurDslSearchResponse.Hits(new TurDslSearchResponse.Total(ids.length, "eq"), 1.0, list), null);
    }
}
