package com.viglet.turing.genai.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
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
    private TurCatalogKitService service;

    @BeforeEach
    void setUp() {
        copilotService = mock(TurCatalogCopilotService.class);
        chatModelResolver = mock(TurDefaultChatModelResolver.class);
        chatModel = mock(ChatModel.class);
        when(chatModelResolver.resolve()).thenReturn(Optional.of(chatModel));
        when(chatModelResolver.defaultInstance()).thenReturn(Optional.empty());
        service = new TurCatalogKitService(copilotService, chatModelResolver, mock(TurLLMTokenUsageService.class));
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
