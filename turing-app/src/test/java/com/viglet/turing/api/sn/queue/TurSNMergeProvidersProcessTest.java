package com.viglet.turing.api.sn.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.merge.TurSNSiteMergeProviders;
import com.viglet.turing.persistence.model.sn.merge.TurSNSiteMergeProvidersField;
import com.viglet.turing.persistence.repository.sn.merge.TurSNSiteMergeProvidersRepository;
import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstance;
import com.viglet.turing.solr.TurSolrInstanceProcess;

@ExtendWith(MockitoExtension.class)
class TurSNMergeProvidersProcessTest {

    @Mock
    private TurSolrInstanceProcess turSolrInstanceProcess;

    @Mock
    private TurSolr turSolr;

    @Mock
    private TurSNSiteMergeProvidersRepository turSNSiteMergeProvidersRepository;

    @InjectMocks
    private TurSNMergeProvidersProcess process;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testMergeDocuments_EmptyProviders() {
        TurSNSite site = new TurSNSite();
        Map<String, Object> attrs = new HashMap<>();

        when(turSNSiteMergeProvidersRepository.findByTurSNSite(site)).thenReturn(Collections.emptyList());

        Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);
        assertEquals(attrs, result);
    }

    @Test
    void testMergeDocuments_ProviderFrom() {
        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSNSiteMergeProviders mergeProviders = new TurSNSiteMergeProviders();
        mergeProviders.setTurSNSite(site);
        mergeProviders.setProviderFrom("providerA");
        mergeProviders.setProviderTo("providerB");
        mergeProviders.setRelationFrom("relFrom");
        mergeProviders.setRelationTo("relTo");
        mergeProviders.setLocale(Locale.US);

        TurSNSiteMergeProvidersField field = new TurSNSiteMergeProvidersField();
        field.setName("field1");
        mergeProviders.setOverwrittenFields(new HashSet<>(Collections.singletonList(field)));

        when(turSNSiteMergeProvidersRepository.findByTurSNSite(site))
                .thenReturn(Collections.singletonList(mergeProviders));

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.SOURCE_APPS, "providerA");
        attrs.put("relFrom", "value1");

        TurSolrInstance solrInstance = mock(TurSolrInstance.class);
        when(turSolrInstanceProcess.initSolrInstance(eq("site1"), any(Locale.class)))
                .thenReturn(Optional.of(solrInstance));

        SolrDocumentList emptyList = new SolrDocumentList();
        when(turSolr.solrResultAnd(eq(solrInstance), anyMap())).thenReturn(emptyList);

        Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);
        assertEquals(attrs, result);
    }

    @Test
    void testMergeDocuments_ProviderTo() {
        TurSNSite site = new TurSNSite();
        site.setName("site1");

        TurSNSiteMergeProviders mergeProviders = new TurSNSiteMergeProviders();
        mergeProviders.setTurSNSite(site);
        mergeProviders.setProviderFrom("providerA");
        mergeProviders.setProviderTo("providerB");
        mergeProviders.setRelationFrom("relFrom");
        mergeProviders.setRelationTo("relTo");
        mergeProviders.setLocale(Locale.US);

        when(turSNSiteMergeProvidersRepository.findByTurSNSite(site))
                .thenReturn(Collections.singletonList(mergeProviders));

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.SOURCE_APPS, "providerB");
        attrs.put("relTo", "value1");
        attrs.put(TurSNFieldName.ID, "1");

        TurSolrInstance solrInstance = mock(TurSolrInstance.class);
        when(turSolrInstanceProcess.initSolrInstance(eq("site1"), any(Locale.class)))
                .thenReturn(Optional.of(solrInstance));

        SolrDocumentList emptyList = new SolrDocumentList();
        when(turSolr.solrResultAnd(eq(solrInstance), anyMap())).thenReturn(emptyList);

        Map<String, Object> result = process.mergeDocuments(site, attrs, Locale.US);
        assertEquals(attrs, result);
    }
}
