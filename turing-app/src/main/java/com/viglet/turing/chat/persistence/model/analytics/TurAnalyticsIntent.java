/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.chat.persistence.model.analytics;

import com.viglet.turing.spring.security.TurAuditable;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.viglet.core.jpa.VigletAssignableUuidGenerator;
import com.viglet.turing.chat.analytics.TurAnalyticsIntentEvictionListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * T28 / §III.5 — per-agent intent catalog entry for the post-hoc chat
 * analytics classifier. Each row carries one human-readable
 * {@link #label} (e.g. {@code "Refund request"}, {@code "Career growth
 * questions"}) plus a newline-separated bag of {@link #samples} —
 * representative user utterances the {@code TurChatIntentClassifier}
 * strategies score session transcripts against.
 *
 * <p>The catalog is the per-agent <em>intent vocabulary</em> the
 * non-LLM classifiers ({@code TurLuceneIntentClassifier} via embedded
 * {@code MoreLikeThis}; the deferred {@code TurEsMltIntentClassifier}
 * via Elasticsearch {@code more_like_this}) work against. Independent
 * from {@link com.viglet.turing.agent.persistence.model.TurChatFlow}'s
 * {@code triggerDescription}: chat flows describe <em>routing</em>
 * intents (where to send a turn); analytics intents describe
 * <em>categorisation</em> intents (what the conversation was about).
 * Same agent can have both, with different vocabularies.
 *
 * <p>Samples are stored as a single newline-separated {@link Lob} for
 * write simplicity — the cost of a junction table buys nothing for the
 * MLT scoring pipeline, which concatenates samples anyway before
 * indexing. Empty samples allowed (admins can stage labels before
 * filling them in); the classifier skips entries with blank samples.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.1
 */
@Getter
@Setter
@Entity
@Table(name = "chat_analytics_intent",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_analytics_intent_agent_label",
                columnNames = { "agent_id", "label" }))
@EntityListeners(TurAnalyticsIntentEvictionListener.class)
public class TurAnalyticsIntent extends TurAuditable<String> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @VigletAssignableUuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * T1319 — optimistic-lock version: a console save made from an older version
     * is refused (409) instead of overwriting a change it never saw.
     */
    @jakarta.persistence.Version
    @jakarta.persistence.Column(name = "version")
    private Long version;

    /** T261 / §XIV.2.5 — multi-tenancy discriminator (see TurSNSite pilot). Hibernate-managed. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "tenantId", length = 40)
    private String tenantId;

    /**
     * Human-readable intent label rendered on dashboards (e.g.
     * {@code "Refund request"}, {@code "Plano de carreira"}). Unique per
     * agent — same wording across agents is fine, the unique constraint
     * is {@code (agent_id, label)}.
     */
    @Column(name = "label", nullable = false, length = 128)
    private String label;

    /**
     * Newline-separated bag of representative user utterances the
     * classifier matches conversation transcripts against. A long-text column
     * because real-world catalogs ship dozens of samples per intent and
     * VARCHAR caps lead to silent truncation. Empty/blank is allowed —
     * the classifier just skips the entry.
     */
    @Column(name = "samples", columnDefinition = "longtext")
    private String samples;

    /**
     * Disables the row without deleting it — useful when staging a new
     * intent or sunsetting an old one without losing history. The
     * classifier filters {@code enabled == 1} on every cycle.
     */
    @Column(name = "enabled", nullable = false)
    private int enabled = 1;

    /**
     * Optional admin notes / description (why this intent exists, what
     * it captures). Not consumed by the classifier; rendered on the
     * admin UI for context.
     */
    @Column(name = "description", columnDefinition = "longtext")
    private String description;

    /**
     * Owning AI agent — the catalog is agent-scoped because intent
     * vocabularies differ per product/persona. {@code JsonIgnore} keeps
     * the payload tight (the URL already encodes the agent id).
     */
    /** T1315 — the owning agent, by id: agents belong to the agent module. */
    @Column(name = "agent_id", nullable = false, length = 36)
    @JsonIgnore
    private String agentId;

    /**
     * T1319 — the audited dates under the names this entity's JSON has always used
     * (ADR 0005 section 7).
     */
    public Instant getCreatedAt() {
        return creationDate;
    }

    public Instant getUpdatedAt() {
        return lastModifiedDate;
    }
}
