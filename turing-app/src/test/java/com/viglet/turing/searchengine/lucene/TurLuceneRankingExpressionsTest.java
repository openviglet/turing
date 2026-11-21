package com.viglet.turing.searchengine.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingCondition.Kind;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

/**
 * T1064 — ranking expressions reorder results on the embedded Lucene engine.
 */
@ExtendWith(MockitoExtension.class)
class TurLuceneRankingExpressionsTest {

    private static final long NOW = Instant.parse("2026-10-10T15:00:00Z").toEpochMilli();

    @Mock
    private TurSESiteSearchConfig siteSearchConfig;

    private final TurSNSite site = new TurSNSite();

    private TurLuceneRankingExpressions rankingExpressions() {
        return new TurLuceneRankingExpressions(siteSearchConfig);
    }

    private void fields(Object... nameAndType) {
        java.util.Map<String, TurSEFieldType> types = new java.util.LinkedHashMap<>();
        for (int i = 0; i < nameAndType.length; i += 2) {
            types.put((String) nameAndType[i], (TurSEFieldType) nameAndType[i + 1]);
        }
        when(siteSearchConfig.fieldTypes(any())).thenReturn(types);
    }

    private static TurSERankingExpression expression(String name, float weight, String... attributeAndValue) {
        List<TurSERankingCondition> conditions = new ArrayList<>();
        for (int i = 0; i < attributeAndValue.length; i += 2) {
            conditions.add(new TurSERankingCondition(attributeAndValue[i], Kind.IS, attributeAndValue[i + 1]));
        }
        return new TurSERankingExpression(name, weight, conditions);
    }

    private static TurSERankingExpression expression(String name, float weight, Kind kind, String attribute,
            String value) {
        return new TurSERankingExpression(name, weight, List.of(new TurSERankingCondition(attribute, kind, value)));
    }

    private void expressions(TurSERankingExpression... expressions) {
        when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of(expressions));
    }

    private static Document job(String id, String category, Instant published) {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new StringField("category", category, Field.Store.YES));
        doc.add(new TextField("title", "job " + id, Field.Store.YES));
        long ms = published.toEpochMilli();
        doc.add(new LongPoint("publicationDate", ms));
        doc.add(new NumericDocValuesField("publicationDate", ms));
        return doc;
    }

    /** Ids in score order for match-all plus the boosts, the shape TurLuceneQueryBuilder builds. */
    private static List<String> rank(List<Query> boosts) throws Exception {
        Instant now = Instant.ofEpochMilli(NOW);
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()))) {
                Document oldIt = job("old-it", "it", now.minus(900, ChronoUnit.DAYS));
                oldIt.add(new LongPoint("salaryMin", 5000));
                oldIt.add(new NumericDocValuesField("salaryMin", 5000));
                writer.addDocument(oldIt);
                writer.addDocument(job("new-sales", "sales", now.minus(1, ChronoUnit.DAYS)));
                writer.addDocument(job("mid-it", "it", now.minus(200, ChronoUnit.DAYS)));
            }
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                BooleanQuery.Builder builder = new BooleanQuery.Builder()
                        .add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
                boosts.forEach(boost -> builder.add(boost, BooleanClause.Occur.SHOULD));
                List<String> ids = new ArrayList<>();
                for (ScoreDoc hit : searcher.search(builder.build(), 10).scoreDocs) {
                    ids.add(searcher.storedFields().document(hit.doc).get("id"));
                }
                return ids;
            }
        }
    }

    @Test
    void recencyExpressionRanksNewestFirst() throws Exception {
        fields("publicationDate", TurSEFieldType.DATE);
        TurSERankingExpression recent = expression("recent", 2f, "publicationDate", "asc");
        expressions(recent);

        List<String> ranked = rank(rankingExpressions().boostQueries(site.getId(), NOW));

        assertEquals(List.of("new-sales", "mid-it", "old-it"), ranked);
    }

    @Test
    void valueExpressionOutweighsRecencyWhenHeavier() throws Exception {
        fields("publicationDate", TurSEFieldType.DATE, "category", TurSEFieldType.STRING);
        expressions(expression("recent", 1f, "publicationDate", "asc"),
                expression("it-first", 10f, "category", "it"));

        List<String> ranked = rank(rankingExpressions().boostQueries(site.getId(), NOW));

        // Both IT jobs outrank the newest sales job; recency orders the two IT jobs.
        assertEquals(List.of("mid-it", "old-it", "new-sales"), ranked);
    }

    @Test
    void conditionsWithinOneExpressionMustAllMatch() throws Exception {
        fields("publicationDate", TurSEFieldType.DATE, "category", TurSEFieldType.STRING);
        TurSERankingExpression recentIt = expression("recent-it", 5f, "category", "it", "publicationDate", "asc");
        expressions(recentIt);

        List<String> ranked = rank(rankingExpressions().boostQueries(site.getId(), NOW));

        assertEquals("mid-it", ranked.getFirst());
        assertEquals("new-sales", ranked.getLast());
    }

    @Test
    void existsConditionBoostsDocumentsThatCarryTheField() throws Exception {
        fields("salaryMin", TurSEFieldType.LONG);
        TurSERankingExpression hasSalary = expression("has-salary", 4f, Kind.EXISTS, "salaryMin", null);
        expressions(hasSalary);

        List<String> ranked = rank(rankingExpressions().boostQueries(site.getId(), NOW));

        // Only old-it carries salaryMin, so it outranks two newer jobs.
        assertEquals("old-it", ranked.getFirst());
    }

    @Test
    void isNotConditionBoostsEveryoneButThatValue() throws Exception {
        fields("category", TurSEFieldType.STRING);
        TurSERankingExpression notIt = expression("not-it", 10f, Kind.IS_NOT, "category", "it");
        expressions(notIt);

        List<String> ranked = rank(rankingExpressions().boostQueries(site.getId(), NOW));

        // T1070 — the sales job is boosted; before, Is not boosted the IT jobs instead.
        assertEquals("new-sales", ranked.getFirst());
    }

    @Test
    void isNotOnTheRecencyFormIsSkipped() {
        fields("publicationDate", TurSEFieldType.DATE);
        TurSERankingExpression notRecent = expression("not-recent", 2f, Kind.IS_NOT, "publicationDate", "asc");
        expressions(notRecent);

        assertTrue(rankingExpressions().boostQueries(site.getId(), NOW).isEmpty());
    }

    @Test
    void noExpressionsMeansNoBoosts() {
        expressions();

        assertTrue(rankingExpressions().boostQueries(site.getId(), NOW).isEmpty());
    }

    @Test
    void anExpressionWithoutConditionsOrWithAnUnusableConditionIsSkipped() {
        fields("salary", TurSEFieldType.INT);
        TurSERankingExpression empty = expression("empty", 3f);
        TurSERankingExpression badNumber = expression("bad", 3f, "salary", "lots");
        TurSERankingExpression zeroWeight = expression("zero", 0f, "salary", "10");
        expressions(empty, badNumber, zeroWeight);

        assertTrue(rankingExpressions().boostQueries(site.getId(), NOW).isEmpty());
    }

    /** T1219 — three equal-text agenda events dated −7, +7 and +180 days from NOW. */
    private static List<ScoreDoc> rankAgenda(List<Query> boosts, List<String> idsOut) throws Exception {
        Instant now = Instant.ofEpochMilli(NOW);
        try (Directory dir = new ByteBuffersDirectory()) {
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer()))) {
                writer.addDocument(job("past-7", "event", now.minus(7, ChronoUnit.DAYS)));
                writer.addDocument(job("next-7", "event", now.plus(7, ChronoUnit.DAYS)));
                writer.addDocument(job("next-180", "event", now.plus(180, ChronoUnit.DAYS)));
                writer.addDocument(job("today", "event", now.truncatedTo(ChronoUnit.DAYS)));
            }
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                BooleanQuery.Builder builder = new BooleanQuery.Builder()
                        .add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
                boosts.forEach(boost -> builder.add(boost, BooleanClause.Occur.SHOULD));
                List<ScoreDoc> hits = List.of(searcher.search(builder.build(), 10).scoreDocs);
                for (ScoreDoc hit : hits) {
                    idsOut.add(searcher.storedFields().document(hit.doc).get("id"));
                }
                return hits;
            }
        }
    }

    @Test
    void upcomingRanksNearestFutureFirstAndLeavesThePastUnboosted() throws Exception {
        fields("publicationDate", TurSEFieldType.DATE);
        TurSERankingExpression upcoming = expression("upcoming", 2f, "publicationDate", "upcoming");
        expressions(upcoming);

        List<String> ids = new ArrayList<>();
        List<ScoreDoc> hits = rankAgenda(rankingExpressions().boostQueries(site.getId(), NOW), ids);

        assertEquals(List.of("today", "next-7", "next-180", "past-7"), ids);
        // The past event still matches, with no boost: only the main query's score.
        assertTrue(hits.get(2).score > hits.get(3).score);
    }

    @Test
    void recencyCountsAFutureDateAsTodayInsteadOfDistance() throws Exception {
        fields("publicationDate", TurSEFieldType.DATE);
        TurSERankingExpression recent = expression("recent", 2f, "publicationDate", "asc");
        expressions(recent);

        List<String> ids = new ArrayList<>();
        List<ScoreDoc> hits = rankAgenda(rankingExpressions().boostQueries(site.getId(), NOW), ids);

        // Before T1219 +180 scored as 180 days old, and +7 tied with −7. Now every
        // future date scores as today, the ceiling, and the past decays from it.
        assertEquals("past-7", ids.getLast());
        float ceiling = hits.getFirst().score;
        for (int i = 0; i < 3; i++) {
            assertEquals(ceiling, hits.get(i).score, 1e-6f);
        }
        assertTrue(hits.get(3).score < ceiling);
    }

    @Test
    void isNotOnTheUpcomingFormIsSkipped() {
        fields("publicationDate", TurSEFieldType.DATE);
        TurSERankingExpression notUpcoming = expression("not-upcoming", 2f, Kind.IS_NOT, "publicationDate", "upcoming");
        expressions(notUpcoming);

        assertTrue(rankingExpressions().boostQueries(site.getId(), NOW).isEmpty());
    }

    @Test
    void recencyPivotMatchesSolrRecipCurve() {
        // recip(x, 3.16e-11, 1, 1) halves at x = 1 / 3.16e-11 ms (about a year).
        assertEquals(1 / 3.16e-11, TurLuceneRankingExpressions.RECENCY_PIVOT_MS, 1);
    }
}
