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
    public static final String TURING_ENTITY = "turing_entity_";
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
    public static final String RECENT_DATES = "{!func}recip(ms(NOW/DAY,%s),3.16e-11,1,1)";
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
