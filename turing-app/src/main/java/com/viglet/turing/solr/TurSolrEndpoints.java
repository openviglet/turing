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
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.impl.LBSolrClient;
import org.apache.solr.common.util.URLUtil;

/**
 * T1247 — the Solr nodes a search-engine instance's endpoint names, and the one
 * place a SolrJ client is built from them.
 *
 * <p>The endpoint takes one of three forms:
 * <ul>
 * <li>one URL — a single node, as before;</li>
 * <li>several URLs separated by commas or whitespace — a node list, served by a
 * load-balancing client that spreads requests across the nodes and, when one
 * stops answering, sends them to the others until it comes back. Every node must
 * hold the same cores, as replicas of one another do;</li>
 * <li>{@code zk://host:2181,host2:2181/chroot} — a ZooKeeper ensemble: a SolrCloud
 * cluster whose state (live nodes, collections, replicas) is read from ZooKeeper,
 * so requests follow the cluster as it changes. Its indexes are collections.</li>
 * </ul>
 *
 * <p>Calls made over raw HTTP (core and collection admin, schema, the JSON DSL) go
 * to {@link #adminRoot(String)}: the first node of a list, a live node of a
 * ZooKeeper cluster.
 *
 * @param urls the node URLs, in the order the endpoint lists them; empty for ZooKeeper
 * @param zkHosts the ZooKeeper ensemble's {@code host:port}s; empty for HTTP nodes
 * @param zkChroot the cluster's ZooKeeper chroot ({@code /solr}), or {@code null}
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurSolrEndpoints(List<String> urls, List<String> zkHosts, String zkChroot) {

    /** The scheme of a ZooKeeper-ensemble endpoint. */
    public static final String ZK_SCHEME = "zk://";
    /** How often a node that stopped answering is probed to bring it back. */
    static final int ALIVE_CHECK_SECONDS = 30;
    private static final String SEPARATORS = "[,\\s]+";

    public TurSolrEndpoints {
        urls = List.copyOf(urls);
        zkHosts = List.copyOf(zkHosts);
        if (urls.isEmpty() == zkHosts.isEmpty()) {
            throw new IllegalArgumentException("A Solr endpoint names HTTP nodes or a ZooKeeper ensemble");
        }
    }

    /** HTTP nodes. */
    public TurSolrEndpoints(List<String> urls) {
        this(urls, List.of(), null);
    }

    /** The nodes {@code endpoint} names, or {@code null} when it names none. */
    public static TurSolrEndpoints parse(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        String trimmed = endpoint.trim();
        if (trimmed.regionMatches(true, 0, ZK_SCHEME, 0, ZK_SCHEME.length())) {
            return parseZooKeeper(trimmed.substring(ZK_SCHEME.length()));
        }
        List<String> urls = split(trimmed).stream()
                .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                .distinct()
                .toList();
        return urls.isEmpty() ? null : new TurSolrEndpoints(urls);
    }

    private static TurSolrEndpoints parseZooKeeper(String spec) {
        int slash = spec.indexOf('/');
        String hosts = slash < 0 ? spec : spec.substring(0, slash);
        String chroot = slash < 0 ? null : spec.substring(slash);
        if (chroot != null && (chroot.isBlank() || "/".equals(chroot))) {
            chroot = null;
        }
        List<String> zkHosts = split(hosts).stream().distinct().toList();
        return zkHosts.isEmpty() ? null : new TurSolrEndpoints(List.of(), zkHosts, chroot);
    }

    private static List<String> split(String value) {
        return Arrays.stream(value.split(SEPARATORS)).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Whether {@code endpoint} names a ZooKeeper ensemble, whose indexes are collections. */
    public static boolean isZooKeeper(String endpoint) {
        TurSolrEndpoints endpoints = parse(endpoint);
        return endpoints != null && endpoints.isZooKeeper();
    }

    /**
     * {@code scheme://host:port} of the node raw-HTTP admin calls (core, collection,
     * schema) are built on: the first node of a list, or a live node of a ZooKeeper
     * cluster, read from its cluster state.
     */
    public static String adminRoot(String endpoint) {
        TurSolrEndpoints endpoints = parse(endpoint);
        if (endpoints == null) {
            return endpoint;
        }
        return root(endpoints.isZooKeeper() ? endpoints.liveNode() : endpoints.first());
    }

    private static String root(String node) {
        try {
            URI uri = URI.create(node);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            return node;
        }
    }

    /** The endpoint, normalized: what one pooled client is kept for. */
    public String key() {
        return isZooKeeper()
                ? ZK_SCHEME + String.join(",", zkHosts) + (zkChroot == null ? "" : zkChroot)
                : String.join(",", urls);
    }

    /** Whether the endpoint is a ZooKeeper ensemble. */
    public boolean isZooKeeper() {
        return !zkHosts.isEmpty();
    }

    /** The first HTTP node: the one raw-HTTP calls go to. A ZooKeeper endpoint lists none. */
    public String first() {
        if (isZooKeeper()) {
            throw new IllegalStateException("A ZooKeeper endpoint lists no HTTP node: read a live one");
        }
        return urls.getFirst();
    }

    /**
     * The base URL raw-HTTP calls go to through {@code client} (one {@link #createClient()}
     * built): a live node of the cluster it follows, or the first node.
     */
    public String baseUrl(SolrClient client) {
        if (client instanceof CloudSolrClient cloud) {
            return liveNode(cloud);
        }
        return first();
    }

    /** Whether the endpoint lists more than one HTTP node. */
    public boolean isNodeList() {
        return urls.size() > 1;
    }

    /** Whether SolrJ can reach these nodes: HTTP(S) URLs, or a ZooKeeper ensemble. */
    public boolean isHttp() {
        return isZooKeeper()
                || urls.stream().allMatch(url -> url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * The SolrJ client for these nodes: one node's client, a load-balancing one over
     * a node list, or a cloud client that follows the ZooKeeper cluster state.
     */
    public SolrClient createClient() {
        if (isZooKeeper()) {
            return cloudClient();
        }
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

    private CloudSolrClient cloudClient() {
        return new CloudSolrClient.Builder(new CloudSolrClient.CloudSolrClientConnection(true, zkHosts, zkChroot))
                .withHttpClient(httpClient(new HttpJdkSolrClient.Builder()))
                .withZkConnectTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /** A live node's base URL ({@code http://host:port/solr}), read from ZooKeeper. */
    private String liveNode() {
        try (CloudSolrClient cloud = cloudClient()) {
            return liveNode(cloud);
        }
    }

    private String liveNode(CloudSolrClient cloud) {
        return cloud.getClusterStateProvider().getLiveNodes().stream().sorted().findFirst()
                .map(node -> URLUtil.getBaseUrlForNodeName(node, "http"))
                .orElseThrow(() -> new IllegalStateException(
                        "No live Solr node in ZooKeeper " + String.join(",", zkHosts)));
    }

    private static HttpJdkSolrClient httpClient(HttpJdkSolrClient.Builder builder) {
        return builder.useHttp1_1(true)
                .withConnectionTimeout(5000, TimeUnit.MILLISECONDS)
                .withIdleTimeout(15000, TimeUnit.MILLISECONDS)
                .build();
    }
}
