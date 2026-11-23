package com.viglet.turing.searchengine.solr;

import com.viglet.turing.searchengine.TurSEFlags;
import com.viglet.turing.searchengine.TurSpellCheckGuard;
import com.viglet.turing.searchengine.TurSEPaging;

import static com.viglet.turing.searchengine.solr.TurSolrConstants.AND;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.ASC;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.BOOST_QUERY;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.COUNT;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.DEF_TYPE;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.EDISMAX;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.EMPTY;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.FACET_OR;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.FILTER_QUERY_OR;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.HYPHEN;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.INDEX;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.NEWEST;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.NO_FACET_NAME;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.OLDEST;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.OR;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.OR_AND;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.OR_OR;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.PLUS_ONE;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.MM;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.Q_OP;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.RELAXED_MINIMUM_MATCH;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.RECENT_DATES;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.UPCOMING;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.UPCOMING_DATES;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.ROWS;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.SOLR_DATE_PATTERN;
import static com.viglet.turing.searchengine.solr.TurSolrConstants.TRUE;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.se.result.spellcheck.TurSESpellCheckResult;
import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.commons.sn.bean.TurSNSitePostParamsBean;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.searchengine.TurSNSiteCustomSortMissingEnum;
import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;
import com.viglet.turing.searchengine.TurSNSiteFacetFieldSortEnum;
import com.viglet.turing.searchengine.TurSNSiteFacetRangeEnum;
import com.viglet.turing.searchengine.TurSNSiteFacetSortEnum;
import com.viglet.turing.searchengine.TurSNQueryRelaxer;
import com.viglet.turing.searchengine.facet.TurSEFacetTypes;
import com.viglet.turing.searchengine.facet.TurSNFacetMapForFilterQuery;
import com.viglet.turing.searchengine.facet.TurSNFacetProperties;
import com.viglet.turing.searchengine.facet.TurSNFilterExclusion;
import com.viglet.turing.searchengine.spi.TurSECustomFacet;
import com.viglet.turing.searchengine.spi.TurSECustomSortItem;
import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TurSolrQueryBuilder {
    public static final String CUSTOM_FACET_QUERY_SEPARATOR = "::";
    private final TurSESiteSearchConfig siteSearchConfig;
    private final TurSNTargetingRules turSNTargetingRules;

    /** T1310 — reads the site only through the site search SPI. */
    public TurSolrQueryBuilder(TurSESiteSearchConfig siteSearchConfig,
            TurSNTargetingRules turSNTargetingRules) {
        this.siteSearchConfig = siteSearchConfig;
        this.turSNTargetingRules = turSNTargetingRules;
    }

    @NotNull
    public SolrQuery prepareSolrQuery(TurSNSiteSearchContext context, TurSESiteSettings turSNSite,
            TurSEParameters turSEParameters, TurSESpellCheckResult turSESpellCheckResult) {
        SolrQuery query = new SolrQuery();
        query.set(DEF_TYPE, EDISMAX);
        query.set(Q_OP, AND);
        if (turSEParameters.isRelaxedMatch()) {
            // T1176 — the relaxed retry ORs the words, half of them required.
            query.set(Q_OP, OR);
            query.set(MM, RELAXED_MINIMUM_MATCH);
        }
        siteSearchConfig.applySearchRules(turSNSite.id(), turSEParameters, context.getLocale());
        setRows(turSNSite, turSEParameters);
        setSortEntry(turSNSite, query, turSEParameters);

        if (usesExactMatch(turSNSite, turSEParameters)) {
            turSEParameters.setQuery(
                    String.format("%s:%s", turSNSite.exactMatchField(), turSEParameters.getQuery()));
        }

        if (TurSpellCheckGuard.isAutoCorrectionEnabled(context, turSNSite.spellCheck(), turSNSite.spellCheckFixes())) {
            query.setQuery(TurSpellCheckGuard.hasCorrectedText(turSESpellCheckResult)
                    ? turSESpellCheckResult.getCorrectedText()
                    : turSEParameters.getQuery());
        } else {
            query.setQuery(turSEParameters.getQuery());
        }
        if (!TurSEPaging.hasGroup(turSEParameters)) {
            query.setRows(turSEParameters.getRows())
                    .setStart(TurSEPaging.firstRow(turSEParameters));
        }
        prepareQueryFilterQuery(turSEParameters.getTurSNFilterParams(), query, turSNSite);
        prepareIdRestriction(turSEParameters.getRestrictIds(), query);
        preparePartition(turSEParameters.getPartition(), query);
        prepareQueryTargetingRules(context.getTurSNSitePostParamsBean(), query);
        if (TurSEPaging.hasGroup(turSEParameters)) {
            prepareGroup(turSEParameters, query);
        }
        prepareBoostQuery(turSNSite, query);
        prepareFieldWeights(turSNSite, query);
        return query;
    }

    /**
     * T1065 — a field's search weight {@code w} adds the user query, run against
     * that field alone, as a boost query worth {@code w - 1}. The main query and
     * its parser are untouched, so the match set never changes and a site with no
     * weight (or weight 1) ranks exactly as before. Added after
     * {@link #prepareBoostQuery}, which sets {@code bq} rather than appending.
     */
    private void prepareFieldWeights(TurSESiteSettings turSNSite, SolrQuery query) {
        siteSearchConfig.enabledFields(turSNSite.id()).stream()
                .filter(field -> field.name() != null && field.searchWeight() != null
                        && field.searchWeight() > 1f)
                .forEach(field -> query.add(BOOST_QUERY, String.format(Locale.US,
                        "{!edismax qf='%s^%.2f' v=$q}", field.name(), field.searchWeight() - 1f)));
    }

    /** T1185 — the configsets' synonym-free copy of {@code title}, for the phrase boost. */
    public static final String TITLE_PHRASE_FIELD = "title_phrase";
    /** T1185 — what a title holding the query as a phrase adds, as on Lucene and Elasticsearch. */
    static final String TITLE_PHRASE_BOOST = TITLE_PHRASE_FIELD + "^10";

    /**
     * T1185 — Lucene's title-phrase boost (T1177) on Solr: edismax's {@code pf} on
     * {@link #TITLE_PHRASE_FIELD}, which the configsets copy from {@code title}
     * with no stemming and no synonyms, so a guide titled with the exact phrase
     * outranks a product that matches only a synonym. Only for a plain phrase
     * ({@link TurSNQueryRelaxer#isPlainPhrase}) on a site whose title is
     * {@code title}; the caller checks that the core has the field.
     */
    public static void prepareTitlePhraseBoost(SolrQuery query, TurSESiteSettings turSNSite, String queryString) {
        String titleField = StringUtils.hasText(turSNSite.defaultTitleField())
                ? turSNSite.defaultTitleField()
                : TurSNFieldName.TITLE;
        if (TurSNFieldName.TITLE.equals(titleField) && TurSNQueryRelaxer.isPlainPhrase(queryString)) {
            query.set("pf", TITLE_PHRASE_BOOST);
        }
    }

    private static boolean usesExactMatch(TurSESiteSettings turSNSite, TurSEParameters turSEParameters) {
        return turSEParameters.getQuery().trim().startsWith("\"")
                && turSEParameters.getQuery().trim().endsWith("\"")
                && StringUtils.hasText(turSNSite.exactMatchField())
                && turSNSite.exactMatch() != null && turSNSite.exactMatch().equals(1);
    }

    private void prepareBoostQuery(TurSESiteSettings turSNSite, SolrQuery query) {
        Map<String, TurSEFieldType> fieldTypes = siteSearchConfig.fieldTypes(turSNSite.id());
        List<TurSERankingExpression> expressions = siteSearchConfig.rankingExpressions(turSNSite.id());
        if (expressions.stream().allMatch(expression -> expression.conditions().isEmpty())) {
            return;
        }
        String[] expressionString = expressions.stream()
                .map(expression -> {
                    String attributes = boostQueryAttributes(expression, fieldTypes);
                    return attributes == null ? null
                            : String.format(Locale.US, "%s^%.1f", "(" + attributes + ")",
                                    expression.weight());
                })
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        if (expressionString.length > 0) {
            query.set(BOOST_QUERY, expressionString);
        }
    }

    /**
     * The ANDed conditions of one expression, or {@code null} when the expression
     * cannot be applied (T1070: an {@code Is not} on the recency form, which has no
     * meaning), so it is left out rather than widened.
     */
    private String boostQueryAttributes(TurSERankingExpression expression,
            Map<String, TurSEFieldType> fieldTypes) {
        List<String> clauses = new ArrayList<>();
        for (TurSERankingCondition condition : expression.conditions()) {
            if (condition.kind() == TurSERankingCondition.Kind.EXISTS) {
                clauses.add(String.format("%s:[* TO *]", condition.attribute()));
                continue;
            }
            boolean negated = condition.kind() == TurSERankingCondition.Kind.IS_NOT;
            String dateFunction = fieldTypes.get(condition.attribute()) == TurSEFieldType.DATE
                    ? dateFunction(condition.value()) : null;
            if (dateFunction != null) {
                if (negated) {
                    log.debug("Ranking expression '{}' skipped: Is not has no meaning on the recency form",
                            expression.name());
                    return null;
                }
                clauses.add(String.format("_query_:\"" + dateFunction + "\"", condition.attribute()));
                continue;
            }
            String match = String.format("%s:\"%s\"", condition.attribute(), condition.value());
            // T1070 — Is not: every document except the ones with that value.
            clauses.add(negated ? "(*:* -" + match + ")" : match);
        }
        return clauses.isEmpty() ? null : String.join(betweenSpaces(AND), clauses);
    }

    /**
     * The function query of a scored DATE condition: {@code asc} (recent past) or
     * {@code upcoming} (T1219: today or later, nearest most); {@code null} for an
     * exact date value.
     */
    private static String dateFunction(String value) {
        if (ASC.equalsIgnoreCase(value)) {
            return RECENT_DATES;
        }
        if (UPCOMING.equalsIgnoreCase(value)) {
            return UPCOMING_DATES;
        }
        return null;
    }

    private void prepareGroup(TurSEParameters turSEParameters, SolrQuery query) {
        query.set(GroupParams.GROUP, TRUE).set(GroupParams.GROUP_FIELD, turSEParameters.getGroup())
                .set(GroupParams.GROUP_LIMIT, turSEParameters.getRows())
                .set(GroupParams.GROUP_OFFSET,
                        TurSEPaging.firstRow(turSEParameters))
                .set(ROWS, 100);

    }

    /**
     * T1103 — restricts the result set to {@code ids} with its own {@code fq}, apart
     * from the facet filters. Each id is quoted, so a URL id needs no escaping
     * beyond {@code \} and {@code "}.
     */
    static void prepareIdRestriction(List<String> ids, SolrQuery query) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        query.addFilterQuery(ids.stream()
                .map(id -> "\"" + id.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(" OR ", TurSNFieldName.ID + ":(", ")")));
    }

    /**
     * T1132 — the search's own filters (facet selections and the key's partition)
     * on a query that is not a search, such as the similar-documents one, with
     * the semantics they have on search.
     */
    public void prepareConstraints(TurSEParameters constraints, SolrQuery query, TurSESiteSettings turSNSite) {
        if (constraints == null) {
            return;
        }
        if (constraints.getTurSNFilterParams() != null) {
            prepareQueryFilterQuery(constraints.getTurSNFilterParams(), query, turSNSite);
        }
        preparePartition(constraints.getPartition(), query);
    }

    /**
     * T1133 — confines the result set to the partition the request's API key is
     * bound to, with its own {@code fq}. The value is quoted like an id; the
     * field name was validated as a plain identifier when the key was saved.
     */
    public static void preparePartition(com.viglet.turing.commons.se.TurSEPartition partition, SolrQuery query) {
        if (partition == null) {
            return;
        }
        query.addFilterQuery(partition.field() + ":\""
                + partition.value().replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
    }

    private void prepareQueryFilterQuery(TurSNFilterParams turSNFilterParams, SolrQuery query,
            TurSESiteSettings turSNSite) {
        // T1233 — each exclusion is its own filter query, so it always narrows and is
        // never joined to a facet group by fqOr, fqItemOperator or the site's item type.
        Optional.ofNullable(turSNFilterParams).ifPresent(params -> TurSNFilterExclusion
                .in(params.getDefaultValues(), params.getAnd(), params.getOr())
                .forEach(exclusion -> query.addFilterQuery(exclusionFilterQuery(exclusion))));
        Optional.of(getFilterQueryMap(turSNFilterParams, turSNSite))
                .filter(facetMapForFilterQuery -> !CollectionUtils.isEmpty(facetMapForFilterQuery))
                .ifPresent(facetMapForFilterQuery -> query.addFilterQuery(String.format("%s(%s)",
                        getFacetTypeConditionInFilterQuery(turSNSite, turSNFilterParams),
                        setFilterQueryString(
                                setFilterQueryMapModified(turSNSite, facetMapForFilterQuery)))));
    }

    @NotNull
    private TurSNFacetMapForFilterQuery setFilterQueryMapModified(TurSESiteSettings turSNSite,
            TurSNFacetMapForFilterQuery facetMapForFilterQuery) {
        TurSNFacetMapForFilterQuery filterQueryMapModified = new TurSNFacetMapForFilterQuery();
        getFilterQueryMap(facetMapForFilterQuery, turSNSite).forEach((facetName, properties) -> {
            if (filterQueryMapModified.containsKey(facetName)) {
                filterQueryMapModified.get(facetName).getFacetItems()
                        .addAll(getFilterQueryValue(properties.getFacetItems()));
            } else {
                filterQueryMapModified.put(facetName,
                        TurSNFacetProperties.builder().facetType(properties.getFacetType())
                                .facetItemType(properties.getFacetItemType())
                                .facetItems(getFilterQueryValue(properties.getFacetItems()))
                                .build());
            }
        });
        return filterQueryMapModified;
    }

    @NotNull
    private static StringBuilder setFilterQueryString(
            TurSNFacetMapForFilterQuery facetMapForFilterQuery) {
        List<String> facetsAnd = new ArrayList<>();
        List<String> facetsOr = new ArrayList<>();
        StringBuilder filterQueryString = new StringBuilder();
        facetMapForFilterQuery.forEach((f, properties) -> {
            StringBuilder filterQueryStringItems = new StringBuilder();
            if (properties.getFacetItemType().equals(TurSNSiteFacetFieldEnum.OR)) {
                filterQueryStringItems.append(String.format("(%s)",
                        String.join(betweenSpaces(OR), properties.getFacetItems())));
            } else {
                filterQueryStringItems.append(String.format("(%s)",
                        String.join(betweenSpaces(AND), properties.getFacetItems())));
            }
            if (properties.getFacetType().equals(TurSNSiteFacetFieldEnum.OR)) {
                facetsOr.add(filterQueryStringItems.toString());
            } else {
                facetsAnd.add(filterQueryStringItems.toString());
            }
        });
        if (!facetsAnd.isEmpty()) {
            filterQueryString.append(String.join(betweenSpaces(AND), facetsAnd));
            if (!facetsOr.isEmpty()) {
                filterQueryString.append(betweenSpaces(OR));
            }
        }
        if (!facetsOr.isEmpty()) {
            filterQueryString.append(String.join(betweenSpaces(OR), facetsOr));
        }
        return filterQueryString;
    }

    private static String betweenSpaces(String operator) {
        return " %s ".formatted(operator);
    }

    @NotNull
    private static List<String> getFilterQueryValue(List<String> value) {
        return value.stream()
                .map(fq -> queryWithoutExpression(fq) ? addDoubleQuotesToValue(fq) : fq)
                .map(TurSolrQueryBuilder::wrapNegationWithMatchAll).toList();
    }

    // Solr cannot evaluate a purely negative boolean clause like `-field:"value"`
    // when it stands alone inside parentheses (e.g. `(A AND (-id:"x"))`). Prefix
    // such items with `*:*` so they become well-formed: `*:* -field:"value"`.
    /** T1233 — {@code -field:"value"}, quoted as a positive selection is. */
    static String exclusionFilterQuery(TurSNFilterExclusion exclusion) {
        String positive = exclusion.positive();
        return HYPHEN + (queryWithoutExpression(positive) ? addDoubleQuotesToValue(positive) : positive);
    }

    private static String wrapNegationWithMatchAll(String fq) {
        return fq != null && fq.startsWith(HYPHEN) ? "*:* " + fq : fq;
    }

    private TurSNFacetMapForFilterQuery getFilterQueryMap(TurSNFilterParams turSNFilterParams,
            TurSESiteSettings turSNSite) {
        TurSNFacetMapForFilterQuery facetMapForFilterQuery = new TurSNFacetMapForFilterQuery();
        List<TurSESearchField> enabledFields = siteSearchConfig.facetPlanFields(siteId(turSNSite));
        List<TurSECustomFacet> customFacets = siteSearchConfig.fieldCustomFacets(siteId(turSNSite));
        Optional.ofNullable(turSNFilterParams).ifPresent(filterQueryParameters -> {
            Optional.ofNullable(filterQueryParameters.getDefaultValues())
                    .ifPresent(f -> f.stream().filter(fItem -> !TurSNFilterExclusion.isExclusion(fItem))
                            .forEach(fItem -> addEnabledFieldAsFacetItem(
                            TurSNSiteFacetFieldEnum.DEFAULT, fItem, enabledFields, customFacets,
                            facetMapForFilterQuery, turSNSite, turSNFilterParams)));
            Optional.ofNullable(filterQueryParameters.getAnd())
                    .ifPresent(f -> f.stream().filter(fItem -> !TurSNFilterExclusion.isExclusion(fItem))
                            .forEach(fItem -> addEnabledFieldAsFacetItem(
                            TurSNSiteFacetFieldEnum.AND, fItem, enabledFields, customFacets,
                            facetMapForFilterQuery, turSNSite, turSNFilterParams)));
            Optional.ofNullable(filterQueryParameters.getOr()).ifPresent(f -> f.stream()
                    .filter(fItem -> !TurSNFilterExclusion.isExclusion(fItem))
                    .forEach(fItem -> addEnabledFieldAsFacetItem(TurSNSiteFacetFieldEnum.OR, fItem,
                            enabledFields, customFacets, facetMapForFilterQuery, turSNSite,
                            turSNFilterParams)));
        });
        return facetMapForFilterQuery;
    }

    private static void addEnabledFieldAsFacetItem(TurSNSiteFacetFieldEnum facetType, String fq,
            List<TurSESearchField> enabledFields, List<TurSECustomFacet> customFacets,
            TurSNFacetMapForFilterQuery facetMapForFilterQuery, TurSESiteSettings turSNSite,
            TurSNFilterParams turSNFilterParams) {

        Optional<CustomFacetFilterMapping> customFacetFilterMapping = getCustomFacetFilterMapping(fq,
                enabledFields, customFacets);
        if (customFacetFilterMapping.isPresent()) {
            CustomFacetFilterMapping mapping = customFacetFilterMapping.get();
            TurSECustomFacet customFacet = mapping.customFacet();
            TurSNSiteFacetFieldEnum currentFacetType = TurSEFacetTypes.isDefault(facetType)
                    ? fieldFacetType(turSNSite, turSNFilterParams, orDefault(customFacet.facetType()))
                    : facetType;
            addEnabledFacetItem(customFacet.name(), currentFacetType,
                    fieldFacetItemType(turSNSite, turSNFilterParams, orDefault(customFacet.facetItemType())),
                    facetMapForFilterQuery,
                    mapping.customFacetItem().filterQuery());
            return;
        }

        TurCommonsUtils.getKeyValueFromColon(fq)
                .flatMap(kv -> enabledFields.stream()
                        .filter(facet -> facet.name().equals(kv.getKey())).findFirst())
                .ifPresentOrElse(facet ->

                {
                    if (TurSEFacetTypes.isDefault(facetType)) {
                        addEnabledFacetItem(facet.name(),
                                fieldFacetType(turSNSite, turSNFilterParams, facet.facetType()),
                                fieldFacetItemType(turSNSite, turSNFilterParams, facet.facetItemType()),
                                facetMapForFilterQuery, fq);
                    } else {
                        addEnabledFacetItem(facet.name(), facetType,
                                fieldFacetItemType(turSNSite, turSNFilterParams, facet.facetItemType()),
                                facetMapForFilterQuery, fq);
                    }
                }, () -> {
                    TurSNSiteFacetFieldEnum facetTypeValue = siteFacetType(turSNSite, turSNFilterParams);
                    addEnabledFacetItem(NO_FACET_NAME.concat(facetTypeValue.toString()),
                            facetTypeValue, TurSEFacetTypes.fromSite(turSNSite.facetItemType()),
                            facetMapForFilterQuery, fq);
                });

    }

    /**
     * T1310 — a field's or custom facet's own type, as sn's facet-type rules
     * decide it for a specific field: the request's operator, the configured
     * type, then the site's.
     */
    private static TurSNSiteFacetFieldEnum fieldFacetType(TurSESiteSettings site,
            TurSNFilterParams filterParams, TurSNSiteFacetFieldEnum configured) {
        return TurSEFacetTypes.facetType(filterParams, true, configured, site.facetType());
    }

    /** T1310 — a field's or custom facet's own item type; see {@link #fieldFacetType}. */
    private static TurSNSiteFacetFieldEnum fieldFacetItemType(TurSESiteSettings site,
            TurSNFilterParams filterParams, TurSNSiteFacetFieldEnum configured) {
        return TurSEFacetTypes.facetItemType(filterParams, true, configured, site.facetItemType());
    }

    /** T1310 — the type of a filter that names no facet: the request's operator, then the site's. */
    private static TurSNSiteFacetFieldEnum siteFacetType(TurSESiteSettings site,
            TurSNFilterParams filterParams) {
        return TurSEFacetTypes.facetType(filterParams, false, TurSNSiteFacetFieldEnum.DEFAULT,
                site.facetType());
    }

    /** T1310 — the item type of a filter that names no facet. */
    private static TurSNSiteFacetFieldEnum siteFacetItemType(TurSESiteSettings site,
            TurSNFilterParams filterParams) {
        return TurSEFacetTypes.facetItemType(filterParams, false, TurSNSiteFacetFieldEnum.DEFAULT,
                site.facetItemType());
    }

    /**
     * The site's id; {@code null} for a constraint without a site, whose filters
     * then match no field (T1132).
     */
    private static String siteId(TurSESiteSettings turSNSite) {
        return turSNSite == null ? null : turSNSite.id();
    }

    /** A custom facet with no type of its own defers, as sn's custom facet definition reads it. */
    private static TurSNSiteFacetFieldEnum orDefault(TurSNSiteFacetFieldEnum facetType) {
        return facetType == null ? TurSNSiteFacetFieldEnum.DEFAULT : facetType;
    }

    private static Optional<CustomFacetFilterMapping> getCustomFacetFilterMapping(String fq,
            List<TurSESearchField> enabledFields,
            List<TurSECustomFacet> customFacets) {
        Map<String, TurSESearchField> fieldById = enabledFields.stream()
                .filter(f -> f.id() != null)
                .collect(Collectors.toMap(TurSESearchField::id, f -> f));
        return TurCommonsUtils.getKeyValueFromColon(fq)
                .flatMap(kv -> customFacets.stream()
                        .filter(customFacet -> customFacet.name().equals(kv.getKey()))
                        .flatMap(customFacet -> customFacet.items().stream()
                                .filter(item -> item.label().equals(kv.getValue()))
                                .map(item -> new CustomFacetFilterMapping(
                                        customFacet.name(),
                                        fieldById.get(customFacet.fieldId()),
                                        customFacet, item)))
                        .findFirst());
    }

    private static void addEnabledFacetItem(String facetName, TurSNSiteFacetFieldEnum facetType,
            TurSNSiteFacetFieldEnum facetItemType,
            TurSNFacetMapForFilterQuery facetMapForFilterQuery, String fq) {

        if (facetMapForFilterQuery.containsKey(facetName)) {
            facetMapForFilterQuery.get(facetName).getFacetItems().add(fq);
        } else {
            facetMapForFilterQuery.put(facetName,
                    TurSNFacetProperties.builder().facetType(facetType).facetItemType(facetItemType)
                            .facetItems(new ArrayList<>(Collections.singletonList(fq))).build());
        }
    }

    private TurSNFacetMapForFilterQuery getFilterQueryMap(
            TurSNFacetMapForFilterQuery facetMapForFilterQuery, TurSESiteSettings turSNSite) {
        // T1310 — the site's enabled DATE facets, read through the site search SPI.
        List<TurSESearchField> dateFacet = siteSearchConfig.dateFacetFields(siteId(turSNSite));
        if (!dateFacet.isEmpty()) {
            return getFilterQueryByDateRange(facetMapForFilterQuery, dateFacet);
        } else {
            return facetMapForFilterQuery;
        }

    }

    @NotNull
    private static TurSNFacetMapForFilterQuery getFilterQueryByDateRange(
            TurSNFacetMapForFilterQuery facetMapForFilterQuery, List<TurSESearchField> dateFacet) {
        TurSNFacetMapForFilterQuery facetMapForFilterQueryFormatted = new TurSNFacetMapForFilterQuery();
        facetMapForFilterQuery.forEach((facetName, properties) -> {
            if (facetMapForFilterQueryFormatted.containsKey(facetName)) {
                facetMapForFilterQueryFormatted.get(facetName).getFacetItems()
                        .addAll(setFilterQueryRangeValue(properties.getFacetItems(), dateFacet));
            } else {
                facetMapForFilterQueryFormatted.put(facetName, TurSNFacetProperties.builder()
                        .facetType(properties.getFacetType())
                        .facetItemType(properties.getFacetItemType())
                        .facetItems(setFilterQueryRangeValue(properties.getFacetItems(), dateFacet))
                        .build());
            }
        });
        return facetMapForFilterQueryFormatted;
    }

    @NotNull
    private static List<String> setFilterQueryRangeValue(List<String> filterQueries,
            List<TurSESearchField> dateFacet) {
        return filterQueries.stream().map(fq -> TurCommonsUtils.getKeyValueFromColon(fq)
                .map(facetKv -> dateFacet.stream()
                        .filter(dateFacetItem -> facetKv.getKey().equals(dateFacetItem.name())
                                && isDateRangeFacet(dateFacetItem))
                        .findFirst().map(dateFacetItem -> setFilterQueryByRangeType(fq, facetKv,
                                dateFacetItem.facetRange()))
                        .orElse(fq))
                .orElse(fq)).toList();
    }

    private static String setFilterQueryByRangeType(String fq, KeyValue<String, String> facetKv,
            TurSNSiteFacetRangeEnum facetRange) {
        try {
            if (withoutExpression(facetKv.getValue())) {
                Date date = solrDateFormatter().parse(facetKv.getValue());
                return switch (facetRange) {
                    case DAY -> setFilterQueryRangeDay(date, facetKv);
                    case MONTH -> setFilterQueryRangeMonth(date, facetKv);
                    case YEAR -> setFilterQueryRangeYear(date, facetKv);
                    case DISABLED -> fq;
                };
            }
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
        return fq;
    }

    @NotNull
    private static SimpleDateFormat solrDateFormatter() {
        return new SimpleDateFormat(SOLR_DATE_PATTERN, Locale.ENGLISH);
    }

    private static String setFilterQueryRangeDay(Date date, KeyValue<String, String> kv) {
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(date);
        endOfDay.set(Calendar.HOUR, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 59);
        return setFilterQueryDateRange(kv, endOfDay);
    }

    private static String setFilterQueryDateRange(KeyValue<String, String> kv, Calendar endOfDay) {
        return String.format("%s:[ %s TO %s ]", kv.getKey(), kv.getValue(),
                solrDateFormatter().format(endOfDay.getTime()));
    }

    private static String setFilterQueryRangeYear(Date date, KeyValue<String, String> kv) {
        Calendar lastDateOfYear = Calendar.getInstance();
        lastDateOfYear.setTime(date);
        lastDateOfYear.set(Calendar.MONTH, 11);
        lastDateOfYear.set(Calendar.DAY_OF_MONTH, 31);
        lastDateOfYear.set(Calendar.HOUR, 23);
        lastDateOfYear.set(Calendar.MINUTE, 59);
        lastDateOfYear.set(Calendar.SECOND, 59);
        lastDateOfYear.set(Calendar.MILLISECOND, 59);
        return setFilterQueryDateRange(kv, lastDateOfYear);
    }

    private static String setFilterQueryRangeMonth(Date date, KeyValue<String, String> kv) {
        Calendar lastDateOfMonth = Calendar.getInstance();
        lastDateOfMonth.setTime(date);
        lastDateOfMonth.set(Calendar.DATE, lastDateOfMonth.getActualMaximum(Calendar.DATE));
        lastDateOfMonth.set(Calendar.HOUR, 23);
        lastDateOfMonth.set(Calendar.MINUTE, 59);
        lastDateOfMonth.set(Calendar.SECOND, 59);
        lastDateOfMonth.set(Calendar.MILLISECOND, 59);
        return setFilterQueryDateRange(kv, lastDateOfMonth);
    }

    private static boolean isDateRangeFacet(TurSESearchField dateFacetItem) {
        return dateFacetItem.type().equals(TurSEFieldType.DATE)
                && dateFacetItem.facetRange() != null
                && !dateFacetItem.facetRange().equals(TurSNSiteFacetRangeEnum.DISABLED);
    }

    private static String getFacetTypeConditionInFilterQuery(TurSESiteSettings turSNSite,
            TurSNFilterParams turSNFilterParams) {
        if (TurSEFacetTypes.typeAndItemType(siteFacetType(turSNSite, turSNFilterParams),
                siteFacetItemType(turSNSite, turSNFilterParams)).equals(OR_AND)
                || TurSEFacetTypes.isOr(turSNFilterParams, turSNSite.facetItemType())) {
            return FILTER_QUERY_OR;
        }
        return EMPTY;
    }

    @NotNull
    private static String addDoubleQuotesToValue(String q) {
        return TurCommonsUtils.getKeyValueFromColon(q)
                .map(kv -> String.format("%s:\"%s\"", kv.getKey(), kv.getValue()))
                .orElse(String.format("\"%s\"", q));
    }

    private static boolean queryWithoutExpression(String q) {
        String value = TurSolrUtils.getValueFromQuery(q);
        return !q.startsWith("(") && withoutExpression(value);

    }

    /** T1111 — {@code {from TO to]} opens an exclusive range, as {@code [} opens an inclusive one. */
    static boolean withoutExpression(String value) {
        return !value.startsWith("[") && !value.startsWith("{") && !value.startsWith("(")
                && !value.endsWith("*");

    }

    public List<String> prepareQueryMLT(TurSESiteSettings turSNSite, SolrQuery query) {
        List<String> turSNSiteMLTFieldExtList = siteSearchConfig.mltFields(turSNSite.id());
        if (hasMLT(turSNSite, turSNSiteMLTFieldExtList)) {
            query.set(MoreLikeThisParams.MLT, true).set(MoreLikeThisParams.MATCH_INCLUDE, true)
                    .set(MoreLikeThisParams.MIN_DOC_FREQ, 1)
                    .set(MoreLikeThisParams.MIN_TERM_FREQ, 1)
                    .set(MoreLikeThisParams.MIN_WORD_LEN, 7).set(MoreLikeThisParams.BOOST, false)
                    .set(MoreLikeThisParams.MAX_QUERY_TERMS, 1000)
                    .set(MoreLikeThisParams.SIMILARITY_FIELDS,
                            String.join(",", turSNSiteMLTFieldExtList));
        }
        return turSNSiteMLTFieldExtList;
    }

    /**
     * Prefixes {@code query} with the exclusion that keeps a facet's own selection
     * from narrowing its counts, by the facet's type and item type: always for
     * OR-OR, and for OR-AND while the request does not filter on
     * {@code filterKey}.
     */
    private static String setFacetTypeConditionInFacet(TurSESiteSettings turSNSite,
            TurSNFilterParams turSNFilterParams, TurSNSiteFacetFieldEnum configuredFacetType,
            TurSNSiteFacetFieldEnum configuredFacetItemType, String query, String filterKey) {
        List<String> fqFields = TurSEFacetTypes.fqFields(turSNFilterParams);
        switch (TurSEFacetTypes.typeAndItemType(
                fieldFacetType(turSNSite, turSNFilterParams, configuredFacetType),
                fieldFacetItemType(turSNSite, turSNFilterParams, configuredFacetItemType))) {
            case OR_OR: {
                return FACET_OR.concat(query);
            }
            case OR_AND: {
                if (!fqFields.contains(filterKey)) {
                    return FACET_OR.concat(query);
                } else {
                    return query;
                }
            }
            default: {
                return query;
            }
        }
    }

    public List<TurSESearchField> prepareQueryFacet(TurSESiteSettings turSNSite, SolrQuery query,
            TurSNFilterParams turSNFilterParams, TurSEParameters turSEParameters) {
        List<TurSESearchField> enabledFacets = siteSearchConfig.facetPlanFacets(turSNSite.id());
        Set<String> removedFacets = turSEParameters != null ? turSEParameters.getRemovedFacets() : null;
        Set<String> addedFacets = turSEParameters != null ? turSEParameters.getAddedFacets() : null;
        if (removedFacets != null && !removedFacets.isEmpty()) {
            enabledFacets = enabledFacets.stream()
                    .filter(f -> !removedFacets.contains(f.name()))
                    .toList();
        }
        if (addedFacets != null && !addedFacets.isEmpty()) {
            List<TurSESearchField> allFields = siteSearchConfig.facetPlanFields(turSNSite.id());
            Set<String> existingNames = enabledFacets.stream()
                    .map(TurSESearchField::name).collect(Collectors.toSet());
            List<TurSESearchField> extraFacets = allFields.stream()
                    .filter(f -> addedFacets.contains(f.name()) && !existingNames.contains(f.name()))
                    .toList();
            enabledFacets = new ArrayList<>(enabledFacets);
            enabledFacets.addAll(extraFacets);
        }
        // T1250 — the request leaves these facets out, rules included: Solr never computes them.
        if (turSEParameters != null && turSEParameters.getExcludedFacets() != null
                && !turSEParameters.getExcludedFacets().isEmpty()) {
            enabledFacets = enabledFacets.stream()
                    .filter(f -> !turSEParameters.isFacetExcluded(f.name()))
                    .toList();
        }
        return setFacetFields(turSNSite, query, turSNFilterParams, enabledFacets);
    }

    public List<TurSESearchField> prepareQueryFacetWithOneFacet(TurSESiteSettings turSNSite,
            SolrQuery query, TurSNFilterParams turSNFilterParams, String facetName) {
        List<TurSESearchField> enabledFields = siteSearchConfig.facetPlanFields(turSNSite.id());
        Set<String> fieldIdsWithMatchingCustomFacet = siteSearchConfig
                .fieldCustomFacets(turSNSite.id()).stream()
                .filter(cf -> facetName.equals(cf.name()))
                .map(TurSECustomFacet::fieldId)
                .collect(Collectors.toSet());
        List<TurSESearchField> enabledFacets = enabledFields.stream()
                .filter(field -> field.name().equals(facetName)
                        || fieldIdsWithMatchingCustomFacet.contains(field.id()))
                .toList();
        return setFacetFields(turSNSite, query, turSNFilterParams, enabledFacets);
    }

    private List<TurSESearchField> setFacetFields(TurSESiteSettings turSNSite, SolrQuery query,
            TurSNFilterParams turSNFilterParams, List<TurSESearchField> enabledFacets) {
        if (wasFacetConfigured(turSNSite, enabledFacets)) {
            query.setFacet(true).setFacetLimit(turSNSite.itemsPerFacet())
                    .setFacetSort(facetSortIsEmptyOrCount(turSNSite) ? COUNT : INDEX);
            Set<String> enabledFacetIds = enabledFacets.stream()
                    .map(TurSESearchField::id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<String, List<TurSECustomFacet>> customFacetsByFieldId =
                    siteSearchConfig.fieldCustomFacets(turSNSite.id())
                            .stream()
                            .filter(cf -> enabledFacetIds.contains(cf.fieldId()))
                            .collect(Collectors.groupingBy(TurSECustomFacet::fieldId));
            enabledFacets.forEach(turSNSiteFacetFieldExt -> {
                setFacetSort(query, turSNSiteFacetFieldExt);
                if (isFacetEnabled(turSNSiteFacetFieldExt)) {
                    if (isDateRangeFacet(turSNSiteFacetFieldExt))
                        addFacetRange(turSNSiteFacetFieldExt, query);
                    else
                        addFacetField(turSNSite, turSNFilterParams, turSNSiteFacetFieldExt, query);
                }

                List<TurSECustomFacet> fieldCustomFacets = customFacetsByFieldId
                        .getOrDefault(turSNSiteFacetFieldExt.id(), Collections.emptyList());
                if (!fieldCustomFacets.isEmpty())
                    addCustomFacetQuery(turSNSite, turSNFilterParams, query, fieldCustomFacets);
            });
        }
        return enabledFacets;
    }

    private static boolean isFacetEnabled(TurSESearchField turSNSiteFieldExt) {
        return turSNSiteFieldExt.facet() == 1;
    }

    private static void setFacetSort(SolrQuery query, TurSESearchField turSNSiteFacetFieldExt) {
        Optional.of(turSNSiteFacetFieldExt).map(TurSESearchField::facetSort)
                .filter(field -> !turSNSiteFacetFieldExt.facetSort()
                        .equals(TurSNSiteFacetFieldSortEnum.DEFAULT))
                .ifPresent(
                        field -> query
                                .set("f." + turSNSiteFacetFieldExt.name() + ".facet.sort",
                                        turSNSiteFacetFieldExt.facetSort().equals(
                                                TurSNSiteFacetFieldSortEnum.ALPHABETICAL) ? INDEX
                                                        : COUNT));
    }

    private static boolean facetSortIsEmptyOrCount(TurSESiteSettings turSNSite) {
        return turSNSite.facetSort() == null
                || turSNSite.facetSort().equals(TurSNSiteFacetSortEnum.COUNT);
    }

    private void addFacetField(TurSESiteSettings turSNSite, TurSNFilterParams turSNFilterParams,
            TurSESearchField facetField, SolrQuery query) {
        query.addFacetField(setFacetTypeConditionInFacet(turSNSite, turSNFilterParams,
                facetField.facetType(), facetField.facetItemType(), facetField.name(), facetField.name()));
    }

    private void addFacetRange(TurSESearchField facetField, SolrQuery query) {
        String fieldName = facetField.name();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        query.addDateRangeFacet(
                fieldName,
                DateUtils.addYears(cal.getTime(), -100),
                DateUtils.addYears(cal.getTime(), 100),
                PLUS_ONE + facetField.facetRange());
    }

    /**
     * One facet query per labelled item of each custom facet on a field. T1083 /
     * T1224 — the item's filter query is the one the Lucene engine reads too
     * ({@link TurSECustomFacet.Item#filterQuery()}), so one item means one range
     * on both.
     */
    private void addCustomFacetQuery(TurSESiteSettings turSNSite, TurSNFilterParams turSNFilterParams,
            SolrQuery query, List<TurSECustomFacet> customFacets) {
        customFacets.stream()
                .filter(entity -> StringUtils.hasText(entity.name()))
                .forEach(customFacet -> {
                    boolean excludeFacetValues = setFacetTypeConditionInFacet(turSNSite, turSNFilterParams,
                            orDefault(customFacet.facetType()), orDefault(customFacet.facetItemType()),
                            customFacet.name(), customFacet.name())
                            .startsWith(FACET_OR);
                    customFacet.items().stream()
                            .filter(item -> StringUtils.hasText(item.label()))
                            .forEach(item -> query.addFacetQuery(getCustomFacetLocalParams(
                                    customFacet.name(), item.label(), excludeFacetValues)
                                    + item.filterQuery()));
                });
    }

    private static String getCustomFacetLocalParams(String customFacetName,
            String itemLabel, boolean excludeFacetValues) {
        String key = String.format("%s%s%s", customFacetName,
                CUSTOM_FACET_QUERY_SEPARATOR, itemLabel);
        if (excludeFacetValues) {
            return String.format("{!key='%s' ex=_all_}", escapeLocalParamValue(key));
        }
        return String.format("{!key='%s'}", escapeLocalParamValue(key));
    }

    private static String escapeLocalParamValue(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private record CustomFacetFilterMapping(String customFacetName,
            TurSESearchField turSNSiteFieldExt,
            TurSECustomFacet customFacet,
            TurSECustomFacet.Item customFacetItem) {
    }

    public List<String> prepareQueryHL(TurSESiteSettings turSNSite, SolrQuery query,
            TurSNSiteSearchContext context) {
        List<String> turSNSiteHlFieldExtList = getHLFields(turSNSite);
        if (context.getTurSNConfig().isHlEnabled()) {
            StringBuilder hlFields = new StringBuilder();
            turSNSiteHlFieldExtList.forEach(turSNSiteHlFieldExt -> {
                if (!hlFields.isEmpty()) {
                    hlFields.append(",");
                }
                hlFields.append(turSNSiteHlFieldExt);
            });
            query.setHighlight(true).setHighlightSnippets(1)
                    .setParam(HighlightParams.FIELDS, hlFields.toString())
                    .setParam(HighlightParams.FRAGSIZE, "0")
                    .setParam(HighlightParams.SIMPLE_PRE, turSNSite.hlPre())
                    .setParam(HighlightParams.SIMPLE_POST, turSNSite.hlPost());
        }
        return turSNSiteHlFieldExtList;
    }

    private List<String> getHLFields(TurSESiteSettings turSNSite) {
        // T707 — never highlight identifier/URL fields even if a field is
        // (mis)configured with hl=1: a <mark> injected into `id` corrupts the
        // value clients feed back to /search/similar, silently breaking "Related".
        return siteSearchConfig.hlFields(turSNSite.id())
                .stream()
                .filter(field -> !TurSNFieldName.isNonHighlightable(field))
                .toList();
    }

    private void setRows(TurSESiteSettings turSNSite, TurSEParameters turSEParameters) {
        if (turSEParameters.getRows() < 0) {
            turSEParameters
                    .setRows((turSNSite.rowsPerPage() > 0) ? turSNSite.rowsPerPage() : 10);
        }
    }

    private void setSortEntry(TurSESiteSettings turSNSite, SolrQuery query,
            TurSEParameters turSEParameters) {
        Optional.ofNullable(turSEParameters.getSort())
                .ifPresent(sort -> {
                    Optional<KeyValue<String, String>> kv = TurCommonsUtils.getKeyValueFromColon(sort);
                    if (kv.isPresent()) {
                        query.setSort(kv.get().getKey(),
                                kv.get().getValue().equals(ASC) ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
                    } else if (sort.equalsIgnoreCase(NEWEST)) {
                        query.setSort(turSNSite.defaultDateField(), SolrQuery.ORDER.desc);
                    } else if (sort.equalsIgnoreCase(OLDEST)) {
                        query.setSort(turSNSite.defaultDateField(), SolrQuery.ORDER.asc);
                    } else if (!sort.equalsIgnoreCase("relevance")) {
                        applyCustomSort(turSNSite, query, sort);
                    }
                });
    }

    private void applyCustomSort(TurSESiteSettings turSNSite, SolrQuery query, String sortName) {
        siteSearchConfig.customSort(turSNSite.id(), sortName)
                .ifPresent(items -> {
                    List<SolrQuery.SortClause> clauses = customSortClauses(items);
                    if (!clauses.isEmpty()) {
                        query.setSorts(clauses);
                    }
                });
    }

    /**
     * The sort clauses of a custom sort, whose levels arrive in position order.
     * T1104 — a level that says where documents missing its field go is preceded by
     * {@code if(exists(field),0,1) asc} (or {@code 1,0} for first), since Solr
     * places missing values by schema, not by request.
     */
    static List<SolrQuery.SortClause> customSortClauses(List<TurSECustomSortItem> items) {
        List<SolrQuery.SortClause> clauses = new ArrayList<>();
        items.forEach(item -> {
            if (item.missing() != null) {
                clauses.add(SolrQuery.SortClause.asc(item.missing() == TurSNSiteCustomSortMissingEnum.LAST
                        ? "if(exists(%s),0,1)".formatted(item.fieldName())
                        : "if(exists(%s),1,0)".formatted(item.fieldName())));
            }
            clauses.add(item.descending()
                    ? SolrQuery.SortClause.desc(item.fieldName())
                    : SolrQuery.SortClause.asc(item.fieldName()));
        });
        return clauses;
    }

    private boolean hasMLT(TurSESiteSettings turSNSite, List<String> turSNSiteMLTFieldExtList) {
        return TurSEFlags.isTrue(turSNSite.mlt())
                && !CollectionUtils.isEmpty(turSNSiteMLTFieldExtList);
    }

    private void prepareQueryTargetingRules(TurSNSitePostParamsBean turSNSitePostParamsBean,
            SolrQuery query) {
        if (isTargetingRulesWithoutCondition(turSNSitePostParamsBean)) {
            targetingRulesWithoutCondition(turSNSitePostParamsBean, query);
        } else if (isTargetingRulesWithCondition(turSNSitePostParamsBean)) {
            targetingRulesWithCondition(turSNSitePostParamsBean, query);
        }
    }

    private static boolean isTargetingRulesWithCondition(
            TurSNSitePostParamsBean turSNSitePostParamsBean) {
        return !CollectionUtils.isEmpty(turSNSitePostParamsBean.getTargetingRulesWithCondition())
                || !CollectionUtils
                        .isEmpty(turSNSitePostParamsBean.getTargetingRulesWithConditionAND())
                || !CollectionUtils
                        .isEmpty(turSNSitePostParamsBean.getTargetingRulesWithConditionOR());
    }

    private static boolean isTargetingRulesWithoutCondition(
            TurSNSitePostParamsBean turSNSitePostParamsBean) {
        return !CollectionUtils.isEmpty(turSNSitePostParamsBean.getTargetingRules());
    }

    private void targetingRulesWithoutCondition(TurSNSitePostParamsBean turSNSitePostParamsBean,
            SolrQuery query) {
        if (!CollectionUtils.isEmpty(turSNSitePostParamsBean.getTargetingRules()))
            query.addFilterQuery(turSNTargetingRules.ruleExpression(TurSNTargetingRuleMethod.AND,
                    turSNSitePostParamsBean.getTargetingRules()));
    }

    private void targetingRulesWithCondition(TurSNSitePostParamsBean turSNSitePostParamsBean,
            SolrQuery query) {
        Map<String, List<String>> formattedRulesAND = new HashMap<>();
        Map<String, List<String>> formattedRulesOR = new HashMap<>();
        Set<String> conditions = new HashSet<>();
        targetingRulesWithCondition(turSNSitePostParamsBean.getTargetingRulesWithCondition(),
                formattedRulesAND, conditions);
        targetingRulesWithCondition(turSNSitePostParamsBean.getTargetingRulesWithConditionAND(),
                formattedRulesAND, conditions);
        targetingRulesWithCondition(turSNSitePostParamsBean.getTargetingRulesWithConditionOR(),
                formattedRulesOR, conditions);
        List<String> rules = new ArrayList<>();
        conditions.forEach(condition -> {
            StringBuilder rule = new StringBuilder();
            boolean containAndRules = formattedRulesAND.containsKey(condition);
            boolean containOrRules = formattedRulesOR.containsKey(condition);
            if (containAndRules)
                rule.append(turSNTargetingRules.andMethod(formattedRulesAND.get(condition)));
            if (containOrRules) {
                rule.append(
                        containAndRules
                                ? String.format(" AND (%s)",
                                        turSNTargetingRules
                                                .orMethod(formattedRulesOR.get(condition)))
                                : turSNTargetingRules.orMethod(formattedRulesOR.get(condition)));
            }
            rules.add(String.format("( %s AND ( %s ) )", condition, rule));
        });

        String targetingRuleQuery = String.format("%s OR (*:* NOT ( %s ) )",
                String.join(betweenSpaces(OR), rules), String.join(betweenSpaces(OR), conditions));
        query.addFilterQuery(targetingRuleQuery);
    }

    private void addFormattedRules(Map<String, List<String>> formattedRules, String key,
            List<String> value) {
        if (formattedRules.containsKey(key))
            formattedRules.get(key).addAll(value);
        else
            formattedRules.put(key, value);
    }

    private void targetingRulesWithCondition(Map<String, List<String>> targetingRulesWithCondition,
            Map<String, List<String>> formattedRules, Set<String> conditions) {
        if (!CollectionUtils.isEmpty(targetingRulesWithCondition)) {
            targetingRulesWithCondition.forEach((key, value) -> {
                conditions.add(key);
                addFormattedRules(formattedRules, key, value);
            });
        }
    }

    private boolean wasFacetConfigured(TurSESiteSettings turSNSite,
            List<TurSESearchField> turSNSiteFacetFieldExtList) {
        return TurSEFlags.isTrue(turSNSite.facet()) && turSNSite.itemsPerFacet() != null
                && !CollectionUtils.isEmpty(turSNSiteFacetFieldExtList);
    }
}
