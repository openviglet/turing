/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent.persistence.model.customtool;

import java.io.Serial;
import java.io.Serializable;

import com.viglet.core.jpa.VigletAssignableUuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * User-defined Spring AI tool callable backed by a Groovy script. The
 * {@code parametersJson} column stores the JSON-serialized list of
 * {@code {name,type}} pairs declaring which arguments the LLM is
 * allowed to pass; {@code returnType} is the JSON-Schema primitive name
 * of the value the script returns.
 *
 * @author Alexandre Oliveira
 * @since 2026.2.5
 */
@Getter
@Setter
@Entity
@Table(name = "agent_custom_tool")
public class TurCustomTool implements Serializable {
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

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "descriptionMetaPrompt", columnDefinition = "longtext")
    private String descriptionMetaPrompt;

    @Column(length = 150)
    private String icon;

    @Column(name = "llmDescription", nullable = false, length = 2000)
    private String llmDescription;

    @Column(name = "llmDescriptionMetaPrompt", columnDefinition = "longtext")
    private String llmDescriptionMetaPrompt;

    @Column(name = "groovyScript", nullable = false, columnDefinition = "longtext")
    private String groovyScript;

    @Column(name = "groovyMetaPrompt", columnDefinition = "longtext")
    private String groovyMetaPrompt;

    @Column(name = "parametersJson", columnDefinition = "longtext")
    private String parametersJson;

    @Column(name = "returnType", nullable = false, length = 20)
    private String returnType;

    @Column(nullable = false)
    private int enabled;
}
