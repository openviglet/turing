/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.genai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.viglet.turing.commons.se.TurSEPartition;
import com.viglet.turing.sn.dsl.TurDslSearchResponse;
import com.viglet.turing.sn.dsl.TurDslSearchService;

/**
 * T1294 — a thin vector pass in SN chat is topped up with the site's own lexical
 * search: "Tell me about surf camps" keeps the one page the vectors found and gains
 * the "Bali Surf Camp" pages the site's search ranks first.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNGenAiLexicalRecallTest {

    private static final Locale EN = Locale.of("en", "US");

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

    @Test
    void aThinVectorPassGainsTheSitesLexicalHits() {
        List<Document> vector = List.of(chunk("sd-1", "san-diego"), chunk("sd-2", "san-diego"));
        when(dslSearchService.search(eq("wknd"), eq("en_US"), any()))
                .thenReturn(Optional.of(hits("bali-1", "bali-2", "san-diego")));
        ArgumentCaptor<SearchRequest> fetched = ArgumentCaptor.forClass(SearchRequest.class);
        when(vectorStore.similaritySearch(fetched.capture())).thenReturn(List.of(
                chunk("b1-a", "bali-1"), chunk("b1-b", "bali-1"), chunk("b2-a", "bali-2"),
                chunk("sd-3", "san-diego")));

        List<Document> merged = genAi.withLexicalRecall(context(null), "surf camps", EN, null, vector);

        assertThat(merged).extracting(TurSNGenAiLexicalRecallTest::source)
                .containsOnly("san-diego", "bali-1", "bali-2");
        // The vector-found page keeps its own chunks; the lexical-only pages bring theirs.
        assertThat(merged).extracting(Document::getId)
                .containsExactlyInAnyOrder("sd-1", "sd-2", "b1-a", "b1-b", "b2-a");
        assertThat(fetched.getValue().getFilterExpression().toString())
                .contains("source_id").contains("bali-1").contains("bali-2");
    }

    @Test
    void theLexicalChunksStayInsideTheFiltersAndThePartition() {
        when(dslSearchService.search(eq("wknd"), eq("en_US"), any()))
                .thenReturn(Optional.of(hits("bali-1")));
        ArgumentCaptor<SearchRequest> fetched = ArgumentCaptor.forClass(SearchRequest.class);
        when(vectorStore.similaritySearch(fetched.capture())).thenReturn(List.of());

        List<Document> vector = List.of(chunk("sd-1", "san-diego"));
        List<Document> merged = genAi.withLexicalRecall(context(new TurSEPartition("tenant", "acme")),
                "surf camps", EN, Map.of("type", List.of("article")), vector);

        assertThat(merged).isSameAs(vector);
        assertThat(fetched.getValue().getFilterExpression().toString())
                .contains("type").contains("article").contains("tenant").contains("acme");
    }

    @Test
    void enoughDistinctDocumentsSkipTheLexicalSearch() {
        List<Document> vector = List.of(chunk("a", "d1"), chunk("b", "d2"), chunk("c", "d3"));

        assertThat(genAi.withLexicalRecall(context(null), "surf camps", EN, null, vector)).isSameAs(vector);
        verifyNoInteractions(dslSearchService);
    }

    @Test
    void noLocaleOrAFailingSearchKeepsTheVectorPass() {
        List<Document> vector = List.of(chunk("sd-1", "san-diego"));
        assertThat(genAi.withLexicalRecall(context(null), "surf camps", null, null, vector)).isSameAs(vector);
        verifyNoInteractions(dslSearchService);

        when(dslSearchService.search(any(), any(), any())).thenThrow(new IllegalStateException("engine down"));
        assertThat(genAi.withLexicalRecall(context(null), "surf camps", EN, null, vector)).isSameAs(vector);
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void anEmptyVectorPassIsFilledFromTheSitesSearch() {
        when(dslSearchService.search(eq("wknd"), eq("en_US"), any()))
                .thenReturn(Optional.of(hits("bali-1")));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk("b1-a", "bali-1")));

        assertThat(genAi.withLexicalRecall(context(null), "surf camps", EN, null, List.of()))
                .extracting(Document::getId).containsExactly("b1-a");
    }

    /** T1302 — the single-turn chat answers from the site's search hits too, given its locale. */
    @Test
    void theSingleTurnChatTakesTheSitesHitsGivenItsLocale() {
        org.springframework.ai.chat.model.ChatModel chatModel = mock(org.springframework.ai.chat.model.ChatModel.class);
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(
                new org.springframework.ai.chat.model.ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(
                        new org.springframework.ai.chat.messages.AssistantMessage("Bali has surf camps.")))));
        TurGenAiContext context = TurGenAiContext.builder().enabled(true).vectorStore(vectorStore)
                .chatModel(chatModel).siteName("wknd").build();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(chunk("sd-1", "san-diego")))
                .thenReturn(List.of(chunk("b1-a", "bali-1")));
        when(dslSearchService.search(eq("wknd"), eq("en_US"), any())).thenReturn(Optional.of(hits("bali-1")));
        TurSNGenAi chat = new TurSNGenAi(null, null, null, null, null, null,
                mock(com.viglet.turing.system.TurGlobalSettingsService.class), null, null, null,
                new TurSNFullContextService(), null, null, null, dslSearchService);

        assertThat(chat.assistant(context, "surf camps in Bali", EN).getText()).isEqualTo("Bali has surf camps.");
        ArgumentCaptor<org.springframework.ai.chat.prompt.Prompt> prompt =
                ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions().getFirst().getText()).contains("text of b1-a");
    }

    @Test
    void fusionCapsALexicalDocumentAtThreeChunks() {
        List<Document> lexical = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            lexical.add(chunk("b-" + i, "bali-1"));
        }

        assertThat(TurSNGenAi.fuseByDocument(List.of(), List.of("bali-1"), lexical)).hasSize(TurSNGenAi.PINNED_CHUNKS);
    }

    private TurGenAiContext context(TurSEPartition partition) {
        return TurGenAiContext.builder().enabled(true).vectorStore(vectorStore).siteName("wknd")
                .partition(partition).build();
    }

    private static Document chunk(String id, String source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(TurSNGenAi.SOURCE_ID, source);
        return Document.builder().id(id).text("text of " + id).metadata(metadata).build();
    }

    private static String source(Document document) {
        return String.valueOf(document.getMetadata().get(TurSNGenAi.SOURCE_ID));
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
