/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.dsl.eval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.field.TurSEFieldType;

/**
 * T1359 — the parse prompt lists the values each facet field holds, so a noun of
 * the query can be mapped to one.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurLlmNLFacetParserSchemaTest {

    private static final TurNLFacetField TYPE = new TurNLFacetField("tipo_evento", TurSEFieldType.STRING, true,
            "Kind of event");
    private static final TurNLFacetField TITLE = new TurNLFacetField("title", TurSEFieldType.TEXT, false, null);

    @Test
    @DisplayName("A facet field lists the values the site holds; a field without values lists none")
    void listsTheValuesOfEachFacet() {
        String schema = TurLlmNLFacetParser.renderSchema(List.of(TYPE, TITLE),
                Map.of("tipo_evento", List.of("Palestra", "Debate", "Lançamento")));

        assertThat(schema)
                .contains("- tipo_evento: string [facet] - Kind of event; values: Palestra | Debate | Lançamento")
                .endsWith("- title: text");
    }

    @Test
    @DisplayName("At most PROMPT_VALUES values of one field go into the prompt")
    void capsTheValuesPerField() {
        List<String> many = IntStream.range(0, TurLlmNLFacetParser.PROMPT_VALUES + 5).mapToObj(i -> "v" + i)
                .toList();

        String schema = TurLlmNLFacetParser.renderSchema(List.of(TYPE), Map.of("tipo_evento", many));

        assertThat(schema).contains("v" + (TurLlmNLFacetParser.PROMPT_VALUES - 1))
                .doesNotContain("v" + TurLlmNLFacetParser.PROMPT_VALUES);
    }

    @Test
    @DisplayName("The four-argument request carries no values, never null")
    void theShortRequestCarriesNoValues() {
        assertThat(new TurNLFacetParser.ParseRequest("site", "pt_BR", "q", List.of(TYPE)).values()).isEmpty();
        assertThat(new TurNLFacetParser.ParseRequest("site", "pt_BR", "q", List.of(TYPE), null).values()).isEmpty();
    }
}
