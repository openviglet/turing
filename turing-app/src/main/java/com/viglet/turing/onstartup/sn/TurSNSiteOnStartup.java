/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.onstartup.sn;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.persistence.model.se.TurSEInstance;
import com.viglet.turing.persistence.model.se.TurSEVendor;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacet;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacetItem;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.se.TurSEInstanceRepository;
import com.viglet.turing.persistence.repository.se.TurSEVendorRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.template.TurSNTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Transactional
public class TurSNSiteOnStartup {
    public static final String SAMPLE_SITE_NAME = "Default Site";
    private static final String SAMPLE_SE_INSTANCE_TITLE = "Default Solr Instance";
    private static final String SAMPLE_CUSTOM_FACET_NAME = "price_range";

    private final TurSNSiteRepository turSNSiteRepository;
    private final TurSEInstanceRepository turSEInstanceRepository;
    private final TurSEVendorRepository turSEVendorRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    private final TurSNTemplate turSNTemplate;

    public TurSNSiteOnStartup(TurSNSiteRepository turSNSiteRepository,
            TurSEInstanceRepository turSEInstanceRepository,
            TurSEVendorRepository turSEVendorRepository,
            TurSNSiteFieldExtRepository turSNSiteFieldExtRepository,
            TurSNTemplate turSNTemplate) {
        this.turSNSiteRepository = turSNSiteRepository;
        this.turSEInstanceRepository = turSEInstanceRepository;
        this.turSEVendorRepository = turSEVendorRepository;
        this.turSNSiteFieldExtRepository = turSNSiteFieldExtRepository;
        this.turSNTemplate = turSNTemplate;
    }

    public void createDefaultRows() {
        log.info("[SN Startup] Initializing sample SN Site and custom facets...");
        TurSNSite sampleSite = turSNSiteRepository.findByName(SAMPLE_SITE_NAME)
                .orElseGet(this::createDefaultSite);
        if (sampleSite != null) {
            log.info("[SN Startup] Using SN Site '{}' (id={}) for sample data.",
                    sampleSite.getName(), sampleSite.getId());
            createCustomFacetExample(sampleSite);
        } else {
            log.warn("[SN Startup] Sample SN Site is null. Skipping custom facet initialization.");
        }
    }

    private TurSNSite createDefaultSite() {
        Optional<TurSEInstance> turSEInstance = getOrCreateDefaultSEInstance();
        if (turSEInstance.isEmpty()) {
            log.warn("Skipping sample SN Site creation because no Search Engine instance exists.");
            return null;
        }
        log.info("[SN Startup] Creating sample SN Site '{}' using SE Instance '{}' (id={}).",
                SAMPLE_SITE_NAME, turSEInstance.get().getTitle(), turSEInstance.get().getId());
        TurSNSite turSNSite = new TurSNSite();
        turSNSite.setName(SAMPLE_SITE_NAME);
        turSNSite.setDescription("Sample site created on first startup");
        turSNSite.setTurSEInstance(turSEInstance.get());
        turSNSiteRepository.save(turSNSite);
        turSNTemplate.createSNSite(turSNSite, "admin", Locale.US);
        log.info("[SN Startup] Sample SN Site '{}' created successfully (id={}).",
                turSNSite.getName(), turSNSite.getId());
        return turSNSite;
    }

    private Optional<TurSEInstance> getOrCreateDefaultSEInstance() {
        Optional<TurSEInstance> existingInstance = turSEInstanceRepository.findAll().stream()
                .findFirst();
        if (existingInstance.isPresent()) {
            log.info("[SN Startup] Reusing existing SE Instance '{}' (id={}).",
                    existingInstance.get().getTitle(), existingInstance.get().getId());
            return existingInstance;
        }

        Optional<TurSEVendor> seVendor = turSEVendorRepository.findById("SOLR")
                .or(() -> turSEVendorRepository.findAll().stream().findFirst());
        if (seVendor.isEmpty()) {
            log.warn("[SN Startup] Cannot create SE Instance: no SE Vendor found.");
            return Optional.empty();
        }
        log.info("[SN Startup] No SE Instance found. Creating default instance with vendor '{}'.",
                seVendor.get().getId());

        TurSEInstance defaultSEInstance = new TurSEInstance();
        defaultSEInstance.setTitle(SAMPLE_SE_INSTANCE_TITLE);
        defaultSEInstance.setDescription("Sample Search Engine Instance created on first startup");
        defaultSEInstance.setEnabled(1);
        defaultSEInstance.setHost("localhost");
        defaultSEInstance.setPort(8983);
        defaultSEInstance.setTurSEVendor(seVendor.get());
        TurSEInstance created = turSEInstanceRepository.save(defaultSEInstance);
        log.info("[SN Startup] Default SE Instance created (id={}, host={}, port={}).",
                created.getId(), created.getHost(), created.getPort());
        return Optional.of(created);
    }

    private void createCustomFacetExample(TurSNSite turSNSite) {
        Optional<TurSNSiteFieldExt> fieldExtOptional = turSNSiteFieldExtRepository
                .findByTurSNSiteAndEnabled(turSNSite, 1)
                .stream()
                .filter(fieldExt -> ID.equals(fieldExt.getName()))
                .findFirst();

        if (fieldExtOptional.isEmpty()) {
            log.warn("[SN Startup] Field '{}' (facet=1, enabled=1) was not found for site '{}'. " +
                    "Custom facet '{}' will not be created.",
                    ID, turSNSite.getName(), SAMPLE_CUSTOM_FACET_NAME);
            return;
        }

        TurSNSiteFieldExt fieldExt = fieldExtOptional.get();
        log.info("[SN Startup] Found field '{}' (id={}) for custom facet initialization.",
                fieldExt.getName(), fieldExt.getId());

        if (fieldExt.getFacet() != 1) {
            fieldExt.setFacet(1);
            if (fieldExt.getFacetName() == null || fieldExt.getFacetName().isBlank()) {
                fieldExt.setFacetName("Ids");
            }
            log.info("[SN Startup] Field '{}' had facet disabled. Enabled facet=1 for custom facet support.",
                    fieldExt.getName());
        }

        if (hasCustomFacet(fieldExt, SAMPLE_CUSTOM_FACET_NAME)) {
            log.info("[SN Startup] Custom facet '{}' already exists for field '{}'. Skipping creation.",
                    SAMPLE_CUSTOM_FACET_NAME, fieldExt.getName());
            turSNSiteFieldExtRepository.save(fieldExt);
            return;
        }

        TurSNSiteCustomFacet customFacet = new TurSNSiteCustomFacet();
        customFacet.setName(SAMPLE_CUSTOM_FACET_NAME);
        customFacet.setFacetPosition(getSampleCustomFacetPosition(fieldExt));
        customFacet.setLabel(getSampleFacetLabels());
        customFacet.setItems(getSampleFacetItems(customFacet));
        customFacet.setTurSNSiteFieldExt(fieldExt);

        Set<TurSNSiteCustomFacet> customFacets = new HashSet<>(
                Optional.ofNullable(fieldExt.getCustomFacets()).orElse(Collections.emptySet()));
        customFacets.add(customFacet);
        fieldExt.setCustomFacets(customFacets);
        turSNSiteFieldExtRepository.save(fieldExt);

        log.info("[SN Startup] Custom facet '{}' created with {} items for field '{}'.",
                SAMPLE_CUSTOM_FACET_NAME, customFacet.getItems().size(), fieldExt.getName());
    }

    private static boolean hasCustomFacet(TurSNSiteFieldExt fieldExt,
            String customFacetName) {
        return Optional.ofNullable(fieldExt.getCustomFacets())
                .orElse(Collections.emptySet()).stream()
                .anyMatch(customFacet -> customFacet.getName().equals(customFacetName));
    }

    private static Integer getSampleCustomFacetPosition(TurSNSiteFieldExt fieldExt) {
        return Optional.ofNullable(fieldExt.getFacetPosition())
                .filter(position -> position > 0)
                .map(position -> position + 1)
                .orElse(1);
    }

    private static Map<String, String> getSampleFacetLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("en-US", "Price Range");
        labels.put("pt-BR", "Faixa de Preço");
        return labels;
    }

    private static Set<TurSNSiteCustomFacetItem> getSampleFacetItems(
            TurSNSiteCustomFacet customFacet) {
        Set<TurSNSiteCustomFacetItem> items = new HashSet<>();
        items.add(createSampleItem("0 - 100", 1, BigDecimal.ZERO, new BigDecimal("100"),
                customFacet));
        items.add(createSampleItem("101 - 500", 2, new BigDecimal("101"),
                new BigDecimal("500"), customFacet));
        items.add(createSampleItem("501+", 3, new BigDecimal("501"), null, customFacet));
        return items;
    }

    private static TurSNSiteCustomFacetItem createSampleItem(String label, Integer position,
            BigDecimal rangeStart, BigDecimal rangeEnd, TurSNSiteCustomFacet customFacet) {
        TurSNSiteCustomFacetItem item = new TurSNSiteCustomFacetItem();
        item.setLabel(label);
        item.setPosition(position);
        item.setRangeStart(rangeStart);
        item.setRangeEnd(rangeEnd);
        item.setTurSNSiteCustomFacet(customFacet);
        return item;
    }
}
