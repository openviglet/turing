package com.viglet.turing.sn.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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

    private TurSNSiteSearchConfigProvider provider() {
        return new TurSNSiteSearchConfigProvider(expressionRepository, conditionRepository);
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
        verifyNoInteractions(expressionRepository, conditionRepository);
    }
}
