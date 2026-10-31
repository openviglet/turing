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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.viglet.turing.commons.sn.field.TurSNFieldLabel;
import com.viglet.turing.genai.rag.TurRagReranker;
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
    private final TurRagReranker reranker;

    public TurCatalogKitService(TurCatalogCopilotService copilotService,
            TurDefaultChatModelResolver chatModelResolver, TurLLMTokenUsageService tokenUsageService,
            TurSNNLFilterService nlFilterService, TurRagReranker reranker) {
        this.copilotService = copilotService;
        this.chatModelResolver = chatModelResolver;
        this.tokenUsageService = tokenUsageService;
        this.nlFilterService = nlFilterService;
        this.reranker = reranker;
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
     * T1175 — {@code productTypes} names the document types that may be staged as a
     * product; without it any document that is not a guide is.
     */
    public record Options(Integer maxGaps, Integer perGap, String availabilityField, Availability availability,
            List<String> guideTypes, List<String> productTypes) {
        static final Options DEFAULTS = new Options(null, null, null, null, null, null);

        /** The knobs before T1167: no guide types. */
        public Options(Integer maxGaps, Integer perGap, String availabilityField, Availability availability) {
            this(maxGaps, perGap, availabilityField, availability, null, null);
        }

        /** The knobs before T1175: no product types. */
        public Options(Integer maxGaps, Integer perGap, String availabilityField, Availability availability,
                List<String> guideTypes) {
            this(maxGaps, perGap, availabilityField, availability, guideTypes, null);
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

        Map<String, List<String>> vocabulary = vocabulary(siteName, locale);
        Map<String, String> reasonLabels = reasonLabels(siteName, locale, vocabulary);
        Plan named;
        try {
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
        Set<String> guideTypes = types(opts.guideTypes());
        for (Gap gap : named.gaps()) {
            // T1171 — a gap that names a facet value searches its words inside it.
            TurCatalogCopilotService.Retrieval retrieval = gap.filterField() == null
                    ? copilotService.retrieve(siteName, locale, gap.query())
                    : copilotService.retrieve(siteName, locale, gap.query(),
                            new TurDslQuery.Term(gap.filterField(), gap.filterValue()));
            if (!retrieval.siteFound()) {
                return Result.error("Catalog site not found: " + siteName);
            }
            retrieval = rankedForPlan(chatModel.get(), plan, gap, retrieval);
            List<KitProduct> products = stageProducts(retrieval, gap, opts, productLimit, staged, guideTypes,
                    reasonLabels);
            if (products.isEmpty()) {
                unfilled.add(gap.name());
            } else {
                stages.add(new Stage(gap.name(), gap.reason(), gap.query(), products));
            }
        }
        return new Result(named.summary(), List.copyOf(stages), List.copyOf(unfilled), null,
                guides(siteName, locale, plan, guideTypes, staged));
    }

    /**
     * T1190 — the gap's candidates re-ordered against the whole plan and the gap
     * through the shared {@link TurRagReranker}, so a product the plan contradicts
     * (its season, its people, its level) ranks after one it does not. Retrieval
     * order ranks the gap query alone, which carries only part of the plan. The
     * citations, context blocks and attributes move together; any failure keeps
     * retrieval order.
     */
    TurCatalogCopilotService.Retrieval rankedForPlan(ChatModel chatModel, String plan, Gap gap,
            TurCatalogCopilotService.Retrieval retrieval) {
        List<TurCatalogCitation> citations = retrieval.citations();
        if (reranker == null || citations.size() < 2) {
            return retrieval;
        }
        List<Document> candidates = new ArrayList<>(citations.size());
        Map<String, Integer> indexById = new HashMap<>();
        for (int i = 0; i < citations.size(); i++) {
            String key = String.valueOf(i);
            indexById.put(key, i);
            candidates.add(new Document(key, candidateText(retrieval, i), Map.of()));
        }
        String query = "PLAN: " + plan + "\nNEED: " + gap.name() + " (" + gap.query() + ")"
                + (gap.reason() == null ? "" : ", " + gap.reason());
        List<Document> ordered;
        try {
            ordered = reranker.reorder(chatModel, query, candidates);
        } catch (RuntimeException e) {
            log.warn("[CatalogKit] re-ranking gap '{}' failed: {}", gap.name(), e.getMessage());
            return retrieval;
        }
        if (ordered == null || ordered.size() != candidates.size()) {
            return retrieval;
        }
        List<TurCatalogCitation> rankedCitations = new ArrayList<>(citations.size());
        List<String> rankedBlocks = new ArrayList<>(citations.size());
        List<Map<String, String>> rankedAttributes = new ArrayList<>(citations.size());
        List<String> blocks = retrieval.contextBlocks();
        for (Document document : ordered) {
            Integer i = indexById.remove(document.getId());
            if (i == null) {
                return retrieval;
            }
            rankedCitations.add(citations.get(i));
            if (i < blocks.size()) {
                rankedBlocks.add(blocks.get(i));
            }
            rankedAttributes.add(retrieval.attributesAt(i));
        }
        return new TurCatalogCopilotService.Retrieval(rankedCitations,
                rankedBlocks.size() == blocks.size() ? rankedBlocks : blocks, retrieval.totalHits(),
                retrieval.groundedSummary(), retrieval.siteFound(), rankedAttributes);
    }

    /** What the reranker reads of a candidate: its context block, else its title and attributes. */
    private static String candidateText(TurCatalogCopilotService.Retrieval retrieval, int i) {
        List<String> blocks = retrieval.contextBlocks();
        if (i < blocks.size() && StringUtils.hasText(blocks.get(i))) {
            return blocks.get(i);
        }
        TurCatalogCitation citation = retrieval.citations().get(i);
        StringBuilder text = new StringBuilder(citation.title() == null ? "" : citation.title());
        retrieval.attributesAt(i).forEach((k, v) -> text.append("\n").append(k).append(": ").append(v));
        return text.isEmpty() ? String.valueOf(citation.id()) : text.toString();
    }

    /**
     * T1167 — up to {@link #MAX_GUIDES} documents the plan text itself retrieves
     * whose {@code type} is one of {@code guideTypes} (case-insensitive), skipping
     * any already staged as a product. No guide types, no retrieval.
     */
    private List<TurCatalogCitation> guides(String siteName, String locale, String plan, Set<String> types,
            Set<String> staged) {
        if (types.isEmpty()) {
            return List.of();
        }
        TurCatalogCopilotService.Retrieval retrieval = copilotService.retrieve(siteName, locale, plan);
        List<TurCatalogCitation> guides = new ArrayList<>();
        List<TurCatalogCitation> citations = retrieval.citations();
        for (int i = 0; i < citations.size() && guides.size() < MAX_GUIDES; i++) {
            TurCatalogCitation citation = citations.get(i);
            if (citation.id() != null && hasType(retrieval.attributesAt(i), types)
                    && !staged.contains(citation.id())) {
                guides.add(citation);
            }
        }
        return List.copyOf(guides);
    }

    /** The requested guide or product types, stripped and lower-cased; empty when none. */
    private static Set<String> types(List<String> requested) {
        Set<String> types = new HashSet<>();
        if (requested != null) {
            requested.stream().filter(StringUtils::hasText)
                    .forEach(t -> types.add(t.strip().toLowerCase(java.util.Locale.ROOT)));
        }
        return types;
    }

    /** Whether the document's {@code type} is one of {@code types}. */
    private static boolean hasType(Map<String, String> attributes, Set<String> types) {
        String type = attributes == null ? null : attributes.get("type");
        return type != null && types.contains(type.strip().toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Up to {@code limit} of the gap's retrieved products, skipping any already
     * staged under an earlier gap, an unavailable one when the request drops
     * them, and (T1172) a document of a requested guide type, which is cited among
     * the guides and is never a product. T1175 — when the request names product
     * types, a document of any other type (a page, an FAQ) is skipped and the stage
     * fills from the next candidate. {@code staged} records each product taken.
     */
    static List<KitProduct> stageProducts(TurCatalogCopilotService.Retrieval retrieval, Gap gap,
            Options opts, int limit, Set<String> staged, Set<String> guideTypes, Map<String, String> reasonLabels) {
        Set<String> productTypes = types(opts.productTypes());
        List<KitProduct> products = new ArrayList<>();
        List<TurCatalogCitation> citations = retrieval.citations();
        for (int i = 0; i < citations.size() && products.size() < limit; i++) {
            TurCatalogCitation citation = citations.get(i);
            Map<String, String> attributes = retrieval.attributesAt(i);
            if (hasType(attributes, guideTypes)
                    || (!productTypes.isEmpty() && !hasType(attributes, productTypes))) {
                continue;
            }
            Boolean available = availability(attributes, opts.availabilityField());
            boolean dropped = Boolean.FALSE.equals(available) && opts.availability() == Availability.DROP;
            if (citation.id() != null && !dropped && staged.add(citation.id())) {
                products.add(new KitProduct(citation.rank(), citation.id(), citation.title(), citation.url(),
                        citation.score(), reason(attributes, gap, opts.availabilityField(), reasonLabels),
                        available));
            }
        }
        return List.copyOf(products);
    }

    /**
     * The product's reason, from its own attributes only: the ones whose name or
     * value shares a word with the gap (its name, query or why) first, then the
     * others, at most {@link #REASON_ATTRIBUTES}, as {@code label: value; …}.
     * T1174 — only the site's facet fields count, the keys of {@code labels},
     * each under its label; a site key or an image URL is never a reason.
     * {@code null} when the product has none of them.
     */
    static String reason(Map<String, String> attributes, Gap gap, String availabilityField,
            Map<String, String> labels) {
        if (attributes == null || attributes.isEmpty() || labels == null || labels.isEmpty()) {
            return null;
        }
        Set<String> gapWords = words(String.join(" ", gap.name(), gap.query(),
                gap.reason() == null ? "" : gap.reason()));
        List<Map.Entry<String, String>> matching = new ArrayList<>();
        List<Map.Entry<String, String>> others = new ArrayList<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String label = labels.get(entry.getKey());
            if (label == null || entry.getKey().equals(availabilityField) || !StringUtils.hasText(entry.getValue())) {
                continue;
            }
            Set<String> entryWords = words(label + " " + entry.getValue());
            entryWords.retainAll(gapWords);
            (entryWords.isEmpty() ? others : matching).add(Map.entry(label, entry.getValue()));
        }
        matching.addAll(others);
        List<String> parts = matching.stream().limit(REASON_ATTRIBUTES)
                .map(e -> e.getKey() + ": " + e.getValue())
                .toList();
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    /**
     * T1174 — the label of each facet field in {@code vocabulary}, whose keys are
     * the allow-list of a reason. A label read that fails falls back to the
     * humanized field name; an empty vocabulary gives no reason at all.
     */
    private Map<String, String> reasonLabels(String siteName, String locale, Map<String, List<String>> vocabulary) {
        if (vocabulary.isEmpty()) {
            return Map.of();
        }
        Map<String, String> labels;
        try {
            labels = nlFilterService.facetLabels(siteName, locale);
        } catch (RuntimeException e) {
            log.warn("[CatalogKit] could not read the facet labels of site '{}': {}", siteName, e.getMessage());
            labels = Map.of();
        }
        Map<String, String> reasonLabels = new HashMap<>();
        for (String field : vocabulary.keySet()) {
            String label = labels == null ? null : labels.get(field);
            reasonLabels.put(field, label != null ? label : TurSNFieldLabel.orHumanized(null, field));
        }
        return reasonLabels;
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
     * the catalog does. A read that fails sends the prompt without them. T1191 — a
     * field with a single value across the catalog (a {@code site} a connector
     * indexes) tells no product apart, so it is neither a gap filter nor a reason.
     */
    private Map<String, List<String>> vocabulary(String siteName, String locale) {
        Map<String, List<String>> read;
        try {
            read = nlFilterService.facetVocabulary(siteName, locale, VOCABULARY_VALUES);
        } catch (RuntimeException e) {
            log.warn("[CatalogKit] could not read the facet values of site '{}': {}", siteName, e.getMessage());
            return Map.of();
        }
        return distinguishing(read);
    }

    /** The fields of {@code vocabulary} with more than one value, in their order. */
    static Map<String, List<String>> distinguishing(Map<String, List<String>> vocabulary) {
        Map<String, List<String>> kept = new java.util.LinkedHashMap<>();
        if (vocabulary != null) {
            vocabulary.forEach((field, values) -> {
                if (values != null && values.size() > 1) {
                    kept.put(field, values);
                }
            });
        }
        return kept;
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
                The search query is plain words a shopper would type, never a field name. \
                Carry into each query what the plan states that bears on that need (when, \
                where, for whom, at what level, for how many), so it asks for what fits \
                this plan and not for the need in general.

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
