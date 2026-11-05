/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.viglet.turing.api.sn.queue.TurSNProcessQueue;
import com.viglet.turing.api.sn.search.TurSNSearchCacheEvictor;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.exchange.TurImportExchange;
import com.viglet.turing.exchange.TurImportExchange.ImportResult;
import com.viglet.turing.persistence.model.dev.token.TurDevToken;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricAccess;
import com.viglet.turing.persistence.repository.dev.token.TurDevTokenRepository;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricClick;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlight;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricAccessRepository;
import com.viglet.turing.persistence.repository.sn.metric.TurSNSiteMetricClickRepository;
import com.viglet.turing.sn.insights.TurSNClickThroughService;
import com.viglet.turing.sn.insights.TurSNSearchInsightsService;
import com.viglet.turing.persistence.repository.sn.spotlight.TurSNSiteSpotlightRepository;
import com.viglet.turing.testutil.AbstractTuringSpringIT;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Contract integration test for {@code @viglet/turing-sdk} (the vanilla JS SDK
 * under {@code frontend/packages/sdk}).
 *
 * <p>This is the automated form of "step 3" from the SDK rollout plan: it boots
 * the full Turing backend on a real HTTP port and asserts every REST/SSE
 * behavior the JS SDK's client + controllers depend on, so the SDK can never
 * silently break when the backend contract drifts. It does NOT execute the JS
 * SDK itself (that belongs in a JS test) — it pins the server side of the wire.
 *
 * <p><b>Self-contained &amp; CI-safe</b>: rather than depending on an external
 * Solr, the test imports one of the marketplace sample sites
 * ({@code mythical-creatures}) which ships with an <b>embedded Lucene</b> search
 * engine. The site definition ({@code export.json}) and its documents
 * ({@code mythical-creatures_content.json}) are committed under
 * {@code src/test/resources/sdk-it/} (copied from
 * {@code frontend/apps/marketplace/mythical-creatures/export/}). The Lucene
 * {@code endpointUrl} is rewritten to a unique temp directory so the in-process
 * index never collides with a locally-running app (which holds
 * {@code ./store/lucene/turing-sample} open) or with a parallel test JVM.
 * Content is indexed synchronously by {@link TurImportExchange} (no JMS queue
 * await), so the assertions are deterministic.
 *
 * <p><b>This is also the suite's only cache-enabled context</b>, and therefore owns the
 * end-to-end proof of Block BU's search-cache eviction — see
 * {@link #indexingEvictsTheCachedAnswerAndChangesIt} and
 * {@link #aConfigurationWriteEvictsTheCachedAnswer}. That is a deliberate trade rather than
 * drift (T912 / §LXVIII.19): a dedicated context costs ~40s for tests that take
 * milliseconds. It is recorded here because the class name promises only the SDK, so
 * nothing else would tell a reader the cache tests are in this file. The cache tests defend
 * themselves — removing {@code turing.search.cache.enabled} fails them on their own
 * precondition rather than making them vacuous — but if this class is ever split, they need
 * a home with the property, not merely a home.
 *
 * <p>Inherits the ephemeral H2 + per-JVM Artemis isolation from
 * {@link AbstractTuringSpringIT}. The {@code @SpringBootTest} here re-declares
 * {@code spring.jmx.enabled=true} (so the parent's mandatory JMX bean still
 * wires) while switching to {@code RANDOM_PORT} for a real servlet container.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jmx.enabled=true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TurSdkContractIT extends AbstractTuringSpringIT {

    private static final String SITE = "mythical-creatures";
    private static final String LOCALE = "en_US";
    /** Slot the SDK reads from a fresh site with no GenAI agent configured. */
    private static final String EXPECTED_DISABLED_REASON = "NO_GENAI";
    /** T859 — the facet field carrying the reserved-character value. */
    private static final String RESERVED_FACET_FIELD = "creature_type";
    /** T859 — a facet value that cannot survive a naive query-string round-trip. */
    private static final String RESERVED_FACET_VALUE = "Ash & Ember";
    private static final String RESERVED_DOCUMENT_ID = "myth-contract-999";
    /** T911 — a document indexed and removed within one test, to move the answer. */
    private static final String CACHE_PROBE_ID = "t911-cache-probe";

    @Value("${local.server.port}")
    private int port;

    /**
     * This IT runs a {@code RANDOM_PORT} context that coexists in the same
     * Maven fork as the cached {@code MOCK}-env contexts of the other ITs.
     * With JMX enabled ({@code spring.jmx.enabled=true}, mandatory for
     * {@code TurQueueBrowserService}), both contexts' {@code MBeanExporter}
     * would register the {@code dataSource} HikariCP MBean under the same fixed
     * ObjectName ({@code com.zaxxer.hikari:name=dataSource,type=HikariDataSource})
     * — the second registration throws {@code InstanceAlreadyExistsException}
     * and the context fails to load (only when run alongside the full suite,
     * not in isolation). {@code spring.jmx.unique-names=true} appends an
     * identity suffix to this context's runtime MBean names, so it never
     * collides with the shared context's registration regardless of load order.
     */
    @DynamicPropertySource
    static void uniqueJmxObjectNames(DynamicPropertyRegistry registry) {
        registry.add("spring.jmx.unique-names", () -> "true");
        // T859 / §LXV.4 — the search cache is ON for this context on purpose: the
        // cache-key collision T858 fixed is invisible with the cache off, and a
        // mocked unit test cannot observe "two distinct bodies, one cached
        // response" at all. This is the one test shape that can.
        registry.add("turing.search.cache.enabled", () -> "true");
    }

    @Autowired
    private TurImportExchange importExchange;

    @Autowired
    private TurSNSiteRepository siteRepository;

    @Autowired
    private TurSNProcessQueue processQueue;

    @Autowired
    private com.viglet.turing.sn.manifest.TurSNSiteManifestService manifestService;

    @Autowired
    private TurSNSiteSpotlightRepository spotlightRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Autowired
    private TurSNSiteMetricAccessRepository metricAccessRepository;

    @Autowired
    private TurSNSiteMetricClickRepository metricClickRepository;

    @Autowired
    private TurSNClickThroughService clickThroughService;

    @Autowired
    private TurDevTokenRepository devTokenRepository;

    @Autowired
    private com.viglet.turing.sn.insights.TurSNSiteMetricsResetService metricsResetService;

    @Autowired
    private com.viglet.turing.sn.TurSNPartitionWriteGuard partitionWriteGuard;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = JsonMapper.builder().build();

    private static Path luceneDir;

    /**
     * Imports the {@code mythical-creatures} Lucene sample (structure + content)
     * once for the class, pointing the Lucene index at a throwaway temp dir.
     */
    @BeforeAll
    void importLuceneSampleSite() throws IOException {
        luceneDir = Files.createTempDirectory("turing-sdk-it-lucene");

        byte[] zip = buildImportZip(luceneDir);
        MockMultipartFile bundle = new MockMultipartFile(
                "file", "mythical-creatures.zip", "application/zip", zip);

        // includeContent=true → indexes the docs synchronously; overwrite=true →
        // upserts the site even if the H2 snapshot already carried it.
        ImportResult result = importExchange.importFromMultipartFile(bundle, true, false, null, true);

        assertTrue(result.siteImported() || siteRepository.findByNameIgnoreCase(SITE).isPresent(),
                "marketplace Lucene site should be imported/present");
        assertTrue(result.searchEngineAvailable(), "embedded Lucene engine should be available");
        assertTrue(siteRepository.findByNameIgnoreCase(SITE).isPresent(),
                "site '" + SITE + "' must exist after import");
    }

    @AfterAll
    void cleanupLuceneDir() {
        if (luceneDir != null && Files.exists(luceneDir)) {
            try (var walk = Files.walk(luceneDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // write.lock may linger on Windows — OS reaps the temp dir later.
                    }
                });
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }
    }

    // ── CSRF: the exact mechanism the SDK's ensureCsrfToken() relies on ──

    @Test
    void csrfEndpointExposesXsrfTokenHeader() throws Exception {
        HttpResponse<String> res = get("/csrf");
        assertEquals(200, res.statusCode(), "/csrf must be reachable");
        String token = res.headers().firstValue("X-XSRF-TOKEN").orElse(null);
        assertNotNull(token, "/csrf must surface the token via the X-XSRF-TOKEN header "
                + "(XSRF-TOKEN cookie is HttpOnly; SDK reads the header)");
        assertTrue(!token.isBlank(), "X-XSRF-TOKEN must be non-blank");
    }

    // ── Search: shape consumed by resolveDocuments() + real Lucene results ──

    @Test
    void searchReturnsDocumentedShapeWithRealResults() throws Exception {
        HttpResponse<String> res = get("/sn/" + SITE + "/search?q=dragon&_setlocale=" + LOCALE);
        assertEquals(200, res.statusCode());

        JsonNode body = json.readTree(res.body());
        JsonNode defaultFields = body.path("queryContext").path("defaultFields");
        // resolveDocuments() maps documents through these six default fields.
        for (String key : List.of("url", "title", "description", "text", "date", "image")) {
            assertTrue(defaultFields.has(key),
                    "queryContext.defaultFields must declare '" + key + "'");
        }
        assertEquals("title", defaultFields.path("title").asString());

        JsonNode documents = body.path("results").path("document");
        assertTrue(documents.isArray() && !documents.isEmpty(),
                "searching 'dragon' on the Lucene sample must return at least one document");
        assertTrue(documents.get(0).path("fields").has("title"),
                "each document must carry a 'fields' map");
    }

    @Test
    void wildcardSearchReturnsAllIndexedDocuments() throws Exception {
        HttpResponse<String> res = get("/sn/" + SITE + "/search?q=*&_setlocale=" + LOCALE);
        assertEquals(200, res.statusCode());

        JsonNode body = json.readTree(res.body());
        int count = body.path("queryContext").path("count").asInt(0);
        assertTrue(count >= 1, "wildcard search ('show all') must return the indexed corpus, got " + count);
    }

    // ── T859 / §LXV.4 — the whole Block AV POST surface, end to end ──

    /**
     * {@code LINKS} is the default, and the promise of T707 is that a consumer
     * that merely switches transport sees the response it already parses. Asserted
     * against the GET response for the <em>canonical</em> equivalent query, which
     * is what the adapter rebuilds the hrefs from.
     */
    @Test
    void postSearchQueryInLinksFormatMatchesTheGetResponse() throws Exception {
        HttpResponse<String> getRes =
                get("/sn/" + SITE + "/search?q=dragon&p=1&_setlocale=" + LOCALE);
        HttpResponse<String> postRes = postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"dragon","locale":"%s"}""".formatted(LOCALE));

        assertEquals(200, postRes.statusCode());
        assertEquals(json.readTree(getRes.body()), json.readTree(postRes.body()),
                "POST /search/query in LINKS format must return the GET response verbatim");
    }

    /**
     * The load-bearing assertion of T708: the structured body is
     * <em>link-free</em>. Not "has fewer links" — none at all, anywhere in the
     * body, so a consumer cannot accidentally keep parsing URLs.
     *
     * <p>
     * The scan covered everything except {@code results} until T864: this test
     * was the first thing to run the structured shape over a response that
     * actually has documents, and it found that
     * {@code results.document[].metadata[]} still carried {@code href}. T864
     * projected those chips onto {@code field}/{@code value}/{@code filterQuery}
     * and this scan lost its carve-out — which is what makes the claim real.
     * </p>
     */
    @Test
    void postSearchQueryInStructuredFormatCarriesNoLinks() throws Exception {
        HttpResponse<String> res = postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED"}"""
                        .formatted(LOCALE));
        assertEquals(200, res.statusCode());

        String body = res.body();
        for (String forbidden : List.of("\"link\"", "\"href\"", "\"cleanUpLink\"",
                "\"cleanUpFacets\"", "/api/sn/")) {
            assertTrue(!body.contains(forbidden),
                    "STRUCTURED response must not contain " + forbidden + ": " + body);
        }
        JsonNode structured = json.readTree(body);
        // T864 — the document's facet chips are actions, not URLs.
        JsonNode chip = structured.path("results").path("document").get(0).path("metadata").get(0);
        assertTrue(!chip.path("filterQuery").asString().isBlank(),
                "each document chip must carry the filterQuery to add to request.fq");
        assertEquals("*", structured.path("request").path("q").asString(),
                "the structured response must echo the effective request");
        assertTrue(structured.path("facets").isArray() && !structured.path("facets").isEmpty(),
                "the sample site is faceted, so the structured response must carry facets");
    }

    /**
     * The production bug Block AV closed, end to end: a facet value containing
     * {@code &} is selected by putting its {@code filterQuery} on the request —
     * no URL is built, parsed or re-encoded anywhere — and it must actually
     * filter.
     */
    @Test
    void structuredFilterQueryWithAReservedCharacterActuallyFilters() throws Exception {
        JsonNode all = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED"}"""
                        .formatted(LOCALE)).body());
        int totalCount = all.path("queryContext").path("count").asInt();
        JsonNode item = facetItem(all, RESERVED_FACET_FIELD, RESERVED_FACET_VALUE);
        assertNotNull(item, "the reserved-character facet value must be offered by the site");

        String filterQuery = item.path("filterQuery").asString();
        assertTrue(filterQuery.contains("&"),
                "the filterQuery must carry the raw '&', not an escaped form: " + filterQuery);

        JsonNode filtered = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED","fq":["%s"]}"""
                        .formatted(LOCALE, filterQuery)).body());
        int filteredCount = filtered.path("queryContext").path("count").asInt();

        assertEquals(item.path("count").asInt(), filteredCount,
                "selecting the facet must return exactly the documents it counted");
        assertTrue(filteredCount < totalCount, "the filter must actually narrow the result set");
        assertEquals(RESERVED_DOCUMENT_ID,
                filtered.path("results").path("document").get(0).path("fields").path("id")
                        .asString(),
                "the filtered result must be the reserved-character document");
    }

    /**
     * T1115 — selecting a facet value keeps the facet in the structured body, says
     * which value is applied and how to clear it; deselecting it restores the
     * unfiltered facet.
     */
    @Test
    void structuredFacetStaysAppliedAndClearableWhenToggled() throws Exception {
        JsonNode all = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED"}"""
                        .formatted(LOCALE)).body());
        JsonNode item = facetItem(all, RESERVED_FACET_FIELD, RESERVED_FACET_VALUE);
        assertNotNull(item, "the facet value must be offered by the site");
        String filterQuery = item.path("filterQuery").asString();

        JsonNode on = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED","fq":["%s"]}"""
                        .formatted(LOCALE, filterQuery)).body());
        JsonNode facet = facet(on, RESERVED_FACET_FIELD);
        assertNotNull(facet, "a filtered facet must stay in facets: " + on.path("facets"));
        JsonNode selected = facetItem(on, RESERVED_FACET_FIELD, RESERVED_FACET_VALUE);
        assertNotNull(selected, "the applied value must still be listed: " + facet);
        assertTrue(selected.path("selected").asBoolean(false),
                "the applied value must be marked selected: " + selected);
        assertTrue(on.path("selectedFilterQueries").toString().contains(RESERVED_FACET_VALUE),
                "the response must list the applied filter: " + on.path("selectedFilterQueries"));
        assertTrue(facet.path("clearFilterQueries").toString().contains(RESERVED_FACET_VALUE),
                "the facet must say what to remove to clear it: " + facet);

        JsonNode off = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED"}"""
                        .formatted(LOCALE)).body());
        JsonNode cleared = facetItem(off, RESERVED_FACET_FIELD, RESERVED_FACET_VALUE);
        assertNotNull(cleared, "the value must be offered again once the filter is removed");
        assertTrue(!cleared.path("selected").asBoolean(false),
                "a removed filter must no longer read as selected: " + cleared);
    }

    /**
     * T858 regression guard, and the exact shape a mocked unit suite cannot
     * express: with the cache <em>enabled</em>, two distinct bodies must not
     * share a cached response. The old key read the (absent) POST query string
     * and collapsed both onto {@code <site>_null}.
     */
    @Test
    void distinctPostBodiesDoNotShareACachedResponse() throws Exception {
        JsonNode dragons = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"dragon","locale":"%s"}""".formatted(LOCALE)).body());
        JsonNode everything = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s"}""".formatted(LOCALE)).body());

        int dragonCount = dragons.path("queryContext").path("count").asInt();
        int totalCount = everything.path("queryContext").path("count").asInt();
        assertTrue(dragonCount > 0, "the 'dragon' query must match something");
        assertTrue(dragonCount < totalCount,
                "a cached response was served for a different body: 'dragon' returned "
                        + dragonCount + " and '*' returned " + totalCount);
    }

    // ── T911: the assertion Block BU spent eleven tasks not making ──

    /**
     * T911 / §LXVIII.18 — <strong>a write changes what the next search returns.</strong>
     *
     * <p>
     * Every eviction task in Block BU was verified by a unit test of a component or a lint
     * over the source; none of them by doing it. The risk that leaves is specific: two
     * independent functions have to agree for eviction to work at all —
     * {@code TurSNSearchCacheKey.of(context)}, which the {@code @Cacheable} SpEL stores
     * under (wrapped since T895 in {@code TurTenantCacheKey.scoped}), and
     * {@code sitePrefix(siteName)}, which the evictor matches. A unit test builds both from
     * the same helpers, so it cannot see them drift apart. This lets the real proxy write a
     * real key and asks the real evictor to remove it.
     * </p>
     *
     * <p>
     * Indexing runs through {@link TurSNProcessQueue#processIndexingQueue} directly rather
     * than over JMS: the same {@code @Transactional} proxy, so the T901 after-commit
     * eviction is exercised as it is in production, without an async await to make the
     * assertion flaky. The document is deindexed at the end so the class stays hermetic
     * whatever order its tests run in — and that restore is a second eviction, through the
     * DELETE branch.
     * </p>
     */
    @Test
    void indexingEvictsTheCachedAnswerAndChangesIt() throws Exception {
        int before = totalDocuments();
        assertTrue(cachedEntriesForSite() > 0,
                "the search must have been cached, or this test asserts nothing about eviction");

        index(TurSNJobAction.CREATE, CACHE_PROBE_ID);
        try {
            assertEquals(0, cachedEntriesForSite(),
                    "indexing left the site's cached answers in place: the key the @Cacheable "
                            + "SpEL stores under and the prefix the evictor matches have drifted");
            assertEquals(before + 1, totalDocuments(),
                    "the next search must see the indexed document");
        } finally {
            index(TurSNJobAction.DELETE, CACHE_PROBE_ID);
        }

        assertEquals(0, cachedEntriesForSite(), "deindexing must evict too");
        assertEquals(before, totalDocuments(), "the probe document must be gone again");
    }

    /**
     * T911 / §LXVIII.18 — the other door. A spotlight is not a document: it reaches the
     * cache through {@code TurSNSearchCacheEvictionListener} (T896) on a JPA callback,
     * scheduled after commit (T903/T904), with no indexing queue involved at all.
     */
    @Test
    void aConfigurationWriteEvictsTheCachedAnswer() throws Exception {
        totalDocuments();
        assertTrue(cachedEntriesForSite() > 0, "the search must have been cached first");

        TurSNSiteSpotlight spotlight = new TurSNSiteSpotlight();
        spotlight.setName("t911-probe");
        spotlight.setLanguage(Locale.forLanguageTag("en-US"));
        spotlight.setTurSNSite(siteRepository.findByNameIgnoreCase(SITE).orElseThrow());
        TurSNSiteSpotlight saved = spotlightRepository.save(spotlight);
        try {
            assertEquals(0, cachedEntriesForSite(),
                    "a spotlight write must clear the cached answers: it changes what a "
                            + "search returns without touching the indexing queue");
        } finally {
            spotlightRepository.deleteById(saved.getId());
        }
    }

    /**
     * T1195 — clearing a site's metrics on the real schema: a search logged with
     * targeting rules (a collection table) and a click before the cut go, the
     * ones after it stay.
     */
    @Test
    void clearingMetricsRemovesTheSearchesAndClicksBeforeTheCut() {
        var site = siteRepository.findByName(SITE).orElseThrow();
        Instant cut = Instant.now().plusSeconds(3600);
        String term = "t1195-" + java.util.UUID.randomUUID();
        TurSNSiteMetricAccess old = new TurSNSiteMetricAccess();
        old.setAccessDate(cut.minusSeconds(60));
        old.setTerm(term);
        old.setTargetingRules(new java.util.HashSet<>(java.util.Set.of("role:probe")));
        old.setTurSNSite(site);
        metricAccessRepository.save(old);
        TurSNSiteMetricAccess kept = new TurSNSiteMetricAccess();
        kept.setAccessDate(cut.plusSeconds(60));
        kept.setTerm(term);
        kept.setTurSNSite(site);
        metricAccessRepository.save(kept);

        var cleared = metricsResetService.clear(site, cut, "it");

        assertTrue(cleared.searches() >= 1, "the search before the cut must go: " + cleared);
        assertTrue(metricAccessRepository.findById(old.getId()).isEmpty(), "the old search is gone");
        assertTrue(metricAccessRepository.findById(kept.getId()).isPresent(), "the later search stays");
        metricAccessRepository.deleteById(kept.getId());
    }

    /**
     * T1194 — in pt_BR an accented word combined with another finds what its
     * folded spelling finds: the two-word query with the accent keeps the product
     * named with both words.
     */
    @Test
    void anAccentedTwoWordQueryFindsWhatItsFoldedSpellingFinds() throws Exception {
        Map<String, Object> attributes = new HashMap<>(Map.of("id", "t1194-borrowdale",
                "title", "Jaqueta impermeável do dia a dia Borrowdale",
                "description", "Uma jaqueta leve para a chuva da cidade."));
        processQueue.processIndexingQueue(new TurSNJobItems(new TurSNJobItem(TurSNJobAction.CREATE,
                List.of(SITE), Locale.forLanguageTag("pt-BR"), attributes, null)));
        try {
            for (String q : List.of("jaqueta impermeável", "jaqueta impermeavel", "impermeável")) {
                List<String> ids = documentIds(postJson("/sn/" + SITE + "/search/query",
                        """
                                {"q":"%s","locale":"pt_BR","rows":50}""".formatted(q), null));
                assertTrue(ids.contains("t1194-borrowdale"), "'" + q + "' must find the jacket: " + ids);
            }
        } finally {
            processQueue.processIndexingQueue(new TurSNJobItems(new TurSNJobItem(TurSNJobAction.DELETE,
                    List.of(SITE), Locale.forLanguageTag("pt-BR"),
                    new HashMap<>(Map.of("id", "t1194-borrowdale")), null)));
        }
    }

    /**
     * T1194 — the same product with its title in Unicode NFD (the á as an a plus a
     * combining acute, as text pasted from macOS arrives): every spelling a browser
     * sends still finds it.
     */
    @Test
    void aDecomposedAccentIsFoundByEverySpelling() throws Exception {
        Map<String, Object> attributes = new HashMap<>(Map.of("id", "t1194-borrowdale-nfd",
                "title", "Jaqueta impermeável do dia a dia Borrowdale"));
        processQueue.processIndexingQueue(new TurSNJobItems(new TurSNJobItem(TurSNJobAction.CREATE,
                List.of(SITE), Locale.forLanguageTag("pt-BR"), attributes, null)));
        try {
            for (String q : List.of("jaqueta impermeável", "jaqueta impermeavel", "impermeável")) {
                List<String> ids = documentIds(postJson("/sn/" + SITE + "/search/query",
                        """
                                {"q":"%s","locale":"pt_BR","rows":50}""".formatted(q), null));
                assertTrue(ids.contains("t1194-borrowdale-nfd"), "'" + q + "' must find the NFD jacket: " + ids);
            }
        } finally {
            processQueue.processIndexingQueue(new TurSNJobItems(new TurSNJobItem(TurSNJobAction.DELETE,
                    List.of(SITE), Locale.forLanguageTag("pt-BR"),
                    new HashMap<>(Map.of("id", "t1194-borrowdale-nfd")), null)));
        }
    }

    /** T1196 — in pt_BR the singular in -ão finds the product the catalog names in -ões. */
    @Test
    void aPortugueseSingularInAoFindsItsPluralInOes() throws Exception {
        Map<String, Object> attributes = new HashMap<>(Map.of("id", "t1196-scafell",
                "title", "Bastões de caminhada Scafell"));
        processQueue.processIndexingQueue(new TurSNJobItems(new TurSNJobItem(TurSNJobAction.CREATE,
                List.of(SITE), Locale.forLanguageTag("pt-BR"), attributes, null)));
        try {
            List<String> ids = documentIds(postJson("/sn/" + SITE + "/search/query", """
                    {"q":"bastão de caminhada","locale":"pt_BR","rows":50}""", null));
            assertTrue(ids.contains("t1196-scafell"), "'bastão de caminhada' must find the poles: " + ids);
        } finally {
            processQueue.processIndexingQueue(new TurSNJobItems(new TurSNJobItem(TurSNJobAction.DELETE,
                    List.of(SITE), Locale.forLanguageTag("pt-BR"),
                    new HashMap<>(Map.of("id", "t1196-scafell")), null)));
        }
    }

    /** Documents matching {@code *}, which also populates the search cache. */
    private int totalDocuments() throws Exception {
        return json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s"}""".formatted(LOCALE)).body())
                .path("queryContext").path("count").asInt();
    }

    /**
     * How many entries the {@code searchAPI} cache holds for this site, read off the native
     * map — the same shape {@code TurSNSearchCacheEvictor} reads, so a key the evictor
     * cannot see is a key this cannot see either.
     */
    private long cachedEntriesForSite() {
        org.springframework.cache.Cache cache =
                cacheManager.getCache(TurSNSearchCacheEvictor.SEARCH_CACHE);
        assertNotNull(cache, "the searchAPI cache must exist when the cache is enabled");
        Object nativeCache = cache.getNativeCache();
        assertTrue(nativeCache instanceof Map,
                "this assertion reads the native map; the manager wired here must provide one");
        return ((Map<?, ?>) nativeCache).keySet().stream()
                .map(String::valueOf)
                .filter(key -> key.toLowerCase(Locale.ROOT).contains("site=" + SITE))
                .count();
    }

    /** Runs one job item through the real queue processor, transaction and all. */
    /**
     * T1122 — a custom facet provisioned through the manifest reaches the search
     * answer with its buckets counted, as a console-made one does.
     */
    @Test
    void manifestCustomFacetReachesTheSearchAnswer() throws Exception {
        var risk = com.viglet.core.manifest.VigletFieldSpec.builder()
                .name("risk").type(com.viglet.core.manifest.VigletFieldType.INT)
                .mandatory(false).multiValued(false).facet(true).build();
        var buckets = new com.viglet.turing.sn.manifest.TurSNManifestCustomFacet("risk", "risk_range", "Risk",
                null, null, List.of(
                        new com.viglet.turing.sn.manifest.TurSNManifestCustomFacetItem("Low", null, null, null,
                                java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(20), null, null),
                        new com.viglet.turing.sn.manifest.TurSNManifestCustomFacetItem("High", null, null, null,
                                java.math.BigDecimal.valueOf(21), java.math.BigDecimal.valueOf(50), null, null)));
        manifestService.provisionRequest(new com.viglet.turing.sn.manifest.TurSNManifestRequest(SITE, null, null,
                null, List.of(LOCALE), List.of(risk), null, List.of(buckets), null));
        Map<String, Integer> probes = Map.of("t1122-low-1", 10, "t1122-low-2", 20, "t1122-high", 40);
        probes.forEach((id, value) -> indexRisk(TurSNJobAction.CREATE, id, value));
        try {
            JsonNode structured = json.readTree(postJson("/sn/" + SITE + "/search/query",
                    """
                            {"q":"*","locale":"%s","responseFormat":"STRUCTURED"}"""
                            .formatted(LOCALE)).body());

            JsonNode facet = facet(structured, "risk_range");
            assertNotNull(facet, "the manifest custom facet must be in facets: "
                    + structured.path("facets").findValuesAsString("name"));
            assertEquals(2, facetItemCount(facet, "Low"), "Low counts the probes at 10 and 20: " + facet);
            assertEquals(1, facetItemCount(facet, "High"), "High counts the probe at 40: " + facet);
        } finally {
            probes.forEach((id, value) -> indexRisk(TurSNJobAction.DELETE, id, value));
        }
    }

    private static int facetItemCount(JsonNode facet, String label) {
        for (JsonNode item : facet.path("items")) {
            if (label.equals(item.path("label").asString()) || label.equals(item.path("value").asString())) {
                return item.path("count").asInt(-1);
            }
        }
        return -1;
    }

    private void indexRisk(TurSNJobAction action, String id, int risk) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        if (action == TurSNJobAction.CREATE) {
            attributes.put("type", "CreatureProbe");
            attributes.put("title", "T1122 risk probe " + id);
            attributes.put("risk", risk);
        }
        processQueue.processIndexingQueue(new TurSNJobItems(
                new TurSNJobItem(action, List.of(SITE), Locale.forLanguageTag("en-US"),
                        attributes, null)));
    }

    private void index(TurSNJobAction action, String id) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        if (action == TurSNJobAction.CREATE) {
            attributes.put("type", "CreatureProbe");
            attributes.put("title", "T911 cache probe");
            attributes.put("text", "a document that exists only to be indexed and removed");
        }
        processQueue.processIndexingQueue(new TurSNJobItems(
                new TurSNJobItem(action, List.of(SITE), Locale.forLanguageTag("en-US"),
                        attributes, null)));
    }

    /** T712 — the autocomplete POST variant takes the same anonymous JSON body. */
    @Test
    void postAutoCompleteQueryAcceptsTheJsonBody() throws Exception {
        HttpResponse<String> res = postJson("/sn/" + SITE + "/ac/query",
                """
                        {"q":"dra","locale":"%s","rows":10,"fq":["%s:%s"]}"""
                        .formatted(LOCALE, RESERVED_FACET_FIELD, RESERVED_FACET_VALUE));

        assertEquals(200, res.statusCode());
        assertTrue(json.readTree(res.body()).isArray(),
                "/ac/query must return a JSON array of suggestions");
    }

    /** T712 — the select-list POST variant, same body, list projection. */
    @Test
    void postSelectListQueryAcceptsTheJsonBody() throws Exception {
        HttpResponse<String> res = postJson("/sn/" + SITE + "/search/list/query",
                """
                        {"q":"*","locale":"%s"}""".formatted(LOCALE));

        assertEquals(200, res.statusCode());
        assertTrue(json.readTree(res.body()).isArray(),
                "/search/list/query must return a JSON array");
    }

    /** T856 — the structured shape is reachable over GET as well. */
    @Test
    void getSearchHonoursTheStructuredResponseFormat() throws Exception {
        HttpResponse<String> res = get("/sn/" + SITE + "/search?q=*&_setlocale=" + LOCALE
                + "&responseFormat=structured");

        assertEquals(200, res.statusCode());
        JsonNode structured = json.readTree(res.body());
        assertTrue(!res.body().contains("\"href\""),
                "GET with responseFormat=structured must return the link-free shape");
        assertEquals("*", structured.path("request").path("q").asString(),
                "the GET structured response must echo the effective request too");
    }

    /**
     * T1114 — "did you mean" must reach a POST-native client: the structured POST
     * body carries the same {@code spellCheck} the GET search computes for the
     * same misspelling.
     */
    @Test
    void structuredPostCarriesTheSpellCheckTheGetSearchReturns() throws Exception {
        JsonNode viaGet = json.readTree(get("/sn/" + SITE + "/search?q=dragn&_setlocale=" + LOCALE
                + "&responseFormat=structured").body()).path("spellCheck");
        JsonNode viaPost = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"dragn","locale":"%s","responseFormat":"STRUCTURED"}"""
                        .formatted(LOCALE)).body()).path("spellCheck");

        assertTrue(viaGet.path("correctedText").asBoolean(false),
                "the GET search must offer a correction for 'dragn': " + viaGet);
        assertEquals("dragon", viaPost.path("correctedTextValue").asString(),
                "the structured POST must carry the correction: " + viaPost);
        assertEquals(viaGet, viaPost, "both transports must agree on spellCheck");
    }

    /**
     * T1120 — an as-you-type search finds the documents a half-typed word starts,
     * and echoes the word as typed.
     */
    @Test
    void instantSearchMatchesTheLastTermAsAPrefix() throws Exception {
        JsonNode instant = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"drag","locale":"%s","responseFormat":"STRUCTURED","mode":"instant"}"""
                        .formatted(LOCALE)).body());
        JsonNode whole = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"dragon","locale":"%s","responseFormat":"STRUCTURED"}"""
                        .formatted(LOCALE)).body());

        assertTrue(instant.path("queryContext").path("count").asInt(0) >= 1,
                "'drag' typed into an instant box must find the dragons: " + instant.path("queryContext"));
        assertTrue(instant.path("queryContext").path("count").asInt(0)
                        >= whole.path("queryContext").path("count").asInt(0),
                "the prefix must find at least what the whole word finds");
        assertEquals("drag", instant.path("request").path("q").asString(),
                "the echo carries what was typed, not the prefix query");
        assertEquals("instant", instant.path("request").path("mode").asString());
    }

    /** T1123 — the AI capabilities probe answers anonymously and agrees with /chat/enabled. */
    @Test
    void aiCapabilitiesAgreeWithChatEnabled() throws Exception {
        HttpResponse<String> res = get("/sn/" + SITE + "/ai/capabilities");
        assertEquals(200, res.statusCode());
        JsonNode capabilities = json.readTree(res.body());
        JsonNode chatEnabled = json.readTree(get("/sn/" + SITE + "/chat/enabled").body());

        assertEquals(chatEnabled.path("reason").asString(), capabilities.path("chat").path("reason").asString());
        assertEquals(capabilities.path("chat"), capabilities.path("answer"),
                "chat and answer read the same route");
        for (String feature : List.of("nlFilters", "copilot", "similarByVector", "summaries")) {
            assertTrue(capabilities.path(feature).has("enabled"), feature + " must be reported: " + capabilities);
        }
    }

    /** T1128 — a click sent with navigator.sendBeacon (text/plain JSON) is accepted. */
    @Test
    void clickAcceptsABeaconBody() throws Exception {
        HttpRequest beacon = HttpRequest.newBuilder(URI.create(base() + "/sn/" + SITE + "/click"))
                .header("Content-Type", "text/plain;charset=UTF-8")
                .POST(BodyPublishers.ofString("{\"term\":\"dragon\",\"documentId\":\"myth-001\",\"position\":1}"))
                .build();

        // 202 when metrics are on, 204 with X-Turing-Metrics when off (T1131); never 415.
        HttpResponse<String> res = http.send(beacon, BodyHandlers.ofString());
        assertEquals(2, res.statusCode() / 100, "a beacon click must be accepted, got " + res.statusCode());
    }

    /**
     * T1133 — two partitions of one site, each with its own key: a key's search
     * answers only its partition's documents, its import cannot write into the
     * other partition, and an endpoint that does not enforce partitions refuses it.
     */
    @Test
    void aPartitionedKeyStaysInsideItsPartition() throws Exception {
        String keyA = partitionedKey("a");
        String keyB = partitionedKey("b");
        indexPartitionProbe(TurSNJobAction.CREATE, "a");
        indexPartitionProbe(TurSNJobAction.CREATE, "b");
        try {
            String everything = """
                    {"q":"*","locale":"%s","rows":200}""".formatted(LOCALE);
            assertEquals(List.of(partitionProbeId("a")), documentIds(postJson("/sn/" + SITE + "/search/query",
                    everything, keyA)));
            assertEquals(List.of(partitionProbeId("b")), documentIds(postJson("/sn/" + SITE + "/search/query",
                    everything, keyB)));
            List<String> unpartitioned = documentIds(postJson("/sn/" + SITE + "/search/query", everything, null));
            assertTrue(unpartitioned.containsAll(List.of(partitionProbeId("a"), partitionProbeId("b"))),
                    "a request without a partitioned key sees the whole site");

            // Autocomplete suggests from the partition's own documents. Since T1226 a
            // partitioned autocomplete answers terms, never titles, so each probe
            // carries a term the other lacks.
            HttpResponse<String> autocomplete = getWithKey("/sn/" + SITE + "/ac?q=zephyr&_setlocale=" + LOCALE,
                    keyA);
            assertEquals(200, autocomplete.statusCode(), autocomplete.body());
            assertTrue(autocomplete.body().contains("zephyra") && !autocomplete.body().contains("zephyrb"),
                    "key A's suggestions must be its own terms only: " + autocomplete.body());

            // T1133i — duplicate clusters are confined to the key's partition.
            HttpResponse<String> duplicates = getWithKey("/sn/" + SITE + "/search/duplicates?id="
                    + partitionProbeId("a") + "&_setlocale=" + LOCALE, keyA);
            assertEquals(200, duplicates.statusCode(), duplicates.body());
            assertTrue(!duplicates.body().contains(partitionProbeId("b")),
                    "key A's duplicate clusters must not name partition B: " + duplicates.body());

            HttpResponse<String> intoB = postJson("/sn/import", """
                    {"turingDocuments":[{"turSNJobAction":"CREATE","siteNames":["%s"],"locale":"en_US",
                     "attributes":{"id":"t1133-forged","title":"forged","%s":"t1133-b"}}]}"""
                    .formatted(SITE, RESERVED_FACET_FIELD), keyA);
            assertEquals(403, intoB.statusCode(), "key A must not write into partition B: " + intoB.body());

            // An id is all a write names, so key A cannot take over or delete B's document by it.
            HttpResponse<String> overwriteB = postJson("/sn/import", """
                    {"turingDocuments":[{"turSNJobAction":"CREATE","siteNames":["%s"],"locale":"en_US",
                     "attributes":{"id":"%s","title":"taken over"}}]}"""
                    .formatted(SITE, partitionProbeId("b")), keyA);
            assertEquals(403, overwriteB.statusCode(), "key A must not replace B's document: " + overwriteB.body());
            HttpResponse<String> deleteB = postJson("/sn/import", """
                    {"turingDocuments":[{"turSNJobAction":"DELETE","siteNames":["%s"],"locale":"en_US",
                     "attributes":{"id":"%s"}}]}""".formatted(SITE, partitionProbeId("b")), keyA);
            assertEquals(403, deleteB.statusCode(), "key A must not delete B's document: " + deleteB.body());
            // The two probes are near-identical, so B is A's most similar document:
            // a partitioned key must not see it, nor read it, nor seed from it.
            HttpResponse<String> similar = getWithKey("/sn/" + SITE + "/search/similar?id="
                    + partitionProbeId("a") + "&_setlocale=" + LOCALE, keyA);
            assertEquals(200, similar.statusCode(), similar.body());
            assertTrue(!similar.body().contains(partitionProbeId("b")), "key A saw B as similar: " + similar.body());
            assertEquals("[]", getWithKey("/sn/" + SITE + "/search/similar?id=" + partitionProbeId("b")
                    + "&_setlocale=" + LOCALE, keyA).body().replace(" ", ""), "key A seeded from B's document");
            // T1132 — the request's own filters narrow the neighbours, with no key at all.
            HttpResponse<String> filtered = get("/sn/" + SITE + "/search/similar?id=" + partitionProbeId("a")
                    + "&_setlocale=" + LOCALE + "&fq%5B%5D=" + RESERVED_FACET_FIELD + ":t1133-a");
            assertEquals(200, filtered.statusCode(), filtered.body());
            assertTrue(!filtered.body().contains(partitionProbeId("b")),
                    "fq[] must keep B out of A's neighbours: " + filtered.body());
            assertTrue(get("/sn/" + SITE + "/search/similar?id=" + partitionProbeId("a") + "&_setlocale=" + LOCALE)
                    .body().contains(partitionProbeId("b")), "without a filter B is A's neighbour, or this "
                            + "test asserts nothing");
            // T1235 — two values of one field: fqi.op=AND admits nothing (no document
            // holds both), fqi.op=OR admits B. Without fqi.op the field's own facet
            // setting decides, so both directions are pinned to prove it is read.
            String bothValues = "&fq%5B%5D=" + RESERVED_FACET_FIELD + ":t1133-a"
                    + "&fq%5B%5D=" + RESERVED_FACET_FIELD + ":t1133-b";
            assertTrue(!get("/sn/" + SITE + "/search/similar?id=" + partitionProbeId("a") + "&_setlocale="
                    + LOCALE + bothValues + "&fqi.op=AND").body().contains(partitionProbeId("b")),
                    "fqi.op=AND must AND the two values of one field");
            HttpResponse<String> eitherValue = get("/sn/" + SITE + "/search/similar?id=" + partitionProbeId("a")
                    + "&_setlocale=" + LOCALE + bothValues + "&fqi.op=OR");
            assertEquals(200, eitherValue.statusCode(), eitherValue.body());
            assertTrue(eitherValue.body().contains(partitionProbeId("b")),
                    "fqi.op=OR must admit B, which holds one of the two values: " + eitherValue.body());
            HttpResponse<String> duplicatesEither = get("/sn/" + SITE + "/search/duplicates?id="
                    + partitionProbeId("a") + "&_setlocale=" + LOCALE + bothValues + "&fqi.op=OR");
            assertEquals(200, duplicatesEither.statusCode(), "duplicates must bind fqi.op: "
                    + duplicatesEither.body());

            assertEquals(404, getWithKey("/sn/" + SITE + "/search/document?id=" + partitionProbeId("b")
                    + "&_setlocale=" + LOCALE, keyA).statusCode(), "key A read B's document");
            assertEquals(200, getWithKey("/sn/" + SITE + "/search/document?id=" + partitionProbeId("a")
                    + "&_setlocale=" + LOCALE, keyA).statusCode(), "key A reads its own document");

            // The metrics a partitioned key reads are its partition's searches only.
            var site = siteRepository.findByName(SITE).orElseThrow();
            String failedA = "t1133-failed-a-" + java.util.UUID.randomUUID();
            String failedB = "t1133-failed-b-" + java.util.UUID.randomUUID();
            for (String[] logged : new String[][] { { failedA, RESERVED_FACET_FIELD + "=t1133-a" },
                    { failedB, RESERVED_FACET_FIELD + "=t1133-b" } }) {
                TurSNSiteMetricAccess access = new TurSNSiteMetricAccess();
                access.setAccessDate(Instant.now());
                access.setTerm(logged[0]);
                access.setNumFound(0);
                access.setPartitionKey(logged[1]);
                access.setTurSNSite(site);
                metricAccessRepository.save(access);
            }
            HttpResponse<String> failed = getWithKey("/sn/" + site.getId()
                    + "/metrics/failed-searches?period=today&rows=50", keyA);
            assertEquals(200, failed.statusCode(), failed.body());
            assertTrue(failed.body().contains(failedA) && !failed.body().contains(failedB),
                    "key A's failed searches must be its own: " + failed.body());

            // T1112 — a DELETE naming a field removes every document holding it, on
            // the real engine: partition B's delete-all leaves A's document alone.
            Map<String, Object> partitionB = new HashMap<>(Map.of(RESERVED_FACET_FIELD, "t1133-b"));
            processQueue.processIndexingQueue(new TurSNJobItems(new TurSNJobItem(TurSNJobAction.DELETE,
                    List.of(SITE), Locale.forLanguageTag("en-US"), partitionB, null)));
            assertEquals(List.of(), documentIds(postJson("/sn/" + SITE + "/search/query", everything, keyB)),
                    "the field delete must remove B's documents");
            assertEquals(List.of(partitionProbeId("a")),
                    documentIds(postJson("/sn/" + SITE + "/search/query", everything, keyA)));

            // Its own document it may rewrite: read from the engine's stored fields, as
            // the import would, without queueing a write that outlives this test.
            partitionWriteGuard.check(new com.viglet.turing.commons.se.TurSEPartition(RESERVED_FACET_FIELD,
                    "t1133-a"), new TurSNJobItems(new TurSNJobItem(TurSNJobAction.CREATE, List.of(SITE),
                            Locale.forLanguageTag("en-US"),
                            new HashMap<>(Map.of("id", partitionProbeId("a"))), null)));
        } finally {
            indexPartitionProbe(TurSNJobAction.DELETE, "a");
            indexPartitionProbe(TurSNJobAction.DELETE, "b");
        }
    }

    /**
     * T1209 — the admin routes a site sharing an SN site needs, over two partitions:
     * a key reads the site's configuration and synonyms, cannot change the shared
     * schema or synonyms, and its spotlights are its partition's alone.
     */
    @Test
    void aPartitionedKeyReachesTheAdminRoutesInsideItsPartition() throws Exception {
        String keyA = partitionedKey("a");
        String keyB = partitionedKey("b");
        var site = siteRepository.findByName(SITE).orElseThrow();
        String spotlights = "/sn/" + site.getId() + "/spotlight";
        String spotlightId = null;
        try {
            assertEquals(200, getWithKey("/sn/" + site.getId(), keyA).statusCode(),
                    "key A reads the site configuration");
            assertEquals(403, getWithKey("/sn/export", keyA).statusCode(), "the export carries every partition");

            HttpResponse<String> change = postJson("/sn/manifest", """
                    {"name":"%s","locales":["%s"],"fields":[{"name":"t1209_new","type":"STRING"}]}"""
                    .formatted(SITE, LOCALE), keyA);
            assertEquals(403, change.statusCode(), "key A must not change the shared schema: " + change.body());
            assertTrue(change.body().contains("every partition shares"), change.body());

            assertEquals(200, getWithKey("/sn/" + site.getId() + "/synonym", keyA).statusCode(),
                    "key A reads the synonyms");
            HttpResponse<String> synonym = postJson("/sn/" + site.getId() + "/synonym", "{}", keyA);
            assertEquals(403, synonym.statusCode(), synonym.body());
            assertTrue(synonym.body().contains("shared by every partition"), synonym.body());

            HttpResponse<String> created = postJson(spotlights, """
                    {"name":"t1209 pin","language":"en_US","turSNSite":{"id":"%s"},
                     "partitionField":"%s","partitionValue":"t1133-b",
                     "turSNSiteSpotlightTerms":[{"name":"t1209pin"}],
                     "turSNSiteSpotlightDocuments":[{"position":1,"title":"t1209-pinned","type":"page",
                     "link":"/t1209","referenceId":"t1209-pinned"}]}"""
                    .formatted(site.getId(), RESERVED_FACET_FIELD), keyA);
            assertEquals(200, created.statusCode(), created.body());
            JsonNode spotlight = json.readTree(created.body());
            spotlightId = spotlight.path("id").asString();
            assertEquals("t1133-a", spotlight.path("partitionValue").asString(),
                    "a spotlight is the creating key's partition's, whatever the body says");

            assertTrue(getWithKey(spotlights, keyA).body().contains(spotlightId), "key A lists its spotlight");
            assertTrue(!getWithKey(spotlights, keyB).body().contains(spotlightId), "key B listed A's spotlight");
            assertEquals("false", deleteWithKey(spotlights + "/" + spotlightId, keyB).body().trim(),
                    "key B deleted A's spotlight");

            String pin = """
                    {"q":"t1209pin","locale":"%s"}""".formatted(LOCALE);
            assertTrue(postJson("/sn/" + SITE + "/search/query", pin, keyA).body().contains("t1209-pinned"),
                    "key A's search shows its own spotlight");
            assertTrue(!postJson("/sn/" + SITE + "/search/query", pin, keyB).body().contains("t1209-pinned"),
                    "key B's search showed A's spotlight");
        } finally {
            if (spotlightId != null) {
                deleteWithKey(spotlights + "/" + spotlightId, keyA);
            }
        }
    }

    /**
     * T1210 — the session a partitioned key opens keeps its partition: replaying
     * the cookie without the key reaches no other partition's document, no route
     * the guard refuses and no admin API.
     */
    @Test
    void aPartitionedKeysSessionStaysInsideItsPartition() throws Exception {
        String keyA = partitionedKey("a");
        indexPartitionProbe(TurSNJobAction.CREATE, "a");
        indexPartitionProbe(TurSNJobAction.CREATE, "b");
        try {
            String everything = """
                    {"q":"*","locale":"%s","rows":200}""".formatted(LOCALE);
            HttpResponse<String> keyed = postJson("/sn/" + SITE + "/search/query", everything, keyA);
            String cookie = keyed.headers().allValues("Set-Cookie").stream()
                    .map(header -> header.split(";", 2)[0])
                    .filter(pair -> pair.startsWith("TURJSESSIONID=") || pair.startsWith("JSESSIONID="))
                    .findFirst().orElse(null);
            org.junit.jupiter.api.Assumptions.assumeTrue(cookie != null,
                    "the key opened no session, so there is none to replay");

            HttpResponse<String> replayed = http.send(HttpRequest.newBuilder(URI.create(base() + "/sn/" + SITE
                    + "/search/query")).header("Content-Type", "application/json").header("Cookie", cookie)
                    .POST(BodyPublishers.ofString(everything)).build(), BodyHandlers.ofString());
            assertEquals(List.of(partitionProbeId("a")), documentIds(replayed),
                    "the replayed session must search partition A only");
            // A key presented beside another key's session is what answers.
            HttpResponse<String> keyBesideSession = http.send(HttpRequest.newBuilder(URI.create(base() + "/sn/"
                    + SITE + "/search/query")).header("Content-Type", "application/json").header("Cookie", cookie)
                    .header("Key", partitionedKey("b")).POST(BodyPublishers.ofString(everything)).build(),
                    BodyHandlers.ofString());
            assertEquals(List.of(partitionProbeId("b")), documentIds(keyBesideSession),
                    "key B beside A's session must search partition B only");
            // Routes a partitioned key is refused on stay refused to its session (the
            // export carries every partition's content).
            for (String path : List.of("/sn/export", "/dev/token")) {
                assertEquals(403, http.send(HttpRequest.newBuilder(URI.create(base() + path))
                        .header("Cookie", cookie).GET().build(), BodyHandlers.ofString()).statusCode(),
                        "the replayed session reached " + path);
            }
        } finally {
            indexPartitionProbe(TurSNJobAction.DELETE, "a");
            indexPartitionProbe(TurSNJobAction.DELETE, "b");
        }
    }

    private HttpResponse<String> deleteWithKey(String path, String key) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base() + path)).header("Key", key).DELETE().build(),
                BodyHandlers.ofString());
    }

    private String partitionedKey(String partition) {
        TurDevToken token = new TurDevToken();
        token.setTitle("T1133 partition " + partition);
        token.setToken("t1133" + partition + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        token.setPartitionField(RESERVED_FACET_FIELD);
        token.setPartitionValue("t1133-" + partition);
        devTokenRepository.save(token);
        return token.getToken();
    }

    private static String partitionProbeId(String partition) {
        return "t1133-probe-" + partition;
    }

    private void indexPartitionProbe(TurSNJobAction action, String partition) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", partitionProbeId(partition));
        if (action == TurSNJobAction.CREATE) {
            // A term only this partition holds, for the autocomplete assertion.
            attributes.put("title", "T1133 partition probe " + partition + " zephyr" + partition);
            attributes.put(RESERVED_FACET_FIELD, "t1133-" + partition);
        }
        processQueue.processIndexingQueue(new TurSNJobItems(
                new TurSNJobItem(action, List.of(SITE), Locale.forLanguageTag("en-US"), attributes, null)));
    }

    private List<String> documentIds(HttpResponse<String> response) throws Exception {
        assertEquals(200, response.statusCode(), response.body());
        List<String> ids = new java.util.ArrayList<>();
        json.readTree(response.body()).path("results").path("document")
                .forEach(document -> ids.add(document.path("fields").path("id").asString()));
        return ids;
    }

    private HttpResponse<String> getWithKey(String path, String key) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base() + path)).header("Key", key).GET().build(),
                BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String body, String key) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body));
        if (key != null) {
            req.header("Key", key);
        }
        return http.send(req.build(), BodyHandlers.ofString());
    }

    /**
     * T1131 — the click-through report runs its queries against the real schema:
     * searches and clicks join per term, and the locale narrows both logs.
     */
    @Test
    void clickThroughReportJoinsSearchesAndClicks() {
        var site = siteRepository.findByName(SITE).orElseThrow();
        Instant since = Instant.now().minusSeconds(1);
        // The H2 file outlives a run, so the term is new each time.
        String term = "ctr-" + java.util.UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            TurSNSiteMetricAccess access = new TurSNSiteMetricAccess();
            access.setAccessDate(Instant.now());
            access.setTerm(term);
            access.setLanguage(Locale.US);
            access.setNumFound(4);
            access.setTurSNSite(site);
            metricAccessRepository.save(access);
        }
        TurSNSiteMetricClick click = new TurSNSiteMetricClick();
        click.setAccessDate(Instant.now());
        click.setTerm(term);
        click.setLanguage(Locale.US);
        click.setDocumentId("myth-001");
        click.setPosition(3);
        click.setTurSNSite(site);
        metricClickRepository.save(click);
        var window = new TurSNSearchInsightsService.Window(since, Instant.now().plusSeconds(1));

        var report = clickThroughService.report(site, window, null, 50);
        var row = report.terms().stream().filter(t -> t.term().equals(term)).findFirst().orElseThrow();
        assertEquals(2, row.searches());
        assertEquals(1, row.clicks());
        assertEquals(0.5, row.ctr());
        assertEquals(3.0, row.avgPosition());

        var otherLocale = clickThroughService.report(site, window, Locale.of("pt", "BR"), 50);
        assertTrue(otherLocale.terms().stream().noneMatch(t -> t.term().equals(term)),
                "a pt_BR report must not count en_US searches");
    }

    /** T857 — the sort options can be inlined instead of separately fetched. */
    @Test
    void structuredResponseCanInlineTheSortOptions() throws Exception {
        JsonNode lean = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED"}"""
                        .formatted(LOCALE)).body());
        assertTrue(lean.path("sortOptions").isMissingNode(),
                "the default structured body must stay lean");

        JsonNode withOptions = json.readTree(postJson("/sn/" + SITE + "/search/query",
                """
                        {"q":"*","locale":"%s","responseFormat":"STRUCTURED","includeSortOptions":true}"""
                        .formatted(LOCALE)).body());
        JsonNode inlined = withOptions.path("sortOptions");
        assertTrue(inlined.isArray() && !inlined.isEmpty(),
                "includeSortOptions must inline the sort options");
        assertEquals(json.readTree(get("/sn/" + SITE + "/search/sort-options").body()), inlined,
                "the inlined list must be exactly what the dedicated endpoint serves");
    }

    // ── chat/enabled: ChatEnabledResponse contract ──

    @Test
    void chatEnabledReturnsDocumentedShape() throws Exception {
        HttpResponse<String> res = get("/sn/" + SITE + "/chat/enabled");
        assertEquals(200, res.statusCode());

        JsonNode body = json.readTree(res.body());
        assertTrue(body.has("enabled") && body.path("enabled").isBoolean(),
                "chat/enabled must return a boolean 'enabled'");
        if (!body.path("enabled").asBoolean()) {
            // A freshly-imported sample has no GenAI agent → deterministic reason.
            assertEquals(EXPECTED_DISABLED_REASON, body.path("reason").asString(),
                    "a site without GenAI must report reason=" + EXPECTED_DISABLED_REASON);
        }
    }

    // ── sort-options: useTuringSortOptions / fetchSortOptions contract ──

    @Test
    void sortOptionsReturnsArray() throws Exception {
        HttpResponse<String> res = get("/sn/" + SITE + "/search/sort-options");
        assertEquals(200, res.statusCode());
        assertTrue(json.readTree(res.body()).isArray(),
                "sort-options must be a JSON array of {value,label}");
    }

    // ── Chat SSE handshake: only meaningful when GenAI is configured ──

    @Test
    void chatConversationHandshakeIsServerSentEvents() throws Exception {
        boolean enabled = json.readTree(get("/sn/" + SITE + "/chat/enabled").body())
                .path("enabled").asBoolean();
        // The sample ships without an agent, so this asserts the SSE handshake
        // only on deployments where chat is actually enabled (no LLM required to
        // verify the content-type negotiation). Skips cleanly otherwise.
        assumeTrue(enabled, "chat disabled for the sample site — skipping SSE handshake assertion");

        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + "/sn/" + SITE + "/chat/conversation"))
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("{\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"locale\":\""
                        + LOCALE + "\"}"))
                .build();
        HttpResponse<String> res = http.send(req, BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        String contentType = res.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.startsWith("text/event-stream"),
                "chat/conversation must negotiate text/event-stream, got: " + contentType);
    }

    // ── helpers ──

    private String base() {
        return "http://localhost:" + port + "/api";
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + path)).GET().build();
        return http.send(req, BodyHandlers.ofString());
    }

    /**
     * T859 — posts an anonymous JSON body, exactly as the JS SDK does. No CSRF
     * token: {@code /api/sn/**} is exempt, which is itself part of the contract
     * the public read surface depends on.
     */
    private HttpResponse<String> postJson(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        return http.send(req, BodyHandlers.ofString());
    }

    /** Finds a facet item by field + value in a structured response. */
    private static JsonNode facetItem(JsonNode structured, String field, String value) {
        JsonNode facet = facet(structured, field);
        if (facet == null) {
            return null;
        }
        for (JsonNode item : facet.path("items")) {
            if (value.equals(item.path("value").asString())) {
                return item;
            }
        }
        return null;
    }

    private static JsonNode facet(JsonNode structured, String field) {
        for (JsonNode facet : structured.path("facets")) {
            if (field.equals(facet.path("name").asString())) {
                return facet;
            }
        }
        return null;
    }

    /**
     * Builds an in-memory import zip from the committed marketplace fixtures,
     * rewriting the Lucene {@code endpointUrl} to {@code targetIndexDir} so the
     * test owns an isolated index.
     */
    private byte[] buildImportZip(Path targetIndexDir) throws IOException {
        byte[] exportJson = rewriteLuceneEndpoint(readResource("/sdk-it/export.json"), targetIndexDir);
        byte[] contentJson = addReservedCharacterDocument(
                readResource("/sdk-it/mythical-creatures_content.json"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry("export.json"));
            zip.write(exportJson);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(SITE + "_content.json"));
            zip.write(contentJson);
            zip.closeEntry();
        }
        return baos.toByteArray();
    }

    /**
     * T859 / §LXV.4 — appends one synthetic creature whose {@code creature_type}
     * facet value contains an {@code &}.
     *
     * <p>
     * The committed fixture is a verbatim copy of the marketplace sample and has
     * no reserved character in any facet value, so it cannot exercise the bug
     * Block AV closed. Rather than diverge the fixture from its source, the
     * document is added <em>in memory</em> at zip time — the same treatment
     * {@link #rewriteLuceneEndpoint} already gives the Lucene endpoint.
     * </p>
     */
    private byte[] addReservedCharacterDocument(byte[] contentJson) {
        ObjectNode root = (ObjectNode) json.readTree(contentJson);
        ArrayNode documents = (ArrayNode) root.path(SITE).path(LOCALE);
        assertTrue(!documents.isEmpty(), "content fixture must carry documents");

        ObjectNode creature = (ObjectNode) documents.get(0).deepCopy();
        creature.put("id", RESERVED_DOCUMENT_ID);
        creature.put("title", "Ash & Ember Chimera");
        creature.put("description", "Synthetic contract-test creature.");
        creature.put("text", "Exists only to prove a reserved character survives the round-trip.");
        creature.set("creature_type", json.createArrayNode().add(RESERVED_FACET_VALUE));
        documents.add(creature);
        return json.writeValueAsBytes(root);
    }

    private byte[] rewriteLuceneEndpoint(byte[] exportJson, Path targetIndexDir) {
        ObjectNode root = (ObjectNode) json.readTree(exportJson);
        JsonNode seArray = root.path("se");
        assertTrue(seArray.isArray() && !seArray.isEmpty(), "export.json must declare a search engine");
        ((ObjectNode) seArray.get(0)).put("endpointUrl",
                targetIndexDir.toString().replace('\\', '/'));
        return json.writeValueAsBytes(root);
    }

    private byte[] readResource(String classpath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(classpath)) {
            assertNotNull(in, "missing test resource: " + classpath);
            return in.readAllBytes();
        }
    }
}
