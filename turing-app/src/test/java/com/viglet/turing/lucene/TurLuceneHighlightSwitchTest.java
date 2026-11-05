package com.viglet.turing.lucene;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;

/**
 * T1226 — the Lucene engine honours a caller's {@code hlEnabled=false}, as Solr
 * does, so autocomplete never returns marked-up text whatever the site's own
 * highlighting setting says.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
class TurLuceneHighlightSwitchTest {

    @Test
    void aSearchWithHighlightingOffAsksForNoHighlightFields() throws Exception {
        TurLuceneQueryBuilder queryBuilder = mock(TurLuceneQueryBuilder.class);
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSite site = new TurSNSite();
        site.setName("site");
        site.setHl(1);
        when(siteRepository.findByNameIgnoreCase("site")).thenReturn(Optional.of(site));
        when(queryBuilder.buildQuery(any(), any(), any())).thenReturn(MatchAllDocsQuery.INSTANCE);
        lenient().when(queryBuilder.buildWildcardQuery(any(), any())).thenReturn(MatchAllDocsQuery.INSTANCE);
        when(queryBuilder.getFacetFields(any())).thenReturn(List.of());
        lenient().when(queryBuilder.getHLFields(any())).thenReturn(List.of());
        TurLucene lucene = new TurLucene(siteRepository, null, queryBuilder, null, null);

        try (Directory directory = new ByteBuffersDirectory();
                IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig())) {
            writer.commit();
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                TurLuceneInstance instance = mock(TurLuceneInstance.class);
                when(instance.getSearcher()).thenReturn(new IndexSearcher(reader));

                lucene.retrieveLuceneFromSN(instance, context(false));
                verify(queryBuilder, never()).getHLFields(any());

                lucene.retrieveLuceneFromSN(instance, context(true));
                verify(queryBuilder, times(1)).getHLFields(any());
            }
        }
    }

    private static TurSNSiteSearchContext context(boolean hlEnabled) {
        TurSNConfig config = new TurSNConfig();
        config.setHlEnabled(hlEnabled);
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ("works");
        params.setP(1);
        params.setRows(10);
        return new TurSNSiteSearchContext("site", config, new TurSEParameters(params), Locale.US,
                URI.create("http://localhost/api/sn/site/ac"));
    }
}
