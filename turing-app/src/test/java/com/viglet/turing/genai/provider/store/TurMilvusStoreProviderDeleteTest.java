package com.viglet.turing.genai.provider.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * T1158 — what a delete by metadata sends to Milvus, Chroma and PGVector. A key or
 * value a store cannot carry must yield nothing, never a match that is wider than
 * what was asked (up to the whole collection).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurMilvusStoreProviderDeleteTest {

    private static Map<String, String> typeAndSite() {
        Map<String, String> match = new LinkedHashMap<>();
        match.put("type", "news");
        match.put("site_key", "a.example.org");
        return match;
    }

    @Test
    void milvusDeletesBySourceIdOverTheMetadataJsonPath() {
        assertEquals("metadata[\"source_id\"] == \"doc-1\"",
                TurMilvusStoreProvider.deleteExpression("source_id", "doc-1"));
    }

    @Test
    void milvusEscapesQuotesInTheValue() {
        assertEquals("metadata[\"source_id\"] == \"a\\\"b\"",
                TurMilvusStoreProvider.deleteExpression("source_id", "a\"b"));
    }

    @Test
    void milvusJoinsEveryEntryWithAnd() {
        assertEquals("metadata[\"type\"] == \"news\" && metadata[\"site_key\"] == \"a.example.org\"",
                TurMilvusStoreProvider.deleteExpression(typeAndSite()));
    }

    @Test
    void milvusRefusesAnEntryItCannotAddressRatherThanDroppingIt() {
        assertNull(TurMilvusStoreProvider.deleteExpression("source id", "doc-1"));
        assertNull(TurMilvusStoreProvider.deleteExpression("source_id", " "));
        assertNull(TurMilvusStoreProvider.deleteExpression(null, "doc-1"));
        Map<String, String> oneBad = typeAndSite();
        oneBad.put("bad key", "x");
        assertNull(TurMilvusStoreProvider.deleteExpression(oneBad));
    }

    @Test
    void chromaUsesTheBareMapForOneEntryAndAndForMore() {
        assertEquals(Map.of("type", "news"), TurChromaStoreProvider.deleteWhere(Map.of("type", "news")));
        assertEquals(Map.of("$and", List.of(Map.of("type", "news"), Map.of("site_key", "a.example.org"))),
                TurChromaStoreProvider.deleteWhere(typeAndSite()));
        assertNull(TurChromaStoreProvider.deleteWhere(Map.of("type", " ")));
    }

    @Test
    void pgVectorBindsOneClausePerEntry() {
        assertEquals("DELETE FROM \"public\".\"c\" WHERE metadata ->> ? = ? AND metadata ->> ? = ?",
                TurPgVectorStoreProvider.deleteSql("\"public\".\"c\"", 2));
    }
}
