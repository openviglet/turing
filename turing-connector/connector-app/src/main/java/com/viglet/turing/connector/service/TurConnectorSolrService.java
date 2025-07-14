package com.viglet.turing.connector.service;

import com.google.common.collect.Lists;
import com.viglet.turing.connector.domain.TurSNSiteLocale;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.*;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;

@Slf4j
@Service
public class TurConnectorSolrService {
    private final TurConnectorIndexingService indexingService;
    private final SolrClient solrClient;
    private final RestClient restClient;

    @Autowired
    public TurConnectorSolrService(TurConnectorIndexingService indexingService,
                                   SolrClient solrClient,
                                   RestClient restClient) {
        this.indexingService = indexingService;
        this.solrClient = solrClient;
        this.restClient = restClient;
    }

    @NotNull
    public Map<String, List<String>> solrExtraContent(String source, String provider) {
        Map<String, List<String>> solrExtraContentMap = new HashMap<>();
        for (String site : indexingService.getSites(source, provider)) {
            for (String environment : indexingService.getEnvironment(site, provider)) {
                turingLocale(site).forEach(siteLocale -> {
                    try {
                        SolrQuery query = new SolrQuery();
                        query.setQuery("*:*");
                        query.setFields(ID);
                        query.setRows(Integer.MAX_VALUE);
                        QueryResponse response = solrClient.query(siteLocale.getCore(), query);
                        List<String> solrIds = new ArrayList<>(response.getResults().stream()
                                .map(solrDocument ->
                                        (String) solrDocument.getFieldValue(ID))
                                .toList());
                        List<String> connectorIds = new ArrayList<>();
                        for (List<String> partition : Lists.partition(solrIds, 100)) {
                            connectorIds.addAll(indexingService
                                    .validateObjectIdList(source, environment, siteLocale, provider, partition));
                        }
                        solrIds.removeAll(connectorIds);
                        solrExtraContentMap.put(siteLocale.getCore(), solrIds);
                    } catch (IOException | SolrServerException e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
        }
        return solrExtraContentMap;
    }


    @NotNull
    public Map<String, List<String>> solrMissingContent(String source, String provider) {
        Map<String, List<String>> solrMissingContentMap = new HashMap<>();
        for (String site : indexingService.getSites(source, provider)) {
            for (String environment : indexingService.getEnvironment(site, provider)) {
                turingLocale(site).forEach(siteLocale -> {
                    List<String> outputIdList = new ArrayList<>();
                    List<String> objectIdList = indexingService.getObjectIdList(source, environment, siteLocale,
                            provider);
                    try {
                        for (List<String> partition : Lists.partition(objectIdList, 20)) {
                            SolrDocumentList documents = solrClient.getById(siteLocale.getCore(), partition);
                            documents.forEach(document ->
                                    outputIdList.add(document.get(ID).toString()));
                        }
                    } catch (IOException | SolrServerException e) {
                        log.error(e.getMessage(), e);
                    }
                    objectIdList.removeAll(outputIdList);
                    solrMissingContentMap.put(siteLocale.getCore(), objectIdList);
                });
            }
        }
        return solrMissingContentMap;
    }

    private List<TurSNSiteLocale> turingLocale(String snSite) {
        TurSNSiteLocale[] turSNSiteLocaleList = restClient.get()
                .uri("/api/sn/name/%s/locale".formatted(snSite))
                .retrieve()
                .body(TurSNSiteLocale[].class);
        if (turSNSiteLocaleList == null) return Collections.emptyList();
        return Arrays.asList(turSNSiteLocaleList);
    }


    public boolean hasContentIdAtSolr(String id, String source, String provider) {
        for (String site : indexingService.getSites(source, provider)) {
            for (TurSNSiteLocale siteLocale : turingLocale(site)) {
                try {
                    if (solrClient.getById(siteLocale.getCore(), id) != null) return true;
                } catch (IOException | SolrServerException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }
}
