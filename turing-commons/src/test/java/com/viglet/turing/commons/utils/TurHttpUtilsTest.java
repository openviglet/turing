package com.viglet.turing.commons.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TurHttpUtilsTest {

    private List<NameValuePair> queryParams;
    private URI testUri;

    @BeforeEach
    void setUp() throws URISyntaxException {
        queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("simpleParam", "value1"));
        queryParams.add(new BasicNameValuePair("param with whitespace", "i have whitespace"));
        queryParams.add(new BasicNameValuePair("param with special chars", "pão de açúcar"));

        testUri = new URIBuilder()
                .setScheme("http")
                .setHost("example.com")
                .addParameter("simpleParam", "i have whitespace")
                .addParameter("param with whitespace", "value2")
                .addParameter("param with special chars", "pão de açúcar")
                .build();
    }

    @Test
    void testRemoveParameterFromQueryByValue() {
        TurHttpUtils.removeParameterFromQueryByValue(queryParams, "pão de açúcar");
        assertEquals(2, queryParams.size());
        assertFalse(queryParams.stream().anyMatch(param -> param.getValue().equals("pão de açúcar")));
    }

    @Test
    void testRemoveParameterFromQueryByKey() {
        TurHttpUtils.removeParameterFromQueryByKey(queryParams, "param with whitespace");
        assertEquals(2, queryParams.size());
        assertFalse(queryParams.stream().anyMatch(param -> param.getName().equals("param with whitespace")));
    }

    @Test
    void testRemoveParameterFromQueryByValueURI() throws URISyntaxException {
        URI resultUri = TurHttpUtils.removeParameterFromQueryByValue(testUri, "pão de açúcar");
        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();

        assertEquals(2, resultParams.size());
        assertFalse(resultParams.stream().anyMatch(param -> param.getValue().equals("pão de açúcar")));
    }

    @Test
    void testRemoveParameterFromQueryByKeyURI() throws URISyntaxException {
        URI resultUri = TurHttpUtils.removeParameterFromQueryByKey(testUri, "param with whitespace");
        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();

        assertEquals(2, resultParams.size());
        assertFalse(resultParams.stream().anyMatch(param -> param.getName().equals("param with whitespace")));
    }

    @Test
    void testAddParamOnQuery() {
        NameValuePair newParam = new BasicNameValuePair("newParam", "new váluÊ");
        TurHttpUtils.addParamOnQuery(queryParams, newParam);

        assertEquals(4, queryParams.size());
        assertTrue(queryParams.stream()
                .anyMatch(param -> param.getName().equals("newParam") && param.getValue().equals("new váluÊ")));
    }

    @Test
    void testAddParamOnQueryURI() throws URISyntaxException {
        NameValuePair newParam = new BasicNameValuePair("newParam", "new váluÊ");
        URI resultUri = TurHttpUtils.addParamOnQuery(testUri, newParam);

        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();
        assertEquals(4, resultParams.size());
        assertTrue(resultParams.stream()
                .anyMatch(param -> param.getName().equals("newParam") && param.getValue().equals("new váluÊ")));
    }

    @Test
    void testSetParamOnQuery() {
        TurHttpUtils.setParamOnQuery(queryParams, "simpleParam", "new váluÊ");
        assertEquals(3, queryParams.size());
        assertTrue(queryParams.stream()
                .anyMatch(param -> param.getName().equals("simpleParam") && param.getValue().equals("new váluÊ")));
    }

    @Test
    void testSetParamURI() throws URISyntaxException {
        URI resultUri = TurHttpUtils.setParam(testUri, "simpleParam", "new váluÊ");
        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();

        assertEquals(3, resultParams.size());
        assertTrue(resultParams.stream()
                .anyMatch(param -> param.getName().equals("simpleParam") && param.getValue().equals("new váluÊ")));
    }

    @Test
    void testAddFacetFilterOnQuery() {
        TurHttpUtils.addFacetFilterOnQuery(queryParams, "facet:value");

        assertEquals(4, queryParams.size());
        assertTrue(queryParams.stream()
                .anyMatch(param -> param.getName().equals("fq[]") && param.getValue().equals("facet:value")));
    }

    @Test
    void testAddFacetFilterOnQueryURI() throws URISyntaxException {
        URI resultUri = TurHttpUtils.addFacetFilterOnQuery(testUri, "facet:value");
        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();

        assertEquals(4, resultParams.size());
        assertTrue(resultParams.stream()
                .anyMatch(param -> param.getName().equals("fq[]") && param.getValue().equals("facet:value")));
    }

    @Test
    void testRemoveParametersFromQueryByKey() throws URISyntaxException {
        List<String> keys = List.of("simpleParam", "param with whitespace");
        URI resultUri = TurHttpUtils.removeParametersFromQueryByKey(testUri, keys);

        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();
        assertTrue(resultParams.size() == 1);
    }

    @Test
    void testRemoveNonExistentParameterFromQueryByKeys() throws URISyntaxException {
        List<String> keys = List.of("nonExistentParam", "param with whitespace", "THis is not a param");
        URI resultUri = TurHttpUtils.removeParametersFromQueryByKey(testUri, keys);

        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();
        assertEquals(2, resultParams.size());
    }

    @Test
    void testRemoveNonExistentParameterFromQueryByValue() {
        TurHttpUtils.removeParameterFromQueryByValue(queryParams, "nonExistentValue");
        assertEquals(3, queryParams.size());
    }

    @Test
    void testRemoveNonExistentParameterFromQueryByValueURI() throws URISyntaxException {
        URI resultUri = TurHttpUtils.removeParameterFromQueryByValue(testUri, "nonExistentValue");
        List<NameValuePair> resultParams = new URIBuilder(resultUri).getQueryParams();
        assertEquals(3, resultParams.size());
    }

    @Test
    void testRemoveNonExistentParameterFromQueryByKey() {
        TurHttpUtils.removeParameterFromQueryByKey(queryParams, "nonExistentKey");
        assertEquals(3, queryParams.size());
    }

    @Test
    void testRemoveFilterQueryByFacet() throws URISyntaxException {
        var uri = new URIBuilder()
                .setScheme("http")
                .setHost("example.com")
                .addParameter("fq[]", "facet1:value1")
                .addParameter("fq[]", "facet2:value2")
                .addParameter("fq[]", "facet1:value3")
                .build();

        var modifiedUri = TurHttpUtils.removeFilterQueryByFacet(uri, "facet1");
        var params = TurHttpUtils.getQueryParams(modifiedUri);

        assertEquals(1, params.size());
        assertTrue(params.stream().anyMatch(param -> param.getName().equals("fq[]") && param.getValue().equals("facet2:value2")));
        assertFalse(params.stream().anyMatch(param -> param.getName().equals("fq[]") && param.getValue().startsWith("facet1:")));
    }

    @Test
    void testRemoveNonExistentFilterQueryByFacet() throws URISyntaxException {
        var uri = new URIBuilder()
                .setScheme("http")
                .setHost("example.com")
                .addParameter("fq[]", "facet1:value1")
                .addParameter("fq[]", "facet2:value2")
                .build();

        var modifiedUri = TurHttpUtils.removeFilterQueryByFacet(uri, "facet3");
        var params = TurHttpUtils.getQueryParams(modifiedUri);

        assertEquals(2, params.size());
        assertTrue(params.stream().anyMatch(param -> param.getName().equals("fq[]") && param.getValue().equals("facet1:value1")));
        assertTrue(params.stream().anyMatch(param -> param.getName().equals("fq[]") && param.getValue().equals("facet2:value2")));
    }

    @Test
    void checkIfIsEncodedAddFacetFilter() throws URISyntaxException {
        var uri = new URIBuilder()
        .setScheme("http")
        .setHost("example.com")
        .addParameter("complex param 1", "pão de açúcar")
        .addParameter("cÔmplex- param 2", "mãe de-deus")
        .build();

        var modifiedUri = TurHttpUtils.addFacetFilterOnQuery(uri, "área: inteligência artificial");
        var params = TurHttpUtils.getQueryParams(modifiedUri);

        assertTrue(params.stream().anyMatch(param -> param.getName().equals("fq[]") && param.getValue().equals("área: inteligência artificial")));
        assertTrue(params.stream().anyMatch(param -> param.getName().equals("complex param 1") && param.getValue().equals("pão de açúcar")));
        assertTrue(params.stream().anyMatch(param -> param.getName().equals("cÔmplex- param 2") && param.getValue().equals("mãe de-deus")));
    }

    @Test
    void checkIfIsEncodedRemoveFacetFilter() throws URISyntaxException {
        var uri = new URIBuilder()
        .setScheme("http")
        .setHost("example.com")
        .addParameter("complex param 1", "pão de açúcar")
        .addParameter("cÔmplex- param 2", "mãe de-deus")
        .addParameter("fq[]", "área: inteligência artificial")
        .build();

        var modifiedUri = TurHttpUtils.removeParameterFromQueryByValue(uri, "área: inteligência artificial");
        var params = TurHttpUtils.getQueryParams(modifiedUri);

        assertFalse(params.stream().anyMatch(param -> param.getName().equals("fq[]") && param.getValue().equals("área: inteligência artificial")));
        assertTrue(params.stream().anyMatch(param -> param.getName().equals("complex param 1") && param.getValue().equals("pão de açúcar")));
        assertTrue(params.stream().anyMatch(param -> param.getName().equals("cÔmplex- param 2") && param.getValue().equals("mãe de-deus")));
    }

}