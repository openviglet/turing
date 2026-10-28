/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.genai.catalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.system.TurDefaultChatModelResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * T1159 — turns a described plan ("two days crossing the mountains in July,
 * first time") into a staged kit: the needs the plan implies, each with the
 * catalog products that fill it.
 *
 * <p>One LLM call names the gaps, as plain lines and not as structured output:
 * {@code SUMMARY: …} then {@code GAP: name | search query | why}. Each gap then
 * runs the shared {@link TurCatalogCopilotService#retrieve}, so the site's
 * planning strategy and hybrid ranking apply to it as to any copilot question.
 * The products come from that retrieval and never from the model, so every id in
 * a stage is one the index returned, and the model names no product and no price.
 * A product already staged under an earlier gap is not repeated.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Slf4j
@Service
public class TurCatalogKitService {

    static final int DEFAULT_MAX_GAPS = 5;
    static final int MAX_GAPS = 8;
    static final int DEFAULT_PER_GAP = 3;
    static final int MAX_PER_GAP = 6;
    static final int MAX_PLAN_CHARS = 2000;
    /** T1127 — the cost stage of the gap-naming call. */
    static final String STAGE_KIT = "sn.copilot.kit";

    private final TurCatalogCopilotService copilotService;
    private final TurDefaultChatModelResolver chatModelResolver;
    private final TurLLMTokenUsageService tokenUsageService;

    public TurCatalogKitService(TurCatalogCopilotService copilotService,
            TurDefaultChatModelResolver chatModelResolver, TurLLMTokenUsageService tokenUsageService) {
        this.copilotService = copilotService;
        this.chatModelResolver = chatModelResolver;
        this.tokenUsageService = tokenUsageService;
    }

    /** One need the plan implies, with the products retrieval found for it. */
    public record Stage(String gap, String reason, String query, List<TurCatalogCitation> products) {
    }

    /**
     * The staged kit, or an error. {@code stages} holds only gaps retrieval found
     * products for; {@code unfilled} names the gaps it found none for, so a client
     * can say what the catalog lacks.
     */
    public record Result(String summary, List<Stage> stages, List<String> unfilled, String error) {
        static Result error(String message) {
            return new Result(null, List.of(), List.of(), message);
        }
    }

    /** A gap as the model named it. */
    record Gap(String name, String query, String reason) {
    }

    /** What the gap-naming call returned, parsed. */
    record Plan(String summary, List<Gap> gaps) {
    }

    public Result plan(String siteName, String locale, String planText, Integer maxGaps, Integer perGap) {
        if (!StringUtils.hasText(planText)) {
            return Result.error("No plan provided");
        }
        Optional<ChatModel> chatModel = chatModelResolver.resolve();
        if (chatModel.isEmpty()) {
            return Result.error("No usable default LLM configured for the catalog copilot");
        }
        int gapLimit = Math.clamp(maxGaps == null ? DEFAULT_MAX_GAPS : maxGaps, 1, MAX_GAPS);
        int productLimit = Math.clamp(perGap == null ? DEFAULT_PER_GAP : perGap, 1, MAX_PER_GAP);
        String plan = planText.strip();
        if (plan.length() > MAX_PLAN_CHARS) {
            plan = plan.substring(0, MAX_PLAN_CHARS);
        }

        Plan named;
        try {
            named = parse(callModel(chatModel.get(), siteName, gapPrompt(siteName, plan, gapLimit)), gapLimit);
        } catch (RuntimeException e) {
            log.warn("[CatalogKit] gap naming failed for site '{}': {}", siteName, e.getMessage());
            return Result.error("Could not name the needs of the plan: " + e.getMessage());
        }
        if (named.gaps().isEmpty()) {
            return new Result(named.summary(), List.of(), List.of(), "The plan implied no need to search for");
        }

        List<Stage> stages = new ArrayList<>();
        List<String> unfilled = new ArrayList<>();
        Set<String> staged = new HashSet<>();
        for (Gap gap : named.gaps()) {
            TurCatalogCopilotService.Retrieval retrieval = copilotService.retrieve(siteName, locale, gap.query());
            if (!retrieval.siteFound()) {
                return Result.error("Catalog site not found: " + siteName);
            }
            List<TurCatalogCitation> products = new ArrayList<>();
            for (TurCatalogCitation citation : retrieval.citations()) {
                if (products.size() == productLimit) {
                    break;
                }
                if (citation.id() != null && staged.add(citation.id())) {
                    products.add(citation);
                }
            }
            if (products.isEmpty()) {
                unfilled.add(gap.name());
            } else {
                stages.add(new Stage(gap.name(), gap.reason(), gap.query(), List.copyOf(products)));
            }
        }
        return new Result(named.summary(), List.copyOf(stages), List.copyOf(unfilled), null);
    }

    private String callModel(ChatModel chatModel, String siteName, String prompt) {
        var response = chatModel.call(new Prompt(prompt));
        chatModelResolver.defaultInstance().ifPresent(instance -> tokenUsageService.recordUsage(instance,
                response, "system", null, STAGE_KIT, null, siteName));
        var result = response.getResult();
        return result != null && result.getOutput() != null ? result.getOutput().getText() : null;
    }

    static String gapPrompt(String siteName, String plan, int gapLimit) {
        return """
                A shopper on the "%s" catalog describes a plan. Name the needs the plan \
                implies that products from such a catalog could fill, most important \
                first, at most %d. Do not name products, brands or prices.

                Reply in plain lines only, no other text:
                SUMMARY: one sentence restating the plan
                GAP: short need name | search query for the catalog | why the plan needs it

                PLAN: %s""".formatted(siteName, gapLimit, plan);
    }

    /** Lenient: unknown lines are ignored, a gap without a query searches its name. */
    static Plan parse(String raw, int gapLimit) {
        if (raw == null) {
            return new Plan(null, List.of());
        }
        String summary = null;
        List<Gap> gaps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String line : raw.split("\\r?\\n")) {
            String text = line.strip().replaceFirst("^\\s*(?:[-*•]|\\d+[.)])\\s*", "");
            String upper = text.toUpperCase(java.util.Locale.ROOT);
            if (upper.startsWith("SUMMARY:") && summary == null) {
                summary = text.substring("SUMMARY:".length()).strip();
            } else if (upper.startsWith("GAP:") && gaps.size() < gapLimit) {
                String[] parts = text.substring("GAP:".length()).split("\\|", 3);
                String name = parts[0].strip();
                String query = parts.length > 1 && !parts[1].isBlank() ? parts[1].strip() : name;
                String reason = parts.length > 2 ? parts[2].strip() : null;
                if (!name.isEmpty() && seen.add(name.toLowerCase(java.util.Locale.ROOT))) {
                    gaps.add(new Gap(name, query, reason == null || reason.isEmpty() ? null : reason));
                }
            }
        }
        return new Plan(summary == null || summary.isEmpty() ? null : summary, List.copyOf(gaps));
    }
}
