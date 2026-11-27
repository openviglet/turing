/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.sn.nl;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.viglet.turing.shared.exception.TurNotFoundException;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.field.TurSNFieldLabel;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExtFacet;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.persistence.repository.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.searchengine.dsl.TurDslQuery;
import com.viglet.turing.searchengine.dsl.TurDslQueryRequest;
import com.viglet.turing.searchengine.dsl.TurDslSearchResponse;
import com.viglet.turing.sn.dsl.TurDslSearchService;
import com.viglet.turing.sn.dsl.eval.TurNLFacetField;
import com.viglet.turing.sn.dsl.eval.TurNLFacetParser;
import com.viglet.turing.sn.facet.TurSNCustomFacetCatalog;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a natural-language request into the filters it names, as data (T1085).
 *
 * <p>The one extractor the NL features read: the {@link TurNLFacetParser} maps the
 * text onto the site's field schema, and this service turns that parse into
 * {@link TurSNNLFilters}. Every value is checked against the values the site
 * actually holds, read with one {@code terms} aggregation per field, and spelled
 * the way the site indexes it ({@code sao paulo} becomes {@code São Paulo}); a
 * value the site does not hold is dropped and reported, never sent as a filter
 * that matches nothing. Each filter carries the words of the text it was read
 * from, and a confidence of 1 when those words are the value itself.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Service
public class TurSNNLFilterService {

    static final String TEXT_FIELD = "_text_";
    static final String UNKNOWN_FIELD = "UNKNOWN_FIELD";
    static final String NOT_IN_SITE = "NOT_IN_SITE";
    /** T1117 — a date range nothing in the text dates: a guess, never sent as a filter. */
    static final String UNGROUNDED_DATE = "UNGROUNDED_DATE";
    static final double LITERAL = 1.0;
    static final double INFERRED = 0.6;
    static final double UNCHECKED = 0.3;
    private static final int GROUNDING_BUCKETS = 1000;
    /** T1359 — at most this many values of one facet field go into the parse prompt. */
    private static final int PROMPT_VALUES = 30;
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    private final TurNLFacetParser parser;
    private final TurDslSearchService dslSearchService;
    private final TurSNCustomFacetCatalog customFacetCatalog;
    private Clock clock = Clock.systemDefaultZone();

    void setClockForTest(Clock clock) {
        this.clock = clock;
    }

    public TurSNNLFilterService(TurSNSiteRepository turSNSiteRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository,
            TurSNSiteLocaleRepository turSNSiteLocaleRepository, TurNLFacetParser parser,
            TurDslSearchService dslSearchService, TurSNCustomFacetCatalog customFacetCatalog) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
        this.turSNSiteLocaleRepository = turSNSiteLocaleRepository;
        this.parser = parser;
        this.dslSearchService = dslSearchService;
        this.customFacetCatalog = customFacetCatalog;
    }

    /**
     * @param locale the search locale; {@code null} means the site's first locale
     * @throws IllegalArgumentException when {@code text} is blank
     * @throws TurNotFoundException     when the site does not exist
     */
    @Transactional(readOnly = true)
    public TurSNNLFilters parse(String siteName, String locale, String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("'text' is required");
        }
        TurSNSite site = turSNSiteRepository.findByNameIgnoreCase(siteName)
                .orElseThrow(() -> new TurNotFoundException("SN site not found: " + siteName));
        if (!parser.isAvailable()) {
            return TurSNNLFilters.unavailable(text);
        }
        Map<String, TurNLFacetField> schema = turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)
                .stream().map(TurNLFacetField::of)
                .collect(Collectors.toMap(TurNLFacetField::name, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        String searchLocale = StringUtils.hasText(locale) ? locale : firstLocale(site);
        // T1359 — the facet values are read once, before the parse: the prompt lists
        // them so a noun can name one, and grounding checks against the same read.
        List<String> facets = schema.values().stream()
                .filter(field -> field.facet() && !field.numeric())
                .map(TurNLFacetField::name)
                .toList();
        Map<String, Set<String>> held = siteValues(site.getName(), searchLocale, facets);
        Map<String, List<String>> vocabulary = new LinkedHashMap<>();
        held.forEach((field, values) -> vocabulary.put(field, values.stream().limit(PROMPT_VALUES).toList()));

        TurDslQueryRequest parsed;
        try {
            parsed = parser.parse(new TurNLFacetParser.ParseRequest(site.getName(), searchLocale, text,
                    List.copyOf(schema.values()), vocabulary));
        } catch (TurNLFacetParser.TurNLFacetParseException e) {
            log.warn("[T1085] Could not parse '{}' for site '{}': {}", text, siteName, e.getMessage());
            return new TurSNNLFilters(true, text, text, List.of(), List.of(), List.of());
        }
        return ground(site.getName(), searchLocale, text, schema, parsed, held, Set.copyOf(facets));
    }

    /**
     * Grounds a parse another caller already made (T1085): the catalog copilot's
     * planners parse with their own schema and passes, and send the result through
     * here so their filters are checked against the site's values like any other.
     *
     * @param schema the schema the parse was made against
     */
    @Transactional(readOnly = true)
    public TurSNNLFilters ground(String siteName, String locale, String text, List<TurNLFacetField> schema,
            TurDslQueryRequest parsed) {
        Map<String, TurNLFacetField> bySchema = new LinkedHashMap<>();
        if (schema != null) {
            schema.forEach(field -> bySchema.putIfAbsent(field.name(), field));
        }
        return ground(siteName, locale, text, bySchema, parsed, Map.of(), Set.of());
    }

    /**
     * @param held the values already read, by field
     * @param read the fields already read, including those that hold nothing
     */
    private TurSNNLFilters ground(String siteName, String searchLocale, String text,
            Map<String, TurNLFacetField> schema, TurDslQueryRequest parsed, Map<String, Set<String>> held,
            Set<String> read) {
        Parse flat = flatten(parsed == null ? null : parsed.query());
        List<TurSNNLFilters.Dropped> dropped = new ArrayList<>();
        Map<String, Set<String>> siteValues = new LinkedHashMap<>(held);
        siteValues.putAll(siteValues(siteName, searchLocale, termFieldsToGround(flat, schema).stream()
                .filter(field -> !read.contains(field)).toList()));
        Map<String, List<String>> terms = rehome(siteName, searchLocale, flat.terms(), schema, siteValues);
        List<TurSNNLFilters.Filter> filters = new ArrayList<>();
        org.apache.lucene.analysis.CharArraySet stopWords = stopWords(searchLocale);
        terms.forEach((field, values) -> termFilter(field, values, schema, siteValues, text, dropped, stopWords)
                .ifPresent(filters::add));
        // T1330 — "today" is the site's today, in its own time zone when it names one.
        java.time.ZoneId zone = flat.ranges().isEmpty() ? clock.getZone() : siteZone(siteName);
        flat.ranges().forEach((field, range) -> rangeFilter(field, range, schema, text, dropped, zone)
                .ifPresent(filters::add));
        // T1360 — a date phrase the parse gave no range still dates the request, on the
        // site's date facet; a date range on a facet that spans two fields is an overlap.
        boolean dated = flat.ranges().keySet().stream()
                .anyMatch(field -> schema.containsKey(field) && schema.get(field).type() == TurSEFieldType.DATE);
        Optional<TurRelativeDateRange> phrase = dated ? Optional.empty()
                : TurRelativeDateRange.resolve(text, LocalDate.now(clock.withZone(siteZone(siteName))));
        if (dated || phrase.isPresent()) {
            DateFacets dateFacets = dateFacets(siteName, schema);
            // an instant bound, which a date field's range takes
            phrase.flatMap(resolved -> dateFacets.only().map(field -> relativeFilter(field,
                    new String[] { resolved.start() + "T00:00:00Z", null }, resolved))).ifPresent(filters::add);
            spanOverlaps(filters, dateFacets.endFields());
        }

        // T1361 — each filter's words leave q, whether q is the parse's free text or the request.
        String q = leftover(StringUtils.hasText(flat.freeText()) ? flat.freeText().trim() : text, filters);
        // T1359 — a word the parse left in q that names a value a facet holds is that filter.
        List<TurSNNLValueNouns.Match> nouns = TurSNNLValueNouns.find(q, searchLocale, siteValues,
                filters.stream().map(TurSNNLFilters.Filter::field).collect(Collectors.toSet()));
        if (!nouns.isEmpty()) {
            filters.addAll(nounFilters(text, q, nouns));
            q = without(q, nouns);
        }
        // T1361 — a word naming the value its field already filters says it again: it leaves q.
        q = without(q, TurSNNLValueNouns.find(q, searchLocale, filteredValues(filters), Set.of()));
        // a q of stop words alone ("o que tem") narrows nothing the filters did not
        if (StringUtils.hasText(searchLocale) && com.viglet.turing.searchengine.TurSNQueryRelaxer.onlyStopWords(q,
                Locale.forLanguageTag(searchLocale.replace('_', '-')))) {
            q = "";
        }
        return new TurSNNLFilters(true, text, q, filters, fq(filters), dropped);
    }

    /** T1361 — the values each value filter carries, by field. */
    private static Map<String, List<String>> filteredValues(List<TurSNNLFilters.Filter> filters) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        filters.stream().filter(filter -> !filter.isRange() && !filter.values().isEmpty())
                .forEach(filter -> values.computeIfAbsent(filter.field(), k -> new ArrayList<>())
                        .addAll(filter.values()));
        return values;
    }

    /**
     * T1360 — the site's date facets: the date fields of {@code schema} that are a
     * facet or carry a custom facet, and the end field of each custom facet that
     * spans two fields (T1224).
     *
     * @param only      the one date facet a date phrase lands on: the only one, else
     *                  the only one that spans two fields; empty when that is a guess
     * @param endFields the end field of each date field whose custom facet has one
     */
    record DateFacets(Optional<String> only, Map<String, String> endFields) {
    }

    private DateFacets dateFacets(String siteName, Map<String, TurNLFacetField> schema) {
        Optional<TurSNSite> site = StringUtils.hasText(siteName) ? turSNSiteRepository.findByNameIgnoreCase(siteName)
                : Optional.empty();
        if (site.isEmpty()) {
            return new DateFacets(Optional.empty(), Map.of());
        }
        Map<String, TurSNSiteFieldExt> dateFields = new LinkedHashMap<>();
        for (TurSNSiteFieldExt ext : turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site.get(), 1)) {
            if (ext.getType() == TurSEFieldType.DATE && schema.containsKey(ext.getName())) {
                dateFields.putIfAbsent(ext.getName(), ext);
            }
        }
        Map<String, String> endFields = new LinkedHashMap<>();
        Set<String> customFaceted = new LinkedHashSet<>();
        for (com.viglet.turing.searchengine.spi.TurSECustomFacet facet : customFacetCatalog.customFacets(site.get())) {
            dateFields.values().stream()
                    .filter(ext -> ext.getId() != null && ext.getId().equals(facet.fieldId()))
                    .findFirst()
                    .ifPresent(ext -> {
                        customFaceted.add(ext.getName());
                        if (StringUtils.hasText(facet.endField())) {
                            endFields.putIfAbsent(ext.getName(), facet.endField());
                        }
                    });
        }
        List<String> faceted = dateFields.values().stream()
                .filter(ext -> ext.getFacet() == 1 || customFaceted.contains(ext.getName()))
                .map(TurSNSiteFieldExt::getName)
                .toList();
        Optional<String> only = Optional.empty();
        if (faceted.size() == 1) {
            only = Optional.of(faceted.get(0));
        } else if (endFields.size() == 1) {
            only = Optional.of(endFields.keySet().iterator().next());
        }
        return new DateFacets(only, endFields);
    }

    /**
     * T1360 — each date range on a field whose custom facet spans two fields becomes
     * the overlap that facet's buckets apply (T1224): it starts by the range's end
     * and ends from its start, so an exhibition under way is on this week. T1361 — a
     * range the parse put on the end field is the same period and becomes the same
     * pair, and a span takes one pair however many of its fields were ranged.
     */
    private static void spanOverlaps(List<TurSNNLFilters.Filter> filters, Map<String, String> endFields) {
        if (endFields.isEmpty()) {
            return;
        }
        Map<String, String> startFields = new LinkedHashMap<>();
        endFields.forEach((start, end) -> startFields.putIfAbsent(end, start));
        Set<String> overlapped = new LinkedHashSet<>();
        List<TurSNNLFilters.Filter> spanned = new ArrayList<>();
        for (TurSNNLFilters.Filter filter : filters) {
            String startField = !filter.isRange() ? null
                    : endFields.containsKey(filter.field()) ? filter.field() : startFields.get(filter.field());
            if (startField == null || filter.rangeStart() == null || filter.rangeEnd() == null) {
                spanned.add(filter);
                continue;
            }
            if (!overlapped.add(startField)) {
                continue;
            }
            spanned.add(new TurSNNLFilters.Filter(startField, List.of(), null, filter.rangeEnd(),
                    filter.sourceWords(), filter.confidence(), filter.grounded()));
            spanned.add(new TurSNNLFilters.Filter(endFields.get(startField), List.of(), filter.rangeStart(), null,
                    filter.sourceWords(), filter.confidence(), filter.grounded()));
        }
        filters.clear();
        filters.addAll(spanned);
    }

    /** T1359 — one filter per field the nouns name, its words as the text spells them. */
    private static List<TurSNNLFilters.Filter> nounFilters(String text, String q,
            List<TurSNNLValueNouns.Match> nouns) {
        Map<String, List<TurSNNLValueNouns.Match>> byField = nouns.stream()
                .collect(Collectors.groupingBy(TurSNNLValueNouns.Match::field, LinkedHashMap::new,
                        Collectors.toList()));
        List<TurSNNLFilters.Filter> filters = new ArrayList<>();
        byField.forEach((field, matches) -> {
            List<String> values = matches.stream().map(TurSNNLValueNouns.Match::value).distinct().toList();
            List<String> sourceWords = matches.stream().map(match -> {
                String spelled = q.substring(match.start(), match.end());
                return sourceWords(text, spelled).orElse(spelled);
            }).toList();
            double confidence = matches.stream().allMatch(TurSNNLValueNouns.Match::exact) ? LITERAL : INFERRED;
            log.debug("[T1359] '{}' names {} of '{}'", sourceWords, values, field);
            filters.add(new TurSNNLFilters.Filter(field, values, null, null, sourceWords, confidence, true));
        });
        return filters;
    }

    /** {@code q} with the nouns' words taken out. */
    private static String without(String q, List<TurSNNLValueNouns.Match> nouns) {
        StringBuilder rest = new StringBuilder(q);
        for (TurSNNLValueNouns.Match match : nouns) {
            for (int i = match.start(); i < match.end(); i++) {
                rest.setCharAt(i, ' ');
            }
        }
        return rest.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * T1169 — the values the site's facet fields hold, the most common first and at
     * most {@code perField} each, so a prompt can word its queries the way the
     * catalog does. A field with no buckets is absent; an unknown site, or a read
     * that fails, answers an empty map.
     *
     * @param locale the search locale; {@code null} means the site's first locale
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> facetVocabulary(String siteName, String locale, int perField) {
        Optional<TurSNSite> site = turSNSiteRepository.findByNameIgnoreCase(siteName);
        if (site.isEmpty()) {
            return Map.of();
        }
        List<String> fields = turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site.get(), 1).stream()
                .map(TurNLFacetField::of)
                .filter(field -> field.facet() && !field.numeric())
                .map(TurNLFacetField::name)
                .distinct()
                .toList();
        String searchLocale = StringUtils.hasText(locale) ? locale : firstLocale(site.get());
        Map<String, List<String>> vocabulary = new LinkedHashMap<>();
        siteValues(site.get().getName(), searchLocale, fields).forEach((field, values) ->
                vocabulary.put(field, values.stream().limit(perField).toList()));
        return vocabulary;
    }

    /**
     * T1174 — the label of each of the site's enabled, non-numeric facet fields,
     * as the facet header reads it: the label of the {@code locale}'s facet row,
     * else the field's {@code facetName}, else the humanized field name. An
     * unknown site answers an empty map.
     *
     * @param locale the search locale; {@code null} means the site's first locale
     */
    @Transactional(readOnly = true)
    public Map<String, String> facetLabels(String siteName, String locale) {
        Optional<TurSNSite> site = turSNSiteRepository.findByNameIgnoreCase(siteName);
        if (site.isEmpty()) {
            return Map.of();
        }
        String searchLocale = StringUtils.hasText(locale) ? locale : firstLocale(site.get());
        Map<String, String> labels = new LinkedHashMap<>();
        for (TurSNSiteFieldExt ext : turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site.get(), 1)) {
            TurNLFacetField field = TurNLFacetField.of(ext);
            if (!field.facet() || field.numeric() || labels.containsKey(field.name())) {
                continue;
            }
            String localeLabel = Optional.ofNullable(ext.getFacetLocales()).orElse(Set.of()).stream()
                    .filter(facet -> facet.getLocale() != null
                            && facet.getLocale().toString().equalsIgnoreCase(searchLocale))
                    .map(TurSNSiteFieldExtFacet::getLabel)
                    .filter(StringUtils::hasText)
                    .findFirst().orElse(null);
            labels.put(field.name(), localeLabel != null ? localeLabel
                    : TurSNFieldLabel.orHumanized(ext.getFacetName(), ext.getName()));
        }
        return labels;
    }

    private String firstLocale(TurSNSite site) {
        return turSNSiteLocaleRepository.findByTurSNSiteOrderByPositionAsc(site).stream()
                .map(TurSNSiteLocale::getLanguage)
                .filter(java.util.Objects::nonNull)
                .map(Locale::toString)
                .findFirst().orElse(Locale.US.toString());
    }

    // ── Flattening the parser's DSL ─────────────────────────────────────────

    /** What the parse names: term values and ranges per field, and the free text. */
    record Parse(Map<String, List<String>> terms, Map<String, String[]> ranges, String freeText) {
    }

    static Parse flatten(TurDslQuery query) {
        Map<String, List<String>> terms = new LinkedHashMap<>();
        Map<String, String[]> ranges = new LinkedHashMap<>();
        StringBuilder freeText = new StringBuilder();
        walk(query, terms, ranges, freeText);
        return new Parse(terms, ranges, freeText.toString());
    }

    private static void walk(TurDslQuery query, Map<String, List<String>> terms, Map<String, String[]> ranges,
            StringBuilder freeText) {
        switch (query) {
            case null -> {
                // nothing parsed
            }
            case TurDslQuery.Bool bool -> {
                // mustNot is an exclusion, which a filter query of this shape cannot carry
                for (List<TurDslQuery> clauses : List.of(nullSafe(bool.filter()), nullSafe(bool.must()),
                        nullSafe(bool.should()))) {
                    clauses.forEach(clause -> walk(clause, terms, ranges, freeText));
                }
            }
            case TurDslQuery.ConstantScore constantScore -> walk(constantScore.filter(), terms, ranges, freeText);
            case TurDslQuery.Term term -> addTerm(terms, term.field(), term.value());
            case TurDslQuery.Terms many -> nullSafe(many.values()).forEach(v -> addTerm(terms, many.field(), v));
            case TurDslQuery.Range range -> ranges.put(range.field(), new String[] {
                    str(range.gte() != null ? range.gte() : range.gt()),
                    str(range.lte() != null ? range.lte() : range.lt()) });
            case TurDslQuery.Match match -> textOrTerm(match.field(), match.query(), terms, freeText);
            case TurDslQuery.MatchPhrase phrase -> textOrTerm(phrase.field(), phrase.query(), terms, freeText);
            default -> log.debug("[T1085] Ignoring a {} clause in an NL parse", query.getClass().getSimpleName());
        }
    }

    private static void textOrTerm(String field, String value, Map<String, List<String>> terms,
            StringBuilder freeText) {
        if (field == null || TEXT_FIELD.equals(field)) {
            if (StringUtils.hasText(value)) {
                freeText.append(freeText.isEmpty() ? "" : " ").append(value.trim());
            }
        } else {
            addTerm(terms, field, value);
        }
    }

    private static void addTerm(Map<String, List<String>> terms, String field, Object value) {
        String text = str(value);
        if (field != null && text != null) {
            List<String> values = terms.computeIfAbsent(field, k -> new ArrayList<>());
            if (!values.contains(text)) {
                values.add(text);
            }
        }
    }

    // ── Grounding in the site's values ──────────────────────────────────────

    /** Facet fields hold a list of values to check against; other fields pass unchecked. */
    private static List<String> termFieldsToGround(Parse flat, Map<String, TurNLFacetField> schema) {
        return flat.terms().keySet().stream()
                .filter(field -> schema.containsKey(field) && schema.get(field).facet()
                        && !schema.get(field).numeric())
                .toList();
    }

    /**
     * T1277 — each value the parse put on a field that cannot hold it, moved to the
     * one facet field that does: the model wrote {@code type=chat} where the site
     * declares {@code kind} and holds {@code CHAT}. A value is homeless when its
     * field is not in the schema, when its facet field was read and holds no such
     * value, or when its field is a string field with no value list to check it
     * against. The facet fields not read yet are read once, and only when a value
     * is homeless; a value no facet holds, or more than one does, stays where the
     * parse put it and is grounded (or dropped) there as before.
     */
    private Map<String, List<String>> rehome(String siteName, String locale, Map<String, List<String>> terms,
            Map<String, TurNLFacetField> schema, Map<String, Set<String>> siteValues) {
        boolean anyHomeless = terms.entrySet().stream()
                .anyMatch(e -> e.getValue().stream().anyMatch(v -> homeless(e.getKey(), v, schema, siteValues)));
        if (!anyHomeless) {
            return terms;
        }
        List<String> facets = schema.values().stream()
                .filter(field -> field.facet() && !field.numeric())
                .map(TurNLFacetField::name)
                .toList();
        siteValues.putAll(siteValues(siteName, locale,
                facets.stream().filter(field -> !siteValues.containsKey(field)).toList()));
        Map<String, List<String>> rehomed = new LinkedHashMap<>();
        terms.forEach((field, values) -> values.forEach(value -> {
            String target = homeless(field, value, schema, siteValues)
                    ? holder(field, value, facets, siteValues).orElse(field)
                    : field;
            if (!target.equals(field)) {
                log.debug("[T1277] Moved '{}' from field '{}' to '{}', the facet that holds it", value, field,
                        target);
            }
            List<String> moved = rehomed.computeIfAbsent(target, k -> new ArrayList<>());
            if (!moved.contains(value)) {
                moved.add(value);
            }
        }));
        return rehomed;
    }

    private static boolean homeless(String field, String value, Map<String, TurNLFacetField> schema,
            Map<String, Set<String>> siteValues) {
        TurNLFacetField declared = schema.get(field);
        if (declared == null) {
            return true;
        }
        if (declared.numeric() || declared.type() == TurSEFieldType.TEXT
                || declared.type() == TurSEFieldType.DATE) {
            return false;
        }
        if (declared.facet()) {
            Set<String> held = siteValues.get(field);
            return held != null && canonical(value, held) == null;
        }
        return true;
    }

    /** The one facet field, other than {@code field}, that holds {@code value}. */
    private static Optional<String> holder(String field, String value, List<String> facets,
            Map<String, Set<String>> siteValues) {
        List<String> holders = facets.stream()
                .filter(facet -> !facet.equals(field) && siteValues.containsKey(facet)
                        && canonical(value, siteValues.get(facet)) != null)
                .toList();
        return holders.size() == 1 ? Optional.of(holders.get(0)) : Optional.empty();
    }

    // ── Back to a query ─────────────────────────────────────────────────────

    /**
     * The grounded filters as a DSL query for the copilot's search: each value
     * filter a term (or terms) filter, a full-text field's value a match, each
     * range a range filter, and {@code q} a match on the full text. The parse's
     * exclusions carry over unchanged, since grounding has nothing to say about
     * what to leave out.
     */
    public static TurDslQuery toQuery(TurSNNLFilters filters, List<TurNLFacetField> schema,
            TurDslQuery parsedQuery) {
        Map<String, TurNLFacetField> bySchema = new LinkedHashMap<>();
        if (schema != null) {
            schema.forEach(field -> bySchema.putIfAbsent(field.name(), field));
        }
        List<TurDslQuery> must = new ArrayList<>();
        List<TurDslQuery> filter = new ArrayList<>();
        for (TurSNNLFilters.Filter f : filters.filters()) {
            TurNLFacetField field = bySchema.get(f.field());
            if (f.isRange()) {
                filter.add(new TurDslQuery.Range(f.field(), bound(f.rangeStart()), null, bound(f.rangeEnd()), null));
            } else if (field != null && field.type() == com.viglet.turing.commons.se.field.TurSEFieldType.TEXT) {
                f.values().forEach(value -> must.add(new TurDslQuery.Match(f.field(), value, null)));
            } else if (f.values().size() == 1) {
                filter.add(new TurDslQuery.Term(f.field(), f.values().get(0)));
            } else {
                filter.add(new TurDslQuery.Terms(f.field(), List.copyOf(f.values())));
            }
        }
        if (StringUtils.hasText(filters.q())) {
            must.add(new TurDslQuery.Match(TEXT_FIELD, filters.q(), null));
        }
        List<TurDslQuery> mustNot = parsedQuery instanceof TurDslQuery.Bool bool && bool.mustNot() != null
                ? bool.mustNot() : List.of();
        if (must.isEmpty() && filter.isEmpty() && mustNot.isEmpty()) {
            return new TurDslQuery.MatchAll();
        }
        return new TurDslQuery.Bool(must, List.of(), mustNot, filter, null);
    }

    /** A range bound as the number it spells, so every engine compares it as one. */
    private static Object bound(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException notLong) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException notNumber) {
                return value;
            }
        }
    }

    /**
     * The values each field holds, by its normalized form, read with one search of
     * {@code terms} aggregations. A field the engine answers no buckets for is
     * absent, and its values pass unchecked.
     */
    private Map<String, Set<String>> siteValues(String siteName, String locale, List<String> fields) {
        if (fields.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> aggs = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            aggs.put("f" + i, Map.of("terms", Map.of("field", fields.get(i), "size", GROUNDING_BUCKETS)));
        }
        Map<String, Set<String>> values = new LinkedHashMap<>();
        try {
            TurDslQueryRequest request = MAPPER.convertValue(
                    Map.of("query", Map.of("match_all", Map.of()), "size", 0, "aggs", aggs),
                    TurDslQueryRequest.class);
            Optional<TurDslSearchResponse> response = dslSearchService.search(siteName, locale, request);
            Map<String, TurDslSearchResponse.AggregationResult> results = response
                    .map(TurDslSearchResponse::aggregations).orElse(Map.of());
            for (int i = 0; i < fields.size(); i++) {
                TurDslSearchResponse.AggregationResult result = results == null ? null : results.get("f" + i);
                if (result != null && result.buckets() != null && !result.buckets().isEmpty()) {
                    values.put(fields.get(i), result.buckets().stream()
                            .map(TurDslSearchResponse.Bucket::key)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
                }
            }
        } catch (RuntimeException e) {
            log.warn("[T1085] Could not read the values of {} on site '{}', filters pass unchecked: {}",
                    fields, siteName, e.getMessage());
        }
        return values;
    }

    private static Optional<TurSNNLFilters.Filter> termFilter(String field, List<String> proposed,
            Map<String, TurNLFacetField> schema, Map<String, Set<String>> siteValues, String text,
            List<TurSNNLFilters.Dropped> dropped, org.apache.lucene.analysis.CharArraySet stopWords) {
        if (!schema.containsKey(field)) {
            proposed.forEach(v -> dropped.add(new TurSNNLFilters.Dropped(field, v, UNKNOWN_FIELD)));
            return Optional.empty();
        }
        Set<String> held = siteValues.get(field);
        boolean grounded = held != null;
        List<String> values = new ArrayList<>();
        List<String> sourceWords = new ArrayList<>();
        double confidence = LITERAL;
        for (String value : proposed) {
            String canonical = grounded ? canonical(value, held) : value;
            boolean partial = false;
            if (canonical == null) {
                // T1300 — the short name a visitor says ("Casarão") for the value an
                // editor filed ("Casarão FESPSP"), when exactly one value holds it.
                canonical = containing(value, held, stopWords);
                partial = canonical != null;
            }
            if (canonical == null) {
                dropped.add(new TurSNNLFilters.Dropped(field, value, NOT_IN_SITE));
                continue;
            }
            if (!values.contains(canonical)) {
                values.add(canonical);
            }
            String filed = canonical;
            Optional<String> words = sourceWords(text, filed).or(() -> sourceWords(text, value));
            words.ifPresent(sourceWords::add);
            confidence = Math.min(confidence, partial ? INFERRED : valueConfidence(grounded, words.isPresent()));
        }
        return values.isEmpty() ? Optional.empty()
                : Optional.of(new TurSNNLFilters.Filter(field, values, null, null, sourceWords, confidence,
                        grounded));
    }

    private static double valueConfidence(boolean grounded, boolean spelledInText) {
        if (!grounded) {
            return UNCHECKED;
        }
        return spelledInText ? LITERAL : INFERRED;
    }

    /**
     * T1362 — today on the site's calendar, the day its relative dates resolve
     * against: in the site's time zone, else the server's.
     */
    @Transactional(readOnly = true)
    public LocalDate today(String siteName) {
        return LocalDate.now(clock.withZone(siteZone(siteName)));
    }

    /** T1330 — the site's time zone, or the clock's when the site names none. */
    private java.time.ZoneId siteZone(String siteName) {
        if (!StringUtils.hasText(siteName)) {
            return clock.getZone();
        }
        try {
            return turSNSiteRepository.findByNameIgnoreCase(siteName)
                    .flatMap(site -> TurSiteTimeZone.of(site.getTimeZone()))
                    .orElse(clock.getZone());
        } catch (RuntimeException e) {
            log.debug("[T1330] time zone of site '{}' not read: {}", siteName, e.getMessage());
            return clock.getZone();
        }
    }

    private Optional<TurSNNLFilters.Filter> rangeFilter(String field, String[] range,
            Map<String, TurNLFacetField> schema, String text, List<TurSNNLFilters.Dropped> dropped,
            java.time.ZoneId zone) {
        if (!schema.containsKey(field)) {
            dropped.add(new TurSNNLFilters.Dropped(field, "[" + range[0] + " TO " + range[1] + "]",
                    UNKNOWN_FIELD));
            return Optional.empty();
        }
        if (range[0] == null && range[1] == null) {
            return Optional.empty();
        }
        if (schema.get(field).type() == TurSEFieldType.DATE) {
            return dateRangeFilter(field, range, text, dropped, zone);
        }
        // T1211 — the bound's span: the amount as the text formats it, with its currency and bound phrase
        List<TurSNNLRangeSpan.Span> spans = new ArrayList<>();
        boolean literal = true;
        for (String bound : range) {
            if (bound != null) {
                Optional<TurSNNLRangeSpan.Span> span = TurSNNLRangeSpan.amount(text, bound);
                span.ifPresent(spans::add);
                literal &= span.isPresent();
            }
        }
        return Optional.of(new TurSNNLFilters.Filter(field, List.of(), range[0], range[1],
                TurSNNLRangeSpan.words(text, spans), literal ? LITERAL : INFERRED, false));
    }

    /**
     * T1117 — a date range from a relative phrase is resolved against today, not
     * read from the model, which fills it from its training date. A range the text
     * neither phrases relatively nor dates by year is a guess, and is dropped
     * rather than sent as a filter that hides what the visitor asked for.
     */
    private Optional<TurSNNLFilters.Filter> dateRangeFilter(String field, String[] range, String text,
            List<TurSNNLFilters.Dropped> dropped, java.time.ZoneId zone) {
        Optional<TurRelativeDateRange> relative = TurRelativeDateRange.resolve(text,
                LocalDate.now(clock.withZone(zone)));
        if (relative.isPresent()) {
            return Optional.of(relativeFilter(field, range, relative.get()));
        }
        List<TurSNNLRangeSpan.Span> spans = new ArrayList<>();
        for (String bound : range) {
            if (bound == null) {
                continue;
            }
            // T1211 — the year's span takes its date phrase with it ("from 2024")
            Optional<TurSNNLRangeSpan.Span> span = TurSNNLRangeSpan.dated(text, bound);
            if (span.isEmpty()) {
                dropped.add(new TurSNNLFilters.Dropped(field,
                        "[" + orStar(range[0]) + " TO " + orStar(range[1]) + "]", UNGROUNDED_DATE));
                return Optional.empty();
            }
            spans.add(span.get());
        }
        boolean literal = java.util.Arrays.stream(range).filter(java.util.Objects::nonNull)
                .allMatch(bound -> sourceWords(text, bound).isPresent());
        return Optional.of(new TurSNNLFilters.Filter(field, List.of(), range[0], range[1],
                TurSNNLRangeSpan.words(text, spans), literal ? LITERAL : INFERRED, false));
    }

    /** The resolved range, spelled like the bounds the model used (a date or an instant). */
    private static TurSNNLFilters.Filter relativeFilter(String field, String[] range,
            TurRelativeDateRange resolved) {
        boolean instant = isInstant(range[0]) || isInstant(range[1]);
        return new TurSNNLFilters.Filter(field, List.of(),
                instant ? resolved.start() + "T00:00:00Z" : resolved.start().toString(),
                instant ? resolved.end() + "T23:59:59Z" : resolved.end().toString(),
                List.of(resolved.phrase()), LITERAL, true);
    }

    private static boolean isInstant(String bound) {
        return bound != null && bound.contains("T");
    }

    /** The site's own spelling of a value, matched without case or accents; {@code null} when it holds none. */
    static String canonical(String value, Set<String> held) {
        if (held.contains(value)) {
            return value;
        }
        String wanted = normalize(value);
        return held.stream().filter(candidate -> normalize(candidate).equals(wanted)).findFirst().orElse(null);
    }

    /**
     * T1300 — the one value of {@code held} whose words include every content word of
     * {@code value}, compared without case or accents and as whole words; stop words
     * ("e", "de") never count. {@code null} when none or several do, so a short name
     * two values share ("Sociologia") is still not guessed. Kept apart from
     * {@link #canonical}, whose exact match T1277's re-homing depends on.
     */
    static String containing(String value, Set<String> held,
            org.apache.lucene.analysis.CharArraySet stopWords) {
        if (held == null || !StringUtils.hasText(value)) {
            return null;
        }
        List<String> wanted = words(value).stream().filter(word -> !stopWords.contains(word)).toList();
        if (wanted.isEmpty()) {
            return null;
        }
        String found = null;
        for (String candidate : held) {
            if (words(candidate).containsAll(wanted)) {
                if (found != null) {
                    return null;
                }
                found = candidate;
            }
        }
        return found;
    }

    /** The folded words of {@code value}. */
    private static List<String> words(String value) {
        return java.util.Arrays.stream(normalize(value).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> !word.isEmpty()).toList();
    }

    /** The stop words of the request's language; none when it names no language. */
    private static org.apache.lucene.analysis.CharArraySet stopWords(String locale) {
        if (!StringUtils.hasText(locale)) {
            return org.apache.lucene.analysis.CharArraySet.EMPTY_SET;
        }
        return com.viglet.turing.searchengine.TurSNQueryRelaxer.stopWords(Locale.forLanguageTag(locale.replace('_', '-')));
    }

    // ── Source words and the leftover query ─────────────────────────────────

    /**
     * The words of {@code text} that spell {@code value}, compared without case or
     * accents and on word boundaries, as the text spells them.
     */
    static Optional<String> sourceWords(String text, String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        Folded folded = Folded.of(text);
        String wanted = normalize(value);
        int from = 0;
        while (from <= folded.text().length() - wanted.length()) {
            int at = folded.text().indexOf(wanted, from);
            if (at < 0) {
                return Optional.empty();
            }
            int end = at + wanted.length();
            if (boundary(folded.text(), at - 1) && boundary(folded.text(), end)) {
                return Optional.of(text.substring(folded.origin()[at], folded.origin()[end - 1] + 1));
            }
            from = at + 1;
        }
        return Optional.empty();
    }

    /**
     * The text with each filter's source words taken out at one position (T1211):
     * the first occurrence on word boundaries that no earlier filter took, so an
     * amount repeated elsewhere in the text ("400 lumen ... under $400") stays.
     */
    static String leftover(String text, List<TurSNNLFilters.Filter> filters) {
        boolean[] removed = new boolean[text.length()];
        for (TurSNNLFilters.Filter filter : filters) {
            for (String source : filter.sourceWords()) {
                // T1361 — the parse's free text may spell the words in another case or accenting
                String words = text.contains(source) ? source : sourceWords(text, source).orElse(source);
                int at = claim(text, words, removed);
                for (int i = at; at >= 0 && i < at + words.length(); i++) {
                    removed[i] = true;
                }
            }
        }
        StringBuilder rest = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            rest.append(removed[i] ? ' ' : text.charAt(i));
        }
        return rest.toString().replaceAll("\\s+", " ").trim();
    }

    /** Where {@code words} occurs untaken, on word boundaries if it can; -1 when nowhere. */
    private static int claim(String text, String words, boolean[] removed) {
        if (!StringUtils.hasText(words)) {
            return -1;
        }
        int fallback = -1;
        for (int at = text.indexOf(words); at >= 0; at = text.indexOf(words, at + 1)) {
            boolean taken = false;
            for (int i = at; i < at + words.length() && !taken; i++) {
                taken = removed[i];
            }
            if (taken) {
                continue;
            }
            if (boundary(text, at - 1) && boundary(text, at + words.length())) {
                return at;
            }
            if (fallback < 0) {
                fallback = at;
            }
        }
        return fallback;
    }

    private static boolean boundary(String text, int index) {
        return index < 0 || index >= text.length() || !Character.isLetterOrDigit(text.charAt(index));
    }

    static List<String> fq(List<TurSNNLFilters.Filter> filters) {
        List<String> fq = new ArrayList<>();
        for (TurSNNLFilters.Filter filter : filters) {
            if (filter.isRange()) {
                fq.add(filter.field() + ":[" + orStar(filter.rangeStart()) + " TO " + orStar(filter.rangeEnd())
                        + "]");
            } else {
                filter.values().forEach(value -> fq.add(filter.field() + ":" + value));
            }
        }
        return fq;
    }

    private static String orStar(String bound) {
        return bound == null ? "*" : bound;
    }

    static String normalize(String value) {
        return Folded.of(value).text().trim();
    }

    /** Lower case without accents, with each folded character's index in the original. */
    static final class Folded {
        private final String text;
        private final int[] origin;

        private Folded(String text, int[] origin) {
            this.text = text;
            this.origin = origin;
        }

        String text() {
            return text;
        }

        int[] origin() {
            return origin;
        }

        static Folded of(String original) {
            StringBuilder folded = new StringBuilder();
            List<Integer> origin = new ArrayList<>();
            for (int i = 0; i < original.length(); i++) {
                String decomposed = Normalizer.normalize(String.valueOf(original.charAt(i)), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
                for (int j = 0; j < decomposed.length(); j++) {
                    folded.append(decomposed.charAt(j));
                    origin.add(i);
                }
            }
            return new Folded(folded.toString(), origin.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
