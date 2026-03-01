package com.viglet.turing.exchange.sn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.viglet.turing.exchange.TurExchange;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.persistence.model.llm.TurLLMVendor;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.se.TurSEVendor;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.genai.TurSNSiteGenAi;
import com.viglet.turing.persistence.model.store.TurStoreInstance;
import com.viglet.turing.persistence.model.store.TurStoreVendor;
import com.viglet.turing.persistence.repository.llm.TurLLMInstanceRepository;
import com.viglet.turing.persistence.repository.llm.TurLLMVendorRepository;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.se.TurSEVendorRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.genai.TurSNSiteGenAiRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.persistence.repository.sn.merge.TurSNSiteMergeProvidersRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingExpressionRepository;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightRepository;
import com.viglet.turing.persistence.repository.store.TurStoreInstanceRepository;
import com.viglet.turing.persistence.repository.store.TurStoreVendorRepository;

@ExtendWith(MockitoExtension.class)
class TurSNSiteImportTest {

    @Mock
    private TurSNSiteRepository turSNSiteRepository;
    @Mock
    private TurSEInstanceRepository turSEInstanceRepository;
    @Mock
    private TurSEVendorRepository turSEVendorRepository;
    @Mock
    private TurSNSiteFieldRepository turSNSiteFieldRepository;
    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    @Mock
    private TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    @Mock
    private TurSNSiteSpotlightRepository turSNSiteSpotlightRepository;
    @Mock
    private TurSNRankingExpressionRepository turSNRankingExpressionRepository;
    @Mock
    private TurSNRankingConditionRepository turSNRankingConditionRepository;
    @Mock
    private TurSNSiteGenAiRepository turSNSiteGenAiRepository;
    @Mock
    private TurLLMInstanceRepository turLLMInstanceRepository;
    @Mock
    private TurLLMVendorRepository turLLMVendorRepository;
    @Mock
    private TurStoreInstanceRepository turStoreInstanceRepository;
    @Mock
    private TurStoreVendorRepository turStoreVendorRepository;
    @Mock
    private TurSNSiteMergeProvidersRepository turSNSiteMergeProvidersRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private TurSNSiteImport turSNSiteImport;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptySet());
    }

    @Test
    void shouldCreateReferencedRootInstancesBeforeImportingSites() {
        TurSEVendor seVendor = new TurSEVendor();
        seVendor.setId("SOLR");

        TurSEInstance exportedSe = new TurSEInstance();
        exportedSe.setId("se-1");
        exportedSe.setTitle("SE 1");
        exportedSe.setDescription("SE Desc");
        exportedSe.setEnabled(1);
        exportedSe.setHost("localhost");
        exportedSe.setPort(8983);
        exportedSe.setTurSEVendor(seVendor);

        TurLLMVendor llmVendor = new TurLLMVendor();
        llmVendor.setId("OPENAI");

        TurLLMInstance exportedLlm = new TurLLMInstance();
        exportedLlm.setId("llm-1");
        exportedLlm.setTitle("LLM 1");
        exportedLlm.setDescription("LLM Desc");
        exportedLlm.setEnabled(1);
        exportedLlm.setUrl("http://localhost:11434");
        exportedLlm.setTurLLMVendor(llmVendor);

        TurStoreVendor storeVendor = new TurStoreVendor();
        storeVendor.setId("CHROMA");

        TurStoreInstance exportedStore = new TurStoreInstance();
        exportedStore.setId("store-1");
        exportedStore.setTitle("Store 1");
        exportedStore.setDescription("Store Desc");
        exportedStore.setEnabled(1);
        exportedStore.setUrl("http://localhost:8000");
        exportedStore.setTurStoreVendor(storeVendor);

        TurExchange exchange = new TurExchange();
        exchange.setSnSites(List.of());
        exchange.setSe(List.of(exportedSe));
        exchange.setLlm(List.of(exportedLlm));
        exchange.setStore(List.of(exportedStore));

        when(turSEInstanceRepository.findById("se-1")).thenReturn(Optional.empty());
        when(turSEVendorRepository.findById("SOLR")).thenReturn(Optional.of(seVendor));
        when(turSEInstanceRepository.save(any(TurSEInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(turLLMInstanceRepository.findById("llm-1")).thenReturn(Optional.empty());
        when(turLLMVendorRepository.findById("OPENAI")).thenReturn(Optional.of(llmVendor));
        when(turLLMInstanceRepository.save(any(TurLLMInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(turStoreInstanceRepository.findById("store-1")).thenReturn(Optional.empty());
        when(turStoreVendorRepository.findById("CHROMA")).thenReturn(Optional.of(storeVendor));
        when(turStoreInstanceRepository.save(any(TurStoreInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        turSNSiteImport.importSNSite(exchange);

        verify(turSEInstanceRepository).save(any(TurSEInstance.class));
        verify(turLLMInstanceRepository).save(any(TurLLMInstance.class));
        verify(turStoreInstanceRepository).save(any(TurStoreInstance.class));
    }

    @Test
    void shouldResolveGenAiLLMAndStoreByIdUsingRootCollections() {
        TurSEInstance existingSe = new TurSEInstance();
        existingSe.setId("se-1");

        TurLLMVendor llmVendor = new TurLLMVendor();
        llmVendor.setId("OPENAI");
        TurLLMInstance existingLlm = new TurLLMInstance();
        existingLlm.setId("llm-1");
        existingLlm.setTitle("LLM 1");
        existingLlm.setUrl("http://localhost:11434");
        existingLlm.setTurLLMVendor(llmVendor);

        TurStoreVendor storeVendor = new TurStoreVendor();
        storeVendor.setId("CHROMA");
        TurStoreInstance existingStore = new TurStoreInstance();
        existingStore.setId("store-1");
        existingStore.setTitle("Store 1");
        existingStore.setUrl("http://localhost:8000");
        existingStore.setTurStoreVendor(storeVendor);

        TurSNSiteGenAiExchange genAiRef = new TurSNSiteGenAiExchange();
        genAiRef.setTurLLMInstance("llm-1");
        genAiRef.setTurStoreInstance("store-1");

        TurSNSiteExchange siteExchange = new TurSNSiteExchange();
        siteExchange.setId("site-1");
        siteExchange.setName("Site 1");
        siteExchange.setTurSEInstance("se-1");
        siteExchange.setTurSNSiteGenAi(genAiRef);

        TurExchange exchange = new TurExchange();
        exchange.setSnSites(List.of(siteExchange));
        exchange.setLlm(List.of(existingLlm));
        exchange.setStore(List.of(existingStore));

        when(turSNSiteRepository.findById("site-1")).thenReturn(Optional.empty());
        when(turSEInstanceRepository.findById("se-1")).thenReturn(Optional.of(existingSe));
        when(turLLMInstanceRepository.findById("llm-1")).thenReturn(Optional.of(existingLlm));
        when(turStoreInstanceRepository.findById("store-1")).thenReturn(Optional.of(existingStore));
        when(turSNSiteGenAiRepository.save(any(TurSNSiteGenAi.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(turSNSiteRepository.saveAndFlush(any(TurSNSite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        turSNSiteImport.importSNSite(exchange);

        ArgumentCaptor<TurSNSiteGenAi> genAiCaptor = ArgumentCaptor.forClass(TurSNSiteGenAi.class);
        verify(turSNSiteGenAiRepository).save(genAiCaptor.capture());

        TurSNSiteGenAi savedGenAi = genAiCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(savedGenAi.getTurLLMInstance()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(savedGenAi.getTurLLMInstance().getId()).isEqualTo("llm-1");
        org.assertj.core.api.Assertions.assertThat(savedGenAi.getTurStoreInstance()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(savedGenAi.getTurStoreInstance().getId()).isEqualTo("store-1");

        verify(turSNSiteRepository).saveAndFlush(any(TurSNSite.class));
        verify(turSEInstanceRepository).findById(eq("se-1"));
    }
}
