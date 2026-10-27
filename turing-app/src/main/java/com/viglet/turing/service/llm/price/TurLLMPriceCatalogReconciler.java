/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.service.llm.price;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.genai.provider.llm.TurLlmModelCatalog;
import com.viglet.turing.genai.provider.llm.TurLlmModelMetadata;
import com.viglet.turing.genai.provider.llm.TurLlmModelOption;
import com.viglet.turing.persistence.model.llm.TurLLMVendor;
import com.viglet.turing.persistence.repository.llm.TurLLMVendorRepository;
import com.viglet.turing.service.llm.price.TurLLMPriceService.CatalogPrice;

import lombok.extern.slf4j.Slf4j;

/**
 * T777 / §LIII.1 (Block BE) — a scheduled reconciler that auto-seeds and refreshes
 * the {@code tur_llm_price} table from the public model catalog's indicative
 * {@code pricing}. Before this the price table was manual-only, so a model with no
 * hand-entered row billed at {@code $0} — the reconciler closes that gap so
 * {@link TurLLMPriceService#computeCost} and {@code TurLLMTokenUsage.costUsd}
 * populate automatically for well-known models.
 *
 * <p>It only ever writes {@link com.viglet.turing.persistence.model.llm.TurLLMPriceSource#CATALOG
 * CATALOG} rows: an operator's {@code MANUAL} (negotiated / hand-edited) rate is
 * never overwritten (see {@link TurLLMPriceService#reconcileCatalogPrices}). Runs
 * off the boot path on a TTL (mirroring the catalog refresh cadence) so a running
 * instance picks up catalog price changes without a redeploy; disabled with
 * {@code turing.model-catalog.price-sync.enabled=false}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Component
public class TurLLMPriceCatalogReconciler {

    private final TurLlmModelCatalog modelCatalog;
    private final TurLLMPriceService priceService;
    private final TurLLMVendorRepository vendorRepository;

    @Value("${turing.model-catalog.price-sync.enabled:true}")
    private boolean enabled;

    public TurLLMPriceCatalogReconciler(TurLlmModelCatalog modelCatalog, TurLLMPriceService priceService,
            TurLLMVendorRepository vendorRepository) {
        this.modelCatalog = modelCatalog;
        this.priceService = priceService;
        this.vendorRepository = vendorRepository;
    }

    /**
     * Walks the effective catalog and reconciles every model that carries an
     * indicative price into the price table. Best-effort: a reconcile failure is
     * logged and never propagated (the schedule keeps running). Runs after an
     * initial delay so it never blocks startup, and after the catalog itself has
     * had a chance to refresh (its initial delay is shorter).
     */
    @Scheduled(
            initialDelayString = "${turing.model-catalog.price-sync.initial-delay-ms:15000}",
            fixedDelayString = "${turing.model-catalog.price-sync.interval-ms:21600000}")
    void syncPricesFromCatalog() {
        if (!enabled) {
            return;
        }
        try {
            Map<String, List<String>> vendorsBySlug = vendorsBySlug();
            // T1144 — before the rows were keyed by vendor id, they were keyed by the
            // catalog's slug, which no lookup ever matches. Drop those first, so a
            // case-insensitive collation cannot hand the old row back as "existing".
            Set<String> vendorIds = new HashSet<>();
            vendorsBySlug.values().forEach(vendorIds::addAll);
            Set<String> slugsOnly = new HashSet<>(vendorsBySlug.keySet());
            slugsOnly.removeAll(vendorIds);
            priceService.dropCatalogRowsKeyedBy(slugsOnly);
            List<CatalogPrice> candidates = collectPricedModels(vendorsBySlug);
            if (candidates.isEmpty()) {
                return;
            }
            int written = priceService.reconcileCatalogPrices(candidates);
            if (written > 0) {
                log.info("Reconciled {} catalog price row(s) from the model catalog ({} priced models found)",
                        written, candidates.size());
            }
        } catch (Exception e) {
            log.debug("Catalog price reconciliation failed, keeping current prices: {}", e.getMessage());
        }
    }

    /**
     * T1144 — the catalog is keyed by plugin slug ({@code openai}), while a call is
     * priced by the instance's vendor id ({@code OPENAI}). This maps each slug to
     * the vendors whose plugin it is, resolved the way model discovery resolves it
     * (the vendor's plugin, else its id, lower-cased). Two vendors may share one.
     */
    private Map<String, List<String>> vendorsBySlug() {
        Map<String, List<String>> bySlug = new HashMap<>();
        for (TurLLMVendor vendor : vendorRepository.findAll()) {
            String plugin = StringUtils.hasText(vendor.getPlugin()) ? vendor.getPlugin() : vendor.getId();
            if (StringUtils.hasText(plugin) && StringUtils.hasText(vendor.getId())) {
                bySlug.computeIfAbsent(plugin.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(vendor.getId());
            }
        }
        return bySlug;
    }

    /**
     * Flattens the catalog to the priced models the reconciler cares about, one
     * candidate per vendor id. A slug no vendor carries is skipped: nothing could
     * be priced with it.
     */
    private List<CatalogPrice> collectPricedModels(Map<String, List<String>> vendorsBySlug) {
        List<CatalogPrice> candidates = new ArrayList<>();
        modelCatalog.allModels().forEach((slug, options) -> {
            List<String> vendorIds = vendorsBySlug.getOrDefault(slug.toLowerCase(Locale.ROOT), List.of());
            for (String vendorId : vendorIds) {
                for (TurLlmModelOption option : options) {
                    CatalogPrice candidate = toCandidate(vendorId, option);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        });
        return candidates;
    }

    /**
     * Builds a {@link CatalogPrice} for a catalog option that carries a usable
     * indicative price, or {@code null} when the option has no metadata / no price.
     */
    private CatalogPrice toCandidate(String vendor, TurLlmModelOption option) {
        TurLlmModelMetadata metadata = option.metadata();
        if (metadata == null || metadata.pricing() == null) {
            return null;
        }
        TurLlmModelMetadata.TurLlmModelPricing pricing = metadata.pricing();
        Double in = pricing.inputPer1M();
        Double out = pricing.outputPer1M();
        if (in == null && out == null) {
            return null;
        }
        return new CatalogPrice(
                vendor,
                option.id(),
                in == null ? 0.0 : in,
                out == null ? 0.0 : out,
                pricing.currency(),
                Boolean.TRUE.equals(pricing.indicative()),
                pricing.source(),
                pricing.lastVerified());
    }
}
