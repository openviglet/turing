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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.shared.exception.TurNotFoundException;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExtFacet;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.persistence.repository.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.searchengine.dsl.TurDslQueryRequest;
import com.viglet.turing.searchengine.dsl.TurDslSearchResponse;
import com.viglet.turing.sn.dsl.TurDslSearchService;
import com.viglet.turing.sn.dsl.eval.TurNLFacetParser;

import tools.jackson.databind.json.JsonMapper;

/**
 * T1085 — a description becomes the q and fq of the site's own search, every value
 * spelled as the site holds it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@ExtendWith(MockitoExtension.class)
class TurSNNLFilterServiceTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String TEXT = "trabalho de meio periodo em sao paulo";

    @Mock
    private TurSNSiteRepository turSNSiteRepository;
    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    @Mock
    private TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    @Mock
    private TurNLFacetParser parser;
    @Mock
    private TurDslSearchService dslSearchService;
    @Mock
    private com.viglet.turing.sn.facet.TurSNCustomFacetCatalog customFacetCatalog;
    @InjectMocks
    private TurSNNLFilterService service;

    private TurSNSite site;

    @BeforeEach
    void setUp() {
        site = new TurSNSite();
        site.setName("vagas");
    }

    private static TurSNSiteFieldExt field(String name, TurSEFieldType type, boolean facet) {
        TurSNSiteFieldExt ext = new TurSNSiteFieldExt();
        ext.setName(name);
        ext.setType(type);
        ext.setFacet(facet ? 1 : 0);
        return ext;
    }

    private void siteWithFields() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("city", TurSEFieldType.STRING, true),
                field("contract", TurSEFieldType.STRING, true),
                field("salary", TurSEFieldType.FLOAT, true)));
    }

    private void parserAnswers(String json) {
        when(parser.parse(any())).thenReturn(MAPPER.readValue(json, TurDslQueryRequest.class));
    }

    private void siteHolds(Map<String, List<String>> bucketsByAgg) {
        Map<String, TurDslSearchResponse.AggregationResult> aggs = new java.util.LinkedHashMap<>();
        bucketsByAgg.forEach((agg, keys) -> aggs.put(agg, new TurDslSearchResponse.AggregationResult(
                keys.stream().map(k -> new TurDslSearchResponse.Bucket(k, 3)).toList())));
        when(dslSearchService.search(eq("vagas"), eq("pt_BR"), any()))
                .thenReturn(Optional.of(new TurDslSearchResponse(1, false, null, aggs)));
    }

    @Test
    @DisplayName("T1169: the facet vocabulary lists the values each non-numeric facet holds, capped per field")
    void facetVocabularyReadsTheSitesValues() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(turSNSiteRepository.findByNameIgnoreCase("nowhere")).thenReturn(Optional.empty());
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("city", TurSEFieldType.STRING, true),
                field("contract", TurSEFieldType.STRING, true),
                field("title", TurSEFieldType.TEXT, false),
                field("salary", TurSEFieldType.FLOAT, true)));
        // f0 = city, f1 = contract: contract holds nothing, so it is left out.
        siteHolds(Map.of("f0", List.of("São Paulo", "Campinas", "Santos"), "f1", List.of()));

        Map<String, List<String>> vocabulary = service.facetVocabulary("vagas", "pt_BR", 2);

        assertThat(vocabulary).containsOnlyKeys("city");
        assertThat(vocabulary.get("city")).containsExactly("São Paulo", "Campinas");
        assertThat(service.facetVocabulary("nowhere", "pt_BR", 2)).isEmpty();
    }

    @Test
    @DisplayName("T1174: a facet's label is its locale's row, else its facetName, else the humanized name")
    void facetLabelsReadTheLocaleLabel() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        TurSNSiteFieldExt city = field("city", TurSEFieldType.STRING, true);
        TurSNSiteFieldExtFacet cityPt = new TurSNSiteFieldExtFacet();
        cityPt.setLocale(Locale.forLanguageTag("pt-BR"));
        cityPt.setLabel("Cidade");
        city.getFacetLocales().add(cityPt);
        TurSNSiteFieldExt contract = field("contract_type", TurSEFieldType.STRING, true);
        contract.setFacetName("Contract");
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(city, contract,
                field("work_mode", TurSEFieldType.STRING, true), field("title", TurSEFieldType.TEXT, false),
                field("salary", TurSEFieldType.FLOAT, true)));

        assertThat(service.facetLabels("vagas", "pt_BR")).containsExactly(Map.entry("city", "Cidade"),
                Map.entry("contract_type", "Contract"), Map.entry("work_mode", "Work Mode"));
    }

    @Test
    @DisplayName("A value without accents is spelled as the site holds it, read from the words that named it")
    void groundsAValueInTheSiteSpelling() {
        siteWithFields();
        parserAnswers("""
                {"query":{"bool":{
                  "filter":[{"term":{"city":"sao paulo"}}],
                  "must":[{"match":{"_text_":"trabalho de meio periodo"}}]}}}""");
        siteHolds(Map.of("f0", List.of("São Paulo", "Campinas")));

        TurSNNLFilters result = service.parse("vagas", "pt_BR", TEXT);

        assertThat(result.available()).isTrue();
        assertThat(result.q()).isEqualTo("trabalho de meio periodo");
        assertThat(result.fq()).containsExactly("city:São Paulo");
        assertThat(result.filters()).singleElement().satisfies(filter -> {
            assertThat(filter.values()).containsExactly("São Paulo");
            assertThat(filter.sourceWords()).containsExactly("sao paulo");
            assertThat(filter.confidence()).isEqualTo(TurSNNLFilterService.LITERAL);
            assertThat(filter.grounded()).isTrue();
        });
        assertThat(result.dropped()).isEmpty();
    }

    @Test
    @DisplayName("A value the site does not hold, or a field it does not have, is dropped with the reason")
    void dropsWhatTheSiteDoesNotHold() {
        siteWithFields();
        parserAnswers("""
                {"query":{"bool":{"filter":[
                  {"terms":{"city":["sao paulo","Atlantis"]}},
                  {"term":{"metro":"perto"}}]}}}""");
        siteHolds(Map.of("f0", List.of("São Paulo")));

        TurSNNLFilters result = service.parse("vagas", "pt_BR", TEXT);

        assertThat(result.fq()).containsExactly("city:São Paulo");
        assertThat(result.dropped()).containsExactlyInAnyOrder(
                new TurSNNLFilters.Dropped("city", "Atlantis", TurSNNLFilterService.NOT_IN_SITE),
                new TurSNNLFilters.Dropped("metro", "perto", TurSNNLFilterService.UNKNOWN_FIELD));
    }

    @Test
    @DisplayName("A value the parser inferred, absent from the text, is kept with a lower confidence")
    void inferredValueHasLowerConfidence() {
        siteWithFields();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"term":{"contract":"clt"}}]}}}""");
        // T1359 — every facet is read before the parse: f0 = city, f1 = contract.
        siteHolds(Map.of("f1", List.of("CLT", "PJ")));

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "vaga com carteira assinada");

        assertThat(result.fq()).containsExactly("contract:CLT");
        assertThat(result.filters().get(0).confidence()).isEqualTo(TurSNNLFilterService.INFERRED);
        assertThat(result.filters().get(0).sourceWords()).isEmpty();
        assertThat(result.q()).isEqualTo("vaga com carteira assinada");
    }

    private void siteWithADateField() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("publication_date", TurSEFieldType.DATE, false)));
        service.setClockForTest(java.time.Clock.fixed(java.time.Instant.parse("2026-10-24T12:00:00Z"),
                java.time.ZoneOffset.UTC));
    }

    @Test
    @DisplayName("T1330 - 'amanhã' at 23:30 in São Paulo is the 20th there, already the 21st in UTC")
    void tomorrowIsTheSitesTomorrow() {
        siteWithADateField();
        service.setClockForTest(java.time.Clock.fixed(java.time.Instant.parse("2026-11-20T02:30:00Z"),
                java.time.ZoneOffset.UTC));
        String model = """
                {"query":{"bool":{
                  "filter":[{"range":{"publication_date":{"gte":"2026-11-21","lte":"2026-11-21"}}}],
                  "must":[{"match":{"_text_":"palestras"}}]}}}""";
        parserAnswers(model);
        assertThat(service.parse("vagas", "pt_BR", "palestras amanhã").fq())
                .as("no zone: the server's (UTC) tomorrow")
                .containsExactly("publication_date:[2026-11-21 TO 2026-11-21]");

        site.setTimeZone("America/Sao_Paulo");
        assertThat(service.parse("vagas", "pt_BR", "palestras amanhã").fq())
                .as("the site's tomorrow")
                .containsExactly("publication_date:[2026-11-20 TO 2026-11-20]");
    }

    @Test
    @DisplayName("T1117 - 'last year' is resolved against today, whatever range the model guessed")
    void lastYearIsResolvedAgainstToday() {
        siteWithADateField();
        parserAnswers("""
                {"query":{"bool":{
                  "filter":[{"range":{"publication_date":{"gte":"2022-01-01","lte":"2023-01-01"}}}],
                  "must":[{"match":{"_text_":"php"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "en_US", "posts about php from last year");

        assertThat(result.q()).isEqualTo("php");
        assertThat(result.fq()).containsExactly("publication_date:[2025-01-01 TO 2025-12-31]");
        assertThat(result.filters().get(0).sourceWords()).containsExactly("last year");
        assertThat(result.filters().get(0).confidence()).isEqualTo(TurSNNLFilterService.LITERAL);
        assertThat(result.dropped()).isEmpty();
    }

    @Test
    @DisplayName("T1117 - a resolved range keeps the instant form the model used")
    void resolvedRangeKeepsTheInstantForm() {
        siteWithADateField();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"range":{"publication_date":{"gte":"2023-05-01T00:00:00Z"}}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "en_US", "news this month");

        // T1360 — this month runs to its end
        assertThat(result.fq()).containsExactly("publication_date:[2026-10-01T00:00:00Z TO 2026-10-31T23:59:59Z]");
    }

    @Test
    @DisplayName("T1117 - a date range nothing in the text dates is dropped, never sent as a filter")
    void anUngroundedDateRangeIsDropped() {
        siteWithADateField();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"range":{"publication_date":{"gte":"2022-01-01","lte":"2023-01-01"}}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "en_US", "recent posts about php");

        assertThat(result.fq()).isEmpty();
        assertThat(result.dropped()).singleElement().satisfies(d -> {
            assertThat(d.field()).isEqualTo("publication_date");
            assertThat(d.reason()).isEqualTo(TurSNNLFilterService.UNGROUNDED_DATE);
        });
    }

    @Test
    @DisplayName("T1117 - a range the text dates by year is kept")
    void aRangeDatedByYearIsKept() {
        siteWithADateField();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"range":{"publication_date":{"gte":"2024-01-01","lte":"2024-12-31"}}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "en_US", "posts from 2024");

        assertThat(result.fq()).containsExactly("publication_date:[2024-01-01 TO 2024-12-31]");
        assertThat(result.filters().get(0).sourceWords()).containsExactly("from 2024");
        assertThat(result.filters().get(0).confidence()).isEqualTo(TurSNNLFilterService.INFERRED);
        assertThat(result.q()).isEqualTo("posts");
    }

    @Test
    @DisplayName("T1211 - a year range spelled on both ends leaves neither year nor its connector in q")
    void aYearRangeOnBothEndsIsOneSpan() {
        siteWithADateField();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"range":{"publication_date":{"gte":"2020-01-01","lte":"2024-12-31"}}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "noticias entre 2020 e 2024 sobre php");

        assertThat(result.filters().get(0).sourceWords()).containsExactly("entre 2020 e 2024");
        assertThat(result.q()).isEqualTo("noticias sobre php");
    }

    private void siteWithAPrice() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("price", TurSEFieldType.FLOAT, false)));
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "{0}")
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', textBlock = """
            a two-person tent under $400        | 400  |      | under $400        | a two-person tent
            a two-person tent below 400 dollars | 400  |      | below 400 dollars | a two-person tent
            tents for $400 or less              | 400  |      | $400 or less      | tents for
            tents over $1,200                   |      | 1200 | over $1,200       | tents
            barraca ate R$ 2.200                | 2200 |      | ate R$ 2.200      | barraca
            barraca até R$ 2.200                | 2200 |      | até R$ 2.200      | barraca
            salario acima de 3000 reais         |      | 3000 | acima de 3000 reais | salario
            barraca abaixo de 1.500,50 reais    | 1500.5 |    | abaixo de 1.500,50 reais | barraca
            tent between $100 and $200          | 200  | 100  | between $100 and $200 | tent
            barraca de 100 a 200 reais          | 200  | 100  | de 100 a 200 reais | barraca
            """)
    @DisplayName("T1211 - a range's bound phrase, currency and formatted amount all leave q")
    void aRangeTakesItsBoundPhraseAndCurrency(String text, String lte, String gte, String words, String q) {
        siteWithAPrice();
        StringBuilder bounds = new StringBuilder();
        if (gte != null) {
            bounds.append("\"gte\":").append(gte);
        }
        if (lte != null) {
            bounds.append(bounds.isEmpty() ? "" : ",").append("\"lte\":").append(lte);
        }
        parserAnswers("{\"query\":{\"bool\":{\"filter\":[{\"range\":{\"price\":{" + bounds + "}}}]}}}");

        TurSNNLFilters result = service.parse("vagas", "en_US", text);

        assertThat(result.filters()).singleElement().satisfies(filter -> {
            assertThat(filter.sourceWords()).containsExactly(words);
            assertThat(filter.confidence()).isEqualTo(TurSNNLFilterService.LITERAL);
        });
        assertThat(result.q()).isEqualTo(q);
    }

    @Test
    @DisplayName("T1211 - an amount the text does not spell keeps q whole, and is inferred")
    void anUnspelledAmountLeavesQWhole() {
        siteWithAPrice();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"range":{"price":{"lte":400}}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "en_US", "a tent under four hundred dollars");

        assertThat(result.filters().get(0).sourceWords()).isEmpty();
        assertThat(result.filters().get(0).confidence()).isEqualTo(TurSNNLFilterService.INFERRED);
        assertThat(result.q()).isEqualTo("a tent under four hundred dollars");
    }

    @Test
    @DisplayName("T1211 - a repeated amount leaves q only where the bound phrase reads it")
    void aRepeatedAmountIsTakenAtItsOwnPosition() {
        siteWithAPrice();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"range":{"price":{"lte":400}}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "en_US", "a 400 lumen lantern under $400");

        assertThat(result.filters().get(0).sourceWords()).containsExactly("under $400");
        assertThat(result.q()).isEqualTo("a 400 lumen lantern");
    }

    @Test
    @DisplayName("T1211 - leftover removes source words once, at their own position")
    void leftoverRemovesEachSpanOnce() {
        TurSNNLFilters.Filter city = new TurSNNLFilters.Filter("city", List.of("Santos"), null, null,
                List.of("santos"), 1, true);

        assertThat(TurSNNLFilterService.leftover("vagas em santos perto de santos", List.of(city)))
                .isEqualTo("vagas em perto de santos");
    }

    @Test
    @DisplayName("A range becomes a range filter query, open where the request names no bound")
    void rangeBecomesARangeFilterQuery() {
        siteWithFields();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"range":{"salary":{"gte":3000}}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "salario acima de 3000 reais");

        assertThat(result.fq()).containsExactly("salary:[3000 TO *]");
        assertThat(result.filters().get(0).isRange()).isTrue();
        assertThat(result.filters().get(0).sourceWords()).containsExactly("acima de 3000 reais");
        assertThat(result.q()).isEqualTo("salario");
        // T1359 — one read, of the facet values the prompt lists; a range needs no other.
        verify(dslSearchService).search(any(), any(), any());
    }

    @Test
    @DisplayName("When the engine lists no values, the parsed values pass unchecked and say so")
    void valuesPassUncheckedWhenTheEngineListsNone() {
        siteWithFields();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"term":{"city":"sao paulo"}}]}}}""");
        when(dslSearchService.search(eq("vagas"), eq("pt_BR"), any())).thenReturn(Optional.empty());

        TurSNNLFilters result = service.parse("vagas", "pt_BR", TEXT);

        assertThat(result.fq()).containsExactly("city:sao paulo");
        assertThat(result.filters().get(0).grounded()).isFalse();
        assertThat(result.filters().get(0).confidence()).isEqualTo(TurSNNLFilterService.UNCHECKED);
        assertThat(result.q()).isEqualTo("trabalho de meio periodo em");
    }

    @Test
    @DisplayName("Without a locale, the site's first locale parses and searches")
    void defaultsToTheSitesFirstLocale() {
        siteWithFields();
        TurSNSiteLocale ptBr = new TurSNSiteLocale();
        ptBr.setLanguage(Locale.of("pt", "BR"));
        when(turSNSiteLocaleRepository.findByTurSNSiteOrderByPositionAsc(site)).thenReturn(List.of(ptBr));
        parserAnswers("""
                {"query":{"bool":{"filter":[{"term":{"city":"sao paulo"}}]}}}""");
        siteHolds(Map.of("f0", List.of("São Paulo")));

        service.parse("vagas", null, TEXT);

        ArgumentCaptor<TurNLFacetParser.ParseRequest> request = ArgumentCaptor
                .forClass(TurNLFacetParser.ParseRequest.class);
        verify(parser).parse(request.capture());
        assertThat(request.getValue().locale()).isEqualTo("pt_BR");
    }

    @Test
    @DisplayName("No LLM means nothing is filtered and the text stays the query")
    void unavailableWithoutAParser() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(false);

        TurSNNLFilters result = service.parse("vagas", "pt_BR", TEXT);

        assertThat(result.available()).isFalse();
        assertThat(result.q()).isEqualTo(TEXT);
        assertThat(result.fq()).isEmpty();
    }

    @Test
    @DisplayName("A blank text is refused, and an unknown site is not found")
    void refusesBlankTextAndUnknownSite() {
        assertThatThrownBy(() -> service.parse("vagas", null, " "))
                .isInstanceOf(IllegalArgumentException.class);
        when(turSNSiteRepository.findByNameIgnoreCase("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.parse("nope", null, TEXT))
                .isInstanceOf(TurNotFoundException.class);
    }

    @Test
    @DisplayName("A field that is not a facet has no value list, so its value passes unchecked without a search")
    void nonFacetFieldPassesUnchecked() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1))
                .thenReturn(List.of(field("company", TurSEFieldType.STRING, false)));
        parserAnswers("""
                {"query":{"bool":{"filter":[{"term":{"company":"acme"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "vagas na acme");

        assertThat(result.fq()).containsExactly("company:acme");
        assertThat(result.filters().get(0).grounded()).isFalse();
        verify(dslSearchService, never()).search(any(), any(), any());
    }

    private void modelCatalog() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("type", TurSEFieldType.STRING, false),
                field("kind", TurSEFieldType.STRING, true),
                field("capabilities", TurSEFieldType.STRING, true)));
    }

    @Test
    @DisplayName("T1277 - a value on a field that cannot hold it moves to the one facet that does")
    void aValueMovesToTheFacetThatHoldsIt() {
        modelCatalog();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"term":{"type":"chat"}}]}}}""");
        // type is no facet, so nothing is read until the value turns out homeless;
        // then kind (f0) and capabilities (f1) are read in one search.
        siteHolds(Map.of("f0", List.of("CHAT", "EMBEDDING"), "f1", List.of("vision", "tools")));

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "Which chat model scores highest?");

        assertThat(result.fq()).containsExactly("kind:CHAT");
        assertThat(result.filters().get(0).grounded()).isTrue();
        assertThat(result.filters().get(0).sourceWords()).containsExactly("chat");
        assertThat(result.dropped()).isEmpty();
    }

    @Test
    @DisplayName("T1277 - a field the site does not declare loses its value to the facet that holds it")
    void anUnknownFieldsValueMovesToItsFacet() {
        modelCatalog();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"term":{"modality":"Vision"}}]}}}""");
        siteHolds(Map.of("f0", List.of("CHAT"), "f1", List.of("vision", "tools")));

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "Which models support vision?");

        assertThat(result.fq()).containsExactly("capabilities:vision");
        assertThat(result.dropped()).isEmpty();
    }

    @Test
    @DisplayName("T1277 - a value no facet holds, or two facets hold, stays where the parse put it")
    void anAmbiguousOrUnheldValueStays() {
        modelCatalog();
        parserAnswers("""
                {"query":{"bool":{"filter":[{"term":{"type":"chat"}},{"term":{"modality":"audio"}}]}}}""");
        siteHolds(Map.of("f0", List.of("CHAT"), "f1", List.of("chat")));

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "chat models with audio");

        assertThat(result.fq()).containsExactly("type:chat");
        assertThat(result.filters().get(0).grounded()).isFalse();
        assertThat(result.dropped()).containsExactly(
                new TurSNNLFilters.Dropped("modality", "audio", TurSNNLFilterService.UNKNOWN_FIELD));
    }

    @Test
    @DisplayName("Grounded filters go back to a query: terms, ranges as numbers, text fields as matches, exclusions kept")
    void groundedFiltersGoBackToAQuery() {
        var schema = List.of(
                new com.viglet.turing.sn.dsl.eval.TurNLFacetField("city", TurSEFieldType.STRING, true, null),
                new com.viglet.turing.sn.dsl.eval.TurNLFacetField("title", TurSEFieldType.TEXT, false, null));
        TurSNNLFilters filters = new TurSNNLFilters(true, "t", "estagio", List.of(
                new TurSNNLFilters.Filter("city", List.of("São Paulo", "Campinas"), null, null, List.of(), 1, true),
                new TurSNNLFilters.Filter("title", List.of("analista"), null, null, List.of(), 1, false),
                new TurSNNLFilters.Filter("salary", List.of(), "3000", null, List.of(), 1, false)),
                List.of(), List.of());
        var exclusion = new com.viglet.turing.searchengine.dsl.TurDslQuery.Term("contract", "PJ");
        var parsed = new com.viglet.turing.searchengine.dsl.TurDslQuery.Bool(List.of(), List.of(), List.of(exclusion),
                List.of(), null);

        var query = (com.viglet.turing.searchengine.dsl.TurDslQuery.Bool) TurSNNLFilterService.toQuery(filters, schema,
                parsed);

        assertThat(query.filter()).containsExactly(
                new com.viglet.turing.searchengine.dsl.TurDslQuery.Terms("city", List.of("São Paulo", "Campinas")),
                new com.viglet.turing.searchengine.dsl.TurDslQuery.Range("salary", 3000L, null, null, null));
        assertThat(query.must()).containsExactly(
                new com.viglet.turing.searchengine.dsl.TurDslQuery.Match("title", "analista", null),
                new com.viglet.turing.searchengine.dsl.TurDslQuery.Match(TurSNNLFilterService.TEXT_FIELD, "estagio", null));
        assertThat(query.mustNot()).containsExactly(exclusion);
    }

    @Test
    @DisplayName("Source words match on word boundaries, without case or accents, as the text spells them")
    void sourceWordsKeepTheTextSpelling() {
        assertThat(TurSNNLFilterService.sourceWords("Vagas em SÃO PAULO hoje", "sao paulo"))
                .contains("SÃO PAULO");
        assertThat(TurSNNLFilterService.sourceWords("salvador", "salva")).isEmpty();
        assertThat(TurSNNLFilterService.canonical("CAMPINAS", java.util.Set.of("Campinas")))
                .isEqualTo("Campinas");
    }

    private void eventSite() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("tipo_evento", TurSEFieldType.STRING, true),
                field("title", TurSEFieldType.TEXT, false)));
        siteHolds(Map.of("f0", List.of("Palestra", "Debate")));
    }

    /** T1360 — an agenda: events start on data_inicio and end on data_fim. */
    private void agendaSite(boolean spansTwoFields) {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        TurSNSiteFieldExt start = field("data_inicio", TurSEFieldType.DATE, true);
        start.setId("start-id");
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(start,
                field("data_fim", TurSEFieldType.DATE, false), field("title", TurSEFieldType.TEXT, false)));
        when(customFacetCatalog.customFacets(site)).thenReturn(List.of(new com.viglet.turing.searchengine.spi
                .TurSECustomFacet("Quando", 1, List.of(), "start-id", spansTwoFields ? "data_fim" : null, null,
                        null)));
        parserAnswers("""
                {"query":{"bool":{"must":[{"match":{"_text_":"o que tem"}}]}}}""");
    }

    @Test
    @DisplayName("T1360: a date phrase with no range lands on the date facet, as the overlap its span applies")
    void aDatePhraseLandsOnTheDateFacetAsAnOverlap() {
        agendaSite(true);
        // Sunday night in São Paulo is already Monday in UTC: the site's week is the one ending today.
        site.setTimeZone("America/Sao_Paulo");
        service.setClockForTest(java.time.Clock.fixed(java.time.Instant.parse("2026-11-30T01:30:00Z"),
                java.time.ZoneOffset.UTC));

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "o que tem esta semana");

        assertThat(result.fq()).containsExactly("data_inicio:[* TO 2026-11-29T23:59:59Z]",
                "data_fim:[2026-11-23T00:00:00Z TO *]");
        assertThat(result.filters()).allSatisfy(filter -> {
            assertThat(filter.sourceWords()).containsExactly("esta semana");
            assertThat(filter.confidence()).isEqualTo(TurSNNLFilterService.LITERAL);
        });
        // T1361 — "o que tem" is only stop words: it would narrow nothing, so q is empty
        assertThat(result.q()).isEmpty();
    }

    @Test
    @DisplayName("T1361: the filters' words leave the parse's free text, and an end-field range is the overlap")
    void theSentencesOwnQAndFqCanRun() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        TurSNSiteFieldExt start = field("data_inicio", TurSEFieldType.DATE, true);
        start.setId("start-id");
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("tipo_evento", TurSEFieldType.STRING, true), start,
                field("data_fim", TurSEFieldType.DATE, false), field("title", TurSEFieldType.TEXT, false)));
        when(customFacetCatalog.customFacets(site)).thenReturn(List.of(new com.viglet.turing.searchengine.spi
                .TurSECustomFacet("Quando", 1, List.of(), "start-id", "data_fim", null, null)));
        siteHolds(Map.of("f0", List.of("Palestra", "Debate")));
        service.setClockForTest(java.time.Clock.fixed(java.time.Instant.parse("2026-11-27T12:00:00Z"),
                java.time.ZoneOffset.UTC));
        parserAnswers("""
                {"query":{"bool":{
                  "filter":[{"term":{"tipo_evento":"Palestra"}},
                            {"range":{"data_fim":{"gte":"2026-11-01","lte":"2026-11-30"}}}],
                  "must":[{"match":{"_text_":"palestras sobre democracia em novembro"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "palestras sobre democracia em novembro");

        assertThat(result.fq()).containsExactly("tipo_evento:Palestra", "data_inicio:[* TO 2026-11-30]",
                "data_fim:[2026-11-01 TO *]");
        assertThat(result.q()).isEqualTo("sobre democracia");
    }

    @Test
    @DisplayName("T1361: a word naming the value its field already filters leaves q, with no second filter")
    void aWordNamingTheFilteredValueLeavesQ() {
        eventSite();
        parserAnswers("""
                {"query":{"bool":{
                  "filter":[{"term":{"tipo_evento":"Palestra"}}],
                  "must":[{"match":{"_text_":"palestras sobre democracia"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "palestras sobre democracia");

        assertThat(result.fq()).containsExactly("tipo_evento:Palestra");
        assertThat(result.q()).isEqualTo("sobre democracia");
    }

    @Test
    @DisplayName("T1360: a date facet on one field takes the phrase as a plain range, across a month's end")
    void aDatePhraseLandsAsARangeWithoutAnEndField() {
        agendaSite(false);
        service.setClockForTest(java.time.Clock.fixed(java.time.Instant.parse("2026-12-02T12:00:00Z"),
                java.time.ZoneOffset.UTC));

        assertThat(service.parse("vagas", "pt_BR", "o que tem esta semana").fq())
                .containsExactly("data_inicio:[2026-11-30T00:00:00Z TO 2026-12-06T23:59:59Z]");
    }

    @Test
    @DisplayName("T1360: with two date facets and nothing to choose between them, a phrase is not guessed")
    void twoDateFacetsAreNotGuessed() {
        when(turSNSiteRepository.findByNameIgnoreCase("vagas")).thenReturn(Optional.of(site));
        when(parser.isAvailable()).thenReturn(true);
        when(turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(
                field("published", TurSEFieldType.DATE, true), field("updated", TurSEFieldType.DATE, true)));
        parserAnswers("""
                {"query":{"bool":{"must":[{"match":{"_text_":"news"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "en_US", "news today");

        assertThat(result.filters()).isEmpty();
        assertThat(result.q()).isEqualTo("news");
    }

    @Test
    @DisplayName("T1359: a noun the parse left in q that names a facet value becomes that filter")
    void aNounInQBecomesItsFacetFilter() {
        eventSite();
        parserAnswers("""
                {"query":{"bool":{"must":[{"match":{"_text_":"palestras sobre democracia"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "palestras sobre democracia");

        assertThat(result.q()).isEqualTo("sobre democracia");
        assertThat(result.fq()).containsExactly("tipo_evento:Palestra");
        assertThat(result.filters()).singleElement().satisfies(filter -> {
            assertThat(filter.values()).containsExactly("Palestra");
            assertThat(filter.sourceWords()).containsExactly("palestras");
            assertThat(filter.confidence()).isEqualTo(TurSNNLFilterService.INFERRED);
            assertThat(filter.grounded()).isTrue();
        });
        ArgumentCaptor<TurNLFacetParser.ParseRequest> request = ArgumentCaptor.captor();
        verify(parser).parse(request.capture());
        assertThat(request.getValue().values()).containsExactly(Map.entry("tipo_evento", List.of("Palestra",
                "Debate")));
        // the values were read once, and grounding reused them
        verify(dslSearchService).search(eq("vagas"), eq("pt_BR"), any());
    }

    @Test
    @DisplayName("T1359: a value used as the topic of another noun stays in q")
    void aTopicStaysInQ() {
        eventSite();
        parserAnswers("""
                {"query":{"bool":{"must":[{"match":{"_text_":"livros sobre palestras"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "livros sobre palestras");

        assertThat(result.q()).isEqualTo("livros sobre palestras");
        assertThat(result.filters()).isEmpty();
    }

    @Test
    @DisplayName("T1359: a field the parse already filtered is not widened by a noun in q")
    void aParsedFieldIsNotWidened() {
        eventSite();
        parserAnswers("""
                {"query":{"bool":{
                  "filter":[{"term":{"tipo_evento":"Palestra"}}],
                  "must":[{"match":{"_text_":"debates"}}]}}}""");

        TurSNNLFilters result = service.parse("vagas", "pt_BR", "palestras e debates");

        assertThat(result.fq()).containsExactly("tipo_evento:Palestra");
        assertThat(result.q()).isEqualTo("debates");
    }
}
