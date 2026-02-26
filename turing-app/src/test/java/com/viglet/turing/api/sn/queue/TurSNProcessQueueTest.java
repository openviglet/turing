package com.viglet.turing.api.sn.queue;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtFacetRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.sn.spotlight.TurSNSpotlightProcess;
import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstance;
import com.viglet.turing.solr.TurSolrInstanceProcess;

@ExtendWith(MockitoExtension.class)
class TurSNProcessQueueTest {

    @Mock
    private TurSolr turSolr;
    @Mock
    private TurSNSiteRepository turSNSiteRepository;
    @Mock
    private TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    @Mock
    private TurSolrInstanceProcess turSolrInstanceProcess;
    @Mock
    private TurSNMergeProvidersProcess turSNMergeProvidersProcess;
    @Mock
    private TurSNSpotlightProcess turSNSpotlightProcess;
    @Mock
    private TurSNSiteFieldRepository turSNSiteFieldRepository;
    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    @Mock
    private TurSNSiteFieldExtFacetRepository turSNSiteFieldExtFacetRepository;
    @Mock
    private TurSEInstanceRepository turSEInstanceRepository;

    @InjectMocks
    private TurSNProcessQueue processQueue;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testReceiveIndexingQueue_Create() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.CREATE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);
        item.setSpecs(new ArrayList<>());

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, "1");
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSEInstance seInstance = new TurSEInstance();
        seInstance.setId("se1");

        TurSNSite site = new TurSNSite();
        site.setName("site1");
        site.setTurSEInstance(seInstance);

        when(turSNSiteRepository.findByName("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);
        when(turSNMergeProvidersProcess.mergeDocuments(eq(site), anyMap(), eq(Locale.US))).thenReturn(attrs);

        TurSolrInstance solrInstance = mock(TurSolrInstance.class);
        when(turSolrInstanceProcess.initSolrInstance(eq("site1"), eq(Locale.US))).thenReturn(Optional.of(solrInstance));
        when(turSEInstanceRepository.findById("se1")).thenReturn(Optional.of(seInstance));

        processQueue.receiveIndexingQueue(jobItems);

        verify(turSolr, times(1)).indexing(eq(solrInstance), eq(site), anyMap());
    }

    @Test
    void testReceiveIndexingQueue_Delete() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.DELETE);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(TurSNFieldName.ID, "1");
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        when(turSNSiteRepository.findByName("site1")).thenReturn(Optional.of(site));
        when(turSNSpotlightProcess.isSpotlightJob(item)).thenReturn(false);

        TurSolrInstance solrInstance = mock(TurSolrInstance.class);
        when(turSolrInstanceProcess.initSolrInstance(eq("site1"), eq(Locale.US))).thenReturn(Optional.of(solrInstance));

        processQueue.receiveIndexingQueue(jobItems);

        verify(turSolr, times(1)).deIndexing(solrInstance, "1");
    }

    @Test
    void testReceiveIndexingQueue_Commit() {
        TurSNJobItems jobItems = new TurSNJobItems();
        TurSNJobItem item = new TurSNJobItem();
        item.setTurSNJobAction(TurSNJobAction.COMMIT);
        item.setSiteNames(Collections.singletonList("site1"));
        item.setLocale(Locale.US);

        Map<String, Object> attrs = new HashMap<>();
        item.setAttributes(attrs);
        jobItems.add(item);

        TurSNSite site = new TurSNSite();
        site.setName("site1");

        when(turSNSiteRepository.findByName("site1")).thenReturn(Optional.of(site));

        TurSolrInstance solrInstance = mock(TurSolrInstance.class);
        when(turSolrInstanceProcess.initSolrInstance(eq("site1"), eq(Locale.US))).thenReturn(Optional.of(solrInstance));

        processQueue.receiveIndexingQueue(jobItems);

        verify(turSolr, times(1)).commit(solrInstance);
    }

    @Test
    void testRemoveDuplicateTerms() {
        Map<String, Object> attrs = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add("term1");
        list.add("term1");
        list.add("term2");
        attrs.put("field1", list);

        Map<String, Object> result = processQueue.removeDuplicateTerms(attrs);
        List<?> resultList = (List<?>) result.get("field1");

        assertTrue(resultList.contains("term1"));
        assertTrue(resultList.contains("term2"));
        assertTrue(resultList.size() == 2);
    }
}
