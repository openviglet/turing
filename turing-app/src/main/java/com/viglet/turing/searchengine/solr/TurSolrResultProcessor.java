package com.viglet.turing.searchengine.solr;

import com.viglet.turing.searchengine.TurSEFlags;
import com.viglet.turing.searchengine.TurSEPaging;

import com.viglet.turing.searchengine.TurSEFieldValues;

import static com.viglet.turing.searchengine.solr.TurSolrConstants.ID;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.MORE_LIKE_THIS;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.TITLE;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.TYPE;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.util.CollectionUtils;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.se.similar.TurSESimilarResult;
import com.viglet.turing.observability.TurMeterNames;
import com.viglet.turing.observability.TurSearchPipelineObservation;
import com.viglet.turing.searchengine.facet.TurSEFacetResult;
import com.viglet.turing.searchengine.facet.TurSEFacetResultAttr;
import com.viglet.turing.searchengine.result.TurSEGenericResults;
import com.viglet.turing.searchengine.result.TurSEGroup;
import com.viglet.turing.searchengine.result.TurSEResult;
import com.viglet.turing.searchengine.result.TurSEResults;
import com.viglet.turing.searchengine.spi.TurSECustomFacet;
import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;

public class TurSolrResultProcessor {

        private final TurSESiteSearchConfig siteSearchConfig;
        private final TurSearchPipelineObservation pipelineObservation;

        /** T1310 — reads the site only through the site search SPI. */
        public TurSolrResultProcessor(TurSESiteSearchConfig siteSearchConfig,
                        TurSearchPipelineObservation pipelineObservation) {
                this.siteSearchConfig = siteSearchConfig;
                this.pipelineObservation = pipelineObservation;
        }

        public TurSEResults getResults(TurSolrInstance turSolrInstance, TurSESiteSettings turSNSite,
                        SolrQuery query, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse,
                        TurSolrQueryBuilder turSolrQueryBuilder) {
                return pipelineObservation.record(TurMeterNames.STAGE_RESULT_PROCESS,
                                () -> buildResults(turSolrInstance, turSNSite, query, queryContext,
                                                queryResponse, turSolrQueryBuilder));
        }

        private TurSEResults buildResults(TurSolrInstance turSolrInstance, TurSESiteSettings turSNSite,
                        SolrQuery query, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse,
                        TurSolrQueryBuilder turSolrQueryBuilder) {
                TurSEResults turSEResults = TurSEResults.builder().build();
                turSEResultsParameters(queryContext.getTurSEParameters(), query, turSEResults, queryResponse);
                processSEResultsFacet(turSNSite, queryContext, turSEResults, queryResponse,
                                queryContext.getFacetFields());
                processResults(turSNSite, queryContext, turSEResults, queryResponse);
                processGroups(query, turSolrInstance, turSNSite, queryContext, turSEResults, queryResponse,
                                turSolrQueryBuilder);
                turSEResults.setSpellCheck(queryContext.getSpellCheckResult());
                return turSEResults;
        }

        private void processResults(TurSESiteSettings turSNSite,
                        TurSolrQueryContext queryContext, TurSEResults turSEResults,
                        QueryResponse queryResponse) {
                List<TurSESimilarResult> similarResults = new ArrayList<>();
                turSEResults.setResults(addSolrDocumentsToSEResults(queryResponse.getResults(), turSNSite,
                                queryContext, queryResponse, similarResults));
                setMLT(turSNSite, queryContext.getMltFields(), turSEResults, similarResults);
        }

        private void processGroups(SolrQuery query, TurSolrInstance turSolrInstance,
                        TurSESiteSettings turSNSite, TurSolrQueryContext queryContext, TurSEResults turSEResults,
                        QueryResponse queryResponse, TurSolrQueryBuilder turSolrQueryBuilder) {
                if (!TurSEPaging.hasGroup(queryContext.getTurSEParameters())
                                || queryResponse.getGroupResponse() == null) {
                        return;
                }

                List<TurSEGroup> turSEGroups = new ArrayList<>();
                populateGroupsFromResponse(queryResponse, turSNSite, queryContext, turSEGroups);

                if (shouldApplyWildcardQuery(turSNSite, query)) {
                        enrichGroupsWithWildcardResults(query, turSolrInstance, turSNSite,
                                        queryContext, queryResponse, turSEGroups);
                }

                turSEResults.setGroups(turSEGroups);
        }

        private void populateGroupsFromResponse(QueryResponse queryResponse, TurSESiteSettings turSNSite,
                        TurSolrQueryContext queryContext, List<TurSEGroup> turSEGroups) {
                var groupResponse = queryResponse.getGroupResponse();
                if (groupResponse == null || CollectionUtils.isEmpty(groupResponse.getValues())) {
                        return;
                }

                for (GroupCommand groupCommand : groupResponse.getValues()) {
                        addGroupsFromCommand(turSNSite, queryContext, queryResponse, turSEGroups, groupCommand);
                }
        }

        private void addGroupsFromCommand(TurSESiteSettings turSNSite, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse, List<TurSEGroup> turSEGroups, GroupCommand groupCommand) {
                if (CollectionUtils.isEmpty(groupCommand.getValues())) {
                        return;
                }

                for (Group group : groupCommand.getValues()) {
                        addGroupIfPresent(turSNSite, queryContext, queryResponse, turSEGroups, group);
                }
        }

        private void addGroupIfPresent(TurSESiteSettings turSNSite, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse, List<TurSEGroup> turSEGroups, Group group) {
                if (group.getGroupValue() == null) {
                        return;
                }

                turSEGroups.add(setTurSEGroup(turSNSite, queryContext, queryResponse, group));
        }

        private boolean shouldApplyWildcardQuery(TurSESiteSettings turSNSite, SolrQuery query) {
                return TurSolr.enabledWildcardNoResults(turSNSite) && TurSolr.isNotQueryExpression(query);
        }

        private void enrichGroupsWithWildcardResults(SolrQuery query, TurSolrInstance turSolrInstance,
                        TurSESiteSettings turSNSite, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse, List<TurSEGroup> turSEGroups) {
                SolrQuery wildcardQuery = query.getCopy();
                TurSolr.addAWildcardInQuery(wildcardQuery);
                TurSolr.executeSolrQuery(turSolrInstance, wildcardQuery)
                                .ifPresent(wildcardResponse -> processWildcardGroupResults(
                                                wildcardResponse, turSNSite, queryContext,
                                                queryResponse, turSEGroups));
        }

        private void processWildcardGroupResults(QueryResponse wildcardResponse, TurSESiteSettings turSNSite,
                        TurSolrQueryContext queryContext, QueryResponse originalResponse,
                        List<TurSEGroup> turSEGroups) {
                wildcardResponse.getGroupResponse().getValues()
                                .forEach(groupCommand -> groupCommand.getValues()
                                                .forEach(group -> updateOrAddGroup(group, turSNSite,
                                                                queryContext, originalResponse,
                                                                turSEGroups)));
        }

        private void updateOrAddGroup(Group group, TurSESiteSettings turSNSite,
                        TurSolrQueryContext queryContext, QueryResponse queryResponse,
                        List<TurSEGroup> turSEGroups) {
                Optional<TurSEGroup> existingGroup = seGroupsHasGroup(turSEGroups, group);

                if (existingGroup.isPresent()) {
                        updateGroupWithWildcardResults(existingGroup.get(), group, turSNSite,
                                        queryContext, queryResponse);
                } else {
                        addNewGroupFromWildcard(group, turSNSite, queryContext, queryResponse,
                                        turSEGroups);
                }
        }

        private void updateGroupWithWildcardResults(TurSEGroup turSEGroup, Group group,
                        TurSESiteSettings turSNSite, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse) {
                if (turSEGroup.getResults().isEmpty() && !group.getResult().isEmpty()) {
                        turSEGroup.setResults(addSolrDocumentsToSEResults(
                                        group.getResult(), turSNSite, queryContext, queryResponse, null));
                }
        }

        private void addNewGroupFromWildcard(Group group, TurSESiteSettings turSNSite,
                        TurSolrQueryContext queryContext, QueryResponse queryResponse,
                        List<TurSEGroup> turSEGroups) {
                Optional.ofNullable(group.getGroupValue())
                                .ifPresent(g -> turSEGroups.add(setTurSEGroup(turSNSite,
                                                queryContext, queryResponse, group)));
        }

        private static Optional<TurSEGroup> seGroupsHasGroup(List<TurSEGroup> turSEGroups,
                        Group group) {
                return turSEGroups.stream().filter(o -> o.getName() != null && group.getGroupValue() != null
                                && o.getName().equals(group.getGroupValue())).findFirst();
        }

        private TurSEGroup setTurSEGroup(TurSESiteSettings turSNSite, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse, Group group) {
                return TurSEGroup.builder().name(group.getGroupValue())
                                .numFound(group.getResult().getNumFound())
                                .currentPage(queryContext.getTurSEParameters().getCurrentPage())
                                .limit(queryContext.getTurSEParameters().getRows())
                                .pageCount(getNumberOfPages(group.getResult().getNumFound(),
                                                queryContext.getTurSEParameters().getRows()))
                                .results(addSolrDocumentsToSEResults(group.getResult(), turSNSite,
                                                queryContext, queryResponse, null))
                                .build();
        }

        private void setMLT(TurSESiteSettings turSNSite, List<String> turSNSiteMLTFieldExtList,
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

        private boolean hasMLT(TurSESiteSettings turSNSite, List<String> turSNSiteMLTFieldExtList) {
                return TurSEFlags.isTrue(turSNSite.mlt())
                                && !CollectionUtils.isEmpty(turSNSiteMLTFieldExtList);
        }

        private List<TurSEResult> addSolrDocumentsToSEResults(SolrDocumentList solrDocumentList,
                        TurSESiteSettings turSNSite, TurSolrQueryContext queryContext,
                        QueryResponse queryResponse, List<TurSESimilarResult> similarResults) {
                List<TurSEResult> results = new ArrayList<>();
                if (solrDocumentList == null || solrDocumentList.isEmpty()) {
                        return results;
                }
                Map<String, TurSESearchField> fieldExtMap = getFieldExtMap(turSNSite);
                Map<String, Object> requiredFields = getRequiredFields(turSNSite);
                solrDocumentList.forEach(document -> {
                        processSEResultsMLT(turSNSite, queryContext.getMltFields(), similarResults, document,
                                        queryResponse);
                        results.add(createTurSEResult(fieldExtMap, requiredFields, document,
                                        getHL(turSNSite, queryContext.getHlFields(), queryResponse, document)));
                });
                return results;
        }

        private void processSEResultsMLT(TurSESiteSettings turSNSite,
                        List<String> turSNSiteMLTFieldExtList,
                        List<TurSESimilarResult> similarResults, SolrDocument document,
                        QueryResponse queryResponse) {
                if (TurSEFlags.isTrue(turSNSite.mlt()) && !turSNSiteMLTFieldExtList.isEmpty()) {
                        @SuppressWarnings("rawtypes")
                        SimpleOrderedMap mltResp = (SimpleOrderedMap) queryResponse.getResponse().get(MORE_LIKE_THIS);
                        ((SolrDocumentList) mltResp.get((String) document.get(ID)))
                                        .forEach(mltDocument -> similarResults.add(TurSESimilarResult.builder()
                                                        .id(TurSEFieldValues.toText(
                                                                        mltDocument.getFieldValue(ID)))
                                                        .title(TurSEFieldValues.toText(mltDocument
                                                                                        .getFieldValue(TITLE)))
                                                        .type(TurSEFieldValues.toText(mltDocument
                                                                                        .getFieldValue(TYPE)))
                                                        .url(TurSEFieldValues.toText(
                                                                        mltDocument.getFieldValue(URL)))
                                                        .build()));
                }
        }

        private void processSEResultsFacet(TurSESiteSettings turSNSite, TurSolrQueryContext queryContext,
                        TurSEResults turSEResults,
                        QueryResponse queryResponse, List<TurSESearchField> turSNSiteFacetFieldExtList) {
                if (!wasFacetConfigured(turSNSite, turSNSiteFacetFieldExtList)) {
                        return;
                }
                List<TurSEFacetResult> facetQueryResults = setFacetQueries(turSNSite,
                                queryContext.getLocale(), queryResponse);
                List<TurSEFacetResult> facetRangeResults = setFacetRanges(queryResponse);
                List<TurSEFacetResult> facetResults = new ArrayList<>(facetQueryResults);
                facetResults.addAll(facetRangeResults);
                facetResults.addAll(setFacetFields(queryResponse, facetRangeResults));
                if (facetResults.isEmpty()) {
                        return;
                }
                // T1310 - the site's facet positions come through the site search SPI.
                Map<String, Integer> positionByName = siteSearchConfig.facetPositions(turSNSite.id());
                facetResults.forEach(fr -> {
                        Integer pos = positionByName.get(fr.getFacet());
                        if (pos != null) {
                                fr.setFacetPosition(pos);
                        }
                });
                turSEResults.setFacetResults(facetResults.stream()
                                .sorted(Comparator.comparing(TurSEFacetResult::getFacetPosition,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList());
        }

        private List<TurSEFacetResult> setFacetQueries(TurSESiteSettings turSNSite,
                        Locale requestLocale,
                        QueryResponse queryResponse) {
                Map<String, Integer> facetQueryValues = Optional.ofNullable(queryResponse.getFacetQuery())
                                .orElse(Collections.emptyMap());
                if (facetQueryValues.isEmpty()) {
                        return Collections.emptyList();
                }

                Map<String, Map<String, Integer>> countsByFacetName = new LinkedHashMap<>();
                facetQueryValues.forEach((key, count) -> {
                        String[] keyValue = key.split(Pattern.quote(
                                        TurSolrQueryBuilder.CUSTOM_FACET_QUERY_SEPARATOR),
                                        2);
                        if (keyValue.length == 2) {
                                countsByFacetName
                                                .computeIfAbsent(keyValue[0], f -> new LinkedHashMap<>())
                                                .put(keyValue[1], count);
                        }
                });

                List<TurSEFacetResult> facetResults = new ArrayList<>();
                Set<String> processedFacetNames = new HashSet<>();
                // T1310 — the site's custom facets come through the site search SPI.
                List<TurSECustomFacet> customFacetDefinitions = siteSearchConfig
                                .fieldCustomFacets(turSNSite.id()).stream()
                                .sorted(Comparator.comparing(TurSolrResultProcessor::getCustomFacetPosition)
                                                .thenComparing(TurSECustomFacet::name,
                                                                Comparator.nullsLast(String::compareToIgnoreCase)))
                                .toList();

                customFacetDefinitions.forEach(customFacet -> {
                        Map<String, Integer> itemCounts = countsByFacetName
                                        .get(customFacet.name());
                        if (itemCounts != null) {
                                TurSEFacetResult result = new TurSEFacetResult();
                                result.setFacet(customFacet.name());
                                customFacet.items().stream()
                                                .sorted(Comparator
                                                                .comparing(this::getCustomFacetItemPosition)
                                                                .thenComparing(TurSECustomFacet.Item::label,
                                                                                Comparator
                                                                                                .nullsLast(String::compareToIgnoreCase)))
                                                .forEach(item -> {
                                                        Integer count = itemCounts
                                                                        .get(item.label());
                                                        if (count != null) {
                                                                String displayLabel = resolveCustomFacetItemLabel(
                                                                                item, requestLocale);
                                                                result.add(item.label(),
                                                                                new TurSEFacetResultAttr(
                                                                                                item.label(),
                                                                                                count,
                                                                                                displayLabel));
                                                        }
                                                });
                                facetResults.add(result);
                                processedFacetNames.add(customFacet.name());
                        }
                });

                countsByFacetName.forEach((facetName, itemCounts) -> {
                        if (!processedFacetNames.contains(facetName)) {
                                TurSEFacetResult result = new TurSEFacetResult();
                                result.setFacet(facetName);
                                itemCounts.entrySet().stream()
                                                .sorted(Map.Entry.comparingByKey())
                                                .forEach(item -> result.add(item.getKey(),
                                                                new TurSEFacetResultAttr(item.getKey(),
                                                                                item.getValue())));
                                facetResults.add(result);
                        }
                });

                return facetResults;
        }

        private Integer getCustomFacetItemPosition(TurSECustomFacet.Item customFacetItem) {
                return Optional.ofNullable(customFacetItem.position())
                                .filter(position -> position > 0)
                                .orElse(Integer.MAX_VALUE);
        }

        /** As sn's custom facet definition ranks it: its position, or last when it has none. */
        private static Integer getCustomFacetPosition(TurSECustomFacet customFacet) {
                return Optional.ofNullable(customFacet.position())
                                .filter(position -> position > 0)
                                .orElse(Integer.MAX_VALUE);
        }

        private static String resolveCustomFacetItemLabel(TurSECustomFacet.Item item,
                        Locale requestLocale) {
                Map<String, String> labels = item.labels();
                if (labels == null || labels.isEmpty() || requestLocale == null) {
                        return item.label();
                }
                String localeKey = requestLocale.toString();
                String translated = labels.get(localeKey);
                if (translated == null || translated.isBlank()) {
                        translated = labels.get(requestLocale.toLanguageTag());
                }
                if (translated == null || translated.isBlank()) {
                        translated = labels.get(requestLocale.getLanguage());
                }
                return (translated == null || translated.isBlank()) ? item.label() : translated;
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

        private boolean wasFacetConfigured(TurSESiteSettings turSNSite,
                        List<TurSESearchField> turSNSiteFacetFieldExtList) {
                return TurSEFlags.isTrue(turSNSite.facet()) && turSNSite.itemsPerFacet() != null
                                && !CollectionUtils.isEmpty(turSNSiteFacetFieldExtList);
        }

        public TurSEResult createTurSEResult(Map<String, TurSESearchField> fieldExtMap,
                        Map<String, Object> requiredFields, SolrDocument document,
                        Map<String, List<String>> hl) {
                addRequiredFieldsToDocument(requiredFields, document);
                return createTurSEResultFromDocument(fieldExtMap, document, hl);
        }

        private TurSEResult createTurSEResultFromDocument(Map<String, TurSESearchField> fieldExtMap,
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

        private static boolean isHLAttribute(Map<String, TurSESearchField> fieldExtMap,
                        Map<String, List<String>> hl, String attribute) {
                return fieldExtMap.containsKey(attribute)
                                && (Collections
                                                .unmodifiableSet(EnumSet.of(TurSEFieldType.TEXT, TurSEFieldType.STRING))
                                                .contains(fieldExtMap.get(attribute).type()))
                                && hl != null && hl.containsKey(attribute);
        }

        private void addRequiredFieldsToDocument(Map<String, Object> requiredFields,
                        SolrDocument document) {
                Arrays.stream(requiredFields.keySet().toArray()).map(String.class::cast)
                                .filter(requiredField -> !document.containsKey(requiredField))
                                .forEach(requiredField -> document.addField(requiredField,
                                                requiredFields.get(requiredField)));
        }

        public Map<String, TurSESearchField> getFieldExtMap(TurSESiteSettings turSNSite) {
                return siteSearchConfig.enabledFields(turSNSite.id()).stream()
                                .collect(Collectors.toMap(TurSESearchField::name,
                                                field -> field, (a, b) -> b));
        }

        public Map<String, Object> getRequiredFields(TurSESiteSettings turSNSite) {
                return new HashMap<>(siteSearchConfig.requiredFields(turSNSite.id()));
        }

        public Map<String, List<String>> getHL(TurSESiteSettings turSNSite,
                        List<String> turSNSiteHlFieldExtList, QueryResponse queryResponse,
                        SolrDocument document) {
                return isHL(turSNSite, turSNSiteHlFieldExtList) && queryResponse.getHighlighting() != null
                                ? queryResponse.getHighlighting().get(document.get(ID).toString())
                                : null;
        }

        public static boolean isHL(TurSESiteSettings turSNSite,
                        List<String> turSNSiteHlFieldExtList) {
                return TurSEFlags.isTrue(turSNSite.hl())
                                && !CollectionUtils.isEmpty(turSNSiteHlFieldExtList);
        }
}
