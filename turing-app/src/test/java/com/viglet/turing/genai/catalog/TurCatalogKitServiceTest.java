package com.viglet.turing.genai.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.sn.nl.TurSNNLFilterService;
import com.viglet.turing.system.TurDefaultChatModelResolver;

/**
 * T1159 — a described plan comes back as staged products by need, every product
 * one the catalog retrieval returned.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurCatalogKitServiceTest {

    private TurCatalogCopilotService copilotService;
    private TurDefaultChatModelResolver chatModelResolver;
    private ChatModel chatModel;
    private TurSNNLFilterService nlFilterService;
    private TurCatalogKitService service;

    @BeforeEach
    void setUp() {
        copilotService = mock(TurCatalogCopilotService.class);
        chatModelResolver = mock(TurDefaultChatModelResolver.class);
        chatModel = mock(ChatModel.class);
        nlFilterService = mock(TurSNNLFilterService.class);
        when(chatModelResolver.resolve()).thenReturn(Optional.of(chatModel));
        when(chatModelResolver.defaultInstance()).thenReturn(Optional.empty());
        service = new TurCatalogKitService(copilotService, chatModelResolver, mock(TurLLMTokenUsageService.class),
                nlFilterService);
    }

    /** T1169 — the gap prompt carries the catalog's facet values, so queries use its words. */
    @Test
    void theGapPromptNamesTheSitesFacetValues() {
        Map<String, List<String>> vocabulary = new LinkedHashMap<>();
        vocabulary.put("category", List.of("Jackets", "Fleece"));
        vocabulary.put("empty", List.of());
        String prompt = TurCatalogKitService.gapPrompt("outdoor", "a rainy weekend", 3, vocabulary);

        assertTrue(prompt.contains("- category: Jackets, Fleece"));
        assertFalse(prompt.contains("empty"));
        assertTrue(prompt.contains("PLAN: a rainy weekend"));
        assertFalse(TurCatalogKitService.gapPrompt("outdoor", "a trip", 3, Map.of())
                .contains("catalog's own words"));
    }

    @Test
    void thePlanSendsTheReadValuesToTheModelAndSurvivesAFailedRead() {
        modelReplies("GAP: Shell | waterproof jacket | rain all weekend");
        retrievalReturns("waterproof jacket", product("j1"));
        when(nlFilterService.facetVocabulary("outdoor", "en", TurCatalogKitService.VOCABULARY_VALUES))
                .thenReturn(Map.of("category", List.of("Jackets")));

        service.plan("outdoor", "en", "a rainy weekend camping trip", null, null);

        ArgumentCaptor<Prompt> sent = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(sent.capture());
        assertTrue(sent.getValue().getContents().contains("- category: Jackets"));

        when(nlFilterService.facetVocabulary(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalStateException("engine down"));
        TurCatalogKitService.Result result = service.plan("outdoor", "en", "a rainy weekend camping trip",
                null, null);
        assertNull(result.error());
        assertEquals(1, result.stages().size());
    }

    private void modelReplies(String text) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }

    private static TurCatalogCitation product(String id) {
        return new TurCatalogCitation(1, id, "Product " + id, "/p/" + id, 1.0, null);
    }

    private void retrievalReturns(String query, TurCatalogCitation... products) {
        when(copilotService.retrieve(eq("outdoor"), eq("en"), eq(query))).thenReturn(
                new TurCatalogCopilotService.Retrieval(List.of(products), List.of(), products.length, null, true));
    }

    @Test
    void stagesTheRetrievedProductsUnderEachNeed() {
        modelReplies("""
                SUMMARY: A two-day mountain crossing in July for a beginner.
                GAP: Shelter | lightweight tent | nights on the trail
                GAP: Water | water filter | streams are the only source
                GAP: Navigation | trail map compass | first time on the route""");
        retrievalReturns("lightweight tent", product("t1"), product("t2"), product("t3"), product("t4"));
        retrievalReturns("water filter", product("w1"), product("t1"));
        retrievalReturns("trail map compass");

        TurCatalogKitService.Result result = service.plan("outdoor", "en",
                "two days crossing the mountains in July, first time", null, 3);

        assertNull(result.error());
        assertEquals("A two-day mountain crossing in July for a beginner.", result.summary());
        assertEquals(2, result.stages().size());
        assertEquals("Shelter", result.stages().get(0).gap());
        assertEquals("nights on the trail", result.stages().get(0).reason());
        assertEquals(List.of("t1", "t2", "t3"),
                result.stages().get(0).products().stream().map(TurCatalogKitService.KitProduct::id).toList());
        // t1 is already staged under Shelter, so Water keeps only w1.
        assertEquals(List.of("w1"),
                result.stages().get(1).products().stream().map(TurCatalogKitService.KitProduct::id).toList());
        assertEquals(List.of("Navigation"), result.unfilled());
    }

    /** T1159 — each product's reason quotes its own attributes, the gap's words first. */
    @Test
    void eachProductsReasonComesFromItsOwnAttributes() {
        modelReplies("GAP: Shelter | lightweight tent | nights on the trail, light to carry");
        when(copilotService.retrieve(eq("outdoor"), eq("en"), eq("lightweight tent"))).thenReturn(
                new TurCatalogCopilotService.Retrieval(List.of(product("t1"), product("t2")), List.of(), 2, null,
                        true, List.of(
                                new java.util.LinkedHashMap<>(java.util.Map.of("color", "green")) {
                                    {
                                        put("weight", "1.2 kg, light");
                                        put("capacity", "2 people");
                                    }
                                },
                                java.util.Map.of("capacity", "3 people"))));

        TurCatalogKitService.Result result = service.plan("outdoor", "en", "a trip", null, null);

        List<TurCatalogKitService.KitProduct> products = result.stages().getFirst().products();
        assertEquals("weight: 1.2 kg, light; color: green", products.get(0).reason(),
                "the attribute sharing a word with the gap leads");
        assertEquals("capacity: 3 people", products.get(1).reason());
        assertNull(products.get(0).available(), "no availability field was named");
    }

    /** T1159 — a named availability field marks, or drops, an unavailable product. */
    @Test
    void anAvailabilityFieldMarksOrDropsUnavailableProducts() {
        modelReplies("GAP: Shelter | tent | nights out");
        when(copilotService.retrieve(eq("outdoor"), eq("en"), eq("tent"))).thenReturn(
                new TurCatalogCopilotService.Retrieval(List.of(product("t1"), product("t2"), product("t3")),
                        List.of(), 3, null, true, List.of(
                                java.util.Map.of("in_stock", "Out of stock"),
                                java.util.Map.of("in_stock", "true"),
                                java.util.Map.of())));

        var marked = service.plan("outdoor", "en", "a trip", new TurCatalogKitService.Options(null, 3,
                "in_stock", TurCatalogKitService.Availability.MARK)).stages().getFirst().products();
        assertEquals(java.util.Arrays.asList(false, true, null),
                marked.stream().map(TurCatalogKitService.KitProduct::available).toList());
        assertNull(marked.get(1).reason(), "the availability field is not a reason");

        var dropped = service.plan("outdoor", "en", "a trip", new TurCatalogKitService.Options(null, 3,
                "in_stock", TurCatalogKitService.Availability.DROP)).stages().getFirst().products();
        assertEquals(List.of("t2", "t3"), dropped.stream().map(TurCatalogKitService.KitProduct::id).toList());
    }

    /** T1167 — a guide of a requested type is cited; a product-typed or staged one is not. */
    @Test
    void citesGuidesOfTheRequestedTypesOnly() {
        modelReplies("GAP: Shelter | tent | nights out");
        retrievalReturns("tent", product("t1"));
        when(copilotService.retrieve(eq("outdoor"), eq("en"), eq("a first mountain crossing"))).thenReturn(
                new TurCatalogCopilotService.Retrieval(
                        List.of(product("g1"), product("t1"), product("p9"), product("g2"), product("g3")),
                        List.of(), 5, null, true, List.of(
                                java.util.Map.of("type", "Guide"),
                                java.util.Map.of("type", "guide"),
                                java.util.Map.of("type", "product"),
                                java.util.Map.of("type", "checklist"),
                                java.util.Map.of("type", "guide"))));

        var guided = service.plan("outdoor", "en", "a first mountain crossing",
                new TurCatalogKitService.Options(null, null, null, null, List.of("guide", "checklist")));

        assertEquals(List.of("g1", "g2"), guided.guides().stream().map(TurCatalogCitation::id).toList(),
                "t1 is staged, p9 is a product, and two guides is the cap");
    }

    @Test
    void withoutGuideTypesNoGuideIsSearched() {
        modelReplies("GAP: Shelter | tent | nights out");
        retrievalReturns("tent", product("t1"));

        var result = service.plan("outdoor", "en", "a first mountain crossing", null, null);

        assertTrue(result.guides().isEmpty());
        verify(copilotService, never()).retrieve("outdoor", "en", "a first mountain crossing");
    }

    @Test
    void aPlanWithoutGapsSearchesNothing() {
        modelReplies("SUMMARY: Just browsing.");

        TurCatalogKitService.Result result = service.plan("outdoor", "en", "just browsing", null, null);

        assertNotNull(result.error());
        verify(copilotService, never()).retrieve(anyString(), anyString(), anyString());
    }

    @Test
    void anUnknownSiteIsAnError() {
        modelReplies("GAP: Shelter | tent | nights out");
        when(copilotService.retrieve(any(), any(), any()))
                .thenReturn(new TurCatalogCopilotService.Retrieval(List.of(), List.of(), 0, null, false));

        assertNotNull(service.plan("missing", "en", "a trip", null, null).error());
    }

    @Test
    void withoutADefaultLlmThereIsNoKit() {
        when(chatModelResolver.resolve()).thenReturn(Optional.empty());

        assertNotNull(service.plan("outdoor", "en", "a trip", null, null).error());
    }

    @Test
    void aFailedModelCallIsAnErrorNotAnException() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("model down"));

        assertTrue(service.plan("outdoor", "en", "a trip", null, null).error().contains("model down"));
    }

    @Test
    void parsingIsLenientAndCapped() {
        TurCatalogKitService.Plan plan = TurCatalogKitService.parse("""
                Here is the kit:
                - GAP: Shelter | tent | nights
                2. GAP: shelter | duplicate | ignored
                GAP: Layers
                GAP: Water | filter | streams""", 2);

        assertNull(plan.summary());
        assertEquals(2, plan.gaps().size());
        assertEquals("Layers", plan.gaps().get(1).name());
        assertEquals("Layers", plan.gaps().get(1).query(), "a gap without a query searches its name");
        assertNull(plan.gaps().get(1).reason());
    }
}
