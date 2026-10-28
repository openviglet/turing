/*
 *
 * Copyright (C) 2016-2025 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.genai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.genai.provider.store.TurStoreChunkPage;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.plugins.se.TurSearchEnginePlugin;
import com.viglet.turing.plugins.se.TurSearchEnginePluginFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * T1158 — removes the RAG chunks of documents the search engine no longer holds:
 * the ones stored before a delete reached the RAG collection, or deleted while
 * the store was down. It pages one site locale's collection, collects the
 * distinct {@link TurSNGenAi#SOURCE_ID}s, asks the engine which of them it still
 * holds, and deletes the rest.
 *
 * <p>An engine that cannot read documents by id would make every chunk look
 * orphaned, so the purge first proves the engine can: it reads one live document
 * id and requires {@code getDocumentsByIds} to resolve it, and refuses otherwise.
 * A chunk naming other sites only ({@link TurSNGenAi#SITES}) is never touched,
 * since this site's engine cannot answer for it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Slf4j
@Service
public class TurSNRagOrphanPurgeService {

    static final int CHUNK_PAGE_SIZE = 200;
    /** Bounds a runaway scan: 200 x 500 = 100 000 chunks per locale. */
    static final int MAX_CHUNK_PAGES = 500;
    static final int ID_BATCH = 100;

    private final TurGenAiContextFactory contextFactory;
    private final TurSearchEnginePluginFactory pluginFactory;
    private final TurRagContextBuilder ragContextBuilder;

    public TurSNRagOrphanPurgeService(TurGenAiContextFactory contextFactory,
            TurSearchEnginePluginFactory pluginFactory, TurRagContextBuilder ragContextBuilder) {
        this.contextFactory = contextFactory;
        this.pluginFactory = pluginFactory;
        this.ragContextBuilder = ragContextBuilder;
    }

    /**
     * The outcome for one site locale.
     *
     * @param locale   the locale's language tag
     * @param scanned  distinct documents found in the RAG collection
     * @param removed  documents whose chunks were deleted
     * @param refused  why nothing was purged, or {@code null} when the purge ran
     */
    public record Result(String locale, int scanned, int removed, String refused) {
        static Result refused(String locale, String reason) {
            return new Result(locale, 0, 0, reason);
        }
    }

    /** Read-only on the database: it reads the site's GenAI binding; the deletes are the store's. */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Result purge(TurSNSite site, TurSNSiteLocale siteLocale) {
        String locale = siteLocale.getLanguage() == null ? "" : siteLocale.getLanguage().toLanguageTag();
        if (site.getTurSNSiteGenAi() == null) {
            return Result.refused(locale, "the site has no GenAI binding");
        }
        TurGenAiContext context = contextFactory.build(site.getTurSNSiteGenAi(), siteLocale.getCore());
        TurRagContextBuilder.RagInfrastructure infra = context.isEnabled() ? context.getRagInfrastructure() : null;
        if (infra == null) {
            return Result.refused(locale, "RAG is not enabled for the site");
        }
        TurSearchEnginePlugin plugin = pluginFactory.getPluginForSite(site);
        String probeFailure = probe(plugin, siteLocale);
        if (probeFailure != null) {
            return Result.refused(locale, probeFailure);
        }
        Set<String> stored = storedDocumentIds(infra, site.getName());
        int removed = 0;
        for (String id : orphans(plugin, siteLocale, stored)) {
            if (ragContextBuilder.deleteByMetadata(infra, Map.of(TurSNGenAi.SOURCE_ID, id))) {
                removed++;
            }
        }
        log.info("RAG orphan purge for SN Site '{}' ({}): {} document(s) stored, {} removed",
                site.getName(), locale, stored.size(), removed);
        return new Result(locale, stored.size(), removed, null);
    }

    /** {@code null} when the engine resolves a live id; otherwise why the purge cannot trust it. */
    private static String probe(TurSearchEnginePlugin plugin, TurSNSiteLocale siteLocale) {
        List<Map<String, Object>> page = plugin.retrieveDocumentPage(siteLocale, Map.of(), 0, 1);
        String liveId = page.isEmpty() ? null : idOf(page.getFirst());
        if (liveId == null) {
            return "the search engine returned no document to verify against";
        }
        boolean resolves = plugin.getDocumentsByIds(siteLocale, List.of(liveId)).stream()
                .map(TurSNRagOrphanPurgeService::idOf)
                .anyMatch(liveId::equals);
        return resolves ? null : "the search engine cannot read documents by id";
    }

    /** The distinct source ids of this site's chunks in the collection. */
    private static Set<String> storedDocumentIds(TurRagContextBuilder.RagInfrastructure infra, String siteName) {
        Set<String> ids = new LinkedHashSet<>();
        for (int page = 0; page < MAX_CHUNK_PAGES; page++) {
            TurStoreChunkPage chunks = infra.storeProvider().listChunks(infra.storeInstance(),
                    infra.storeCredential(), infra.collectionName(), page, CHUNK_PAGE_SIZE, Map.of());
            if (chunks == null || chunks.chunks().isEmpty()) {
                break;
            }
            for (TurStoreChunkPage.Chunk chunk : chunks.chunks()) {
                Map<String, Object> metadata = chunk.metadata() == null ? Map.of() : chunk.metadata();
                Object sourceId = metadata.get(TurSNGenAi.SOURCE_ID);
                if (sourceId != null && belongsTo(metadata.get(TurSNGenAi.SITES), siteName)) {
                    ids.add(sourceId.toString());
                }
            }
            if (chunks.chunks().size() < CHUNK_PAGE_SIZE) {
                break;
            }
        }
        return ids;
    }

    /** A chunk with no site recorded is this collection's; one naming sites must name this one. */
    static boolean belongsTo(Object sites, String siteName) {
        if (sites == null) {
            return true;
        }
        if (sites instanceof Collection<?> names) {
            return names.stream().anyMatch(name -> Objects.equals(String.valueOf(name), siteName));
        }
        return Objects.equals(sites.toString(), siteName);
    }

    private static List<String> orphans(TurSearchEnginePlugin plugin, TurSNSiteLocale siteLocale,
            Set<String> stored) {
        List<String> all = new ArrayList<>(stored);
        List<String> orphans = new ArrayList<>();
        for (int from = 0; from < all.size(); from += ID_BATCH) {
            List<String> batch = all.subList(from, Math.min(all.size(), from + ID_BATCH));
            Set<String> held = new HashSet<>();
            for (Map<String, Object> doc : plugin.getDocumentsByIds(siteLocale, batch)) {
                String id = idOf(doc);
                if (id != null) {
                    held.add(id);
                }
            }
            batch.stream().filter(id -> !held.contains(id)).forEach(orphans::add);
        }
        return orphans;
    }

    private static String idOf(Map<String, Object> doc) {
        Object id = doc == null ? null : doc.get(TurSNFieldName.ID);
        return id == null ? null : id.toString();
    }
}
