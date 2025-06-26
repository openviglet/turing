package com.viglet.turing.connector.plugin.aem;

import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentDefinitionProcess;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.aem.commons.mappers.TurAemSourceAttr;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemPluginModel;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemTargetAttribute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.*;

@Slf4j
public class TurAemPluginUtils {
    public static boolean isNotValidType(TurAemModel turAemModel, TurAemObject aemObject, String type) {
        return !isPage(type) &&
                !isContentFragment(turAemModel, type, aemObject) &&
                !isStaticFile(turAemModel, type);
    }


    public static boolean isPage(String type) {
        return type.equals(CQ_PAGE);
    }

    public static boolean isStaticFile(TurAemModel turAemModel, String type) {
        return isAsset(turAemModel, type) && turAemModel.getSubType().equals(STATIC_FILE);
    }

    public static boolean isContentFragment(TurAemModel turAemModel, String type, TurAemObject aemObject) {
        return isAsset(turAemModel, type) &&
                turAemModel.getSubType().equals(CONTENT_FRAGMENT) &&
                aemObject.isContentFragment();
    }

    public static boolean isAsset(TurAemModel turAemModel, String type) {
        return type.equals(DAM_ASSET) && StringUtils.isNotEmpty(turAemModel.getSubType());
    }

    public static boolean isPublish(TurAemSource turAemSource) {
        return turAemSource.isPublish() &&
                StringUtils.isNotEmpty(turAemSource.getPublishSNSite());
    }

    public static boolean isAuthor(TurAemSource turAemSource) {
        return turAemSource.isAuthor() &&
                StringUtils.isNotEmpty(turAemSource.getAuthorSNSite());
    }

    public static @NotNull Map<String, Object> getJobItemAttributes(TurAemSourceContext turAemSourceContext,
                                                                     TurAemTargetAttrValueMap targetAttrValueMap) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SITE, turAemSourceContext.getSiteName());
        targetAttrValueMap.entrySet().stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .forEach(entry -> getJobItemAttribute(entry, attributes));
        return attributes;
    }

    private static void getJobItemAttribute(Map.Entry<String, TurMultiValue> entry, Map<String, Object> attributes) {
        String attributeName = entry.getKey();
        entry.getValue().stream()
                .filter(StringUtils::isNotBlank)
                .forEach(attributeValue -> {
                    if (attributes.containsKey(attributeName)) {
                        TurAemCommonsUtils.addItemInExistingAttribute(attributeValue, attributes, attributeName);
                    } else {
                        TurAemCommonsUtils.addFirstItemToAttribute(attributeName, attributeValue, attributes);
                    }
                });
    }

    public static @NotNull TurAemTargetAttrValueMap getTargetAttrValueMap(
            TurAemObject aemObject,
            TurAemModel turAemModel,
            List<TurSNAttributeSpec> turSNAttributeSpecList,
            TurAemSourceContext turAemSourceContext,
            TurAemContentDefinitionProcess turAemContentDefinitionProcess) {
        TurAemTargetAttrValueMap turAemTargetAttrValueMap = new TurAemAttrProcess()
                .prepareAttributeDefs(aemObject, turAemContentDefinitionProcess, turSNAttributeSpecList,
                        turAemSourceContext);
        turAemTargetAttrValueMap.merge(TurAemCommonsUtils.runCustomClassFromContentType(turAemModel,
                aemObject, turAemSourceContext));
        return turAemTargetAttrValueMap;
    }

    public static @NotNull List<TurAemTargetAttr> getTurAemTargetAttrs(TurAemPluginModel pluginModel) {
        return pluginModel.getTargetAttrs()
                .stream()
                .map(targetAttr -> TurAemTargetAttr.builder()
                        .name(targetAttr.getName())
                        .sourceAttrs(getTurAemSourceAttrs(targetAttr))
                        .build())
                .collect(Collectors.toList());
    }

    public static @NotNull List<TurAemSourceAttr> getTurAemSourceAttrs(TurAemTargetAttribute targetAttr) {
        return targetAttr.getSourceAttrs()
                .stream()
                .map(sourceAttr -> TurAemSourceAttr.builder()
                        .className(sourceAttr.getClassName())
                        .name(sourceAttr.getName())
                        .convertHtmlToText(false)
                        .uniqueValues(false)
                        .build())
                .toList();
    }
}
