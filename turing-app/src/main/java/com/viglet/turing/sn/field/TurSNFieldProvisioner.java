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

package com.viglet.turing.sn.field;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.LocaleUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.viglet.turing.client.sn.job.TurSNJobAttributeSpec;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteField;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExtFacet;
import com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteCustomFacetRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtFacetRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.persistence.repository.field.TurSNSiteFieldRepository;
import com.viglet.turing.sn.persistence.repository.locale.TurSNSiteLocaleRepository;
import com.viglet.turing.searchengine.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.TurSNFieldType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single, idempotent code path that converges an SN site to a field declared by
 * a {@link TurSNJobAttributeSpec}: it creates the {@code TurSNSiteField}, its
 * {@code TurSNSiteFieldExt} (with facet flag, localized facet labels and the
 * {@code mandatory → required} flag), and registers the field in every locale's
 * search-engine core that is missing it. Re-running with an already-declared
 * field is a no-op.
 *
 * <p>This was extracted from {@code TurSNProcessQueue.createMissingFields(...)}
 * so the indexing-time auto-create path (a job item carrying attribute specs)
 * and the declarative field-manifest provisioning path (T382) share one
 * implementation — the "builds on {@code TurSNJobAttributeSpec}" intent from
 * the roadmap.</p>
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TurSNFieldProvisioner {

    /** Reserved facetName map key carrying the default (non-localized) label. */
    public static final String DEFAULT = "default";

    private final TurSNSiteFieldRepository turSNSiteFieldRepository;
    private final TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
    private final TurSNSiteFieldExtFacetRepository turSNSiteFieldExtFacetRepository;
    private final TurSNSiteCustomFacetRepository turSNSiteCustomFacetRepository;
    private final TurSNSiteLocaleRepository turSNSiteLocaleRepository;
    private final TurSEInstanceRepository turSEInstanceRepository;
    private final TurSearchEnginePluginFactory pluginFactory;

    /**
     * Ensures the field declared by {@code spec} exists on {@code turSNSite}.
     *
     * @return {@code true} when the field was created, {@code false} when it
     *         already existed (idempotent no-op).
     */
    public boolean ensureField(TurSNSite turSNSite, TurSNJobAttributeSpec spec) {
        if (turSNSiteFieldExtRepository.existsByTurSNSiteAndName(turSNSite, spec.getName())) {
            return false;
        }
        final TurSNSiteField turSNSiteField = saveSiteField(turSNSite, spec);
        saveFacetLocales(spec, saveSiteFieldExt(turSNSite, spec, turSNSiteField));
        turSNSiteLocaleRepository.findByTurSNSite(turSNSite).stream()
                .filter(turSNSiteLocale -> !existsFieldInSearchEngine(turSNSite,
                        turSNSiteLocale.getCore(), spec.getName()))
                .forEach(turSNSiteLocale -> createFieldInSearchEngine(turSNSite,
                        turSNSiteLocale.getCore(), turSNSiteField));
        return true;
    }

    /**
     * T1296 — the non-breaking attributes of an existing field that differ from
     * {@code spec}: {@code facet}, {@code required}, {@code description} and the facet
     * labels. Read-only, for the dry-run plan. The booleans are compared as declared
     * (a spec that omits them says {@code false}); {@code description} and the labels
     * only when the spec carries them. Empty when the field does not exist.
     */
    public List<String> fieldDifferences(TurSNSite turSNSite, TurSNJobAttributeSpec spec) {
        Set<String> differences = new java.util.LinkedHashSet<>();
        turSNSiteFieldExtRepository.findByTurSNSiteAndName(turSNSite, spec.getName())
                .forEach(ext -> differences.addAll(differences(ext, spec)));
        return List.copyOf(differences);
    }

    /**
     * T1296 — converges the non-breaking attributes of an existing field onto
     * {@code spec} in place (see {@link #fieldDifferences}): the facet switch, the
     * required flag, the description, the default facet label when the spec names
     * one, and the per-locale labels, which become exactly the spec's when it carries
     * any. Saving the ext evicts the site's search snapshot.
     *
     * @return whether anything changed
     */
    public boolean convergeField(TurSNSite turSNSite, TurSNJobAttributeSpec spec) {
        boolean changed = false;
        for (TurSNSiteFieldExt ext : turSNSiteFieldExtRepository.findByTurSNSiteAndName(turSNSite, spec.getName())) {
            List<String> differences = differences(ext, spec);
            if (differences.isEmpty()) {
                continue;
            }
            ext.setFacet(spec.isFacet() ? 1 : 0);
            ext.setRequired(spec.isMandatory() ? 1 : 0);
            if (spec.getDescription() != null) {
                ext.setDescription(spec.getDescription());
            }
            Map<String, String> facetName = spec.getFacetName();
            if (facetName != null && facetName.containsKey(DEFAULT)) {
                ext.setFacetName(facetName.get(DEFAULT));
            }
            turSNSiteFieldExtRepository.save(ext);
            if (differences.contains("facetLocales")) {
                replaceFacetLocales(ext, facetName);
            }
            changed = true;
        }
        return changed;
    }

    private List<String> differences(TurSNSiteFieldExt ext, TurSNJobAttributeSpec spec) {
        List<String> differences = new java.util.ArrayList<>();
        if (ext.getFacet() != (spec.isFacet() ? 1 : 0)) {
            differences.add("facet");
        }
        if (ext.getRequired() != (spec.isMandatory() ? 1 : 0)) {
            differences.add("mandatory");
        }
        if (spec.getDescription() != null && !spec.getDescription().equals(ext.getDescription())) {
            differences.add("description");
        }
        Map<String, String> facetName = spec.getFacetName();
        if (facetName != null) {
            if (facetName.containsKey(DEFAULT)
                    && !java.util.Objects.equals(facetName.get(DEFAULT), ext.getFacetName())) {
                differences.add("facetName");
            }
            if (!localeLabels(facetName).equals(liveLocaleLabels(ext))) {
                differences.add("facetLocales");
            }
        }
        return differences;
    }

    /** The declared per-locale labels, keyed by the locale's canonical spelling. */
    private static Map<String, String> localeLabels(Map<String, String> facetName) {
        Map<String, String> labels = new java.util.TreeMap<>();
        facetName.forEach((key, value) -> {
            if (!DEFAULT.equals(key)) {
                labels.put(LocaleUtils.toLocale(key).toString(), value);
            }
        });
        return labels;
    }

    private Map<String, String> liveLocaleLabels(TurSNSiteFieldExt ext) {
        Map<String, String> labels = new java.util.TreeMap<>();
        turSNSiteFieldExtFacetRepository.findByTurSNSiteFieldExt(ext).forEach(row -> labels
                .put(row.getLocale() == null ? "" : row.getLocale().toString(), row.getLabel()));
        return labels;
    }

    /** The ext's per-locale label rows replaced by the declared ones. */
    private void replaceFacetLocales(TurSNSiteFieldExt ext, Map<String, String> facetName) {
        Set<TurSNSiteFieldExtFacet> live = turSNSiteFieldExtFacetRepository.findByTurSNSiteFieldExt(ext);
        ext.getFacetLocales().removeAll(live);
        turSNSiteFieldExtFacetRepository.deleteAll(live);
        TurSNJobAttributeSpec labels = new TurSNJobAttributeSpec();
        labels.setFacetName(facetName);
        saveFacetLocales(labels, ext);
    }

    /**
     * Applies a declared breaking migration (T386 / §XX.6): drops the live
     * field — its {@code TurSNSiteFieldExt}/{@code TurSNSiteField} rows and its
     * search-engine schema definition in every locale core — then recreates it
     * from {@code spec}. Destructive: values indexed under the old type are gone
     * and the source must reindex. Only ever called when the manifest declares a
     * {@link com.viglet.core.manifest.VigletFieldMigration} for the field.
     *
     * @return {@code true} once the field has been recreated.
     */
    public boolean recreateField(TurSNSite turSNSite, TurSNJobAttributeSpec spec) {
        List<TurSNSiteFieldExt> liveExts = turSNSiteFieldExtRepository
                .findByTurSNSite(Sort.unsorted(), turSNSite).stream()
                .filter(ext -> ext.getName().equals(spec.getName()))
                .toList();
        TurSEFieldType liveType = liveExts.stream().findFirst()
                .map(TurSNSiteFieldExt::getType).orElse(spec.getType());

        dropFieldInSearchEngine(turSNSite, spec.getName(), liveType);
        liveExts.forEach(this::deleteFieldExt);
        turSNSiteFieldRepository.findByTurSNSite(turSNSite).stream()
                .filter(field -> field.getName().equals(spec.getName()))
                .forEach(turSNSiteFieldRepository::delete);
        return ensureField(turSNSite, spec);
    }

    /**
     * T1241 — deletes an ext with its facet-locale and custom-facet rows first. On
     * a site created in this same transaction those rows are managed but were never
     * added to the ext's collections, so the ext's cascade cannot reach them and
     * they would still reference it at flush ({@code TransientPropertyValueException}).
     */
    private void deleteFieldExt(TurSNSiteFieldExt ext) {
        turSNSiteFieldExtFacetRepository.deleteAll(turSNSiteFieldExtFacetRepository.findByTurSNSiteFieldExt(ext));
        ext.getFacetLocales().clear();
        turSNSiteCustomFacetRepository.deleteAll(turSNSiteCustomFacetRepository.findByFieldExtWithDetails(ext));
        ext.getCustomFacets().clear();
        turSNSiteFieldExtRepository.delete(ext);
    }

    private void dropFieldInSearchEngine(TurSNSite turSNSite, String name, TurSEFieldType liveType) {
        if (turSNSite.getTurSEInstance() == null) {
            return;
        }
        turSEInstanceRepository.findById(turSNSite.getTurSEInstance().getId()).ifPresent(seInstance ->
                turSNSiteLocaleRepository.findByTurSNSite(turSNSite).stream()
                        .filter(locale -> pluginFactory.getPluginForSite(turSNSite)
                                .fieldExists(seInstance, locale.getCore(), name))
                        .forEach(locale -> pluginFactory.getPluginForSite(turSNSite)
                                .deleteField(seInstance, locale.getCore(), name, liveType)));
    }

    private void saveFacetLocales(TurSNJobAttributeSpec spec, TurSNSiteFieldExt turSNSiteFieldExt) {
        if (spec.getFacetName() == null) {
            return;
        }
        Set<TurSNSiteFieldExtFacet> facetLocales = new HashSet<>();
        spec.getFacetName().forEach((key, value) -> {
            if (!key.equals(DEFAULT)) {
                TurSNSiteFieldExtFacet turSNSiteFieldExtFacet = new TurSNSiteFieldExtFacet();
                turSNSiteFieldExtFacet.setLocale(LocaleUtils.toLocale(key));
                turSNSiteFieldExtFacet.setLabel(value);
                turSNSiteFieldExtFacet.setTurSNSiteFieldExt(turSNSiteFieldExt);
                facetLocales.add(turSNSiteFieldExtFacet);
            }
        });
        turSNSiteFieldExtFacetRepository.saveAll(facetLocales);
        // T1241 — both sides of the association, so a later delete in this
        // transaction cascades to these rows.
        turSNSiteFieldExt.getFacetLocales().addAll(facetLocales);
    }

    @NotNull
    private TurSNSiteFieldExt saveSiteFieldExt(TurSNSite turSNSite, TurSNJobAttributeSpec spec,
            TurSNSiteField turSNSiteField) {
        Map<String, String> facetName = spec.getFacetName();
        TurSNSiteFieldExt turSNSiteFieldExt = TurSNSiteFieldExt.builder().enabled(1)
                .name(turSNSiteField.getName()).description(turSNSiteField.getDescription())
                .facet(spec.isFacet() ? 1 : 0)
                .facetName(facetName != null ? facetName.get(DEFAULT) : null).hl(0)
                .multiValued(turSNSiteField.getMultiValued()).mlt(0)
                .required(spec.isMandatory() ? 1 : 0)
                .externalId(turSNSiteField.getId()).snType(TurSNFieldType.SE)
                .type(turSNSiteField.getType()).turSNSite(turSNSite).build();
        turSNSiteFieldExtRepository.save(turSNSiteFieldExt);
        return turSNSiteFieldExt;
    }

    @NotNull
    private TurSNSiteField saveSiteField(TurSNSite turSNSite, TurSNJobAttributeSpec spec) {
        TurSNSiteField turSNSiteField = TurSNSiteField.builder().name(spec.getName())
                .description(spec.getDescription()).type(spec.getType())
                .multiValued(spec.isMultiValued() ? 1 : 0).turSNSite(turSNSite).build();
        turSNSiteFieldRepository.save(turSNSiteField);
        return turSNSiteField;
    }

    private void createFieldInSearchEngine(TurSNSite turSNSite, String coreName,
            TurSNSiteField turSNSiteField) {
        turSEInstanceRepository.findById(turSNSite.getTurSEInstance().getId())
                .ifPresent(seInstance -> pluginFactory.getPluginForSite(turSNSite)
                        .addOrUpdateField(seInstance, coreName, turSNSiteField.getName(),
                                turSNSiteField.getType(), true, turSNSiteField.getMultiValued() == 1, true));
    }

    private boolean existsFieldInSearchEngine(TurSNSite turSNSite, String coreName, String name) {
        if (turSNSite.getTurSEInstance() == null) {
            return false;
        }
        return turSEInstanceRepository.findById(turSNSite.getTurSEInstance().getId())
                .map(seInstance -> pluginFactory.getPluginForSite(turSNSite)
                        .fieldExists(seInstance, coreName, name))
                .orElse(false);
    }
}
