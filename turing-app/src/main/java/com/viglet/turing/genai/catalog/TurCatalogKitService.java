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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.viglet.turing.service.llm.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.sn.dsl.TurDslQuery;
import com.viglet.turing.sn.nl.TurSNNLFilterService;
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

    /** T1169 — at most this many values per facet field go into the gap prompt. */
    static final int VOCABULARY_VALUES = 15;
    /** T1169 — at most this many facet fields go into the gap prompt. */
    static final int VOCABULARY_FIELDS = 8;

    private final TurCatalogCopilotService copilotService;
    private final TurDefaultChatModelResolver chatModelResolver;
    private final TurLLMTokenUsageService tokenUsageService;
    private final TurSNNLFilterService nlFilterService;

    public TurCatalogKitService(TurCatalogCopilotService copilotService,
            TurDefaultChatModelResolver chatModelResolver, TurLLMTokenUsageService tokenUsageService,
            TurSNNLFilterService nlFilterService) {
        this.copilotService = copilotService;
        this.chatModelResolver = chatModelResolver;
        this.tokenUsageService = tokenUsageService;
        this.nlFilterService = nlFilterService;
    }

    /** One need the plan implies, with the products retrieval found for it. */
    public record Stage(String gap, String reason, String query, List<KitProduct> products) {
    }

    /**
     * A staged product: the citation retrieval returned, a one-line {@code reason}
     * made only of this product's own attributes (never the model's words), and
     * {@code available} when the request named an availability field: {@code false}
     * for a value that reads as unavailable, {@code null} when the product has no
     * such field.
     */
    public record KitProduct(int rank, String id, String title, String url, Double score, String reason,
            Boolean available) {
    }

    /** What the request asks for an unavailable product: kept and marked, or dropped. */
    public enum Availability {
        MARK, DROP
    }

    /**
     * The kit's optional knobs; every field may be {@code null}. T1167 —
     * {@code guideTypes} names the document types that count as a guide on this
     * site (a site's types differ, so none is guessed); without it no guide is cited.
     */
    public record Options(Integer maxGaps, Integer perGap, String availabilityField, Availability availability,
            List<String> guideTypes) {
        static final Options DEFAULTS = new Options(null, null, null, null, null);

        /** The knobs before T1167: no guide types. */
        public Options(Integer maxGaps, Integer perGap, String availabilityField, Availability availability) {
            this(maxGaps, perGap, availabilityField, availability, null);
        }
    }

    /** T1167 — at most this many guides are cited beside the stages. */
    static final int MAX_GUIDES = 2;

    /** Values of an availability field that read as "not available", lower-cased. */
    static final Set<String> UNAVAILABLE = Set.of("false", "0", "no", "n", "unavailable", "out of stock",
            "out_of_stock", "out-of-stock", "outofstock", "sold out", "sold_out", "discontinued");
    /** At most this many attributes go into a product's reason. */
    static final int REASON_ATTRIBUTES = 2;

    /**
     * The staged kit, or an error. {@code stages} holds only gaps retrieval found
     * products for; {@code unfilled} names the gaps it found none for, so a client
     * can say what the catalog lacks. T1167 — {@code guides} cites the site's own
     * advice for the plan (documents of a requested guide type), empty unless the
     * request named guide types.
     */
    public record Result(String summary, List<Stage> stages, List<String> unfilled, String error,
            List<TurCatalogCitation> guides) {

        public Result {
            guides = guides == null ? List.of() : guides;
        }

        /** The result before T1167: no guides. */
        public Result(String summary, List<Stage> stages, List<String> unfilled, String error) {
            this(summary, stages, unfilled, error, List.of());
        }

        static Result error(String message) {
            return new Result(null, List.of(), List.of(), message);
        }
    }

    /**
     * A gap as the model named it. T1171 — {@code filterField} / {@code filterValue}
     * name one of the site's facet values the need maps to, in the site's own
     * spelling, or are both {@code null}.
     */
    record Gap(String name, String query, String reason, String filterField, String filterValue) {

        Gap(String name, String query, String reason) {
            this(name, query, reason, null, null);
        }
    }

    /** What the gap-naming call returned, parsed. */
    record Plan(String summary, List<Gap> gaps) {
    }

    public Result plan(String siteName, String locale, String planText, Integer maxGaps, Integer perGap) {
        return plan(siteName, locale, planText, new Options(maxGaps, perGap, null, null));
    }

    public Result plan(String siteName, String locale, String planText, Options options) {
        Options opts = options == null ? Options.DEFAULTS : options;
        Integer maxGaps = opts.maxGaps();
        Integer perGap = opts.perGap();
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
            Map<String, List<String>> vocabulary = vocabulary(siteName, locale);
            named = parse(callModel(chatModel.get(), siteName,
                    gapPrompt(siteName, plan, gapLimit, vocabulary)), gapLimit, vocabulary);
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
            // T1171 — a gap that names a facet value searches its words inside it.
            TurCatalogCopilotService.Retrieval retrieval = gap.filterField() == null
                    ? copilotService.retrieve(siteName, locale, gap.query())
                    : copilotService.retrieve(siteName, locale, gap.query(),
                            new TurDslQuery.Term(gap.filterField(), gap.filterValue()));
            if (!retrieval.siteFound()) {
                return Result.error("Catalog site not found: " + siteName);
            }
            List<KitProduct> products = stageProducts(retrieval, gap, opts, productLimit, staged);
            if (products.isEmpty()) {
                unfilled.add(gap.name());
            } else {
                stages.add(new Stage(gap.name(), gap.reason(), gap.query(), products));
            }
        }
        return new Result(named.summary(), List.copyOf(stages), List.copyOf(unfilled), null,
                guides(siteName, locale, plan, opts.guideTypes(), staged));
    }

    /**
     * T1167 — up to {@link #MAX_GUIDES} documents the plan text itself retrieves
     * whose {@code type} is one of {@code guideTypes} (case-insensitive), skipping
     * any already staged as a product. No guide types, no retrieval.
     */
    private List<TurCatalogCitation> guides(String siteName, String locale, String plan, List<String> guideTypes,
            Set<String> staged) {
        if (guideTypes == null || guideTypes.isEmpty()) {
            return List.of();
        }
        Set<String> types = new HashSet<>();
        guideTypes.stream().filter(StringUtils::hasText)
                .forEach(t -> types.add(t.strip().toLowerCase(java.util.Locale.ROOT)));
        TurCatalogCopilotService.Retrieval retrieval = copilotService.retrieve(siteName, locale, plan);
        List<TurCatalogCitation> guides = new ArrayList<>();
        List<TurCatalogCitation> citations = retrieval.citations();
        for (int i = 0; i < citations.size() && guides.size() < MAX_GUIDES; i++) {
            TurCatalogCitation citation = citations.get(i);
            String type = retrieval.attributesAt(i).get("type");
            if (citation.id() != null && type != null && types.contains(type.strip().toLowerCase(java.util.Locale.ROOT))
                    && !staged.contains(citation.id())) {
                guides.add(citation);
            }
        }
        return List.copyOf(guides);
    }

    /**
     * Up to {@code limit} of the gap's retrieved products, skipping any already
     * staged under an earlier gap, and an unavailable one when the request drops
     * them. {@code staged} records each product taken.
     */
    private static List<KitProduct> stageProducts(TurCatalogCopilotService.Retrieval retrieval, Gap gap,
            Options opts, int limit, Set<String> staged) {
        List<KitProduct> products = new ArrayList<>();
        List<TurCatalogCitation> citations = retrieval.citations();
        for (int i = 0; i < citations.size() && products.size() < limit; i++) {
            TurCatalogCitation citation = citations.get(i);
            Map<String, String> attributes = retrieval.attributesAt(i);
            Boolean available = availability(attributes, opts.availabilityField());
            boolean dropped = Boolean.FALSE.equals(available) && opts.availability() == Availability.DROP;
            if (citation.id() != null && !dropped && staged.add(citation.id())) {
                products.add(new KitProduct(citation.rank(), citation.id(), citation.title(), citation.url(),
                        citation.score(), reason(attributes, gap, opts.availabilityField()), available));
            }
        }
        return List.copyOf(products);
    }

    /**
     * The product's reason, from its own attributes only: the ones whose name or
     * value shares a word with the gap (its name, query or why) first, then the
     * others, at most {@link #REASON_ATTRIBUTES}, as {@code key: value; …}.
     * {@code null} when the product lists no attribute.
     */
    static String reason(Map<String, String> attributes, Gap gap, String availabilityField) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        Set<String> gapWords = words(String.join(" ", gap.name(), gap.query(),
                gap.reason() == null ? "" : gap.reason()));
        List<Map.Entry<String, String>> matching = new ArrayList<>();
        List<Map.Entry<String, String>> others = new ArrayList<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equals(availabilityField)) {
                continue;
            }
            Set<String> entryWords = words(entry.getKey() + " " + entry.getValue());
            entryWords.retainAll(gapWords);
            (entryWords.isEmpty() ? others : matching).add(entry);
        }
        matching.addAll(others);
        List<String> parts = matching.stream().limit(REASON_ATTRIBUTES)
                .map(e -> e.getKey() + ": " + e.getValue())
                .toList();
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static Set<String> words(String text) {
        Set<String> words = new HashSet<>();
        for (String word : text.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.length() > 2) {
                words.add(word);
            }
        }
        return words;
    }

    /** {@code null} when no field was named or the product lacks it; else whether it reads available. */
    static Boolean availability(Map<String, String> attributes, String field) {
        if (!StringUtils.hasText(field) || attributes == null || !attributes.containsKey(field)) {
            return null;
        }
        String value = attributes.get(field).strip().toLowerCase(java.util.Locale.ROOT);
        return !UNAVAILABLE.contains(value);
    }

    private String callModel(ChatModel chatModel, String siteName, String prompt) {
        var response = chatModel.call(new Prompt(prompt));
        chatModelResolver.defaultInstance().ifPresent(instance -> tokenUsageService.recordUsage(instance,
                response, "system", null, STAGE_KIT, null, siteName));
        var result = response.getResult();
        return result != null && result.getOutput() != null ? result.getOutput().getText() : null;
    }

    /**
     * T1169 — the site's facet values, so the model words its gap queries the way
     * the catalog does. A read that fails sends the prompt without them.
     */
    private Map<String, List<String>> vocabulary(String siteName, String locale) {
        try {
            return nlFilterService.facetVocabulary(siteName, locale, VOCABULARY_VALUES);
        } catch (RuntimeException e) {
            log.warn("[CatalogKit] could not read the facet values of site '{}': {}", siteName, e.getMessage());
            return Map.of();
        }
    }

    /**
     * T1171 — the facet values are listed as {@code field=value} choices for an
     * optional fourth part, not as a {@code field: values} block: the model copied
     * that layout into its queries ({@code product_cat: Fleece and midlayers}), which
     * no field matches as text.
     */
    static String gapPrompt(String siteName, String plan, int gapLimit, Map<String, List<String>> vocabulary) {
        StringBuilder words = new StringBuilder();
        if (vocabulary != null) {
            vocabulary.entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                    .limit(VOCABULARY_FIELDS)
                    .forEach(e -> e.getValue().forEach(v -> words.append("- ").append(e.getKey())
                            .append('=').append(v).append('\n')));
        }
        String catalogWords = words.isEmpty() ? "" : """

                The catalog's own values. Word each search query with them where they \
                fit, and when one names what the need is, add it as a fourth part, \
                copied exactly as field=value:
                %s""".formatted(words);
        return """
                A shopper on the "%s" catalog describes a plan. Name the needs the plan \
                implies that products from such a catalog could fill, most important \
                first, at most %d. Do not name products, brands or prices.
                %s
                Reply in plain lines only, no other text:
                SUMMARY: one sentence restating the plan
                GAP: short need name | search query for the catalog | why the plan needs it%s
                The search query is plain words a shopper would type, never a field name.

                PLAN: %s""".formatted(siteName, gapLimit, catalogWords,
                words.isEmpty() ? "" : " | field=value (optional)", plan);
    }

    /**
     * {@code name | query | why | field=value}; a missing query is the name, a
     * nameless gap is {@code null}. T1171 — the filter is kept only when the field is
     * in {@code vocabulary} and the value one of its values (case-insensitive), in
     * the vocabulary's spelling; anything else is dropped and the gap searches its
     * words alone.
     */
    private static Gap parseGap(String text, Map<String, List<String>> vocabulary) {
        String[] parts = text.split("\\|", 4);
        String name = parts[0].strip();
        if (name.isEmpty()) {
            return null;
        }
        String query = parts.length > 1 && !parts[1].isBlank() ? parts[1].strip() : name;
        String reason = parts.length > 2 && !parts[2].isBlank() ? parts[2].strip() : null;
        String[] filter = parts.length > 3 ? filter(parts[3], vocabulary) : null;
        return filter == null ? new Gap(name, query, reason)
                : new Gap(name, query, reason, filter[0], filter[1]);
    }

    /** {@code field=value} resolved against the vocabulary, as {@code [field, value]}, or {@code null}. */
    private static String[] filter(String text, Map<String, List<String>> vocabulary) {
        int eq = text.indexOf('=');
        if (eq < 0 || vocabulary == null) {
            return null;
        }
        String field = text.substring(0, eq).strip();
        String value = text.substring(eq + 1).strip();
        for (Map.Entry<String, List<String>> entry : vocabulary.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(field) && entry.getValue() != null) {
                for (String known : entry.getValue()) {
                    if (known.equalsIgnoreCase(value)) {
                        return new String[] { entry.getKey(), known };
                    }
                }
            }
        }
        return null;
    }

    /** {@link #parse(String, int, Map)} with no vocabulary: no gap keeps a filter. */
    static Plan parse(String raw, int gapLimit) {
        return parse(raw, gapLimit, Map.of());
    }

    /** Lenient: unknown lines are ignored, a gap without a query searches its name. */
    static Plan parse(String raw, int gapLimit, Map<String, List<String>> vocabulary) {
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
                Gap gap = parseGap(text.substring("GAP:".length()), vocabulary);
                if (gap != null && seen.add(gap.name().toLowerCase(java.util.Locale.ROOT))) {
                    gaps.add(gap);
                }
            }
        }
        return new Plan(summary == null || summary.isEmpty() ? null : summary, List.copyOf(gaps));
    }
}
