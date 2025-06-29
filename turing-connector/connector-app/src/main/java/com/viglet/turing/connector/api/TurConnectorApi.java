/*
 *
 * Copyright (C) 2016-2025 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexing;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v2/connector")
@Tag(name = "Connector API", description = "Connector API")
public class TurConnectorApi {
    public static final String ID = "id";
    public static final String KEY = "Key";
    private final TurConnectorIndexingRepository turConnectorIndexingRepository;
    private final String solrEndpoint;
    private final String turingUrl;
    private final String turingApiKey;

    @Inject
    public TurConnectorApi(@Value("${turing.url:http://localhost:2700}") String turingUrl,
                           @Value("${turing.apiKey}") String turingApiKey,
                           @Value("${turing.solr.endpoint:http://localhost:8983}") String solrEndpoint,
                           TurConnectorIndexingRepository turConnectorIndexingRepository) {
        configureUnirest();
        this.turConnectorIndexingRepository = turConnectorIndexingRepository;
        this.solrEndpoint = solrEndpoint;
        this.turingUrl = turingUrl;
        this.turingApiKey = turingApiKey;
    }

    @GetMapping("status")
    public Map<String, String> status() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        return status;
    }

    @GetMapping("validate/{source}")
    public TurConnectorValidateDifference validateSource(@PathVariable String source) {
        return TurConnectorValidateDifference.builder()
                .missing(solrMissingContent(source))
                .extra(solrExtraContent(source))
                .build();
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

    private @NotNull Map<String, List<String>> solrExtraContent(String source) {
        Map<String, List<String>> solrExtraContentMap = new HashMap<>();
        for (String site : turConnectorIndexingRepository.distinctSitesBySource(source)) {
            for (String environment : turConnectorIndexingRepository.distinctEnvironmentBySite(site)) {
                turingLocale(site).forEach(siteLocale -> {
                    try (SolrClient solrClient = new HttpJdkSolrClient
                            .Builder("%s/solr".formatted(solrEndpoint))
                            .withDefaultCollection(siteLocale.getCore())
                            .build()) {
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
                            connectorIds.addAll(turConnectorIndexingRepository
                                    .distinctObjectIdBySourceAndLocaleAndEnvironmentAndIdIn(source, siteLocale.getLanguage(),
                                            environment, partition));
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

    private @NotNull Map<String, List<String>> solrMissingContent(String source) {
        Map<String, List<String>> solrMissingContentMap = new HashMap<>();
        for (String site: turConnectorIndexingRepository.distinctSitesBySource(source)) {
            for (String environment : turConnectorIndexingRepository.distinctEnvironmentBySite(site)) {
                turingLocale(site).forEach(siteLocale -> {
                    List<String> outputIdList = new ArrayList<>();
                    List<String> objectIdList = turConnectorIndexingRepository
                            .findAllObjectIdsBySourceAndLocaleAndEnvironment(source, siteLocale.getLanguage(),
                                    environment);
                    try (SolrClient solrClient = new HttpJdkSolrClient
                            .Builder("%s/solr".formatted(solrEndpoint))
                            .withDefaultCollection(siteLocale.getCore())
                            .build()) {
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

    @GetMapping("monitoring/index/{source}")
    public ResponseEntity<List<TurConnectorIndexing>> monitoryIndexByName(@PathVariable String source) {
        return turConnectorIndexingRepository.findAllBySourceOrderByModificationDateDesc(source, Limit.of(50))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());

    }
}
