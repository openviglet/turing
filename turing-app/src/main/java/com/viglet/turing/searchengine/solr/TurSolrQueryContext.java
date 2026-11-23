package com.viglet.turing.searchengine.solr;

import java.util.List;
import java.util.Locale;

import org.apache.solr.client.solrj.request.SolrQuery;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.result.spellcheck.TurSESpellCheckResult;
import com.viglet.turing.searchengine.spi.TurSESearchField;

import lombok.Builder;
import lombok.Data;

/**
 * Context object for Solr query execution from Semantic Navigation.
 * Encapsulates all parameters needed for executing and processing Solr queries
 * to reduce method parameter count.
 */
@Data
@Builder
public class TurSolrQueryContext {

    /** Solr query object */
    private SolrQuery query;

    /** Search parameters */
    private TurSEParameters turSEParameters;

    /** The names of the MLT (More Like This) fields */
    private List<String> mltFields;

    /** The fields faceted on (T1310: SPI fields, not sn entities) */
    private List<TurSESearchField> facetFields;

    /** The names of the highlighted fields */
    private List<String> hlFields;

    /** Spell check result */
    private TurSESpellCheckResult spellCheckResult;

    /** Flag to indicate if query is for rendering facets */
    private boolean queryToRenderFacet;

    /** Locale of the search request, used to resolve localized facet labels. */
    private Locale locale;
}
