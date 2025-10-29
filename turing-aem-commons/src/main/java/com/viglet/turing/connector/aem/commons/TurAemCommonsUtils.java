/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.aem.commons;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR_CONTENT;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR_PRIMARY_TYPE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JSON;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.ONCE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.SLING;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.TEXT;
import static org.apache.jackrabbit.JcrConstants.JCR_TITLE;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.google.common.net.UrlEscapers;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.client.sn.job.TurSNJobAttributeSpec;
import com.viglet.turing.commons.cache.TurCustomClassCache;
import com.viglet.turing.commons.exception.TurRuntimeException;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.connector.aem.commons.bean.TurAemContext;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.config.IAemConfiguration;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.context.TurAemLocalePathContext;
import com.viglet.turing.connector.aem.commons.ext.TurAemExtContentInterface;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurAemCommonsUtils {

    private static final Cache<String, Optional<String>> responseBodyCache =
            Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(5)).build();

    private TurAemCommonsUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<String> getDependencies(JSONObject infinityJson) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(infinityJson.toString());
            return getContentValues(root);

        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptySet();
    }

    private static Set<String> getContentValues(JsonNode node) {
        Set<String> results = new HashSet<>();
        extractContentValues(node, results);
        return results;
    }

    private static void extractContentValues(JsonNode node, Set<String> results) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                extractContentValues(entry.getValue(), results);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                extractContentValues(item, results);
            }
        } else if (node.isTextual()) {
            String value = node.asText();
            if (value.startsWith("/content")) {
                results.add(value);
            }
        }
    }

    public static boolean isTypeEqualContentType(JSONObject jsonObject,
            TurAemConfiguration turAemSourceContext) {
        return jsonObject.has(JCR_PRIMARY_TYPE) && jsonObject.getString(JCR_PRIMARY_TYPE)
                .equals(turAemSourceContext.getContentType());
    }

    public static Optional<String> getSiteName(TurAemConfiguration turAemSourceContext,
            JSONObject jsonObject) {
        return getSiteName(jsonObject).map(Optional::of).orElseGet(() -> {
            log.error("No site name the {} root path ({})", turAemSourceContext.getRootPath(),
                    turAemSourceContext.getId());
            return Optional.empty();
        });
    }

    public static boolean usingContentTypeParameter(TurAemConfiguration turAemSourceContext) {
        return StringUtils.isNotBlank(turAemSourceContext.getContentType());
    }

    public static boolean isNotOnceConfig(String path, IAemConfiguration config) {
        if (StringUtils.isNotBlank(config.getOncePatternPath())) {
            Pattern p = Pattern.compile(config.getOncePatternPath());
            Matcher m = p.matcher(path);
            return !m.lookingAt();
        }
        return true;
    }

    public static String configOnce(TurAemConfiguration turAemSourceContext) {
        return "%s/%s".formatted(turAemSourceContext.getId(), ONCE);
    }

    public static TurAemTargetAttrValueMap runCustomClassFromContentType(TurAemModel turAemModel,
            TurAemObject aemObject, TurAemConfiguration turAemSourceContext) {
        return StringUtils.isNotEmpty(turAemModel.getClassName())
                ? TurCustomClassCache.getCustomClassMap(turAemModel.getClassName())
                        .map(customClassMap -> ((TurAemExtContentInterface) customClassMap)
                                .consume(aemObject, turAemSourceContext))
                        .orElseGet(TurAemTargetAttrValueMap::new)
                : new TurAemTargetAttrValueMap();
    }

    public static void addFirstItemToAttribute(String attributeName, String attributeValue,
            Map<String, Object> attributes) {
        attributes.put(attributeName, attributeValue);
    }

    @NotNull
    public static Date getDeltaDate(TurAemObject aemObject) {
        if (aemObject.getLastModified() != null)
            return aemObject.getLastModified().getTime();
        if (aemObject.getCreatedDate() != null)
            return aemObject.getCreatedDate().getTime();
        return new Date();
    }

    public static List<TurSNAttributeSpec> getDefinitionFromModel(
            List<TurSNAttributeSpec> turSNAttributeSpecList, Map<String, Object> targetAttrMap) {
        List<TurSNAttributeSpec> turSNAttributeSpecFromModelList = new ArrayList<>();
        targetAttrMap.forEach((key, value) -> turSNAttributeSpecList.stream()
                .filter(turSNAttributeSpec -> turSNAttributeSpec.getName() != null
                        && turSNAttributeSpec.getName().equals(key))
                .findFirst().ifPresent(turSNAttributeSpecFromModelList::add));
        return turSNAttributeSpecFromModelList;
    }

    public static Optional<String> getSiteName(JSONObject jsonSite) {
        if (jsonSite.has(JCR_CONTENT) && jsonSite.getJSONObject(JCR_CONTENT).has(JCR_TITLE)) {
            return jsonSite.getJSONObject(JCR_CONTENT).getString(JCR_TITLE).describeConstable();
        }
        return Optional.empty();
    }

    public static boolean checkIfFileHasNotImageExtension(String s) {
        String[] imageExtensions = {".jpg", ".png", ".jpeg", ".svg", ".webp"};
        return Arrays.stream(imageExtensions).noneMatch(suffix -> s.toLowerCase().endsWith(suffix));
    }

    public static void addItemInExistingAttribute(String attributeValue,
            Map<String, Object> attributes, String attributeName) {
        if (attributes.get(attributeName) instanceof ArrayList) {
            addItemToArray(attributes, attributeName, attributeValue);
        } else {
            convertAttributeSingleValueToArray(attributes, attributeName, attributeValue);
        }
    }

    private static void convertAttributeSingleValueToArray(Map<String, Object> attributes,
            String attributeName, String attributeValue) {
        attributes.put(attributeName,
                Lists.newArrayList(attributes.get(attributeName), attributeValue));
    }

    private static void addItemToArray(Map<String, Object> attributes, String attributeName,
            String attributeValue) {
        List<String> attributeValues = new ArrayList<>(((List<?>) attributes.get(attributeName))
                .stream().map(String.class::cast).toList());
        attributeValues.add(attributeValue);
        attributes.put(attributeName, attributeValues);

    }

    @NotNull
    public static List<TurSNJobAttributeSpec> castSpecToJobSpec(
            List<TurSNAttributeSpec> turSNAttributeSpecList) {
        return turSNAttributeSpecList.stream().filter(Objects::nonNull)
                .map(TurSNJobAttributeSpec.class::cast).toList();
    }

    public static Locale getLocaleByPath(TurAemConfiguration turAemSourceContext, String path) {
        for (TurAemLocalePathContext turAemSourceLocalePath : turAemSourceContext
                .getLocalePaths()) {
            if (hasPath(turAemSourceLocalePath, path)) {
                return turAemSourceLocalePath.getLocale();
            }
        }
        return turAemSourceContext.getDefaultLocale();
    }

    private static boolean hasPath(TurAemLocalePathContext turAemSourceLocalePath, String path) {
        return path.startsWith(turAemSourceLocalePath.getPath());
    }

    public static Locale getLocaleFromAemObject(TurAemConfiguration turAemSourceContext,
            TurAemObject aemObject) {
        return getLocaleByPath(turAemSourceContext, aemObject.getPath());
    }

    public static Optional<JSONObject> getInfinityJson(String url,
            TurAemConfiguration turAemSourceContext, boolean useCache) {
        String infinityJsonUrl = String.format(url.endsWith(JSON) ? "%s%s" : "%s%s.infinity.json",
                turAemSourceContext.getUrl(), url);
        return getResponseBody(infinityJsonUrl, turAemSourceContext, useCache)
                .<Optional<JSONObject>>map(responseBody -> {
                    if (isResponseBodyJSONArray(responseBody) && !url.endsWith(JSON)) {
                        return getInfinityJson(
                                new JSONArray(responseBody).toList().getFirst().toString(),
                                turAemSourceContext, useCache);
                    } else if (isResponseBodyJSONObject(responseBody)) {
                        return Optional.of(new JSONObject(responseBody));
                    }
                    return getInfinityJsonNotFound(infinityJsonUrl);
                }).orElseGet(() -> getInfinityJsonNotFound(infinityJsonUrl));

    }

    private static Optional<JSONObject> getInfinityJsonNotFound(String infinityJsonUrl) {
        log.warn("Request Not Found {}", infinityJsonUrl);
        return Optional.empty();
    }

    public static boolean hasProperty(JSONObject jsonObject, String property) {
        return jsonObject.has(property) && jsonObject.get(property) != null;
    }

    public static String getPropertyValue(Object property) {
        try {
            if (property instanceof JSONArray propertyArray) {
                return !propertyArray.isEmpty() ? propertyArray.get(0).toString() : "";
            } else if (property != null) {
                return property.toString();
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static boolean isResponseBodyJSONArray(String responseBody) {
        return responseBody.startsWith("[");
    }

    public static boolean isResponseBodyJSONObject(String responseBody) {
        return responseBody.startsWith("{");
    }

    public static <T> Optional<T> getResponseBody(String url,
            TurAemConfiguration turAemSourceContext, Class<T> clazz, boolean useCache) {
        return getResponseBody(url, turAemSourceContext, useCache).map(json -> {
            if (!TurCommonsUtils.isValidJson(json)) {
                return null;
            }
            try {
                return new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(json, clazz);
            } catch (JsonProcessingException e) {
                log.error("URL {} - {}", url, e.getMessage(), e);
            }
            return null;
        });
    }

    public static @NotNull Optional<String> getResponseBody(String url,
            TurAemConfiguration turAemSourceContext, boolean useCache) {
        if (useCache) {
            return fetchResponseBodyCached(url, turAemSourceContext);
        } else {
            return fetchResponseBodyWithoutCache(url, turAemSourceContext);
        }
    }

    public static @NotNull Optional<String> fetchResponseBodyWithoutCache(String url,
            TurAemConfiguration turAemSourceContext) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultHeaders(List.of(new BasicHeader(HttpHeaders.AUTHORIZATION, basicAuth(
                        turAemSourceContext.getUsername(), turAemSourceContext.getPassword()))))
                .build()) {
            HttpGet request = new HttpGet(
                    URI.create(UrlEscapers.urlFragmentEscaper().escape(url)).normalize());
            String json = httpClient.execute(request, response -> {
                log.debug("Request Status {} - {}", response.getCode(), url);
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            });
            if (TurCommonsUtils.isValidJson(json)) {
                log.debug("Valid JSON - {}", url);
                return Optional.ofNullable(json);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("URL {} - {}", url, e.getMessage(), e);
            throw new TurRuntimeException(e);
        }
    }

    public static @NotNull Optional<String> fetchResponseBodyCached(String url,
            TurAemConfiguration turAemSourceContext) {
        if (responseBodyCache.asMap().containsKey(url))
            log.debug("Using Cache to request {}", url);
        else
            log.debug("Creating Cache to request {}", url);
        String cacheKey = url;
        return responseBodyCache.get(cacheKey,
                k -> fetchResponseBodyWithoutCache(url, turAemSourceContext));
    }

    private static String basicAuth(String username, String password) {
        return "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public static String getJsonNodeToComponent(JSONObject jsonObject) {
        StringBuilder components = new StringBuilder();
        if (jsonObject.has(TEXT) && jsonObject.get(TEXT) instanceof String text) {
            components.append(text);
        }
        jsonObject.toMap().forEach((key, value) -> {
            if (!key.startsWith(JCR) && !key.startsWith(SLING)
                    && (jsonObject.get(key) instanceof JSONObject jsonObjectNode)) {
                components.append(getJsonNodeToComponent(jsonObjectNode));
            }
        });
        return components.toString();
    }

    public static Locale getLocaleFromContext(TurAemConfiguration turAemSourceContext,
            TurAemContext context) {
        return getLocaleFromAemObject(turAemSourceContext, context.getCmsObjectInstance());
    }
}
