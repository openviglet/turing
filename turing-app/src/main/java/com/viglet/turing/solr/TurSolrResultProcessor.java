package com.viglet.turing.solr;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.se.result.spellcheck.TurSESpellCheckResult;
import com.viglet.turing.commons.se.similar.TurSESimilarResult;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.se.facet.TurSEFacetResult;
import com.viglet.turing.se.facet.TurSEFacetResultAttr;
import com.viglet.turing.se.result.TurSEGenericResults;
import com.viglet.turing.se.result.TurSEGroup;
import com.viglet.turing.se.result.TurSEResult;
import com.viglet.turing.se.result.TurSEResults;
import com.viglet.turing.sn.TurSNFieldProcess;
import com.viglet.turing.sn.TurSNUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.viglet.turing.solr.TurSolrConstants.*;

public class TurSolrResultProcessor {

    private final TurSNFieldProcess turSNFieldProcess;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    public TurSolrResultProcessor(TurSNFieldProcess turSNFieldProcess,
                                  TurSNSiteFieldExtRepository turSNSiteFieldExtRepository) {
        this.turSNFieldProcess = turSNFieldProcess;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
    }

    public TurSEResults getResults(TurSolrInstance turSolrInstance, TurSNSite turSNSite,
                                   TurSEParameters turSEParameters, SolrQuery query,
                                   List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList,
                                   List<TurSNSiteFieldExt> turSNSiteFacetFieldExtList,
                                   List<TurSNSiteFieldExt> turSNSiteHlFieldExtList,
                                   TurSESpellCheckResult turSESpellCheckResult, QueryResponse queryResponse,
                                   TurSolrQueryBuilder turSolrQueryBuilder) {
        TurSEResults turSEResults = TurSEResults.builder().build();
        turSEResultsParameters(turSEParameters, query, turSEResults, queryResponse);
        processSEResultsFacet(turSNSite, turSEResults, queryResponse, turSNSiteFacetFieldExtList);
        processResults(turSNSite, turSNSiteMLTFieldExtList, turSNSiteHlFieldExtList, turSEResults,
                queryResponse);
        processGroups(query, turSolrInstance, turSNSite, turSEParameters, turSNSiteMLTFieldExtList,
                turSNSiteHlFieldExtList, turSEResults, queryResponse, turSolrQueryBuilder);
        turSEResults.setSpellCheck(turSESpellCheckResult);
        return turSEResults;
    }

    private void processResults(TurSNSite turSNSite,
                                List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList,
                                List<TurSNSiteFieldExt> turSNSiteHlFieldExtList, TurSEResults turSEResults,
                                QueryResponse queryResponse) {
        List<TurSESimilarResult> similarResults = new ArrayList<>();
        turSEResults.setResults(addSolrDocumentsToSEResults(queryResponse.getResults(), turSNSite,
                turSNSiteMLTFieldExtList, queryResponse, similarResults, turSNSiteHlFieldExtList));
        setMLT(turSNSite, turSNSiteMLTFieldExtList, turSEResults, similarResults);
    }

    private void processGroups(SolrQuery query, TurSolrInstance turSolrInstance,
                               TurSNSite turSNSite, TurSEParameters turSEParameters,
                               List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList,
                               List<TurSNSiteFieldExt> turSNSiteHlFieldExtList, TurSEResults turSEResults,
                               QueryResponse queryResponse, TurSolrQueryBuilder turSolrQueryBuilder) {
        if (turSolrQueryBuilder.hasGroup(turSEParameters) && queryResponse.getGroupResponse() != null) {
            List<TurSEGroup> turSEGroups = new ArrayList<>();
            queryResponse.getGroupResponse().getValues()
                    .forEach(groupCommand -> groupCommand.getValues()
                            .forEach(group -> Optional.ofNullable(group.getGroupValue())
                                    .ifPresent(g -> turSEGroups.add(setTurSEGroup(turSNSite,
                                            turSEParameters, turSNSiteMLTFieldExtList,
                                            turSNSiteHlFieldExtList, queryResponse, group)))));
            if (TurSolr.enabledWildcardNoResults(turSNSite) && TurSolr.isNotQueryExpression(query)) {
                SolrQuery wildcardQuery = query.getCopy();
                TurSolr.addAWildcardInQuery(wildcardQuery);
                TurSolr.executeSolrQuery(turSolrInstance, wildcardQuery)
                        .ifPresent(queryResponseWildcard -> queryResponseWildcard.getGroupResponse()
                                .getValues()
                                .forEach(groupCommand -> groupCommand.getValues()
                                        .forEach(group -> seGroupsHasGroup(turSEGroups, group)
                                                .ifPresentOrElse(turSEGroup -> {
                                                    if (turSEGroup.getResults().isEmpty()
                                                            && !group.getResult().isEmpty()) {
                                                        turSEGroup.setResults(
                                                                addSolrDocumentsToSEResults(
                                                                        group.getResult(),
                                                                        turSNSite,
                                                                        turSNSiteMLTFieldExtList,
                                                                        queryResponse, null,
                                                                        turSNSiteHlFieldExtList));
                                                    }
                                                }, () -> Optional.ofNullable(group.getGroupValue())
                                                        .ifPresent(
                                                                g -> turSEGroups.add(setTurSEGroup(
                                                                        turSNSite, turSEParameters,
                                                                        turSNSiteMLTFieldExtList,
                                                                        turSNSiteHlFieldExtList,
                                                                        queryResponse, group)))))));
            }
            turSEResults.setGroups(turSEGroups);
        }
    }

    private static Optional<TurSEGroup> seGroupsHasGroup(List<TurSEGroup> turSEGroups,
                                                         Group group) {
        return turSEGroups.stream().filter(o -> o.getName() != null && group.getGroupValue() != null
                && o.getName().equals(group.getGroupValue())).findFirst();
    }

    private TurSEGroup setTurSEGroup(TurSNSite turSNSite, TurSEParameters turSEParameters,
                                     List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList,
                                     List<TurSNSiteFieldExt> turSNSiteHlFieldExtList, QueryResponse queryResponse,
                                     Group group) {
        return TurSEGroup.builder().name(group.getGroupValue())
                .numFound(group.getResult().getNumFound())
                .currentPage(turSEParameters.getCurrentPage()).limit(turSEParameters.getRows())
                .pageCount(getNumberOfPages(group.getResult().getNumFound(),
                        turSEParameters.getRows()))
                .results(addSolrDocumentsToSEResults(group.getResult(), turSNSite,
                        turSNSiteMLTFieldExtList, queryResponse, null, turSNSiteHlFieldExtList))
                .build();
    }

    private void setMLT(TurSNSite turSNSite, List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList,
                        TurSEResults turSEResults, List<TurSESimilarResult> similarResults) {
        if (hasMLT(turSNSite, turSNSiteMLTFieldExtList))
            turSEResults.setSimilarResults(similarResults);
    }
    public void turSEResultsParameters(TurSEParameters turSEParameters, SolrQuery query,
                                        TurSEResults turSEResults, QueryResponse queryResponse) {
        if (queryResponse.getResults() != null) {
            turSEResults.setNumFound(queryResponse.getResults().getNumFound());
            turSEResults.setStart(queryResponse.getResults().getStart());
        } else if (queryResponse.getGroupResponse() != null) {
            turSEResults.setNumFound(queryResponse.getGroupResponse().getValues().stream()
                    .mapToInt(GroupCommand::getMatches).sum());
        }
        turSEResults.setElapsedTime(queryResponse.getElapsedTime());
        turSEResults.setQTime(queryResponse.getQTime());
        turSEResults.setQueryString(query.getQuery());
        turSEResults.setSort(turSEParameters.getSort());
        turSEResults.setLimit(turSEParameters.getRows());
        turSEResults.setPageCount(getNumberOfPages(turSEResults));
        turSEResults.setCurrentPage(turSEParameters.getCurrentPage());
    }

    private int getNumberOfPages(TurSEGenericResults turSEGenericResults) {
        return getNumberOfPages(turSEGenericResults.getNumFound(), turSEGenericResults.getLimit());
    }

    private int getNumberOfPages(long numFound, int limit) {
        return (int) Math.ceil(numFound / (double) limit);
    }

    private boolean hasMLT(TurSNSite turSNSite, List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList) {
        return TurSNUtils.isTrue(turSNSite.getMlt())
                && !CollectionUtils.isEmpty(turSNSiteMLTFieldExtList);
    }

    private List<TurSEResult> addSolrDocumentsToSEResults(SolrDocumentList solrDocumentList,
                                                          TurSNSite turSNSite, List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList,
                                                          QueryResponse queryResponse, List<TurSESimilarResult> similarResults,
                                                          List<TurSNSiteFieldExt> turSNSiteHlFieldExtList) {
        List<TurSEResult> results = new ArrayList<>();
        Optional.ofNullable(solrDocumentList).ifPresent(documents -> documents.forEach(document -> {
            processSEResultsMLT(turSNSite, turSNSiteMLTFieldExtList, similarResults, document,
                    queryResponse);
            results.add(createTurSEResult(getFieldExtMap(turSNSite), getRequiredFields(turSNSite),
                    document, getHL(turSNSite, turSNSiteHlFieldExtList, queryResponse, document)));
        }));
        return results;
    }

    private void processSEResultsMLT(TurSNSite turSNSite,
                                     List<TurSNSiteFieldExt> turSNSiteMLTFieldExtList,
                                     List<TurSESimilarResult> similarResults, SolrDocument document,
                                     QueryResponse queryResponse) {
        if (TurSNUtils.isTrue(turSNSite.getMlt()) && !turSNSiteMLTFieldExtList.isEmpty()) {
            @SuppressWarnings("rawtypes")
            SimpleOrderedMap mltResp = (SimpleOrderedMap) queryResponse.getResponse().get(MORE_LIKE_THIS);
            ((SolrDocumentList) mltResp.get((String) document.get(ID)))
                    .forEach(mltDocument -> similarResults.add(TurSESimilarResult.builder()
                            .id(TurSolrField.convertFieldToString(mltDocument.getFieldValue(ID)))
                            .title(TurSolrField
                                    .convertFieldToString(mltDocument.getFieldValue(TITLE)))
                            .type(TurSolrField
                                    .convertFieldToString(mltDocument.getFieldValue(TYPE)))
                            .url(TurSolrField.convertFieldToString(mltDocument.getFieldValue(URL)))
                            .build()));
        }
    }

    private void processSEResultsFacet(TurSNSite turSNSite, TurSEResults turSEResults,
                                       QueryResponse queryResponse, List<TurSNSiteFieldExt> turSNSiteFacetFieldExtList) {
        if (wasFacetConfigured(turSNSite, turSNSiteFacetFieldExtList)) {
            List<TurSEFacetResult> facetRangeResults = setFacetRanges(queryResponse);
            List<TurSEFacetResult> facetResults = new ArrayList<>(facetRangeResults);
            facetResults.addAll(setFacetFields(queryResponse, facetRangeResults));
            facetResults.forEach(facet -> {
                turSNFieldProcess.getTurSNSiteFieldOrdering(turSNSite.getId())
                        .ifPresent(fields -> fields.forEach(fieldExtension -> facetResults.stream()
                                .filter(facetResult -> fieldExtension.getFacetPosition() != null
                                        && facetResult.getFacet().equals(fieldExtension.getName()))
                                .findFirst().ifPresent(facetResult -> facetResult
                                        .setFacetPosition(fieldExtension.getFacetPosition()))));
                turSEResults.setFacetResults(facetResults.stream()
                        .sorted(Comparator.comparing(TurSEFacetResult::getFacetPosition)).toList());
            });
        }
    }

    private static List<TurSEFacetResult> setFacetRanges(QueryResponse queryResponse) {
        List<TurSEFacetResult> facetResults = new ArrayList<>();
        queryResponse.getFacetRanges().forEach(facet -> {
            TurSEFacetResult turSEFacetResult = new TurSEFacetResult();
            turSEFacetResult.setFacet(facet.getName());
            for (Object countItem : facet.getCounts()) {
                RangeFacet.Count count = (RangeFacet.Count) countItem;
                turSEFacetResult.add(count.getValue(),
                        new TurSEFacetResultAttr(count.getValue(), count.getCount()));
            }
            facetResults.add(turSEFacetResult);
        });
        return facetResults;
    }

    private static List<TurSEFacetResult> setFacetFields(QueryResponse queryResponse,
                                                         List<TurSEFacetResult> facetRangeList) {
        List<TurSEFacetResult> facetResults = new ArrayList<>();
        queryResponse.getFacetFields().forEach(facet -> {
            if (facetRangeList.stream()
                    .noneMatch(rangeItem -> facet.getName().equals(rangeItem.getFacet()))) {
                TurSEFacetResult turSEFacetResult = new TurSEFacetResult();
                turSEFacetResult.setFacet(facet.getName());
                facet.getValues().forEach(item -> turSEFacetResult.add(item.getName(),
                        new TurSEFacetResultAttr(item.getName(), (int) item.getCount())));
                facetResults.add(turSEFacetResult);
            }
        });
        return facetResults;
    }

    private boolean wasFacetConfigured(TurSNSite turSNSite,
                                       List<TurSNSiteFieldExt> turSNSiteFacetFieldExtList) {
        return TurSNUtils.isTrue(turSNSite.getFacet()) && turSNSite.getItemsPerFacet() != null
                && !CollectionUtils.isEmpty(turSNSiteFacetFieldExtList);
    }

    public TurSEResult createTurSEResult(Map<String, TurSNSiteFieldExt> fieldExtMap,
                                          Map<String, Object> requiredFields, SolrDocument document,
                                          Map<String, List<String>> hl) {
        addRequiredFieldsToDocument(requiredFields, document);
        return createTurSEResultFromDocument(fieldExtMap, document, hl);
    }

    private TurSEResult createTurSEResultFromDocument(Map<String, TurSNSiteFieldExt> fieldExtMap,
                                                      SolrDocument document, Map<String, List<String>> hl) {
        Map<String, Object> fields = new HashMap<>();
        for (String attribute : document.getFieldNames()) {
            Object attrValue = document.getFieldValue(attribute);
            if (isHLAttribute(fieldExtMap, hl, attribute)) {
                attrValue = hl.get(attribute).getFirst();
            }
            if (attribute != null && fields.containsKey(attribute)) {
                Object existingValue = fields.get(attribute);
                List<Object> attributeValues;
                if (existingValue instanceof List<?>) {

                    attributeValues = new ArrayList<>((List<?>) existingValue);
                } else {
                    attributeValues = new ArrayList<>();
                    attributeValues.add(existingValue);
                }
                attributeValues.add(attrValue);
                fields.put(attribute, attributeValues);
            } else {
                fields.put(attribute, attrValue);
            }
        }
        return TurSEResult.builder().fields(fields).build();
    }

    private static boolean isHLAttribute(Map<String, TurSNSiteFieldExt> fieldExtMap,
                                         Map<String, List<String>> hl, String attribute) {
        return fieldExtMap.containsKey(attribute)
                && (Collections
                .unmodifiableSet(EnumSet.of(TurSEFieldType.TEXT, TurSEFieldType.STRING))
                .contains(fieldExtMap.get(attribute).getType()))
                && hl != null && hl.containsKey(attribute);
    }

    private void addRequiredFieldsToDocument(Map<String, Object> requiredFields,
                                             SolrDocument document) {
        Arrays.stream(requiredFields.keySet().toArray()).map(String.class::cast)
                .filter(requiredField -> !document.containsKey(requiredField))
                .forEach(requiredField -> document.addField(requiredField,
                        requiredFields.get(requiredField)));
    }

    public Map<String, TurSNSiteFieldExt> getFieldExtMap(TurSNSite turSNSite) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndEnabled(turSNSite, 1).stream()
                .collect(Collectors.toMap(TurSNSiteFieldExt::getName,
                        turSNSiteFieldExt -> turSNSiteFieldExt, (a, b) -> b));
    }

    public Map<String, Object> getRequiredFields(TurSNSite turSNSite) {
        return turSNSiteFieldExtRepository.findByTurSNSiteAndRequiredAndEnabled(turSNSite, 1, 1)
                .stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(TurSNSiteFieldExt::getName,
                        TurSNSiteFieldExt::getDefaultValue, (a, b) -> b));
    }

    public Map<String, List<String>> getHL(TurSNSite turSNSite,
                                            List<TurSNSiteFieldExt> turSNSiteHlFieldExtList, QueryResponse queryResponse,
                                            SolrDocument document) {
        return isHL(turSNSite, turSNSiteHlFieldExtList) && queryResponse.getHighlighting() != null
                ? queryResponse.getHighlighting().get(document.get(ID).toString())
                : null;
    }

    public static boolean isHL(TurSNSite turSNSite,
                                List<TurSNSiteFieldExt> turSNSiteHlFieldExtList) {
        return TurSNUtils.isTrue(turSNSite.getHl())
                && !CollectionUtils.isEmpty(turSNSiteHlFieldExtList);
    }
}
