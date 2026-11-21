package com.viglet.turing.sn.persistence.repository.searchrule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.searchrule.TurSNSiteSearchRule;

/**
 * Repository for {@link TurSNSiteSearchRule}.
 *
 * @author Alexandre Oliveira
 * @since 2026.2.4
 */
public interface TurSNSiteSearchRuleRepository extends JpaRepository<TurSNSiteSearchRule, String> {

    @Query("SELECT DISTINCT sr FROM TurSNSiteSearchRule sr " +
           "LEFT JOIN FETCH sr.conditions " +
           "LEFT JOIN FETCH sr.actions " +
           "WHERE sr.turSNSite = :site " +
           "ORDER BY sr.position")
    List<TurSNSiteSearchRule> findByTurSNSiteWithDetails(@Param("site") TurSNSite site);

    @Query("SELECT DISTINCT sr FROM TurSNSiteSearchRule sr " +
           "LEFT JOIN FETCH sr.conditions " +
           "LEFT JOIN FETCH sr.actions " +
           "WHERE sr.turSNSite = :site AND sr.id = :id")
    Optional<TurSNSiteSearchRule> findByTurSNSiteAndId(@Param("site") TurSNSite site,
                                                        @Param("id") String id);

    @Query("SELECT DISTINCT sr FROM TurSNSiteSearchRule sr " +
           "LEFT JOIN FETCH sr.conditions " +
           "LEFT JOIN FETCH sr.actions " +
           "WHERE sr.turSNSite.id = :siteId AND sr.enabled = 1 " +
           "ORDER BY sr.position")
    List<TurSNSiteSearchRule> findEnabledByTurSNSiteId(@Param("siteId") String siteId);
}
