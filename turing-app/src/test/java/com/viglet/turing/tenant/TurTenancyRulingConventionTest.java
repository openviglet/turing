/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.annotations.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import jakarta.persistence.Entity;

/**
 * T1318 — every entity carries a tenancy ruling (ADR 0005, "Tenancy rulings").
 * A tenant-owned row uses Hibernate's {@code @TenantId}; a plain
 * {@code tenantId} column is allowed only where it is deliberate (the shared
 * BYO-infra pool, cross-tenant billing rollups, a lookup that precedes tenant
 * resolution); anything else is either platform-global or reached only through
 * a tenant-scoped owner. A new entity with no ruling fails here.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurTenancyRulingConventionTest {

    /** Plain {@code tenantId}, stamped by hand, readable across tenants on purpose. */
    static final Set<String> PLAIN_TENANT_COLUMN = Set.of(
            // §XIV.5.1 — the BYO-infra pool: null tenantId is the GLOBAL pool every tenant sees.
            "TurSEInstance", "TurLLMInstance", "TurEmbeddingModel", "TurStoreInstance",
            "TurIntegrationInstance", "TurMcpServer",
            // T276 — billing rolls usage up across tenants with GROUP BY tenantId.
            "TurLLMTokenUsage", "TurAiUsageDaily",
            // The bearer filter finds the key before any tenant is bound; the key names it.
            "TurGatewayKey");

    /** Platform-wide by design: catalogs, identity, and tenancy itself. */
    static final Set<String> GLOBAL = Set.of(
            "TurLLMVendor", "TurSEVendor", "TurStoreVendor", "TurLLMPrice", "TurLocale",
            "TurConfigVar", "TurSNSourceType",
            // One identity spans tenants; membership is what ties a user to one.
            "TurUser", "TurGroup", "TurRole", "TurPrivilege",
            "TurTenant", "TurTenantMembership");

    /** Rows reached through a tenant-scoped owner, which isolates them. */
    static final Map<String, String> THROUGH_OWNER = owners();

    private static Map<String, String> owners() {
        Map<String, String> owners = new HashMap<>();
        for (String child : List.of("TurSNSiteField", "TurSNSiteFieldExt", "TurSNSiteFieldExtFacet",
                "TurSNSiteCustomFacet", "TurSNSiteCustomFacetItem", "TurSNSiteCustomSort",
                "TurSNSiteCustomSortItem", "TurSNSiteGenAi", "TurSNSiteLocale",
                "TurSNSiteMergeProviders", "TurSNSiteMergeProvidersField", "TurSNSiteSearchRule",
                "TurSNSiteSearchRuleAction", "TurSNSiteSearchRuleCondition", "TurSNSiteSpotlight",
                "TurSNSiteSpotlightDocument", "TurSNSiteSpotlightTerm", "TurSNRankingExpression",
                "TurSNRankingCondition", "TurSNSiteMetricAccess", "TurSNSiteMetricClick",
                "TurSNSiteMicrothesaurus", "TurSNSiteMicrothesaurusConfig", "TurAnswerShare", "TurSNSynonym")) {
            owners.put(child, "TurSNSite");
        }
        for (String child : List.of("TurAIAgentSlot", "TurChatCitationRecord", "TurChatSlotAuditEntry",
                "TurChatTurnFeedback", "TurDistillationJob", "TurAnswerSignalEvent")) {
            owners.put(child, "TurAIAgent");
        }
        owners.put("TurIntentAction", "TurIntent");
        owners.put("TurAgentEvalCase", "TurAgentEvalSet");
        owners.put("TurEvalDatasetRow", "TurEvalDataset");
        owners.put("TurEvalGraderConfig", "TurEvalGraderStack");
        owners.put("TurMicrothesaurus", "TurKnowledgeBase");
        owners.put("TurThesaurusTerm", "TurKnowledgeBase");
        owners.put("TurThesaurusTermRelation", "TurKnowledgeBase");
        owners.put("TurLLMInstanceCapability", "TurLLMInstance");
        owners.put("TurVendorFile", "TurLLMInstance");
        owners.put("TurBatchJob", "TurLLMInstance");
        // T1341 — a gateway batch input file belongs to the key that uploaded it.
        owners.put("TurGatewayBatchFile", "TurGatewayKey");
        owners.put("TurRagBm25Core", "TurStoreInstance");
        return owners;
    }

    private enum Column { TENANT_ID, PLAIN, NONE }

    private static Map<String, Column> entities() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        Map<String, Column> entities = new HashMap<>();
        for (var candidate : scanner.findCandidateComponents("com.viglet.turing")) {
            Class<?> entity = Class.forName(candidate.getBeanClassName());
            Column column = Column.NONE;
            for (Class<?> c = entity; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(TenantId.class)) {
                        column = Column.TENANT_ID;
                    } else if (field.getName().equals("tenantId") && column == Column.NONE) {
                        column = Column.PLAIN;
                    }
                }
            }
            assertThat(entities.put(entity.getSimpleName(), column))
                    .as("entity names are unique: " + entity.getSimpleName()).isNull();
        }
        return entities;
    }

    @Test
    void everyEntityHasATenancyRuling() throws Exception {
        Map<String, Column> entities = entities();
        assertThat(entities).as("the scan found the model").hasSizeGreaterThan(100);
        List<String> offenders = new ArrayList<>();
        entities.forEach((name, column) -> {
            switch (column) {
                case TENANT_ID -> {
                    if (PLAIN_TENANT_COLUMN.contains(name) || GLOBAL.contains(name)
                            || THROUGH_OWNER.containsKey(name)) {
                        offenders.add(name + ": has @TenantId but is listed under another ruling");
                    }
                }
                case PLAIN -> {
                    if (!PLAIN_TENANT_COLUMN.contains(name)) {
                        offenders.add(name + ": a plain tenantId outside the deliberate list;"
                                + " a tenant-owned row uses @TenantId");
                    }
                }
                case NONE -> {
                    if (!GLOBAL.contains(name) && !THROUGH_OWNER.containsKey(name)) {
                        offenders.add(name + ": no tenant column, not global, no tenant-scoped owner");
                    }
                }
            }
        });
        assertThat(offenders).as("entities without a valid tenancy ruling").isEmpty();
    }

    @Test
    void theRulingListsNameRealEntitiesAndOwnersAreTenantScoped() throws Exception {
        Map<String, Column> entities = entities();
        Set<String> listed = new TreeSet<>(PLAIN_TENANT_COLUMN);
        listed.addAll(GLOBAL);
        listed.addAll(THROUGH_OWNER.keySet());
        assertThat(entities.keySet()).as("every listed name is an entity").containsAll(listed);
        THROUGH_OWNER.forEach((child, owner) -> assertThat(entities.get(owner))
                .as(child + "'s owner " + owner + " is tenant-scoped")
                .isIn(Column.TENANT_ID, Column.PLAIN));
    }
}
