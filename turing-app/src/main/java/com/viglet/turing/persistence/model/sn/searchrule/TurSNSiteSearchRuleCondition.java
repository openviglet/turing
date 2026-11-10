package com.viglet.turing.persistence.model.sn.searchrule;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viglet.turing.persistence.utils.TurAssignableUuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A condition that must match for a search rule to be applied.
 *
 * @author Alexandre Oliveira
 * @since 2026.2.4
 */
@Entity
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "sn_site_search_rule_condition")
@JsonIgnoreProperties({"turSNSiteSearchRule"})
public class TurSNSiteSearchRuleCondition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Order in which conditions are combined: saved {@code position} first,
     * then {@code id} so legacy rows without a position still evaluate the
     * same way on every load.
     *
     * @since 2026.2.9
     */
    public static final Comparator<TurSNSiteSearchRuleCondition> POSITION_ORDER = Comparator
            .comparing(TurSNSiteSearchRuleCondition::getPosition, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(TurSNSiteSearchRuleCondition::getId, Comparator.nullsLast(String::compareTo));

    @Id
    @TurAssignableUuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "parameter", nullable = false, length = 50)
    private TurSNSiteSearchRuleParameterEnum parameter;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 20)
    private TurSNSiteSearchRuleOperatorEnum operator;

    @Column(name = "fieldName", length = 255)
    private String fieldName;

    @Column(name = "value", length = 500)
    private String value;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "logicOperator", length = 5)
    private TurSNSiteSearchRuleLogicOperatorEnum logicOperator = TurSNSiteSearchRuleLogicOperatorEnum.AND;

    /**
     * Zero-based order of the condition inside its rule, as the user arranged
     * it in the console. AND/OR are folded left to right in this order, so it
     * changes the result when a group mixes both. Null on rules saved before
     * this column existed.
     *
     * @since 2026.2.9
     */
    @Column(name = "position")
    private Integer position;

    @ManyToOne
    @JoinColumn(name = "search_rule_id", nullable = false)
    private TurSNSiteSearchRule turSNSiteSearchRule;
}
