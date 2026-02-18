package com.viglet.turing.solr;

import static com.viglet.turing.solr.TurSolrConstants.BOOST;
import static com.viglet.turing.solr.TurSolrConstants.SCORE;
import static com.viglet.turing.solr.TurSolrConstants.TURING_ENTITY;
import static com.viglet.turing.solr.TurSolrConstants.TYPE;
import static com.viglet.turing.solr.TurSolrConstants.VERSION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteField;
import com.viglet.turing.sn.field.TurSNSiteFieldService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurSolrDocumentHandler {
    private final int commitWithin;
    private final TurSNSiteFieldService turSNSiteFieldUtils;

    public TurSolrDocumentHandler(int commitWithin, TurSNSiteFieldService turSNSiteFieldUtils) {
        this.commitWithin = commitWithin;
        this.turSNSiteFieldUtils = turSNSiteFieldUtils;
    }

    public void indexing(TurSolrInstance turSolrInstance, TurSNSite turSNSite,
            Map<String, Object> attributes) {
        log.debug("Executing indexing ...");
        attributes.remove(SCORE);
        attributes.remove(VERSION);
        attributes.remove(BOOST);
        this.addDocument(turSolrInstance, turSNSite, attributes);
    }

    public void deIndexing(TurSolrInstance turSolrInstance, String id) {
        log.debug("Executing deIndexing ...");
        this.deleteDocument(turSolrInstance, id);
    }

    public void deIndexingByType(TurSolrInstance turSolrInstance, String type) {
        log.debug("Executing deIndexing by type {}...", type);
        this.deleteDocumentByType(turSolrInstance, type);
    }

    public void deleteDocument(TurSolrInstance turSolrInstance, String id) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.deleteById(id);
            updateRequest.setCommitWithin(commitWithin);
            updateRequest.process(turSolrInstance.getSolrClient(), turSolrInstance.getCore());
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void deleteDocumentByType(TurSolrInstance turSolrInstance, String type) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.deleteByQuery(TYPE + ":" + type);
            updateRequest.setCommitWithin(commitWithin);
            updateRequest.process(turSolrInstance.getSolrClient(), turSolrInstance.getCore());
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String concatenateString(@SuppressWarnings("rawtypes") List list) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (Object valueItem : list) {
            sb.append(TurSolrField.convertFieldToString(valueItem));
            if (i++ != list.size() - 1)
                sb.append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    public void addDocument(TurSolrInstance turSolrInstance, TurSNSite turSNSite,
            Map<String, Object> attributes) {
        Map<String, TurSNSiteField> turSNSiteFieldMap = turSNSiteFieldUtils.toMap(turSNSite);
        SolrInputDocument document = new SolrInputDocument();
        Optional.ofNullable(attributes).ifPresent(attr -> {
            attr.forEach((key, value) -> processAttribute(turSNSiteFieldMap, document, key, value));
            addSolrDocument(turSolrInstance, document);
        });

    }

    private void processAttribute(Map<String, TurSNSiteField> turSNSiteFieldMap,
            SolrInputDocument document, String key, Object attribute) {
        Optional.ofNullable(attribute).ifPresent(attr -> {
            switch (attr) {
                case Integer integer -> processInteger(document, key, integer);
                case JSONArray objects -> processJSONArray(turSNSiteFieldMap, document, key,
                        objects);
                case ArrayList<?> arrayList -> processArrayList(turSNSiteFieldMap, document, key,
                        arrayList);
                default -> processOtherTypes(document, key, attribute);
            }
        });
    }

    private void processOtherTypes(SolrInputDocument document, String key, Object attribute) {
        document.addField(key, TurSolrField.convertFieldToString(attribute));
    }

    private void processInteger(SolrInputDocument document, String key, Object attribute) {
        document.addField(key, attribute);
    }

    private void processJSONArray(Map<String, TurSNSiteField> turSNSiteFieldMap,
            SolrInputDocument document, String key, Object attribute) {
        JSONArray value = (JSONArray) attribute;
        if (key.startsWith(TURING_ENTITY) || isMultiValued(turSNSiteFieldMap, key)) {
            Optional.ofNullable(value).ifPresent(v -> IntStream.range(0, value.length())
                    .forEachOrdered(i -> document.addField(key, value.getString(i))));
        } else {
            document.addField(key,
                    concatenateString(Optional.ofNullable(value)
                            .map(v -> IntStream.range(0, value.length()).mapToObj(value::getString)
                                    .collect(Collectors.toCollection(ArrayList::new)))
                            .orElse(new ArrayList<>())));
        }
    }

    private static boolean isMultiValued(Map<String, TurSNSiteField> turSNSiteFieldMap,
            String key) {
        return turSNSiteFieldMap.get(key) != null
                && turSNSiteFieldMap.get(key).getMultiValued() == 1;
    }

    private void processArrayList(Map<String, TurSNSiteField> turSNSiteFieldMap,
            SolrInputDocument document, String key, Object attribute) {
        @SuppressWarnings("rawtypes")
        List attributeList = (ArrayList) attribute;
        Optional.ofNullable(attributeList).ifPresent(values -> {
            if (key.startsWith(TURING_ENTITY) || isMultiValued(turSNSiteFieldMap, key)) {
                for (Object valueItem : values) {
                    document.addField(key, TurSolrField.convertFieldToString(valueItem));
                }
            } else {
                document.addField(key, concatenateString(values));
            }
        });
    }

    private void addSolrDocument(TurSolrInstance turSolrInstance, SolrInputDocument document) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.add(document);
            updateRequest.setCommitWithin(commitWithin);
            updateRequest.process(turSolrInstance.getSolrClient(), turSolrInstance.getCore());
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
