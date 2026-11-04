/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.genai.deepdive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.genai.TurSNChatRouteResolver;
import com.viglet.turing.genai.catalog.TurCatalogCitation;
import com.viglet.turing.genai.catalog.TurCatalogCopilotService;
import com.viglet.turing.genai.catalog.TurCatalogKitService;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.service.llm.budget.TurChatCostBudgetGate;
import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.sn.TurSNSearchProcess;
import com.viglet.turing.sn.dsl.TurDslCallerFilters;
import com.viglet.turing.system.TurDefaultChatModelResolver;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * T1220 — the deep-dive research mode behind {@code POST /api/sn/{site}/research}:
 * a question whose answer is spread over many documents becomes a sectioned,
 * cited overview instead of one paragraph.
 *
 * <p>It is the catalog kit's loop (T1159) with a composing step, not a new
 * planner. One plain-lines LLM call (never structured output) names 3 to 6
 * sub-questions; each runs the shared {@link TurCatalogCopilotService#retrieve},
 * inside the caller's {@code filters} through {@link TurDslCallerFilters}; a
 * second call composes cited sections from the numbered sources alone. A section
 * the model wrote without a source the retrieval returned is dropped, a citation
 * to a number no source has is removed, and a step that found nothing is reported
 * as such: what the index does not cover is said, never filled.
 *
 * <p>Both calls are metered under {@link #STAGE_RESEARCH}, attributed to the site
 * and to its effective agent, so the spend appears in the site's cost summary and
 * counts against the agent's monthly budget.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Slf4j
@Service
public class TurSNDeepDiveService {

    /** The cost stage of both LLM calls. */
    static final String STAGE_RESEARCH = "sn.research";
    static final int MIN_STEPS = 3;
    static final int DEFAULT_STEPS = 5;
    static final int MAX_STEPS = 6;
    /** At most this many sources of one step go into the composing prompt. */
    static final int SOURCES_PER_STEP = 4;
    /** At most this many sources in all. */
    static final int MAX_SOURCES = 20;
    /** A source's excerpt is cut at this many characters. */
    static final int EXCERPT_CHARS = 1200;
    static final int MAX_QUESTION_CHARS = 1000;

    static final String EVENT_PLAN = "plan";
    static final String EVENT_STEP = "step";
    static final String EVENT_SECTION = "section";
    static final String EVENT_UNCOVERED = "uncovered";
    static final String EVENT_SOURCES = "sources";
    static final String EVENT_DONE = "done";
    static final String EVENT_ERROR = "error";

    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,3})]");

    private final TurSNSearchProcess turSNSearchProcess;
    private final TurSNChatRouteResolver chatRouteResolver;
    private final TurDefaultAgentResolver defaultAgentResolver;
    private final TurDefaultChatModelResolver chatModelResolver;
    private final TurCatalogCopilotService copilotService;
    private final TurLLMTokenUsageService tokenUsageService;
    private final TurChatCostBudgetGate budgetGate;

    public TurSNDeepDiveService(TurSNSearchProcess turSNSearchProcess, TurSNChatRouteResolver chatRouteResolver,
            TurDefaultAgentResolver defaultAgentResolver, TurDefaultChatModelResolver chatModelResolver,
            TurCatalogCopilotService copilotService, TurLLMTokenUsageService tokenUsageService,
            TurChatCostBudgetGate budgetGate) {
        this.turSNSearchProcess = turSNSearchProcess;
        this.chatRouteResolver = chatRouteResolver;
        this.defaultAgentResolver = defaultAgentResolver;
        this.chatModelResolver = chatModelResolver;
        this.copilotService = copilotService;
        this.tokenUsageService = tokenUsageService;
        this.budgetGate = budgetGate;
    }

    /** One sub-question as the plan named it. */
    record Step(int index, String question, String query) {
    }

    /** What the planning call returned, parsed. */
    record Plan(String summary, List<Step> steps) {
    }

    /** One numbered source of the overview: the citation and its excerpt. */
    record Source(int number, TurCatalogCitation citation, String excerpt, int step) {
    }

    /** A composed section: its heading, its text and the source numbers it cites. */
    record Section(String title, String text, List<Integer> citations) {
    }

    /** What the composing call returned once checked against the sources. */
    record Composition(List<Section> sections, List<String> uncovered) {
    }

    /**
     * Streams the overview as named SSE events: {@code plan}, one {@code step} per
     * sub-question (with {@code hits: 0} when it found nothing), the {@code section}s,
     * {@code uncovered}, {@code sources} grouped by document type, then {@code done}.
     * Every refusal and failure is a single terminal {@code error} event.
     */
    public Flux<ServerSentEvent<String>> research(String siteName, String question, Locale locale,
            Map<String, List<String>> filters, Integer maxSteps) {
        if (!StringUtils.hasText(question)) {
            return Flux.just(error("No question provided"));
        }
        return Flux.<ServerSentEvent<String>>create(sink -> {
            try {
                TurDslCallerFilters.with(filters, () -> {
                    run(siteName, question.strip(), locale, maxSteps, sink::next);
                    return null;
                });
            } catch (RuntimeException e) {
                log.warn("[SNResearch] research failed for site '{}': {}", siteName, e.getMessage());
                sink.next(error("The research service is temporarily unavailable."));
            }
            sink.complete();
        }, FluxSink.OverflowStrategy.BUFFER).subscribeOn(Schedulers.boundedElastic());
    }

    private void run(String siteName, String rawQuestion, Locale locale, Integer maxSteps,
            Consumer<ServerSentEvent<String>> emit) {
        Optional<TurSNSite> siteOpt = turSNSearchProcess.getSNSite(siteName);
        if (siteOpt.isEmpty()) {
            emit.accept(error("Site not found: " + siteName));
            return;
        }
        TurSNSite site = siteOpt.get();
        // The same readiness the answer endpoint reads, so /research and /answer agree.
        TurSNChatRouteResolver.Decision route = chatRouteResolver.resolve(site.getTurSNSiteGenAi());
        if (!route.available()) {
            emit.accept(error("AI is not enabled for site: " + siteName + " (" + route.reason() + ")"));
            return;
        }
        if (locale == null) {
            emit.accept(error("No indexed locale on site: " + siteName));
            return;
        }
        if (budgetGate.isAnonymousChatHardCapExceeded()) {
            emit.accept(error("The AI budget for this month is exhausted."));
            return;
        }
        Optional<ChatModel> chatModel = chatModelResolver.resolve();
        if (chatModel.isEmpty()) {
            emit.accept(error("No usable default LLM configured for research"));
            return;
        }
        TurAIAgent agent = defaultAgentResolver.resolveEffectiveAgent(site.getTurSNSiteGenAi());
        String agentId = agent == null ? null : agent.getId();
        String question = rawQuestion.length() > MAX_QUESTION_CHARS
                ? rawQuestion.substring(0, MAX_QUESTION_CHARS) : rawQuestion;
        String localeName = locale.toString();
        int stepLimit = Math.clamp(maxSteps == null ? DEFAULT_STEPS : maxSteps, MIN_STEPS, MAX_STEPS);

        Plan plan = parsePlan(call(chatModel.get(), siteName, agentId,
                planPrompt(siteName, localeName, question, stepLimit)), stepLimit);
        if (plan.steps().isEmpty()) {
            emit.accept(error("Could not plan the research for this question"));
            return;
        }
        emit.accept(event(EVENT_PLAN, Map.of("summary", nullToEmpty(plan.summary()),
                "steps", plan.steps().stream().map(s -> Map.of("index", s.index(), "question", s.question(),
                        "query", s.query())).toList())));

        List<Source> sources = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<String> emptySteps = new ArrayList<>();
        for (Step step : plan.steps()) {
            TurCatalogCopilotService.Retrieval retrieval = copilotService.retrieve(siteName, localeName,
                    step.query());
            if (!retrieval.siteFound()) {
                emit.accept(error("Site not found: " + siteName));
                return;
            }
            List<Integer> numbers = collect(retrieval, step, sources, seen);
            if (retrieval.citations().isEmpty()) {
                emptySteps.add(step.question());
            }
            emit.accept(event(EVENT_STEP, Map.of("index", step.index(), "question", step.question(),
                    "hits", retrieval.citations().size(), "sources", numbers)));
        }
        if (sources.isEmpty()) {
            emit.accept(event(EVENT_UNCOVERED, Map.of("items", emptySteps)));
            emit.accept(error("The index holds nothing on this question"));
            return;
        }

        Composition composition = parseComposition(call(chatModel.get(), siteName, agentId,
                composePrompt(siteName, localeName, question, plan, sources)), sources.size());
        for (Section section : composition.sections()) {
            emit.accept(event(EVENT_SECTION, Map.of("title", section.title(), "text", section.text(),
                    "citations", section.citations())));
        }
        List<String> uncovered = new ArrayList<>(emptySteps);
        composition.uncovered().stream().filter(item -> !uncovered.contains(item)).forEach(uncovered::add);
        emit.accept(event(EVENT_UNCOVERED, Map.of("items", uncovered)));
        emit.accept(event(EVENT_SOURCES, Map.of("groups", groupByType(sources))));
        emit.accept(event(EVENT_DONE, Map.of("sections", composition.sections().size(),
                "sources", sources.size())));
    }

    /**
     * Adds the step's first {@link #SOURCES_PER_STEP} new documents to
     * {@code sources}, numbered from 1 across the whole overview; a document an
     * earlier step already brought keeps its number. Returns the step's numbers.
     */
    static List<Integer> collect(TurCatalogCopilotService.Retrieval retrieval, Step step, List<Source> sources,
            Set<String> seen) {
        List<Integer> numbers = new ArrayList<>();
        List<TurCatalogCitation> citations = retrieval.citations();
        List<String> blocks = retrieval.contextBlocks();
        int added = 0;
        for (int i = 0; i < citations.size() && added < SOURCES_PER_STEP; i++) {
            TurCatalogCitation citation = citations.get(i);
            String key = citation.id() != null ? citation.id() : citation.url();
            if (key == null) {
                continue;
            }
            if (!seen.add(key)) {
                sources.stream().filter(s -> key.equals(s.citation().id() != null ? s.citation().id()
                        : s.citation().url())).findFirst().ifPresent(s -> numbers.add(s.number()));
                continue;
            }
            if (sources.size() >= MAX_SOURCES) {
                break;
            }
            String excerpt = i < blocks.size() && blocks.get(i) != null ? blocks.get(i) : nullToEmpty(citation.title());
            if (excerpt.length() > EXCERPT_CHARS) {
                excerpt = excerpt.substring(0, EXCERPT_CHARS);
            }
            int number = sources.size() + 1;
            sources.add(new Source(number, citation.withRank(number), excerpt, step.index()));
            numbers.add(number);
            added++;
        }
        return numbers;
    }

    /** The sources by document type, in first-seen order; a document without one is {@code other}. */
    static Map<String, List<TurCatalogCitation>> groupByType(List<Source> sources) {
        Map<String, List<TurCatalogCitation>> groups = new LinkedHashMap<>();
        for (Source source : sources) {
            String type = StringUtils.hasText(source.citation().type()) ? source.citation().type() : "other";
            groups.computeIfAbsent(type, k -> new ArrayList<>()).add(source.citation());
        }
        return groups;
    }

    private String call(ChatModel chatModel, String siteName, String agentId, String prompt) {
        var response = chatModel.call(new Prompt(prompt));
        chatModelResolver.defaultInstance().ifPresent(instance -> tokenUsageService.recordUsage(instance,
                response, "system", agentId, STAGE_RESEARCH, null, siteName));
        var result = response.getResult();
        return result != null && result.getOutput() != null ? result.getOutput().getText() : null;
    }

    static String planPrompt(String siteName, String locale, String question, int stepLimit) {
        return """
                A visitor of the "%s" site wants to understand a topic in depth. Split the \
                question into between %d and %d sub-questions that together cover it, each \
                answerable from the site's own documents, most important first.
                Reply in plain lines only, no other text:
                SUMMARY: one sentence restating what the visitor wants to understand
                STEP: the sub-question | plain search words for the site's search
                The search words are what a person would type, never a field name.
                Write the summary, the sub-questions and the search words in %s.

                QUESTION: %s""".formatted(siteName, Math.min(MIN_STEPS, stepLimit), stepLimit,
                TurCatalogKitService.language(locale), question);
    }

    /** Lenient: unknown lines are ignored, a step without search words searches its question. */
    static Plan parsePlan(String raw, int stepLimit) {
        if (raw == null) {
            return new Plan(null, List.of());
        }
        String summary = null;
        List<Step> steps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String line : raw.split("\\r?\\n")) {
            String text = stripBullet(line);
            String upper = text.toUpperCase(Locale.ROOT);
            if (upper.startsWith("SUMMARY:") && summary == null) {
                summary = text.substring("SUMMARY:".length()).strip();
            } else if (upper.startsWith("STEP:") && steps.size() < stepLimit) {
                String[] parts = text.substring("STEP:".length()).split("\\|", 2);
                String question = parts[0].strip();
                if (question.isEmpty() || !seen.add(question.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                String query = parts.length > 1 && !parts[1].isBlank() ? parts[1].strip() : question;
                steps.add(new Step(steps.size() + 1, question, query));
            }
        }
        return new Plan(summary == null || summary.isEmpty() ? null : summary, List.copyOf(steps));
    }

    static String composePrompt(String siteName, String locale, String question, Plan plan, List<Source> sources) {
        StringBuilder numbered = new StringBuilder();
        for (Source source : sources) {
            TurCatalogCitation citation = source.citation();
            numbered.append('[').append(source.number()).append("] ")
                    .append(nullToEmpty(citation.title()));
            if (StringUtils.hasText(citation.url())) {
                numbered.append(" (").append(citation.url()).append(')');
            }
            numbered.append('\n').append(source.excerpt().strip()).append("\n\n");
        }
        StringBuilder steps = new StringBuilder();
        plan.steps().forEach(step -> steps.append("- ").append(step.question()).append('\n'));
        return """
                Write an in-depth overview for a visitor of the "%s" site, from the \
                numbered sources below and nothing else.
                Reply in plain lines only:
                SECTION: a short heading
                then one or more paragraphs for that section, each claim followed by the \
                number of the source it comes from, like [2] or [1][3].
                Repeat for each section, one per sub-question the sources answer.
                For each sub-question the sources do not answer, write one line:
                UNCOVERED: what the sources do not say
                Never state anything the sources do not say, never cite a number that is \
                not listed, and never fill a gap from general knowledge.
                Write in %s.

                QUESTION: %s
                SUB-QUESTIONS:
                %s
                SOURCES:
                %s""".formatted(siteName, TurCatalogKitService.language(locale), question, steps, numbered);
    }

    /**
     * The sections the model wrote, checked against the {@code sourceCount}
     * numbered sources: a citation to a number outside {@code 1..sourceCount} is
     * removed from the text, and a section left citing nothing is dropped and its
     * heading reported as uncovered, so every section returned cites a document.
     */
    static Composition parseComposition(String raw, int sourceCount) {
        List<Section> sections = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();
        if (raw == null) {
            return new Composition(sections, uncovered);
        }
        String title = null;
        StringBuilder body = new StringBuilder();
        for (String line : raw.split("\\r?\\n")) {
            String text = stripBullet(line);
            String upper = text.toUpperCase(Locale.ROOT);
            if (upper.startsWith("SECTION:")) {
                addSection(title, body, sourceCount, sections, uncovered);
                title = text.substring("SECTION:".length()).strip();
                body.setLength(0);
            } else if (upper.startsWith("UNCOVERED:")) {
                String item = text.substring("UNCOVERED:".length()).strip();
                if (!item.isEmpty() && !uncovered.contains(item)) {
                    uncovered.add(item);
                }
            } else if (title != null) {
                body.append(line.strip()).append('\n');
            }
        }
        addSection(title, body, sourceCount, sections, uncovered);
        return new Composition(List.copyOf(sections), List.copyOf(uncovered));
    }

    private static void addSection(String title, StringBuilder body, int sourceCount, List<Section> sections,
            List<String> uncovered) {
        if (title == null) {
            return;
        }
        Set<Integer> cited = new LinkedHashSet<>();
        Matcher matcher = CITATION.matcher(body);
        StringBuilder kept = new StringBuilder();
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            boolean valid = number >= 1 && number <= sourceCount;
            if (valid) {
                cited.add(number);
            }
            matcher.appendReplacement(kept, valid ? Matcher.quoteReplacement(matcher.group()) : "");
        }
        matcher.appendTail(kept);
        String text = kept.toString().replaceAll("[ \\t]+([.,;:])", "$1").replaceAll("\\n{3,}", "\n\n").strip();
        if (cited.isEmpty() || text.isEmpty()) {
            if (!title.isEmpty() && !uncovered.contains(title)) {
                uncovered.add(title);
            }
            return;
        }
        sections.add(new Section(title.isEmpty() ? "—" : title, text, List.copyOf(cited)));
    }

    private static String stripBullet(String line) {
        return line.strip().replaceFirst("^\\s*(?:[-*•]|\\d+[.)])\\s*", "").replaceAll("^\\*\\*|\\*\\*$", "");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static ServerSentEvent<String> error(String message) {
        return event(EVENT_ERROR, Map.of("message", nullToEmpty(message)));
    }

    static ServerSentEvent<String> event(String name, Object payload) {
        String data;
        try {
            data = JSON.writeValueAsString(payload);
        } catch (RuntimeException e) {
            data = "{}";
        }
        return ServerSentEvent.<String>builder(data).event(name).build();
    }
}
