/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.api.sn.genai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.api.exception.TurNotFoundException;
import com.viglet.turing.api.sn.genai.TurSNSitePersonaAPI.TurSNSitePersonaOption;
import com.viglet.turing.api.sn.genai.TurSNSitePersonaContentFitAPI.ContentFitRequest;
import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.genai.persona.fit.TurContentFitResult;
import com.viglet.turing.genai.persona.fit.TurPersonaContentFitEvaluator;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.persona.TurPersona;
import com.viglet.turing.persistence.model.persona.TurPersonaKind;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.sn.TurSNSearchProcess;

/**
 * T1143 — the audience personas a site lists are exactly the ids its content-fit
 * endpoint accepts.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@ExtendWith(MockitoExtension.class)
class TurSNSitePersonaAPITest {

    private static final String SITE = "turing-docs";

    @Mock
    private TurSNSearchProcess turSNSearchProcess;
    @Mock
    private TurDefaultAgentResolver turDefaultAgentResolver;
    @Mock
    private TurPersonaContentFitEvaluator evaluator;

    private TurSNSitePersonaAPI api;
    private TurSNSitePersonaContentFitAPI contentFit;
    private TurSNSiteGenAi genAi;

    @BeforeEach
    void setUp() {
        TurSNSiteAudiencePersonas personas = new TurSNSiteAudiencePersonas(turSNSearchProcess,
                turDefaultAgentResolver);
        api = new TurSNSitePersonaAPI(personas);
        contentFit = new TurSNSitePersonaContentFitAPI(personas, evaluator);

        TurAIAgent agent = new TurAIAgent();
        agent.setPersonas(Set.of(persona("p-zeta", "Zeta reader", TurPersonaKind.AUDIENCE),
                persona("p-alpha", "Alpha reader", TurPersonaKind.BOTH),
                persona("p-speaker", "Brand voice", TurPersonaKind.SPEAKER)));

        genAi = new TurSNSiteGenAi();
        TurSNSite site = new TurSNSite();
        site.setTurSNSiteGenAi(genAi);
        lenient().when(turSNSearchProcess.getSNSite(SITE)).thenReturn(Optional.of(site));
        lenient().when(turDefaultAgentResolver.resolveEffectiveAgent(genAi)).thenReturn(agent);
    }

    @Test
    void listsOnlyAudiencePersonasByName() {
        assertThat(api.list(SITE)).containsExactly(
                new TurSNSitePersonaOption("p-alpha", "Alpha reader", "About Alpha reader"),
                new TurSNSitePersonaOption("p-zeta", "Zeta reader", "About Zeta reader"));
    }

    @Test
    void everyListedIdIsAcceptedByContentFit() {
        when(evaluator.evaluateText(any(), eq("text"), any(), any(), eq(false)))
                .thenReturn(new TurContentFitResult(0.5, "ok", List.of(), List.of(),
                        0.5, null, true, null, false, null, "text"));

        for (TurSNSitePersonaOption option : api.list(SITE)) {
            assertThat(contentFit.evaluate(SITE, option.id(), new ContentFitRequest("text", null))
                    .personaId()).isEqualTo(option.id());
        }
    }

    @Test
    void aSiteWithNoAgentListsNothing() {
        when(turDefaultAgentResolver.resolveEffectiveAgent(genAi)).thenReturn(null);
        assertThat(api.list(SITE)).isEmpty();
    }

    @Test
    void anUnknownSiteIsNotFound() {
        when(turSNSearchProcess.getSNSite("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.list("ghost")).isInstanceOf(TurNotFoundException.class);
    }

    private static TurPersona persona(String id, String name, TurPersonaKind kind) {
        TurPersona p = new TurPersona();
        p.setId(id);
        p.setName(name);
        p.setDescription("About " + name);
        p.setPersonaKind(kind);
        return p;
    }
}
