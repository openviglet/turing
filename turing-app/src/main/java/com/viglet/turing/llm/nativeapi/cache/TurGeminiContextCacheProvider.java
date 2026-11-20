/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.llm.nativeapi.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.genai.Client;
import com.google.genai.types.CachedContent;
import com.google.genai.types.Content;
import com.google.genai.types.CreateCachedContentConfig;
import com.google.genai.types.Part;
import com.viglet.turing.llm.nativeapi.TurNativeProviderClient;
import com.viglet.turing.llm.provider.TurProviderOptionsParser;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * T499 / §X.20 — the Gemini {@link TurContextCacheProvider}: explicit, named,
 * TTL'd context caching via {@code client.caches().create(...)}. The stable
 * prefix (system instruction + any pinned corpus) is uploaded once into a
 * {@code cachedContents} object; subsequent turns reference it by name on
 * {@code GenerateContentConfig.cachedContent} and Google bills the cached tokens
 * at ~75% off — reusable across turns, conversations <b>and</b> users.
 *
 * <p>A per-process registry keyed by a SHA-256 of {@code (model + prefix)} avoids
 * re-creating the same cache: identical prefixes (e.g. one per-site system prompt
 * reused across thousands of visitor turns) map to one cache until its TTL nears.
 * The cache key deliberately spans only the prefix and model — not the user or
 * conversation — so the discount is shared, which is the whole point of an
 * explicit cache.
 *
 * <p>Fail-open by contract: a too-small prefix (Gemini enforces a per-model
 * minimum token floor), a quota error, or any SDK exception yields
 * {@link Optional#empty()} and the turn proceeds uncached.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Component
public class TurGeminiContextCacheProvider implements TurContextCacheProvider {

    static final String PLUGIN_TYPE = "gemini";
    /** Default cache lifetime when the instance doesn't configure one. */
    static final Duration DEFAULT_TTL = Duration.ofHours(1);
    /** Skew before {@code expiresAt} at which a registry entry is treated as stale. */
    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(30);
    private static final String DISPLAY_PREFIX = "turing-ctx-";

    private final TurNativeProviderClient nativeClient;
    private final TurProviderOptionsParser optionsParser;
    private final ConcurrentMap<String, CacheEntry> registry = new ConcurrentHashMap<>();

    public TurGeminiContextCacheProvider(TurNativeProviderClient nativeClient,
            TurProviderOptionsParser optionsParser) {
        this.nativeClient = nativeClient;
        this.optionsParser = optionsParser;
    }

    @Override
    public String getPluginType() {
        return PLUGIN_TYPE;
    }

    @Override
    public boolean supports(TurLLMInstance instance) {
        return instance != null && instance.getTurLLMVendor() != null
                && PLUGIN_TYPE.equalsIgnoreCase(vendorPlugin(instance));
    }

    @Override
    public Optional<TurContextCacheRef> ensureCache(TurContextCacheRequest request) {
        if (request == null || request.isEmpty() || !StringUtils.hasText(request.model())) {
            return Optional.empty();
        }
        String key = cacheKey(request);
        Instant now = Instant.now();

        CacheEntry cached = registry.get(key);
        if (cached != null && cached.isFresh(now)) {
            return Optional.of(cached.toRef(true));
        }
        if (cached != null) {
            // Expired (or near-expiry) — drop it and create a new one below.
            registry.remove(key, cached);
        }

        return createCache(request, key, now);
    }

    private Optional<TurContextCacheRef> createCache(TurContextCacheRequest request, String key,
            Instant now) {
        Optional<Client> client = nativeClient.gemini(request.instance());
        if (client.isEmpty()) {
            return Optional.empty();
        }
        Duration ttl = request.ttl() != null && !request.ttl().isZero() && !request.ttl().isNegative()
                ? request.ttl()
                : DEFAULT_TTL;
        try {
            CreateCachedContentConfig.Builder config = CreateCachedContentConfig.builder()
                    .displayName(DISPLAY_PREFIX + key.substring(0, Math.min(16, key.length())))
                    .ttl(ttl);
            if (StringUtils.hasText(request.systemInstruction())) {
                config.systemInstruction(
                        Content.fromParts(Part.fromText(request.systemInstruction())));
            }
            List<Content> pinned = pinnedContents(request);
            if (!pinned.isEmpty()) {
                config.contents(pinned);
            }
            CachedContent created = client.get().caches.create(request.model(), config.build());
            String handle = created.name().orElse(null);
            if (!StringUtils.hasText(handle)) {
                return Optional.empty();
            }
            Instant expiresAt = created.expireTime().orElse(now.plus(ttl));
            Integer tokens = created.usageMetadata()
                    .flatMap(u -> u.totalTokenCount())
                    .orElse(null);
            CacheEntry entry = new CacheEntry(handle, expiresAt, tokens);
            registry.put(key, entry);
            log.info("[Native][Gemini] context cache created handle='{}' model='{}' tokens={} ttl={}",
                    handle, request.model(), tokens, ttl);
            return Optional.of(entry.toRef(false));
        } catch (RuntimeException e) {
            // Fail-open: a too-small prefix, quota, or transport error must never
            // fail the turn — the call simply proceeds uncached.
            log.warn("[Native][Gemini] context cache create failed for model '{}' "
                    + "(proceeding uncached): {}", request.model(), e.getMessage());
            return Optional.empty();
        }
    }

    private static List<Content> pinnedContents(TurContextCacheRequest request) {
        List<Content> contents = new ArrayList<>();
        for (String block : request.pinnedContents()) {
            if (StringUtils.hasText(block)) {
                contents.add(Content.builder().role("user").parts(Part.fromText(block)).build());
            }
        }
        return contents;
    }

    /** SHA-256 over model + system instruction + pinned blocks (order-sensitive). */
    private static String cacheKey(TurContextCacheRequest request) {
        StringBuilder material = new StringBuilder();
        material.append(request.model() == null ? "" : request.model()).append('\0');
        material.append(request.systemInstruction() == null ? "" : request.systemInstruction());
        for (String block : request.pinnedContents()) {
            material.append('\0').append(block == null ? "" : block);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS — unreachable; degrade to a length tag.
            return Integer.toHexString(material.toString().hashCode());
        }
    }

    private String vendorPlugin(TurLLMInstance instance) {
        String plugin = instance.getTurLLMVendor().getPlugin();
        if (!StringUtils.hasText(plugin)) {
            plugin = instance.getTurLLMVendor().getId();
        }
        return plugin == null ? null : plugin.toLowerCase(Locale.ROOT);
    }

    /** Lets tests resolve the configured TTL from an instance's provider options. */
    Duration resolveTtl(Map<String, Object> options) {
        Integer seconds = optionsParser.intValue(options, "contextCacheTtlSeconds");
        if (seconds == null || seconds <= 0) {
            return DEFAULT_TTL;
        }
        return Duration.ofSeconds(seconds);
    }

    /** Registry entry: a live Gemini cache handle plus its expiry. */
    private record CacheEntry(String handle, Instant expiresAt, Integer tokens) {

        boolean isFresh(Instant now) {
            return expiresAt == null || now.isBefore(expiresAt.minus(EXPIRY_SKEW));
        }

        TurContextCacheRef toRef(boolean reused) {
            return new TurContextCacheRef(PLUGIN_TYPE, handle, tokens, expiresAt, reused);
        }
    }
}
