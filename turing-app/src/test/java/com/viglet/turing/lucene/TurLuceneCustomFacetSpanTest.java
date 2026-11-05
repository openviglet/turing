package com.viglet.turing.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacet;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacetItem;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacetOperatorEnum;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteCustomFacetRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.se.facet.TurSEFacetResultAttr;

/**
 * T1224 — an agenda facet over an event's start and end dates on the Lucene
 * engine: a span running now is under "Next 7 days", not "Past"; one that ended
 * yesterday is under "Past"; an event with no end date is the point at its start.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
class TurLuceneCustomFacetSpanTest {

    private static final Duration DAY = Duration.ofDays(1);

    private final TurSNSiteFieldExtRepository fieldRepository = mock(TurSNSiteFieldExtRepository.class);
    private final TurSNSiteCustomFacetRepository customFacetRepository = mock(TurSNSiteCustomFacetRepository.class);
    private final TurLuceneCustomFacets customFacets = new TurLuceneCustomFacets(fieldRepository,
            customFacetRepository);
    private final TurSNSite site = new TurSNSite();
    private final Instant now = Instant.now();

    private Directory directory;
    private IndexWriter writer;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    @BeforeEach
    void setUp() throws Exception {
        TurSNSiteFieldExt start = field("f-start", "start");
        when(fieldRepository.findByTurSNSiteAndEnabled(site, 1)).thenReturn(List.of(start, field("f-end", "end")));

        TurSNSiteCustomFacet agenda = new TurSNSiteCustomFacet();
        agenda.setId("cf-agenda");
        agenda.setName("agenda");
        agenda.setEndField("end");
        agenda.setTurSNSiteFieldExt(start);
        agenda.getItems().add(item("Next 7 days", 1, TurSNSiteCustomFacetOperatorEnum.BETWEEN_INCLUSIVE,
                "NOW/DAY", "NOW/DAY+7DAYS"));
        agenda.getItems().add(item("Past", 2, TurSNSiteCustomFacetOperatorEnum.LESS_THAN, null, "NOW/DAY"));
        when(customFacetRepository.findByFieldExtsWithDetails(anyList())).thenReturn(List.of(agenda));

        directory = new ByteBuffersDirectory();
        writer = new IndexWriter(directory, new IndexWriterConfig(new TurLucenePerFieldAnalyzer()));
        add("running", now.minus(DAY.multipliedBy(5)), now.plus(DAY.multipliedBy(5)));
        add("ended", now.minus(DAY.multipliedBy(10)), now.minus(DAY.multipliedBy(2)));
        add("pointPast", now.minus(DAY.multipliedBy(3)), null);
        add("pointSoon", now.plus(DAY.multipliedBy(2)), null);
        add("later", now.plus(DAY.multipliedBy(10)), now.plus(DAY.multipliedBy(20)));
        writer.commit();
        reader = DirectoryReader.open(writer);
        searcher = new IndexSearcher(reader);
    }

    @AfterEach
    void tearDown() throws Exception {
        reader.close();
        writer.close();
        directory.close();
    }

    private static TurSNSiteFieldExt field(String id, String name) {
        TurSNSiteFieldExt field = new TurSNSiteFieldExt();
        field.setId(id);
        field.setName(name);
        field.setType(TurSEFieldType.DATE);
        return field;
    }

    private static TurSNSiteCustomFacetItem item(String label, int position,
            TurSNSiteCustomFacetOperatorEnum operator, String start, String end) {
        TurSNSiteCustomFacetItem item = new TurSNSiteCustomFacetItem();
        item.setLabel(label);
        item.setPosition(position);
        item.setOperator(operator);
        item.setRangeStartExpression(start);
        item.setRangeEndExpression(end);
        item.setLabels(Map.of());
        return item;
    }

    private void add(String id, Instant start, Instant end) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new SortedSetDocValuesField("id", new BytesRef(id)));
        doc.add(new LongPoint("start", start.toEpochMilli()));
        if (end != null) {
            doc.add(new LongPoint("end", end.toEpochMilli()));
        }
        writer.addDocument(doc);
    }

    @Test
    void countsASpanUnderEveryItemItOverlapsAndPastAsEnded() {
        Map<String, TurSEFacetResultAttr> items = customFacets
                .count(searcher, MatchAllDocsQuery.INSTANCE, site, null, null)
                .getFirst().getTurSEFacetResultAttr();

        assertEquals(2, items.get("Next 7 days").getCount());
        assertEquals(2, items.get("Past").getCount());
    }

    @Test
    void clickingNextSevenDaysReturnsTheRunningSpanAndTheUpcomingPoint() throws Exception {
        assertEquals(Set.of("pointSoon", "running"), idsFor("agenda:Next 7 days"));
    }

    @Test
    void clickingPastReturnsWhatEndedAndThePastPointNotTheRunningSpan() throws Exception {
        assertEquals(Set.of("ended", "pointPast"), idsFor("agenda:Past"));
    }

    private Set<String> idsFor(String fq) throws Exception {
        TurLuceneQueryBuilder queryBuilder = new TurLuceneQueryBuilder(fieldRepository, null, null, null, null,
                customFacets);
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ("*");
        params.setRows(10);
        params.setP(1);
        params.setFq(List.of(fq));
        Query query = queryBuilder.buildQuery(site, new TurSEParameters(params));
        Set<String> ids = new TreeSet<>();
        for (ScoreDoc hit : searcher.search(query, 10).scoreDocs) {
            ids.add(searcher.storedFields().document(hit.doc).get("id"));
        }
        return ids;
    }
}
