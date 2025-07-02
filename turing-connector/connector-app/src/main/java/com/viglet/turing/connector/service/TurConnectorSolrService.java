package com.viglet.turing.connector.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.viglet.turing.connector.domain.TurSNSiteLocale;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;

@Slf4j
@Service
public class TurConnectorSolrService {

    public static final String KEY = "Key";
    private final TurConnectorIndexingService indexingService;
    private final String solrEndpoint;
    private final String turingUrl;
    private final String turingApiKey;

    @Autowired
    public TurConnectorSolrService(TurConnectorIndexingService indexingService,
                                   @Value("${turing.url:http://localhost:2700}") String turingUrl,
                                   @Value("${turing.apiKey}") String turingApiKey,
                                   @Value("${turing.solr.endpoint:http://localhost:8983}") String solrEndpoint) {
        this.indexingService = indexingService;
        this.solrEndpoint = solrEndpoint;
        this.turingUrl = turingUrl;
        this.turingApiKey = turingApiKey;
        configureUnirest();
    }

    private void configureUnirest() {
        Unirest.setTimeouts(0, 0);
        Unirest.setObjectMapper(new ObjectMapper() {
            final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            public String writeValue(Object value) {
                try {
                    return mapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage(), e);
                }
                return null;
            }

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return mapper.readValue(value, valueType);
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage(), e);
                }
                return null;
            }
        });
    }

    @NotNull
    public Map<String, List<String>> solrExtraContent(String source) {
        Map<String, List<String>> solrExtraContentMap = new HashMap<>();
        for (String site : indexingService.getSitesBySource(source)) {
            for (String environment : indexingService.getEnvironmentBySite(site)) {
                turingLocale(site).forEach(siteLocale -> {
                    try (SolrClient solrClient = getSolrClient(siteLocale)) {
                        SolrQuery query = new SolrQuery();
                        query.setQuery("*:*");
                        query.setFields(ID);
                        query.setRows(Integer.MAX_VALUE);
                        QueryResponse response = solrClient.query(query);
                        List<String> solrIds = new ArrayList<>(response.getResults().stream()
                                .map(solrDocument ->
                                        (String) solrDocument.getFieldValue(ID))
                                .toList());
                        List<String> connectorIds = new ArrayList<>();
                        for (List<String> partition : Lists.partition(solrIds, 100)) {
                            connectorIds.addAll(indexingService
                                    .validateObjectIdList(source, environment, siteLocale, partition));
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

    private HttpJdkSolrClient getSolrClient(TurSNSiteLocale siteLocale) {
        return new HttpJdkSolrClient
                .Builder("%s/solr".formatted(solrEndpoint))
                .withDefaultCollection(siteLocale.getCore())
                .build();
    }

    @NotNull
    public Map<String, List<String>> solrMissingContent(String source) {
        Map<String, List<String>> solrMissingContentMap = new HashMap<>();
        for (String site : indexingService.getSitesBySource(source)) {
            for (String environment : indexingService.getEnvironmentBySite(site)) {
                turingLocale(site).forEach(siteLocale -> {
                    List<String> outputIdList = new ArrayList<>();
                    List<String> objectIdList = indexingService.getObjectIdList(source, environment, siteLocale);
                    try (SolrClient solrClient = getSolrClient(siteLocale)) {
                        for (List<String> partition : Lists.partition(objectIdList, 100)) {
                            SolrDocumentList documents = solrClient.getById(partition);
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
        try {
            TurSNSiteLocale[] turSNSiteLocaleList = Unirest.get("%s/api/sn/name/%s/locale".formatted(turingUrl, snSite))
                    .header(KEY, turingApiKey)
                    .asObject(TurSNSiteLocale[].class).getBody();
            if (turSNSiteLocaleList == null) return Collections.emptyList();
            return Arrays.asList(turSNSiteLocaleList);
        } catch (UnirestException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    public boolean hasContentIdAtSolr(String id, String source) {
        for (String site : indexingService.getSitesBySource(source)) {
            for (TurSNSiteLocale siteLocale : turingLocale(site)) {
                try (SolrClient solrClient = getSolrClient(siteLocale)) {
                    if (solrClient.getById(id) != null) return true;
                } catch (IOException | SolrServerException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }
}
