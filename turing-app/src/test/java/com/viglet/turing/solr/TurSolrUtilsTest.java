package com.viglet.turing.solr;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurSolrUtilsTest {

    @Test
    void testGetSolrFieldTypeText() {
        assertEquals("text_general", TurSolrUtils.getSolrFieldType(TurSEFieldType.TEXT));
    }

    @Test
    void testGetSolrFieldTypeString() {
        assertEquals("string", TurSolrUtils.getSolrFieldType(TurSEFieldType.STRING));
    }

    @Test
    void testGetSolrFieldTypeInt() {
        assertEquals("pint", TurSolrUtils.getSolrFieldType(TurSEFieldType.INT));
    }

    @Test
    void testGetSolrFieldTypeBool() {
        assertEquals("boolean", TurSolrUtils.getSolrFieldType(TurSEFieldType.BOOL));
    }

    @Test
    void testGetSolrFieldTypeDate() {
        assertEquals("pdate", TurSolrUtils.getSolrFieldType(TurSEFieldType.DATE));
    }

    @Test
    void testGetSolrFieldTypeLong() {
        assertEquals("plong", TurSolrUtils.getSolrFieldType(TurSEFieldType.LONG));
    }

    @Test
    void testGetSolrFieldTypeArray() {
        assertEquals("strings", TurSolrUtils.getSolrFieldType(TurSEFieldType.ARRAY));
    }

    @Test
    void testGetValueFromQueryWithColon() {
        String query = "title:test";
        assertEquals("test", TurSolrUtils.getValueFromQuery(query));
    }

    @Test
    void testGetValueFromQueryWithoutColon() {
        String query = "test";
        assertEquals("test", TurSolrUtils.getValueFromQuery(query));
    }

    @Test
    void testFirstRowPositionFromCurrentPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setP(1);
        searchParams.setRows(10);
        TurSEParameters params = new TurSEParameters(searchParams);
        assertEquals(0, TurSolrUtils.firstRowPositionFromCurrentPage(params));
    }

    @Test
    void testFirstRowPositionFromCurrentPageSecondPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setP(2);
        searchParams.setRows(10);
        TurSEParameters params = new TurSEParameters(searchParams);
        assertEquals(10, TurSolrUtils.firstRowPositionFromCurrentPage(params));
    }

    @Test
    void testLastRowPositionFromCurrentPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setP(1);
        searchParams.setRows(10);
        TurSEParameters params = new TurSEParameters(searchParams);
        assertEquals(10, TurSolrUtils.lastRowPositionFromCurrentPage(params));
    }

    @Test
    void testLastRowPositionFromCurrentPageSecondPage() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setP(2);
        searchParams.setRows(10);
        TurSEParameters params = new TurSEParameters(searchParams);
        assertEquals(20, TurSolrUtils.lastRowPositionFromCurrentPage(params));
    }

    @Test
    void testCreateTurSEResultFromDocument() {
        SolrDocument document = new SolrDocument();
        document.addField("id", "123");
        document.addField("title", "Test Title");
        document.addField("content", "Test Content");

        var result = TurSolrUtils.createTurSEResultFromDocument(document);
        
        assertNotNull(result);
        assertEquals("123", result.getFields().get("id"));
        assertEquals("Test Title", result.getFields().get("title"));
        assertEquals("Test Content", result.getFields().get("content"));
    }

    @Test
    void testStrSuffixConstant() {
        assertEquals("_str", TurSolrUtils.STR_SUFFIX);
    }

    @Test
    void testSchemaApiUrlConstant() {
        assertEquals("%s/solr/%s/schema", TurSolrUtils.SCHEMA_API_URL);
    }
}
