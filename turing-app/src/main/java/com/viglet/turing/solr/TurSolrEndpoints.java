/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.solr;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.impl.LBSolrClient;

/**
 * T1247 — the Solr nodes a search-engine instance's endpoint names, and the one
 * place a SolrJ client is built from them.
 *
 * <p>The endpoint is one URL (a single node, as before) or several separated by
 * commas or whitespace (a node list). A node list is served by a load-balancing
 * client that spreads requests across the nodes and, when one stops answering,
 * sends them to the others until it comes back. Every node must hold the same
 * cores, as replicas of one another do.
 *
 * <p>Calls made over raw HTTP (core admin, schema, the JSON DSL) go to
 * {@link #first()}.
 *
 * @param urls the node URLs, in the order the endpoint lists them, never empty
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSolrEndpoints(List<String> urls) {

    /** How often a node that stopped answering is probed to bring it back. */
    static final int ALIVE_CHECK_SECONDS = 30;

    public TurSolrEndpoints {
        urls = List.copyOf(urls);
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("A Solr endpoint names at least one node");
        }
    }

    /** The nodes {@code endpoint} names, or {@code null} when it names none. */
    public static TurSolrEndpoints parse(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        List<String> urls = Arrays.stream(endpoint.split("[,\\s]+"))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                .distinct()
                .toList();
        return urls.isEmpty() ? null : new TurSolrEndpoints(urls);
    }

    /**
     * {@code scheme://host:port} of the first node {@code endpoint} names: the root
     * the raw-HTTP admin calls (core, collection, schema) are built on.
     */
    public static String adminRoot(String endpoint) {
        TurSolrEndpoints endpoints = parse(endpoint);
        String node = endpoints != null ? endpoints.first() : endpoint;
        try {
            URI uri = URI.create(node);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            return node;
        }
    }

    /** The first node: the one raw-HTTP calls go to. */
    public String first() {
        return urls.getFirst();
    }

    /** Whether the endpoint lists more than one node. */
    public boolean isNodeList() {
        return urls.size() > 1;
    }

    /** Whether every node is reached over HTTP(S), the only scheme SolrJ speaks here. */
    public boolean isHttp() {
        return urls.stream().allMatch(url -> url.startsWith("http://") || url.startsWith("https://"));
    }

    /** The SolrJ client for these nodes: one node's client, or a load-balancing one over all of them. */
    public SolrClient createClient() {
        if (!isNodeList()) {
            return httpClient(new HttpJdkSolrClient.Builder(first()));
        }
        HttpJdkSolrClient http = httpClient(new HttpJdkSolrClient.Builder());
        LBSolrClient.Endpoint[] endpoints = urls.stream().map(LBSolrClient.Endpoint::new)
                .toArray(LBSolrClient.Endpoint[]::new);
        return new LBSolrClient.Builder<>(http, endpoints)
                .setAliveCheckInterval(ALIVE_CHECK_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    private static HttpJdkSolrClient httpClient(HttpJdkSolrClient.Builder builder) {
        return builder.useHttp1_1(true)
                .withConnectionTimeout(5000, TimeUnit.MILLISECONDS)
                .withIdleTimeout(15000, TimeUnit.MILLISECONDS)
                .build();
    }
}
