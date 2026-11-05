package com.viglet.turing.sn.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

/**
 * T1228 — entities at import: only for a site that opted in, only names the text
 * holds, a Thesaurus variant folded into its preferred label, and an unchanged
 * document never asks the model twice.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
class TurSNEntityIndexerTest {

    private static final String TEXT = "Bruno Bosso and Antonio Prado spoke at USP in São Paulo.";

    private final TurSNSiteGenAiRepository genAiRepository = mock(TurSNSiteGenAiRepository.class);
    private final TurAIAgentRepository agentRepository = mock(TurAIAgentRepository.class);
    private final TurDefaultAgentResolver agentResolver = mock(TurDefaultAgentResolver.class);
    private final TurSNEntityExtractor extractor = mock(TurSNEntityExtractor.class);
    private final TurMicrothesaurusDictionaryService dictionaryService = mock(TurMicrothesaurusDictionaryService.class);
    private final TurSNFieldProvisioner provisioner = mock(TurSNFieldProvisioner.class);
    private final TurSNEntityIndexer indexer = new TurSNEntityIndexer(genAiRepository, agentRepository,
            agentResolver, extractor, dictionaryService, provisioner);

    private final TurSNSite site = new TurSNSite();
    private final TurSNSiteGenAi genAi = new TurSNSiteGenAi();
    private final TurLLMInstance llm = new TurLLMInstance();

    @BeforeEach
    void setUp() {
        genAi.setId("genai-1");
        site.setName("portal");
        site.setTurSNSiteGenAi(genAi);
        llm.setId("llm-1");
        TurAIAgent agent = new TurAIAgent();
        agent.setLlmInstances(Set.of(llm));
        when(genAiRepository.findById("genai-1")).thenReturn(Optional.of(genAi));
        when(agentResolver.resolveDefaultAgent()).thenReturn(agent);
        when(dictionaryService.getDictionary(any(), any())).thenReturn(TurRecognitionDictionary.builder()
                .add("Bosso, Bruno", false, false, new TurRecognizedTerm("t1", "Bruno Bosso", List.of("Bruno Bosso")))
                .add("Bruno Bosso", false, false, new TurRecognizedTerm("t1", "Bruno Bosso", List.of("Bruno Bosso")))
                .build());
    }

    private static Map<String, Object> document() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("title", "Lecture");
        attributes.put("text", TEXT);
        return attributes;
    }

    @Test
    void aSiteThatNeverOptedInIsLeftAloneAndSpendsNothing() {
        Map<String, Object> attributes = document();

        indexer.enrich(site, Locale.US, attributes);

        verify(extractor, never()).extract(anyString(), any(), anyString());
        assertThat(attributes).doesNotContainKeys(TurSNEntityIndexer.PERSON_FIELD, TurSNEntityIndexer.PLACE_FIELD);
    }

    @Test
    void writesTheNamesTheTextHoldsAndDropsInventedOnes() {
        genAi.setEntityExtraction(true);
        when(extractor.extract(eq("portal"), eq(llm), anyString())).thenReturn(new TurSNEntities(
                List.of("Antonio Prado", "Bruno Bosso", "Carla Mendes"), List.of("USP"), List.of("São Paulo")));
        Map<String, Object> attributes = document();

        indexer.enrich(site, Locale.US, attributes);

        assertThat(attributes.get(TurSNEntityIndexer.PERSON_FIELD))
                .isEqualTo(List.of("Antonio Prado", "Bruno Bosso"));
        assertThat(attributes.get(TurSNEntityIndexer.ORGANIZATION_FIELD)).isEqualTo(List.of("USP"));
        assertThat(attributes.get(TurSNEntityIndexer.PLACE_FIELD)).isEqualTo(List.of("São Paulo"));
        verify(provisioner, times(3)).ensureField(eq(site), any());
    }

    @Test
    void anUnchangedDocumentIsExtractedOnce() {
        genAi.setEntityExtraction(true);
        when(extractor.extract(anyString(), any(), anyString()))
                .thenReturn(new TurSNEntities(List.of("Antonio Prado"), List.of(), List.of()));

        indexer.enrich(site, Locale.US, document());
        Map<String, Object> again = document();
        indexer.enrich(site, Locale.US, again);

        verify(extractor, times(1)).extract(anyString(), any(), anyString());
        assertThat(again.get(TurSNEntityIndexer.PERSON_FIELD)).isEqualTo(List.of("Antonio Prado"));
    }

    @Test
    void aThesaurusVariantIsWrittenAsItsPreferredLabel() {
        TurRecognitionDictionary dictionary = dictionaryService.getDictionary(site, Locale.US);
        String text = TurTextNormalizer.fold("Por Bosso, Bruno e outros");

        assertThat(TurSNEntityIndexer.kept(List.of("Bosso, Bruno", "outros autores"), text, dictionary))
                .containsExactly("Bruno Bosso");
    }

    @Test
    void aValueMustOccurAsWholeWords() {
        String text = TurTextNormalizer.fold("Visited Parisian cafés and Paris.");

        assertThat(TurSNEntityIndexer.occursIn(text, "Paris")).isTrue();
        assertThat(TurSNEntityIndexer.occursIn(TurTextNormalizer.fold("Parisian cafés"), "Paris")).isFalse();
        assertThat(TurSNEntityIndexer.occursIn(TurTextNormalizer.fold("Sao Paulo"), "São Paulo")).isTrue();
    }

    @Test
    void aFailingModelNeverBlocksIndexing() {
        genAi.setEntityExtraction(true);
        when(extractor.extract(anyString(), any(), anyString())).thenThrow(new IllegalStateException("rate limit"));
        Map<String, Object> attributes = document();

        indexer.enrich(site, Locale.US, attributes);

        assertThat(attributes).containsOnlyKeys("title", "text");
    }
}
