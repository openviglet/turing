package com.viglet.turing.connector.aem.commons.ext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.mappers.TurAemSourceAttr;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import lombok.extern.slf4j.Slf4j;

/**
 * Extension for extracting page components from AEM pages. Processes responsive grid components
 * within the page structure.
 */
@Slf4j
public class TurAemExtPageComponents implements TurAemExtAttributeInterface {

    private static final String ROOT = "root";
    private static final String SLING_RESOURCE_TYPE = "sling:resourceType";
    private static final String RESPONSIVEGRID_RESOURCE_TYPE =
            "wcm/foundation/components/responsivegrid";
    private static final String COMPONENT_SEPARATOR = "\n";

    @Override
    public TurMultiValue consume(TurAemTargetAttr turAemTargetAttr,
            TurAemSourceAttr turAemSourceAttr, TurAemObject aemObject,
            TurAemConfiguration turAemConfiguration) {
        log.debug("Executing TurAemExtPageComponents for path: {}",
                aemObject != null ? aemObject.getPath() : "unknown");

        return extractResponsiveGridComponents(aemObject);
    }

    /**
     * Extracts all responsive grid components from the AEM object.
     *
     * @param aemObject The AEM object containing the page structure
     * @return TurMultiValue containing extracted component content
     */
    @NotNull
    public TurMultiValue extractResponsiveGridComponents(@Nullable TurAemObject aemObject) {
        if (!hasValidJcrContent(aemObject)) {
            log.debug("No valid JCR content found in AEM object");
            return TurMultiValue.empty();
        }

        try {
            Optional<JSONObject> rootNode = getRootNode(aemObject.getJcrContentNode());
            if (rootNode.isEmpty()) {
                log.debug("No root node found in JCR content");
                return TurMultiValue.empty();
            }

            List<String> components = extractComponentsFromRoot(rootNode.get());
            String combinedContent = components.stream().filter(Objects::nonNull)
                    .filter(content -> !content.trim().isEmpty())
                    .collect(Collectors.joining(COMPONENT_SEPARATOR));

            return TurMultiValue.singleItem(combinedContent);

        } catch (JSONException e) {
            log.error("Error processing JSON structure for AEM object", e);
            return TurMultiValue.empty();
        }
    }

    /**
     * Checks if the AEM object has valid JCR content.
     */
    private boolean hasValidJcrContent(@Nullable TurAemObject aemObject) {
        return aemObject != null && aemObject.getJcrContentNode() != null;
    }

    /**
     * Retrieves the root node from JCR content if it exists and is valid.
     */
    @NotNull
    private Optional<JSONObject> getRootNode(@NotNull JSONObject jcrContentNode) {
        if (!jcrContentNode.has(ROOT)) {
            return Optional.empty();
        }

        Object rootObject = jcrContentNode.get(ROOT);
        if (rootObject instanceof JSONObject) {
            return Optional.of((JSONObject) rootObject);
        }

        log.debug("Root node exists but is not a JSONObject: {}",
                rootObject.getClass().getSimpleName());
        return Optional.empty();
    }

    /**
     * Extracts components from the root node.
     */
    @NotNull
    private List<String> extractComponentsFromRoot(@NotNull JSONObject rootNode) {
        List<String> components = new ArrayList<>();

        rootNode.keySet().forEach(key -> {
            try {
                processNodeIfResponsiveGrid(rootNode, key).ifPresent(components::add);
            } catch (JSONException e) {
                log.warn("Error processing node with key: {}", key, e);
            }
        });

        log.debug("Extracted {} components from root node", components.size());
        return components;
    }

    /**
     * Processes a node if it's a responsive grid component.
     */
    @NotNull
    private Optional<String> processNodeIfResponsiveGrid(@NotNull JSONObject parentNode,
            @NotNull String nodeKey) {
        Object nodeObject = parentNode.get(nodeKey);
        if (!(nodeObject instanceof JSONObject node)) {
            return Optional.empty();
        }

        if (!isResponsiveGrid(node)) {
            return Optional.empty();
        }

        log.trace("Processing responsive grid node: {}", nodeKey);
        return convertNodeToText(node);
    }

    /**
     * Checks if a node is a responsive grid component.
     */
    private boolean isResponsiveGrid(@NotNull JSONObject node) {
        return node.has(SLING_RESOURCE_TYPE)
                && RESPONSIVEGRID_RESOURCE_TYPE.equals(node.optString(SLING_RESOURCE_TYPE));
    }

    /**
     * Converts a JSON node to text content.
     */
    @NotNull
    private Optional<String> convertNodeToText(@NotNull JSONObject node) {
        try {
            String componentHtml = TurAemCommonsUtils.getJsonNodeToComponent(node);
            if (componentHtml.trim().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(TurCommonsUtils.html2Text(componentHtml))
                    .filter(text -> !text.trim().isEmpty());

        } catch (Exception e) {
            log.error("Error converting node to text", e);
            return Optional.empty();
        }
    }
}
