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

import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_TAGS;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.commons.cache.TurCustomClassCache;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemContext;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.ext.TurAemExtAttributeInterface;
import com.viglet.turing.connector.aem.commons.mappers.TurAemSourceAttr;
import com.viglet.turing.connector.aem.commons.mappers.TurAemTargetAttr;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TurAemAttrProcess {
        public static final String CQ_TAGS_PATH = "/content/_cq_tags";

        public TurAemTargetAttrValueMap prepareAttributeDefs(TurAemSession turAemSession,
                        TurAemObject aemObject) {

                TurAemContext context = new TurAemContext(aemObject);
                TurAemTargetAttrValueMap turAemTargetAttrValueMap =
                                new TurAemTargetAttrValueMap();
                turAemSession.getModel().getTargetAttrs().stream().filter(Objects::nonNull)
                                .forEach(targetAttr -> {
                                        log.debug("TargetAttr: {}", targetAttr);
                                        context.setTurAemTargetAttr(targetAttr);
                                        if (TurAemAttrUtils.hasCustomClass(targetAttr)) {
                                                turAemTargetAttrValueMap.merge(process(
                                                                turAemSession, context));
                                        } else {
                                                targetAttr.getSourceAttrs().stream()
                                                                .filter(Objects::nonNull)
                                                                .forEach(sourceAttr -> turAemTargetAttrValueMap
                                                                                .merge(addTargetAttrValuesBySourceAttr(
                                                                                                turAemSession,
                                                                                                targetAttr,
                                                                                                sourceAttr,
                                                                                                context)));
                                        }
                                });
                return turAemTargetAttrValueMap;

        }

        public TurAemTargetAttrValueMap addTargetAttrValuesBySourceAttr(TurAemSession turAemSession,
                        TurAemTargetAttr targetAttr, TurAemSourceAttr sourceAttr,
                        TurAemContext context) {

                context.setTurAemSourceAttr(sourceAttr);
                TurAemTargetAttrValueMap targetAttrValues = process(turAemSession, context);
                return sourceAttr.isUniqueValues()
                                ? TurAemAttrUtils.getTurAttrDefUnique(targetAttr, targetAttrValues)
                                : targetAttrValues;
        }

        public TurAemTargetAttrValueMap process(TurAemSession turAemSession,
                        TurAemContext context) {
                log.debug("Target Attribute Name: {} and Source Attribute Name: {}",
                                context.getTurAemTargetAttr().getName(),
                                context.getTurAemSourceAttr().getName());
                return TurAemAttrUtils.hasTextValue(context.getTurAemTargetAttr())
                                ? TurAemAttrUtils.getTextValue(context)
                                : getCustomClassValue(context, turAemSession.getAttributeSpecs(),
                                                turAemSession.getConfiguration());
        }

        private @NotNull TurAemTargetAttrValueMap getCustomClassValue(TurAemContext context,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemConfiguration turAemSourceContext) {
                TurAemTargetAttrValueMap turAemTargetAttrValueMap =
                                TurAemAttrUtils.hasCustomClass(context)
                                                ? attributeByClass(context, turAemSourceContext)
                                                : attributeByCMS(context);
                turAemTargetAttrValueMap
                                .merge(generateNewAttributesFromCqTags(context, turAemSourceContext,
                                                turSNAttributeSpecList, turAemTargetAttrValueMap));
                return turAemTargetAttrValueMap;
        }

        private TurAemTargetAttrValueMap attributeByCMS(TurAemContext context) {
                final Object jcrProperty = TurAemAttrUtils.getJcrProperty(context,
                                context.getTurAemSourceAttr().getName());
                return TurAemAttrUtils.hasJcrPropertyValue(jcrProperty)
                                ? TurAemAttrUtils.addValuesToAttributes(
                                                context.getTurAemTargetAttr(),
                                                context.getTurAemSourceAttr(), jcrProperty)
                                : new TurAemTargetAttrValueMap();
        }

        private TurAemTargetAttrValueMap generateNewAttributesFromCqTags(TurAemContext context,
                        TurAemConfiguration turAemSourceContext,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemTargetAttrValueMap turAemTargetAttrValueMapFromClass) {
                TurAemTargetAttrValueMap turAemTargetAttrValueMap = new TurAemTargetAttrValueMap();
                String attributeName = context.getTurAemSourceAttr().getName();
                if (CQ_TAGS.equals(attributeName)) {
                        String targetName = context.getTurAemTargetAttr().getName();
                        if (turAemTargetAttrValueMapFromClass.containsKey(targetName)) {
                                TurAemAttrUtils.processTagsFromTargetAttr(context,
                                                turAemSourceContext, turSNAttributeSpecList,
                                                turAemTargetAttrValueMapFromClass, targetName,
                                                turAemTargetAttrValueMap);
                        } else {
                                TurAemAttrUtils.processTagsFromSourceAttr(context,
                                                turAemSourceContext, turSNAttributeSpecList,
                                                attributeName, turAemTargetAttrValueMap);
                        }
                }
                return turAemTargetAttrValueMap;
        }

        private TurAemTargetAttrValueMap attributeByClass(TurAemContext context,
                        TurAemConfiguration turAemSourceContext) {
                String className = context.getTurAemSourceAttr().getClassName();
                log.debug("ClassName : {}", className);
                return TurCustomClassCache.getCustomClassMap(className)
                                .map(classInstance -> TurAemTargetAttrValueMap.singleItem(
                                                context.getTurAemTargetAttr().getName(),
                                                ((TurAemExtAttributeInterface) classInstance)
                                                                .consume(context.getTurAemTargetAttr(),
                                                                                context.getTurAemSourceAttr(),
                                                                                context.getCmsObjectInstance(),
                                                                                turAemSourceContext),
                                                false))
                                .orElseGet(TurAemTargetAttrValueMap::new);

        }
}
