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

package com.viglet.turing.connector.plugin.aem;

import static com.viglet.turing.commons.se.field.TurSEFieldType.STRING;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DEFAULT;
import static org.apache.jackrabbit.JcrConstants.JCR_TITLE;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObjectGeneric;
import com.viglet.turing.connector.aem.commons.bean.TurAemContext;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.mappers.TurAemSourceAttr;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurAemAttrUtils {
    public static final String CQ_TAGS_PATH = "/content/_cq_tags";

    public static boolean hasCustomClass(TurAemTargetAttr targetAttr) {
        return targetAttr.getSourceAttrs() == null
                && StringUtils.isNotBlank(targetAttr.getClassName());
    }

    public static boolean hasTextValue(TurAemTargetAttr turAemTargetAttr) {
        return StringUtils.isNotEmpty(turAemTargetAttr.getTextValue());
    }

    @Nullable
    public static Object getJcrProperty(TurAemContext context, String sourceAttrName) {
        return Optional.ofNullable(sourceAttrName).map(attrName -> {
            TurAemObjectGeneric aemObject = context.getCmsObjectInstance();
            if (isValidNode(attrName, aemObject)) {
                return aemObject.getJcrContentNode().get(attrName);
            } else if (aemObject.getAttributes().containsKey(attrName))
                return aemObject.getAttributes().get(attrName);
            return null;
        }).orElse(null);
    }

    public static boolean isValidNode(String attrName, TurAemObjectGeneric aemObject) {
        return aemObject.getJcrContentNode() != null && aemObject.getJcrContentNode().has(attrName);
    }

    @NotNull
    public static TurSNAttributeSpec getTurSNAttributeSpec(String facet,
            Map<String, String> facetLabel) {
        return TurSNAttributeSpec.builder().name(facet).description(facetLabel.get(DEFAULT))
                .facetName(facetLabel).facet(true).mandatory(false).type(STRING).multiValued(true)
                .build();
    }

    public static Map<String, String> getTagLabels(JSONObject tagJson) {
        Map<String, String> labels = new HashMap<>();
        if (tagJson.has(JCR_TITLE))
            labels.put(DEFAULT, tagJson.getString(JCR_TITLE));
        Iterator<String> keys = tagJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String titleStartWith = JCR_TITLE + ".";
            if (key.startsWith(titleStartWith)) {
                String locale = normalizeLocale(key.replaceAll(titleStartWith, ""));
                labels.put(locale, tagJson.getString(key));
            }
        }
        return labels;
    }

    public static String normalizeLocale(String locale) {
        String[] parts = locale.split("_");
        if (parts.length == 2)
            return "%s_%s".formatted(parts[0].toLowerCase(), parts[1].toUpperCase());
        return locale;
    }

    public static TurAemTargetAttrValueMap addValuesToAttributes(TurAemTargetAttr turAemTargetAttr,
            TurAemSourceAttr turAemSourceAttr, Object jcrProperty) {

        if (turAemSourceAttr.isConvertHtmlToText()) {
            return TurAemTargetAttrValueMap.singleItem(turAemTargetAttr.getName(),
                    TurCommonsUtils.html2Text(TurAemCommonsUtils.getPropertyValue(jcrProperty)),
                    false);
        } else if (jcrProperty != null) {
            TurMultiValue turMultiValue = new TurMultiValue();
            if (isJSONArray(jcrProperty)) {
                ((JSONArray) jcrProperty).forEach(item -> turMultiValue.add(item.toString()));
            } else {
                turMultiValue.add(TurAemCommonsUtils.getPropertyValue(jcrProperty));
            }
            if (!turMultiValue.isEmpty()) {
                return TurAemTargetAttrValueMap.singleItem(turAemTargetAttr.getName(),
                        turMultiValue, false);
            }
        }
        return new TurAemTargetAttrValueMap();
    }

    private static boolean isJSONArray(Object jcrProperty) {
        return jcrProperty instanceof JSONArray jsonArray && !jsonArray.isEmpty();
    }

    public static boolean hasCustomClass(TurAemContext context) {
        return StringUtils.isNotBlank(context.getTurAemSourceAttr().getClassName());
    }

    public static boolean hasJcrPropertyValue(Object jcrProperty) {
        return ObjectUtils.allNotNull(jcrProperty,
                TurAemCommonsUtils.getPropertyValue(jcrProperty));
    }

    public static TurAemTargetAttrValueMap getTurAttrDefUnique(TurAemTargetAttr turAemTargetAttr,
            TurAemTargetAttrValueMap turAemTargetAttrValueMap) {
        return TurAemTargetAttrValueMap
                .singleItem(
                        turAemTargetAttr.getName(), turAemTargetAttrValueMap
                                .get(turAemTargetAttr.getName()).stream().distinct().toList(),
                        false);
    }

    public static TurSNAttributeSpec setTagFacet(TurAemConfiguration turAemSourceContext,
            String facetId) {
        return TurAemCommonsUtils
                .getInfinityJson((CQ_TAGS_PATH + "/%s").formatted(facetId), turAemSourceContext,
                        true)
                .map(infinityJson -> getTurSNAttributeSpec(facetId, getTagLabels(infinityJson)))
                .orElse(new TurSNAttributeSpec());
    }

    public static String addTagToAttrValueList(TurAemContext context,
            TurAemConfiguration turAemSourceContext, String facet, String value) {
        return TurAemCommonsUtils.getInfinityJson((CQ_TAGS_PATH + "/%s/%s").formatted(facet, value),
                turAemSourceContext, true).map(infinityJson -> {
                    Locale locale =
                            TurAemCommonsUtils.getLocaleFromContext(turAemSourceContext, context);
                    String titleLocale = locale.toString().toLowerCase();
                    String titleLanguage = locale.getLanguage().toLowerCase();
                    Map<String, String> tagLabels = getTagLabels(infinityJson);
                    if (tagLabels.containsKey(titleLocale))
                        return tagLabels.get(titleLocale);
                    else if (tagLabels.containsKey(titleLanguage))
                        return tagLabels.get(titleLanguage);
                    else
                        return tagLabels.getOrDefault(DEFAULT, value);
                }).orElse(value);
    }

    public static @NotNull TurAemTargetAttrValueMap getTextValue(TurAemContext context) {
        return TurAemTargetAttrValueMap.singleItem(context.getTurAemTargetAttr(), false);
    }

    public static void processTagsFromSourceAttr(TurAemContext context,
            TurAemConfiguration turAemSourceContext,
            List<TurSNAttributeSpec> turSNAttributeSpecList, String attributeName,
            TurAemTargetAttrValueMap turAemTargetAttrValueMap) {
        Optional.ofNullable((JSONArray) getJcrProperty(context, attributeName)).ifPresent(
                property -> property.forEach(tag -> formatTags(context, turAemSourceContext,
                        turSNAttributeSpecList, tag.toString(), turAemTargetAttrValueMap)));
    }

    public static void processTagsFromTargetAttr(TurAemContext context,
            TurAemConfiguration turAemSourceContext,
            List<TurSNAttributeSpec> turSNAttributeSpecList,
            TurAemTargetAttrValueMap turAemTargetAttrValueMapFromClass, String targetName,
            TurAemTargetAttrValueMap turAemTargetAttrValueMap) {
        turAemTargetAttrValueMapFromClass.get(targetName)
                .forEach(tag -> formatTags(context,
                        turAemSourceContext, turSNAttributeSpecList, tag,
                        turAemTargetAttrValueMap));
    }

    public static void formatTags(TurAemContext context, TurAemConfiguration turAemSourceContext,
            List<TurSNAttributeSpec> turSNAttributeSpecList, String tag,
            TurAemTargetAttrValueMap turAemTargetAttrValueMap) {
        TurCommonsUtils.getKeyValueFromColon(tag)
                .ifPresent(
                        kv -> handleTagFacet(context, turAemSourceContext, turSNAttributeSpecList,
                                turAemTargetAttrValueMap, kv));
    }

    private static void handleTagFacet(TurAemContext context,
            TurAemConfiguration turAemSourceContext,
            List<TurSNAttributeSpec> turSNAttributeSpecList,
            TurAemTargetAttrValueMap turAemTargetAttrValueMap,
            KeyValue<String, String> kv) {
        Optional.ofNullable(kv.getKey()).ifPresent(facet -> {
            processTagFacet(context, turAemSourceContext, turSNAttributeSpecList,
                    turAemTargetAttrValueMap, kv, facet);
        });
    }

    private static void processTagFacet(TurAemContext context,
            TurAemConfiguration turAemSourceContext,
            List<TurSNAttributeSpec> turSNAttributeSpecList,
            TurAemTargetAttrValueMap turAemTargetAttrValueMap,
            KeyValue<String, String> kv,
            String facet) {
        turSNAttributeSpecList.add(setTagFacet(turAemSourceContext, facet));
        Optional.ofNullable(kv.getValue())
                .ifPresent(value -> turAemTargetAttrValueMap
                        .addWithSingleValue(facet, addTagToAttrValueList(context,
                                turAemSourceContext, facet, value),
                                false));
    }
}
