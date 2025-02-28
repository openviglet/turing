package com.viglet.turing.commons.utils;


import com.viglet.turing.commons.sn.search.TurSNParamType;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class for handling parameters in URIs and query strings.
 * @author Gabriel F. Gomazako
 * @since 3.6.9
 */
@Slf4j
public class TurHttpUtils {

    private TurHttpUtils() {}

    /**
     * Removes a parameter from the query list by its value.
     * @param currentQuery The list of query parameters to be modified.
     * @param value The value of the parameter to be removed. All parameters with this value will be removed.
     * 
     */
    public static void removeParameterFromQueryByValue(List<NameValuePair> currentQuery, String value) {
        // value, se for uma faceta, está no formato "<facet>:<facet element>"
        Predicate<NameValuePair> exists = param -> param.getValue().equals(value);
        currentQuery.removeIf(exists);
    }

    /**
     * Removes a parameter from the query list by its key.
     * @param currentQuery The list of query parameters to be modified.
     * @param key The key of the parameter to be removed. All parameters with this key will be removed.
     */
    public static void removeParameterFromQueryByKey(List<NameValuePair> currentQuery, String key) {
        Predicate<NameValuePair> exists = param -> param.getName().equals(key);
        currentQuery.removeIf(exists);
    }

    /**
     * Remove all parameters from the URI query list that have the given key.
     * @param uri The URI to be modified.
     * @param value The value of the parameter to be removed. All parameters with this value will be removed.
     * @return a new URI with the specified parameter removed, or the original URI if an exception occurs
     * 
     */
    public static URI removeParameterFromQueryByValue(URI uri, String value) {
        var newUri = new URIBuilder(uri);
        var query = newUri.getQueryParams();

        removeParameterFromQueryByValue(query, value);

        // setParameters subscreve toda a query anterior
        newUri.setParameters(query);

        try {
            return newUri.build();
        } catch (URISyntaxException e) {
            log.error("Exception while trying to remove parameter from URI", e);
            return uri;
        }
    }

    /**
     * Remove all parameters from the URI query list that have the given key.
     * @param uri The original URI.
     * @param key The key of the parameter to be removed. All parameters with this key will be removed.
     * @return a new URI with the specified parameter removed, or the original URI if an exception occurs
     */
    public static URI removeParameterFromQueryByKey(URI uri, String key) {
        var newUri = new URIBuilder(uri);
        newUri.removeParameter(key);
        try {
            return newUri.build();
        } catch (URISyntaxException e) {
            log.error("Exception while trying to remove parameter from URI", e);
            return uri;
        }
    }



    /** 
     * Batch removes all the Parameters from Query that have the given keys.
     * If does not exist, it will be ignored.
     * @param uri The original URI.
     * @param keys The keys of the parameters to be removed.
     * @return a new URI with the specified parameters removed, or the original URI if an exception occurs
     */
    public static URI removeParametersFromQueryByKey(URI uri, List<String> keys) {
        var newUri = new URIBuilder(uri);

        for (String key : keys) {
            newUri.removeParameter(key);
        }
        try {
            return newUri.build();
        } catch (URISyntaxException e) {
            log.error("Exception while trying to remove parameter from URI", e);
            return uri;
        }
    }

    /**
     * Adds a parameter to the original query list.
     *
     * @param originalQuery the list of original query parameters
     * @param param the parameter to be added to the query
     */
    public static void addParamOnQuery(List<NameValuePair> originalQuery, NameValuePair param) {
        originalQuery.add(param);
    }

    public static URI addParamOnQuery(URI uri, NameValuePair param) {
        var newUri = new URIBuilder(uri);
        newUri.addParameter(param.getName(), param.getValue());

        try {
            return newUri.build();
        } catch (URISyntaxException e) {
            log.error("Exception while trying to add parameter to URI", e);
            return uri;
        }
    }

    /** 
     * Adds a Facet Filter to the given list of query parameters.
     * @param currentQuery The list of query parameters to be modified.
     * @param fqValue The value of the facet to be added. It is expected to be in the format "<facet>:<facet element>"
     */
    public static void addFacetFilterOnQuery(List<NameValuePair> currentQuery, String fqValue) {
        //Espera receber uma faceta no formato "<facet>:<facet element>"
        var newParam = new BasicNameValuePair(TurSNParamType.FILTER_QUERIES_DEFAULT, fqValue);
        addParamOnQuery(currentQuery, newParam);
    }

    /*
     * Adds a Facet Filter to the given URI.
     * @param uri The original URI.
     * @param fqValue The value of the facet to be added. It is expected to be in the format "<facet>:<facet element>"
     */
    public static URI addFacetFilterOnQuery(URI uri, String fqValue) {
        // Espera receber uma faceta no formato "<facet>:<facet element>"
        var newParam = new BasicNameValuePair(TurSNParamType.FILTER_QUERIES_DEFAULT, fqValue);
        return addParamOnQuery(uri, newParam);
    }

    /**
     * Get the Query Parameters from the given URI as a list of NameValuePair objects by using the URIBuilder.
     * @param uri The URI from which to get the query parameters.
     * @return a list of NameValuePair objects representing the query parameters
     */
    public static List<NameValuePair> getQueryParams(URI uri){
        return new URIBuilder(uri).getQueryParams();
    }

    /**
     * Sets a parameter to the given URI.  Adds if it doesn't exist, replaces if it does.
     * </br>Equivalent to the deprecated {@link TurSNUtils.java} addOrReplaceParameter()
     * @param uri   the original URI to which the parameter will be added
     * @param key the name of the parameter to be set
     * @param value the value of the parameter to be set
     * @return a new URI with the specified parameter set, or the original URI if an exception occurs
     */
    public static URI setParam(URI uri, String key, String value){
        var newUriBuilder = new URIBuilder(uri);
        newUriBuilder.setParameter(key, value);

        try {
            return newUriBuilder.build();
        } catch (URISyntaxException e) {
            log.error("Exception while trying to set parameter to URI, returning the original URI", e);
            return uri;
        }
    }


    /**
     * Sets a parameter in the given list of query parameters. If the parameter.
     * Add if it doesn't exist, replaces if it does.
     * @param currentQuery the list of current query parameters
     * @param key the name of the parameter to set
     * @param value the value of the parameter to set
     */
    public static void setParamOnQuery(List<NameValuePair> currentQuery, String key, String value){
        currentQuery.removeIf(p -> p.getName().equals(key));
        currentQuery.add(new BasicNameValuePair(key, value));
    }


    // facet = "facet"
    // query elements = "facet:facet item"
    public static void removeFilterQueryByFacet(List<NameValuePair> currentQuery, String facet) {
        Predicate<NameValuePair> exists = param -> param.getValue().startsWith(facet + ":");
        currentQuery.removeIf(exists);
    }

    public static URI removeFilterQueryByFacet(URI uri, String facet) {
        var newUri = new URIBuilder(uri);
        var query = newUri.getQueryParams();

        removeFilterQueryByFacet(query, facet);

        // setParameters subscreve toda a query anterior
        newUri.setParameters(query);

        try {
            return newUri.build();
        } catch (URISyntaxException e) {
            log.error("Exception while trying to remove parameter from URI", e);
            return uri;
        }
    }

    public static URI removeFilterQueryByFacet(URI uri, List<String> facets){
        var newUri = new URIBuilder(uri);
        var query = newUri.getQueryParams();

        for (String facet : facets) {
            removeFilterQueryByFacet(query, facet);
        }

        // setParameters subscreve toda a query anterior
        newUri.setParameters(query);

        try {
            return newUri.build();
        } catch (URISyntaxException e) {
            log.error("Exception while trying to remove parameter from URI", e);
            return uri;
        }
    }

}
