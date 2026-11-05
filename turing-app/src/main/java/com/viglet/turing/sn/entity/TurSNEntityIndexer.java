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

package com.viglet.turing.sn.entity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.viglet.turing.client.sn.job.TurSNJobAttributeSpec;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.genai.TurDefaultAgentResolver;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.repository.agent.TurAIAgentRepository;
import com.viglet.turing.persistence.repository.sn.genai.TurSNSiteGenAiRepository;
import com.viglet.turing.sn.field.TurSNFieldProvisioner;
import com.viglet.turing.sn.kb.TurMicrothesaurusDictionaryService;
import com.viglet.turing.sn.kb.TurRecognitionDictionary;
import com.viglet.turing.sn.kb.TurRecognizedTerm;
import com.viglet.turing.sn.kb.TurTextNormalizer;

import lombok.extern.slf4j.Slf4j;

/**
 * T1228 (Block AQ) — the index-time enricher that fills the entity facets: the
 * people, organisations and places a document names, found by the site's LLM, so
 * a site can be browsed by them without a vocabulary that lists them. Wired into
 * {@code TurSNProcessQueue.index(...)} beside the microthesaurus enricher (T672),
 * which keeps recognising the terms a vocabulary does list.
 *
 * <ol>
 * <li>Returns at once unless the site's GenAI binding opted in
 * ({@code entityExtraction}): a site that never did indexes as before and spends
 * no tokens.</li>
 * <li>The text is the title, description and body, cut to
 * {@value #MAX_TEXT_CHARS} characters; its hash (with the model's id) keys a
 * bounded cache, so a re-index of unchanged content asks the model nothing.</li>
 * <li>Every value the model returns is kept only when the text holds it (accent
 * and case folded), so a name the model invented never becomes a facet.</li>
 * <li>A value the site's Thesaurus recognises whole is written as the term's
 * preferred label, so {@code USED_FOR} variants of one person fold into one
 * facet value.</li>
 * </ol>
 *
 * <p><strong>Defensive</strong>: any failure is logged and the document is indexed
 * without entities, never blocked.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Slf4j
@Component
public class TurSNEntityIndexer {

    /** The facet field of each entity kind. */
    public static final String PERSON_FIELD = "entity_person";
    public static final String ORGANIZATION_FIELD = "entity_organization";
    public static final String PLACE_FIELD = "entity_place";

    static final int MAX_TEXT_CHARS = 12_000;
    private static final int CACHE_SIZE = 2_000;
    private static final int MAX_VALUES_PER_KIND = 30;

    private final TurSNSiteGenAiRepository turSNSiteGenAiRepository;
    private final TurAIAgentRepository turAIAgentRepository;
    private final TurDefaultAgentResolver defaultAgentResolver;
    private final TurSNEntityExtractor extractor;
    private final TurMicrothesaurusDictionaryService dictionaryService;
    private final TurSNFieldProvisioner fieldProvisioner;
    private final Map<String, TurSNEntities> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, TurSNEntities> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    public TurSNEntityIndexer(TurSNSiteGenAiRepository turSNSiteGenAiRepository,
            TurAIAgentRepository turAIAgentRepository, TurDefaultAgentResolver defaultAgentResolver,
            TurSNEntityExtractor extractor, TurMicrothesaurusDictionaryService dictionaryService,
            TurSNFieldProvisioner fieldProvisioner) {
        this.turSNSiteGenAiRepository = turSNSiteGenAiRepository;
        this.turAIAgentRepository = turAIAgentRepository;
        this.defaultAgentResolver = defaultAgentResolver;
        this.extractor = extractor;
        this.dictionaryService = dictionaryService;
        this.fieldProvisioner = fieldProvisioner;
    }

    /** Adds the document's entities to {@code attributes}; no-op unless the site opted in. */
    public void enrich(TurSNSite turSNSite, Locale locale, Map<String, Object> attributes) {
        if (turSNSite == null || attributes == null || turSNSite.getTurSNSiteGenAi() == null) {
            return;
        }
        try {
            Optional<TurSNSiteGenAi> genAi = turSNSiteGenAiRepository.findById(turSNSite.getTurSNSiteGenAi().getId())
                    .filter(TurSNSiteGenAi::isEntityExtraction);
            if (genAi.isEmpty()) {
                return;
            }
            String text = StringUtils.left(extractText(turSNSite, attributes), MAX_TEXT_CHARS);
            if (StringUtils.isBlank(text)) {
                return;
            }
            TurLLMInstance llm = llm(genAi.get());
            if (llm == null) {
                log.warn("[T1228] site '{}' extracts entities but its agent has no LLM; skipped",
                        turSNSite.getName());
                return;
            }
            String key = llm.getId() + ":" + sha256(text);
            TurSNEntities raw = cache.get(key);
            if (raw == null) {
                raw = extractor.extract(turSNSite.getName(), llm, text);
                cache.put(key, raw);
            }
            write(turSNSite, locale, attributes, text, raw);
        } catch (Exception e) {
            log.warn("[T1228] entity extraction failed on site '{}': {}", turSNSite.getName(), e.getMessage());
        }
    }

    private void write(TurSNSite turSNSite, Locale locale, Map<String, Object> attributes, String text,
            TurSNEntities raw) {
        TurRecognitionDictionary dictionary = dictionaryService.getDictionary(turSNSite, locale);
        String foldedText = TurTextNormalizer.fold(text);
        put(turSNSite, attributes, PERSON_FIELD, kept(raw.people(), foldedText, dictionary));
        put(turSNSite, attributes, ORGANIZATION_FIELD, kept(raw.organizations(), foldedText, dictionary));
        put(turSNSite, attributes, PLACE_FIELD, kept(raw.places(), foldedText, dictionary));
    }

    private void put(TurSNSite turSNSite, Map<String, Object> attributes, String field, List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        fieldProvisioner.ensureField(turSNSite, TurSNJobAttributeSpec.builder()
                .name(field)
                .type(TurSEFieldType.STRING)
                .description("T1228 entities at import: named by the site's LLM, checked against the text.")
                .mandatory(false)
                .multiValued(true)
                .facet(true)
                .build());
        attributes.put(field, values);
    }

    /**
     * The values the text holds, each as the Thesaurus's preferred label when it
     * recognises the value whole, de-duplicated in order.
     */
    static List<String> kept(Collection<String> values, String foldedText, TurRecognitionDictionary dictionary) {
        Set<String> kept = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = StringUtils.normalizeSpace(value);
            if (StringUtils.isBlank(trimmed) || !occursIn(foldedText, trimmed)) {
                continue;
            }
            kept.add(preferred(trimmed, dictionary));
            if (kept.size() >= MAX_VALUES_PER_KIND) {
                break;
            }
        }
        return new ArrayList<>(kept);
    }

    /** Whether the folded text holds {@code value} as whole words. */
    static boolean occursIn(String foldedText, String value) {
        String needle = TurTextNormalizer.fold(value);
        int from = 0;
        while (true) {
            int at = foldedText.indexOf(needle, from);
            if (at < 0) {
                return false;
            }
            int end = at + needle.length();
            boolean left = at == 0 || !TurTextNormalizer.isWordChar(foldedText.charAt(at - 1));
            boolean right = end == foldedText.length() || !TurTextNormalizer.isWordChar(foldedText.charAt(end));
            if (left && right) {
                return true;
            }
            from = at + 1;
        }
    }

    /** The Thesaurus term's label when the dictionary recognises {@code value} as exactly one term. */
    private static String preferred(String value, TurRecognitionDictionary dictionary) {
        if (dictionary == null || dictionary.isEmpty()) {
            return value;
        }
        Collection<TurRecognizedTerm> terms = dictionary.recognize(value);
        return terms.size() == 1 ? terms.iterator().next().label() : value;
    }

    /** The site agent's LLM (lowest id, as agent chat picks it), else the Default AI Agent's. */
    private TurLLMInstance llm(TurSNSiteGenAi genAi) {
        TurAIAgent agent = genAi.getTurAIAgent() == null ? defaultAgentResolver.resolveDefaultAgent()
                : turAIAgentRepository.findById(genAi.getTurAIAgent().getId()).orElse(null);
        if (agent == null || agent.getLlmInstances() == null) {
            return null;
        }
        return agent.getLlmInstances().stream().min(Comparator.comparing(TurLLMInstance::getId)).orElse(null);
    }

    private static String extractText(TurSNSite turSNSite, Map<String, Object> attributes) {
        List<String> parts = new ArrayList<>();
        append(parts, attributes.get(StringUtils.defaultIfBlank(turSNSite.getDefaultTitleField(),
                TurSNFieldName.TITLE)));
        append(parts, attributes.get(StringUtils.defaultIfBlank(turSNSite.getDefaultDescriptionField(),
                TurSNFieldName.ABSTRACT)));
        append(parts, attributes.get(StringUtils.defaultIfBlank(turSNSite.getDefaultTextField(),
                TurSNFieldName.TEXT)));
        return String.join("\n", parts).strip();
    }

    private static void append(List<String> parts, Object value) {
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> append(parts, item));
        } else if (value != null && StringUtils.isNotBlank(value.toString())) {
            parts.add(value.toString().strip());
        }
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
