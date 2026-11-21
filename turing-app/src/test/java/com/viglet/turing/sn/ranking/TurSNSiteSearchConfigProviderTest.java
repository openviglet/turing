package com.viglet.turing.sn.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingCondition.Kind;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingCondition;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingExpression;
import com.viglet.turing.sn.persistence.repository.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.sn.persistence.repository.ranking.TurSNRankingExpressionRepository;
import com.viglet.turing.sn.persistence.repository.sort.TurSNSiteCustomSortRepository;
import com.viglet.turing.sn.persistence.model.sort.TurSNSiteCustomSort;
import com.viglet.turing.sn.persistence.model.sort.TurSNSiteCustomSortItem;
import com.viglet.turing.searchengine.TurSNSiteCustomSortMissingEnum;
import com.viglet.turing.searchengine.TurSNSiteCustomSortOrderEnum;
import com.viglet.turing.searchengine.spi.TurSECustomSortItem;

/**
 * T1310 — sn hands the engines its ranking expressions as SPI records, so the
 * engines no longer read sn's entities.
 */
@ExtendWith(MockitoExtension.class)
class TurSNSiteSearchConfigProviderTest {

    @Mock
    private TurSNRankingExpressionRepository expressionRepository;
    @Mock
    private TurSNRankingConditionRepository conditionRepository;
    @Mock
    private TurSNSiteCustomSortRepository customSortRepository;
    @Mock
    private com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository fieldExtRepository;
    @Mock
    private com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldRepository fieldRepository;
    @Mock
    private com.viglet.turing.sn.api.TurSNSites siteRepository;
    @Mock
    private com.viglet.turing.sn.api.TurSNSiteLocales siteLocaleRepository;
    @Mock
    private com.viglet.turing.sn.domain.TurSNSiteLocaleRepositoryPort siteLocaleRepositoryPort;

    private TurSNSiteSearchConfigProvider provider() {
        return new TurSNSiteSearchConfigProvider(expressionRepository, conditionRepository, customSortRepository,
                fieldExtRepository, fieldRepository, siteRepository, siteLocaleRepository,
                siteLocaleRepositoryPort);
    }

    private static TurSNRankingCondition condition(String attribute, int kind, String value) {
        TurSNRankingCondition condition = new TurSNRankingCondition();
        condition.setAttribute(attribute);
        condition.setCondition(kind);
        condition.setValue(value);
        return condition;
    }

    @Test
    void expressionsComeWithTheirConditionsInRepositoryOrder() {
        TurSNRankingExpression recent = new TurSNRankingExpression();
        recent.setName("recent");
        recent.setWeight(2f);
        TurSNRankingExpression empty = new TurSNRankingExpression();
        empty.setName("empty");
        empty.setWeight(1f);
        when(expressionRepository.findByTurSNSiteId(any(), eq("site-1"))).thenReturn(List.of(recent, empty));
        when(conditionRepository.findByTurSNRankingExpression(recent))
                .thenReturn(Set.of(condition("publicationDate", 0, "asc")));
        when(conditionRepository.findByTurSNRankingExpression(empty)).thenReturn(Set.of());

        List<TurSERankingExpression> expressions = provider().rankingExpressions("site-1");

        assertThat(expressions).containsExactly(
                new TurSERankingExpression("recent", 2f,
                        List.of(new TurSERankingCondition("publicationDate", Kind.IS, "asc"))),
                new TurSERankingExpression("empty", 1f, List.of()));
    }

    @Test
    void theConditionKindsMapOneToOne() {
        assertThat(TurSNSiteSearchConfigProvider.toCondition(condition("a", TurSNRankingCondition.IS, "x")).kind())
                .isEqualTo(Kind.IS);
        assertThat(TurSNSiteSearchConfigProvider.toCondition(condition("a", TurSNRankingCondition.IS_NOT, "x")).kind())
                .isEqualTo(Kind.IS_NOT);
        assertThat(TurSNSiteSearchConfigProvider.toCondition(condition("a", TurSNRankingCondition.EXISTS, null)).kind())
                .isEqualTo(Kind.EXISTS);
    }

    @Test
    void aSiteWithoutAnIdHasNoExpressions() {
        assertThat(provider().rankingExpressions(null)).isEmpty();
        verifyNoInteractions(expressionRepository, conditionRepository, customSortRepository, fieldExtRepository,
                fieldRepository);
    }

    @Test
    void aCustomSortComesInPositionOrderWithUnpositionedLevelsLast() {
        TurSNSiteCustomSort sort = TurSNSiteCustomSort.builder().name("closingSoonest").build();
        sort.setItems(new HashSet<>(List.of(
                TurSNSiteCustomSortItem.builder().fieldName("title").build(),
                TurSNSiteCustomSortItem.builder().fieldName("publishedAt").position(2)
                        .sortOrder(TurSNSiteCustomSortOrderEnum.DESC).build(),
                TurSNSiteCustomSortItem.builder().fieldName("expiresAt").position(1)
                        .missing(TurSNSiteCustomSortMissingEnum.LAST).build())));
        when(customSortRepository.findBySiteIdAndName("site-1", "closingSoonest")).thenReturn(Optional.of(sort));

        assertThat(provider().customSort("site-1", "closingSoonest")).contains(List.of(
                new TurSECustomSortItem("expiresAt", TurSNSiteCustomSortOrderEnum.ASC,
                        TurSNSiteCustomSortMissingEnum.LAST),
                new TurSECustomSortItem("publishedAt", TurSNSiteCustomSortOrderEnum.DESC, null),
                new TurSECustomSortItem("title", TurSNSiteCustomSortOrderEnum.ASC, null)));
    }

    @Test
    void anUnknownCustomSortIsEmpty() {
        when(customSortRepository.findBySiteIdAndName("site-1", "nope")).thenReturn(Optional.empty());

        assertThat(provider().customSort("site-1", "nope")).isEmpty();
    }

    @Test
    void fieldTypesKeepTheFirstFieldOfAName() {
        when(fieldExtRepository.findByTurSNSiteId(any(), eq("site-1"))).thenReturn(List.of(
                com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt.builder().name("price")
                        .type(com.viglet.turing.commons.se.field.TurSEFieldType.DOUBLE).build(),
                com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt.builder().name("price")
                        .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING).build(),
                com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt.builder().name("untyped").build()));

        assertThat(provider().fieldTypes("site-1"))
                .containsExactly(java.util.Map.entry("price", com.viglet.turing.commons.se.field.TurSEFieldType.DOUBLE));
    }

    @Test
    void indexFieldsCarryTypeAndMultiValuedWithTheLastFieldOfANameWinning() {
        when(fieldRepository.findByTurSNSiteId("site-1")).thenReturn(List.of(
                com.viglet.turing.sn.persistence.model.field.TurSNSiteField.builder().name("tags")
                        .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING).multiValued(0).build(),
                com.viglet.turing.sn.persistence.model.field.TurSNSiteField.builder().name("tags")
                        .type(com.viglet.turing.commons.se.field.TurSEFieldType.STRING).multiValued(1).build(),
                com.viglet.turing.sn.persistence.model.field.TurSNSiteField.builder().name("untyped").build()));

        assertThat(provider().indexFields("site-1")).containsOnly(
                java.util.Map.entry("tags", new com.viglet.turing.searchengine.spi.TurSEIndexField("tags",
                        com.viglet.turing.commons.se.field.TurSEFieldType.STRING, true)),
                java.util.Map.entry("untyped",
                        new com.viglet.turing.searchengine.spi.TurSEIndexField("untyped", null, false)));
    }

    @Test
    void aSiteWithoutAnIdHasNoIndexFields() {
        assertThat(provider().indexFields(null)).isEmpty();
        verifyNoInteractions(fieldRepository);
    }

    @Test
    void aSitesCoreNamesItsSEInstanceAndTheLocalesCore() {
        com.viglet.turing.sn.persistence.model.TurSNSite site = new com.viglet.turing.sn.persistence.model.TurSNSite();
        site.setName("Docs");
        site.setTurSEInstanceId("se-1");
        com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale siteLocale =
                new com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale();
        siteLocale.setCore("docs_en");
        when(siteRepository.findByNameIgnoreCase("docs")).thenReturn(Optional.of(site));
        when(siteLocaleRepository.findByTurSNSiteAndLanguage(site, java.util.Locale.US)).thenReturn(siteLocale);

        assertThat(provider().siteCore("docs", java.util.Locale.US)).contains(
                new com.viglet.turing.searchengine.spi.TurSESiteCore("Docs", "se-1", "docs_en"));
    }

    @Test
    void aSiteWithoutTheLocaleOrUnknownHasNoCore() {
        com.viglet.turing.sn.persistence.model.TurSNSite site = new com.viglet.turing.sn.persistence.model.TurSNSite();
        site.setName("Docs");
        when(siteRepository.findByNameIgnoreCase("docs")).thenReturn(Optional.of(site));
        when(siteLocaleRepository.findByTurSNSiteAndLanguage(site, java.util.Locale.FRENCH)).thenReturn(null);
        when(siteRepository.findByNameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThat(provider().siteCore("docs", java.util.Locale.FRENCH)).isEmpty();
        assertThat(provider().siteCore("missing", java.util.Locale.US)).isEmpty();
        assertThat(provider().siteCore(null, java.util.Locale.US)).isEmpty();
    }

    @Test
    void aCoresLanguageIsTheFirstLocaleThatNamesOne() {
        com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale unnamed =
                new com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale();
        com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale english =
                new com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale();
        english.setLanguage(java.util.Locale.US);
        when(siteLocaleRepository.findByCore("docs_en")).thenReturn(List.of(unnamed, english));

        assertThat(provider().coreLanguage("docs_en")).contains(java.util.Locale.US);
        assertThat(provider().coreLanguage(null)).isEmpty();
    }

    @Test
    void aSiteIdIsFoundByNameIgnoringCase() {
        com.viglet.turing.sn.persistence.model.TurSNSite site = new com.viglet.turing.sn.persistence.model.TurSNSite();
        site.setId("site-1");
        when(siteRepository.findByNameIgnoreCase("Docs")).thenReturn(Optional.of(site));
        when(siteRepository.findByNameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThat(provider().siteId("Docs")).contains("site-1");
        assertThat(provider().siteId("missing")).isEmpty();
        assertThat(provider().siteId(null)).isEmpty();
    }

    @Test
    void aCoresUsageNamesEachSiteLocaleThatWritesToIt() {
        when(siteLocaleRepositoryPort.findCoreUsage("core1")).thenReturn(List.of(
                new com.viglet.turing.sn.domain.TurSNSiteCoreUsageDomain("locale-1", java.util.Locale.US, "site-1",
                        "Docs")));
        when(siteLocaleRepositoryPort.existsByCore("core1")).thenReturn(true);

        assertThat(provider().coreUsage("core1")).containsExactly(
                new com.viglet.turing.searchengine.spi.TurSECoreUsage("site-1", "Docs", "locale-1",
                        java.util.Locale.US));
        assertThat(provider().coreInUse("core1")).isTrue();
    }}
