package com.viglet.turing.genai.provider.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * T1158 — the expression a Milvus delete by metadata sends. A key or value the
 * expression cannot carry must yield no expression, never one that matches the
 * whole collection.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurMilvusStoreProviderDeleteTest {

    @Test
    void deletesBySourceIdOverTheMetadataJsonPath() {
        assertEquals("metadata[\"source_id\"] == \"doc-1\"",
                TurMilvusStoreProvider.deleteExpression("source_id", "doc-1"));
    }

    @Test
    void escapesQuotesInTheValue() {
        assertEquals("metadata[\"source_id\"] == \"a\\\"b\"",
                TurMilvusStoreProvider.deleteExpression("source_id", "a\"b"));
    }

    @Test
    void refusesAKeyOrValueItCannotAddress() {
        assertNull(TurMilvusStoreProvider.deleteExpression("source id", "doc-1"));
        assertNull(TurMilvusStoreProvider.deleteExpression("source_id", " "));
        assertNull(TurMilvusStoreProvider.deleteExpression(null, "doc-1"));
    }
}
