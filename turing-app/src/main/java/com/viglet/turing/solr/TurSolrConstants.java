package com.viglet.turing.solr;

public final class TurSolrConstants {
    private TurSolrConstants() {
        throw new IllegalStateException("Solr Constants class");
    }
    public static final String NEWEST = "newest";
    public static final String OLDEST = "oldest";
    public static final String ASC = "asc";
    public static final String COUNT = "count";
    public static final String SCORE = "score";
    public static final String VERSION = "_version_";
    public static final String BOOST = "boost";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String URL = "url";
    public static final String MORE_LIKE_THIS = "moreLikeThis";
    public static final String BOOST_QUERY = "bq";
    public static final String QUERY = "q";
    public static final String TRUE = "true";
    public static final String EDISMAX = "edismax";
    public static final String DEF_TYPE = "defType";
    public static final String AND = "AND";
    public static final String Q_OP = "q.op";
    /** T1176 — edismax's minimum-should-match parameter. */
    public static final String MM = "mm";
    /** T1176 — the relaxed retry's minimum match: half of the words, rounded down. */
    public static final String RELAXED_MINIMUM_MATCH = "50%";
    /**
     * The recency boost of a DATE condition {@code asc}. T1219: the distance is
     * clamped at zero, so a future date scores as today instead of above 1.
     */
    public static final String RECENT_DATES = "{!func}recip(max(ms(NOW/DAY,%s),0),3.16e-11,1,1)";
    /** T1219 — the DATE condition value that boosts today or later, nearest most. */
    public static final String UPCOMING = "upcoming";
    /** T1219 — the upcoming-first boost: the recency curve forward in time, zero for the past. */
    public static final String UPCOMING_DATES =
            "{!func}if(gte(ms(%1$s,NOW/DAY),0),recip(ms(%1$s,NOW/DAY),3.16e-11,1,1),0)";
    public static final String TUR_SUGGEST = "/tur_suggest";
    public static final String TUR_SPELL = "/tur_spell";
    public static final String FILTER_QUERY_OR = "{!tag=_all_}";
    public static final String FACET_OR = "{!ex=_all_}";
    public static final String PLUS_ONE = "+1";
    public static final String EMPTY = "";
    public static final String SOLR_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String INDEX = "index";
    public static final String ROWS = "rows";
    public static final String OR = "OR";
    public static final String NO_FACET_NAME = "__no_facet_name__";
    public static final String OR_OR = "OR-OR";
    public static final String OR_AND = "OR-AND";
    public static final String ALL = "_all_";
    public static final String HYPHEN = "-";
}
