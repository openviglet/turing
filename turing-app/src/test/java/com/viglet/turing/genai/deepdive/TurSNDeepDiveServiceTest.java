package com.viglet.turing.genai.deepdive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.transaction.PlatformTransactionManager;

import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.genai.TurSNChatRouteResolver;
import com.viglet.turing.genai.catalog.TurCatalogCitation;
import com.viglet.turing.genai.catalog.TurCatalogCopilotService;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.service.llm.budget.TurChatCostBudgetGate;
import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.sn.TurSNSearchProcess;
import com.viglet.turing.sn.dsl.TurDslCallerFilters;
import com.viglet.turing.sn.dsl.TurDslQuery;
import com.viglet.turing.sn.nl.TurSNNLFilterService;
import com.viglet.turing.system.TurDefaultChatModelResolver;

import tools.jackson.databind.json.JsonMapper;

/**
 * T1220 — the deep-dive research stream: plan, retrieve per step inside the
 * caller's filters, compose cited sections, report what the index lacks.
 */
@ExtendWith(MockitoExtension.class)
class TurSNDeepDiveServiceTest {

    private static final Map<String, List<String>> FILTERS = Map.of("site_key", List.of("acervo"));

    @Mock
    private TurSNSearchProcess searchProcess;
    @Mock
    private TurSNChatRouteResolver routeResolver;
    @Mock
    private TurDefaultAgentResolver agentResolver;
    @Mock
    private TurDefaultChatModelResolver chatModelResolver;
    @Mock
    private TurCatalogCopilotService copilotService;
    @Mock
    private TurLLMTokenUsageService tokenUsageService;
    @Mock
    private TurChatCostBudgetGate budgetGate;
    @Mock
    private TurSNNLFilterService nlFilterService;

    private final ChatModel chatModel = mock(ChatModel.class);
    private final TurLLMInstance instance = new TurLLMInstance();
    private final List<Map<String, List<String>>> filtersSeen = new ArrayList<>();

    private TurSNDeepDiveService service() {
        return new TurSNDeepDiveService(searchProcess, routeResolver, agentResolver, chatModelResolver,
                copilotService, tokenUsageService, budgetGate, nlFilterService,
                mock(PlatformTransactionManager.class));
    }

    private void site() {
        when(searchProcess.getSNSite("acervo")).thenReturn(Optional.of(new TurSNSite()));
    }

    private void ready() {
        site();
        when(routeResolver.resolve(any())).thenReturn(
                new TurSNChatRouteResolver.Decision(TurSNChatRouteResolver.Route.RAG, null));
        when(chatModelResolver.resolve()).thenReturn(Optional.of(chatModel));
        when(chatModelResolver.defaultInstance()).thenReturn(Optional.of(instance));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> groups(List<ServerSentEvent<String>> events) {
        return (Map<String, Object>) payload(named(events, "sources").getFirst()).get("groups");
    }

    private void replies(String plan, String composition) {
        when(chatModel.call(any(Prompt.class))).thenReturn(response(plan), response(composition));
    }

    private static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static TurCatalogCitation doc(String id, String type) {
        return new TurCatalogCitation(1, id, "Title " + id, "https://example.org/" + id, 1.0, null, type);
    }

    private void retrieves(String query, TurCatalogCitation... docs) {
        when(copilotService.retrieve(eq("acervo"), eq("pt_BR"), eq(query))).thenAnswer(inv -> {
            filtersSeen.add(TurDslCallerFilters.current());
            List<String> blocks = new ArrayList<>();
            for (TurCatalogCitation doc : docs) {
                blocks.add("Text of " + doc.id());
            }
            return new TurCatalogCopilotService.Retrieval(List.of(docs), blocks, docs.length, null, true);
        });
    }

    private List<ServerSentEvent<String>> run() {
        return service().research("acervo", "O que a instituição fez em saúde pública?", Locale.of("pt", "BR"),
                FILTERS, null).collectList().block();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(ServerSentEvent<String> event) {
        return JsonMapper.builder().build().readValue(event.data(), Map.class);
    }

    private static List<ServerSentEvent<String>> named(List<ServerSentEvent<String>> events, String name) {
        return events.stream().filter(e -> name.equals(e.event())).toList();
    }

    @Test
    void composesCitedSectionsInsideTheFiltersAndReportsTheEmptyStep() {
        ready();
        replies("""
                SUMMARY: o que a instituição produziu sobre saúde pública
                STEP: Quais pesquisas? | pesquisas saúde pública
                STEP: Quais eventos? | eventos saúde
                STEP: Quais cursos? | cursos saúde
                """, """
                SECTION: Pesquisas
                A Acervo estudou o SUS [1] e a atenção básica [2][9].
                SECTION: Eventos
                Um seminário discutiu vacinas [3].
                SECTION: Opinião
                A saúde pública é importante.
                UNCOVERED: cursos de saúde
                """);
        retrieves("pesquisas saúde pública", doc("p1", "research"), doc("p2", "research"));
        retrieves("eventos saúde", doc("e1", "evento"), doc("p1", "research"));
        retrieves("cursos saúde");

        List<ServerSentEvent<String>> events = run();

        // Every read stayed inside the caller's filters.
        assertThat(filtersSeen).hasSize(3).allMatch(FILTERS::equals);
        assertThat(named(events, "plan")).hasSize(1);

        List<ServerSentEvent<String>> steps = named(events, "step");
        assertThat(steps).hasSize(3);
        assertThat(payload(steps.get(1)).get("sources")).isEqualTo(List.of(3, 1)); // p1 keeps number 1
        assertThat(payload(steps.get(2)).get("hits")).isEqualTo(0);

        List<ServerSentEvent<String>> sections = named(events, "section");
        assertThat(sections).hasSize(2);
        Map<String, Object> research = payload(sections.getFirst());
        assertThat(research.get("citations")).isEqualTo(List.of(1, 2));
        assertThat((String) research.get("text")).doesNotContain("[9]");
        // Every section cites a document; the uncited one is reported, not shown.
        sections.forEach(s -> assertThat((List<?>) payload(s).get("citations")).isNotEmpty());
        assertThat(payload(named(events, "uncovered").getFirst()).get("items"))
                .isEqualTo(List.of("Quais cursos?", "cursos de saúde", "Opinião"));

        assertThat(groups(events)).containsOnlyKeys("research", "evento");
        assertThat(events.getLast().event()).isEqualTo("done");
        // Both calls are metered as the research stage of this site.
        verify(tokenUsageService, times(2)).recordUsage(eq(instance), any(), eq("system"), any(),
                eq(TurSNDeepDiveService.STAGE_RESEARCH), isNull(), eq("acervo"));
    }

    @Test
    void aThinStepWidensInsideTheFacetValueItNames() {
        ready();
        replies("""
                STEP: Quais eventos de saúde? | seminário saúde
                STEP: Quais pesquisas? | pesquisas
                STEP: Quais cursos? | cursos
                """, """
                SECTION: Eventos
                Um seminário sobre vacinas [1].
                """);
        retrieves("seminário saúde");
        retrieves("pesquisas", doc("r1", null), doc("r2", null), doc("r3", null), doc("r4", null));
        retrieves("cursos", doc("c1", null), doc("c2", null), doc("c3", null), doc("c4", null));
        when(nlFilterService.facetVocabulary(eq("acervo"), eq("pt_BR"), any(Integer.class)))
                .thenReturn(Map.of("tema", List.of("Saúde Pública", "Educação"), "site", List.of("acervo")));
        when(copilotService.retrieve(eq("acervo"), eq("pt_BR"), eq("seminário saúde"),
                eq(new TurDslQuery.Term("tema", "Saúde Pública"))))
                .thenReturn(new TurCatalogCopilotService.Retrieval(List.of(doc("e1", "evento")),
                        List.of("Seminário de vacinas"), 1, null, true));

        List<ServerSentEvent<String>> events = run();

        Map<String, Object> first = payload(named(events, "step").getFirst());
        assertThat(first).containsEntry("widenedBy", "tema=Saúde Pública").containsEntry("sources", List.of(1));
        assertThat(named(events, "section")).hasSize(1);
    }

    @Test
    void citedDatedSourcesBecomeATimelineInDateOrder() {
        List<TurSNDeepDiveService.Source> sources = List.of(
                new TurSNDeepDiveService.Source(1, doc("a", null), "A", 1, "2024-05-01"),
                new TurSNDeepDiveService.Source(2, doc("b", null), "B", 1, "2019-03-10"),
                new TurSNDeepDiveService.Source(3, doc("c", null), "C", 1, null),
                new TurSNDeepDiveService.Source(4, doc("d", null), "D", 2, "2021-01-01"));
        List<TurSNDeepDiveService.Section> sections = List.of(
                new TurSNDeepDiveService.Section("S", "x [1][2][3]", List.of(1, 2, 3)));

        List<Map<String, Object>> timeline = TurSNDeepDiveService.timeline(sources, sections);

        // Oldest first; the undated source and the uncited one (4) are left out.
        assertThat(timeline).extracting(e -> e.get("source")).containsExactly(2, 1);
        assertThat(timeline.getFirst()).containsEntry("date", "2019-03-10");
    }

    @Test
    void aDocumentDateIsReadFromADateNamedAttribute() {
        assertThat(TurSNDeepDiveService.date(Map.of("type", "evento", "eventEnd", "2026-11-20T00:00:00Z")))
                .isEqualTo("2026-11-20");
        assertThat(TurSNDeepDiveService.date(new java.util.LinkedHashMap<>(Map.of(
                "modificationDate", "2025-01-01T10:00:00Z")))).isEqualTo("2025-01-01");
        assertThat(TurSNDeepDiveService.date(Map.of("code", "2026-11-20"))).isNull();
        assertThat(TurSNDeepDiveService.date(Map.of("publicationDate", "yesterday"))).isNull();
    }

    @Test
    void anIndexWithNothingOnTheQuestionIsAnErrorNotAnInventedOverview() {
        ready();
        when(chatModel.call(any(Prompt.class))).thenReturn(response("""
                STEP: A? | a
                STEP: B? | b
                STEP: C? | c
                """));
        retrieves("a");
        retrieves("b");
        retrieves("c");

        List<ServerSentEvent<String>> events = run();

        assertThat(events.getLast().event()).isEqualTo("error");
        assertThat(named(events, "section")).isEmpty();
        assertThat((List<?>) payload(named(events, "uncovered").getFirst()).get("items")).hasSize(3);
    }

    @Test
    void aSiteWithoutAiIsRefusedBeforeAnyModelCall() {
        site();
        when(routeResolver.resolve(any())).thenReturn(
                new TurSNChatRouteResolver.Decision(TurSNChatRouteResolver.Route.UNAVAILABLE, null));

        List<ServerSentEvent<String>> events = run();

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().event()).isEqualTo("error");
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void parsePlanIsLenientAndBounded() {
        TurSNDeepDiveService.Plan plan = TurSNDeepDiveService.parsePlan("""
                Here is the plan:
                1. SUMMARY: s
                - STEP: one? | words one
                - STEP: two?
                - STEP: one? | duplicate
                - STEP: three? | three
                """, 2);

        assertThat(plan.summary()).isEqualTo("s");
        assertThat(plan.steps()).extracting(TurSNDeepDiveService.Step::query)
                .containsExactly("words one", "two?");
    }

    @Test
    void collectNumbersDocumentsOnceAcrossSteps() {
        List<TurSNDeepDiveService.Source> sources = new ArrayList<>();
        var seen = new HashSet<String>();
        var step1 = new TurSNDeepDiveService.Step(1, "q1", "q1");
        var step2 = new TurSNDeepDiveService.Step(2, "q2", "q2");

        TurSNDeepDiveService.collect(new TurCatalogCopilotService.Retrieval(
                List.of(doc("a", null), doc("b", null)), List.of("A", "B"), 2, null, true), step1, sources, seen);
        List<Integer> second = TurSNDeepDiveService.collect(new TurCatalogCopilotService.Retrieval(
                List.of(doc("b", null), doc("c", null)), List.of("B", "C"), 2, null, true), step2, sources, seen);

        assertThat(second).containsExactly(2, 3);
        assertThat(sources).extracting(s -> s.citation().rank()).containsExactly(1, 2, 3);
        assertThat(TurSNDeepDiveService.groupByType(sources)).containsOnlyKeys("other");
    }
}
