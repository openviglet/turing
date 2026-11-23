package com.viglet.turing.searchengine.solr;

import com.viglet.turing.searchengine.TurSEFieldValues;

import static com.viglet.turing.searchengine.solr.TurSolrConstants.BOOST;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.SCORE;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.TYPE;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.VERSION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.spi.TurSEIndexField;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurSolrDocumentHandler {
    private final int commitWithin;
    private final TurSESiteSearchConfig siteSearchConfig;
    private final TurDecimalFieldNormalizer turDecimalFieldNormalizer;

    public TurSolrDocumentHandler(int commitWithin, TurSESiteSearchConfig siteSearchConfig,
            TurDecimalFieldNormalizer turDecimalFieldNormalizer) {
        this.commitWithin = commitWithin;
        this.siteSearchConfig = siteSearchConfig;
        this.turDecimalFieldNormalizer = turDecimalFieldNormalizer;
    }

    public void indexing(TurSolrInstance turSolrInstance, String siteId,
            Map<String, Object> attributes) {
        log.debug("Executing indexing ...");
        attributes.remove(SCORE);
        attributes.remove(VERSION);
        attributes.remove(BOOST);
        this.addDocument(turSolrInstance, siteId, attributes);
    }

    /**
     * Indexes a batch of documents in a single Solr update request — drops the
     * per-doc HTTP overhead during bulk imports.
     *
     * <p>Throws on failure so the surrounding Resilience4j retry/circuit-breaker
     * sees the error and can retry the batch. Callers that need graceful
     * degradation (e.g. fall back to per-doc indexing) catch
     * {@link RuntimeException}.
     *
     * @return number of documents sent to Solr.
     */
    public int indexingBatch(TurSolrInstance turSolrInstance, String siteId,
            List<Map<String, Object>> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        log.debug("Executing batch indexing for {} documents ...", documents.size());
        Map<String, TurSEIndexField> indexFields = siteSearchConfig.indexFields(siteId);
        List<SolrInputDocument> solrDocs = new ArrayList<>(documents.size());
        for (Map<String, Object> attributes : documents) {
            if (attributes == null) {
                continue;
            }
            attributes.remove(SCORE);
            attributes.remove(VERSION);
            attributes.remove(BOOST);
            SolrInputDocument document = new SolrInputDocument();
            attributes.forEach((key, value) -> processAttribute(indexFields, document, key, value));
            solrDocs.add(document);
        }
        if (solrDocs.isEmpty()) {
            return 0;
        }
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.add(solrDocs);
            updateRequest.setCommitWithin(commitWithin);
            updateRequest.process(turSolrInstance.getUpdateClient(), turSolrInstance.getCore());
            return solrDocs.size();
        } catch (SolrServerException | IOException e) {
            // Throw so the resilience layer (and the import service's per-doc
            // fallback) can react; the caller logs the final outcome.
            throw new TurSolrIndexException(
                    "Batch indexing of " + solrDocs.size() + " documents failed: " + e.getMessage(), e);
        }
    }

    /**
     * Marker exception for failed Solr write operations (single-doc, batch, and
     * delete). Carries the original Solr/IO failure as its cause; matched by the
     * Resilience4j retry config and by the import service's per-doc fallback.
     */
    public static class TurSolrIndexException extends RuntimeException {
        public TurSolrIndexException(String message, Throwable cause) {
            super(message, cause);
        }
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
            updateRequest.process(turSolrInstance.getUpdateClient(), turSolrInstance.getCore());
        } catch (SolrServerException | IOException e) {
            // Surface the failure instead of swallowing it — a logged-and-ignored error
            // here silently reports a de-index as successful while the document remains.
            throw new TurSolrIndexException("Error de-indexing Solr document id=" + id, e);
        }
    }

    public void deleteDocumentByType(TurSolrInstance turSolrInstance, String type) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.deleteByQuery(TYPE + ":" + type);
            updateRequest.setCommitWithin(commitWithin);
            updateRequest.process(turSolrInstance.getUpdateClient(), turSolrInstance.getCore());
        } catch (SolrServerException | IOException e) {
            throw new TurSolrIndexException("Error de-indexing Solr documents type=" + type, e);
        }
    }

    /**
     * T1112 — deletes every document whose fields hold all of {@code fields}'
     * values. Each value is quoted, so it needs no escaping beyond {@code \} and
     * {@code "}; the field names were validated at the import API.
     */
    public void deleteDocumentsByFields(TurSolrInstance turSolrInstance, Map<String, String> fields) {
        String query = fields.entrySet().stream()
                .map(field -> field.getKey() + ":\""
                        + field.getValue().replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(" AND "));
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.deleteByQuery(query);
            updateRequest.setCommitWithin(commitWithin);
            updateRequest.process(turSolrInstance.getUpdateClient(), turSolrInstance.getCore());
        } catch (SolrServerException | IOException e) {
            throw new TurSolrIndexException("Error de-indexing Solr documents " + fields, e);
        }
    }

    private String concatenateString(@SuppressWarnings("rawtypes") List list) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (Object valueItem : list) {
            sb.append(TurSEFieldValues.toText(valueItem));
            if (i++ != list.size() - 1)
                sb.append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    public void addDocument(TurSolrInstance turSolrInstance, String siteId,
            Map<String, Object> attributes) {
        Map<String, TurSEIndexField> indexFields = siteSearchConfig.indexFields(siteId);
        SolrInputDocument document = new SolrInputDocument();
        Optional.ofNullable(attributes).ifPresent(attr -> {
            attr.forEach((key, value) -> processAttribute(indexFields, document, key, value));
            addSolrDocument(turSolrInstance, document);
        });

    }

    private void processAttribute(Map<String, TurSEIndexField> indexFields,
            SolrInputDocument document, String key, Object attribute) {
        Optional.ofNullable(attribute).ifPresent(attr -> {
            TurSEIndexField indexField = indexFields.get(key);
            TurSEFieldType fieldType = Optional.ofNullable(indexField)
                    .map(TurSEIndexField::type)
                    .orElse(null);

            if (turDecimalFieldNormalizer.isDecimalFieldType(fieldType)) {
                processDecimalAttribute(indexFields, document, key, attr, fieldType);
                return;
            }

            switch (attr) {
                case Integer integer -> processInteger(document, key, integer);
                case JSONArray objects -> processJSONArray(indexFields, document, key,
                        objects);
                case ArrayList<?> arrayList -> processArrayList(indexFields, document, key,
                        arrayList);
                default -> processOtherTypes(document, key, attribute);
            }
        });
    }

    private void processDecimalAttribute(Map<String, TurSEIndexField> indexFields,
            SolrInputDocument document, String key, Object attribute, TurSEFieldType fieldType) {
        boolean multiValued = isMultiValued(indexFields, key);

        switch (attribute) {
            case JSONArray jsonArray -> {
                if (jsonArray.length() == 0) {
                    return;
                }

                if (multiValued) {
                    IntStream.range(0, jsonArray.length())
                            .mapToObj(jsonArray::get)
                            .forEach(value -> addDecimalValue(document, key, value, fieldType));
                } else {
                    addDecimalValue(document, key, jsonArray.get(0), fieldType);
                }
            }
            case ArrayList<?> arrayList -> {
                if (arrayList.isEmpty()) {
                    return;
                }

                if (multiValued) {
                    arrayList.forEach(value -> addDecimalValue(document, key, value, fieldType));
                } else {
                    addDecimalValue(document, key, arrayList.getFirst(), fieldType);
                }
            }
            default -> addDecimalValue(document, key, attribute, fieldType);
        }
    }

    private void addDecimalValue(SolrInputDocument document, String key, Object value,
            TurSEFieldType fieldType) {
        if (fieldType == TurSEFieldType.CURRENCY) {
            String rawCurrency = TurSEFieldValues.toText(value);
            toSolrCurrencyValue(rawCurrency)
                    .ifPresentOrElse(
                            normalizedCurrency -> document.addField(key, normalizedCurrency),
                            () -> log.warn("Skipping invalid currency value for field '{}': {}", key,
                                    rawCurrency));
            return;
        }

        turDecimalFieldNormalizer.normalizeNumericValue(fieldType, value)
                .ifPresentOrElse(
                        normalizedValue -> document.addField(key, normalizedValue),
                        () -> log.warn("Skipping invalid decimal value for field '{}': {}", key,
                                TurSEFieldValues.toText(value)));
    }

    private Optional<String> toSolrCurrencyValue(String rawCurrency) {
        if (rawCurrency == null) {
            return Optional.empty();
        }

        String normalizedCurrency = rawCurrency.trim();
        int separatorIndex = normalizedCurrency.lastIndexOf(',');
        if (separatorIndex <= 0 || separatorIndex == normalizedCurrency.length() - 1) {
            return Optional.empty();
        }

        String amountPart = normalizedCurrency.substring(0, separatorIndex).trim();
        String currencyCode = normalizedCurrency.substring(separatorIndex + 1).trim()
                .toUpperCase(Locale.ROOT);
        if (!currencyCode.matches("^[A-Z]{3}$")) {
            return Optional.empty();
        }

        // Currency amounts in API payloads use dot as decimal separator (standard
        // JSON/REST format).
        // Also accept comma-decimal notation (e.g. "19,92,BRL") by normalizing to dot
        // form.
        // Preserve the original string to avoid reformatting (e.g. "150.00" must not
        // become "150.0").
        String canonicalAmount = amountPart.replace(',', '.');
        try {
            Double.parseDouble(canonicalAmount); // validate only
            return Optional.of(String.format(Locale.ROOT, "%s,%s", canonicalAmount, currencyCode));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void processOtherTypes(SolrInputDocument document, String key, Object attribute) {
        document.addField(key, TurSEFieldValues.toText(attribute));
    }

    private void processInteger(SolrInputDocument document, String key, Object attribute) {
        document.addField(key, attribute);
    }

    private void processJSONArray(Map<String, TurSEIndexField> indexFields,
            SolrInputDocument document, String key, Object attribute) {
        JSONArray value = (JSONArray) attribute;
        if (isMultiValued(indexFields, key)) {
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

    private static boolean isMultiValued(Map<String, TurSEIndexField> indexFields,
            String key) {
        return indexFields.get(key) != null
                && indexFields.get(key).multiValued();
    }

    private void processArrayList(Map<String, TurSEIndexField> indexFields,
            SolrInputDocument document, String key, Object attribute) {
        @SuppressWarnings("rawtypes")
        List attributeList = (ArrayList) attribute;
        Optional.ofNullable(attributeList).ifPresent(values -> {
            if (isMultiValued(indexFields, key)) {
                for (Object valueItem : values) {
                    document.addField(key, TurSEFieldValues.toText(valueItem));
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
            updateRequest.process(turSolrInstance.getUpdateClient(), turSolrInstance.getCore());
        } catch (org.apache.solr.common.SolrException e) {
            // T1343 — a 4xx is Solr refusing this document (a value its field type
            // cannot parse, an unknown field): name it, since retrying cannot help.
            if (e.code() >= 400 && e.code() < 500) {
                throw new com.viglet.turing.searchengine.TurSEDocumentRefusedException(
                        String.valueOf(document.getFieldValue("id")), e.getMessage(), e);
            }
            throw e;
        } catch (SolrServerException | IOException e) {
            // Propagate instead of swallowing: a logged-and-ignored error here would
            // falsely report the document as "Indexed" while it was actually lost. Letting
            // it surface lets the resilience layer retry transient failures and the circuit
            // breaker react to persistent ones.
            throw new TurSolrIndexException("Error writing Solr document: " + e.getMessage(), e);
        }
    }
}
