package com.viglet.turing.connector.plugin.aem.service;

import static com.viglet.turing.client.sn.TurSNConstants.ID_ATTR;
import static com.viglet.turing.client.sn.TurSNConstants.SOURCE_APPS_ATTR;
import static com.viglet.turing.client.sn.job.TurSNJobAction.CREATE;
import static com.viglet.turing.client.sn.job.TurSNJobAction.DELETE;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.DEINDEXED;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DATA_MASTER;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.METADATA;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.SITE;
import static com.viglet.turing.connector.aem.commons.bean.TurAemEnv.AUTHOR;
import static com.viglet.turing.connector.aem.commons.bean.TurAemEnv.PUBLISHING;
import static com.viglet.turing.connector.commons.logging.TurConnectorLoggingUtils.setSuccessStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
import com.viglet.turing.connector.plugin.aem.TurAemContentDefinitionService;
import com.viglet.turing.connector.plugin.aem.TurAemPluginUtils;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TurAemJobService {
        private final TurAemContentMappingService turAemContentMappingService;
        private final TurAemSourceService turAemSourceService;
        private final TurAemService turAemService;
        private final TurConnectorContext turConnectorContext;
        private final TurAemContentDefinitionService turAemContentDefinitionService;

        public TurAemJobService(TurAemContentMappingService turAemContentMappingService,
                        TurAemSourceService turAemSourceService,
                        TurAemService turAemService,
                        TurConnectorContext turConnectorContext,
                        TurAemContentDefinitionService turAemContentDefinitionService) {
                this.turAemContentMappingService = turAemContentMappingService;
                this.turAemSourceService = turAemSourceService;
                this.turAemService = turAemService;
                this.turConnectorContext = turConnectorContext;
                this.turAemContentDefinitionService = turAemContentDefinitionService;
        }

        public TurSNJobItem deIndexJob(TurConnectorSession session, List<String> sites,
                        Locale locale, String objectId, String environment) {
                TurSNJobItem turSNJobItem = new TurSNJobItem(DELETE, sites, locale, Map.of(ID_ATTR,
                                objectId, SOURCE_APPS_ATTR, session.getProviderName()));
                turSNJobItem.setEnvironment(environment);
                setSuccessStatus(turSNJobItem, session, DEINDEXED);
                return turSNJobItem;
        }

        public TurSNJobItem deIndexJob(TurConnectorSession session,
                        TurConnectorIndexing turConnectorIndexingDTO) {
                return deIndexJob(session, turConnectorIndexingDTO.getSites(),
                                turConnectorIndexingDTO.getLocale(),
                                turConnectorIndexingDTO.getObjectId(),
                                turConnectorIndexingDTO.getEnvironment());
        }

        public @NotNull TurSNJobItem getTurSNJobItem(TurAemObject aemObject,
                        List<TurSNAttributeSpec> turSNAttributeSpecList, Locale locale,
                        TurAemSourceContext turAemSourceContext, TurAemSource turAemSource,
                        TurConnectorSession session, Map<String, Object> attributes) {
                TurSNJobItem jobItem = new TurSNJobItem(CREATE,
                                session.getSites().stream().toList(), locale, attributes,
                                TurAemCommonsUtils.castSpecToJobSpec(TurAemCommonsUtils
                                                .getDefinitionFromModel(turSNAttributeSpecList,
                                                                attributes)));
                jobItem.setChecksum(String.valueOf(turAemContentDefinitionService.getDeltaDate(aemObject,
                                turAemSourceContext,
                                turAemContentMappingService.getTurAemContentMapping(turAemSource))
                                .getTime()));
                jobItem.setEnvironment(turAemSourceContext.getEnvironment().toString());
                return jobItem;
        }

        public void indexObject(@NotNull TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource, boolean standalone) {
                indexingAuthor(aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                                session, turAemSource, standalone);
                indexingPublish(aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                                session, turAemSource, standalone);
        }

        public void indexingAuthor(TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource, boolean standalone) {
                if (turAemSourceService.isAuthor(turAemSource)) {
                        indexByEnvironment(AUTHOR, turAemSource.getAuthorSNSite(), aemObject,
                                        turAemModel, turSNAttributeSpecList, turAemSourceContext,
                                        turAemSource, session, standalone);
                }
                ;
        }

        public void indexingPublish(TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session,
                        TurAemSource turAemSource, boolean standalone) {
                if (turAemSourceService.isPublish(turAemSource)) {
                        if (aemObject.isDelivered()) {
                                indexByEnvironment(PUBLISHING, turAemSource.getPublishSNSite(),
                                                aemObject, turAemModel, turSNAttributeSpecList,
                                                turAemSourceContext, turAemSource, session,
                                                standalone);
                        } else if (standalone) {
                                forcingDeIndex(aemObject, turAemSourceContext, session,
                                                turAemSource);
                        } else {
                                ignoringDeIndexLog(aemObject, turAemSourceContext, session);
                        }
                }
        }

        private void ignoringDeIndexLog(TurAemObject aemObject,
                        TurAemSourceContext turAemSourceContext, TurConnectorSession session) {
                log.info("Ignoring deIndex because {} is not publishing.", TurAemPluginUtils
                                .getObjectDetailForLogs(aemObject, turAemSourceContext, session));
        }

        private void forcingDeIndex(TurAemObject aemObject, TurAemSourceContext turAemSourceContext,
                        TurConnectorSession session, TurAemSource turAemSource) {
                TurSNJobItem deIndexJobItem = deIndexJob(session,
                                List.of(turAemSource.getPublishSNSite()),
                                TurAemCommonsUtils.getLocaleFromAemObject(
                                                turAemSourceContext, aemObject),
                                aemObject.getPath(), PUBLISHING.toString());
                TurJobItemWithSession turJobItemWithSession = new TurJobItemWithSession(
                                deIndexJobItem, session, aemObject.getDependencies(), true);
                turConnectorContext.addJobItem(turJobItemWithSession);
                log.info("Forcing deIndex because {} is not publishing.", TurAemPluginUtils
                                .getObjectDetailForLogs(aemObject, turAemSourceContext, session));
        }

        private void indexByEnvironment(TurAemEnv turAemEnv, String snSite,
                        @NotNull TurAemObject aemObject, TurAemModel turAemModel,
                        List<TurSNAttributeSpec> turSNAttributeSpecList,
                        TurAemSourceContext originalTurAemSourceContext, TurAemSource turAemSource,
                        TurConnectorSession session, boolean standalone) {
                TurAemSourceContext turAemSourceContext = new TurAemSourceContext(originalTurAemSourceContext);
                turAemSourceContext.setEnvironment(turAemEnv);
                turAemSourceContext.setTurSNSite(snSite);
                session.setSites(Collections.singletonList(snSite));
                createIndexJobAndSendToConnectorQueue(aemObject, turAemModel,
                                turSNAttributeSpecList,
                                TurAemCommonsUtils.getLocaleFromAemObject(turAemSourceContext,
                                                aemObject),
                                turAemSourceContext, turAemSource, session, standalone);
        }

        private void createIndexJobAndSendToConnectorQueue(TurAemObject aemObject,
                        TurAemModel turAemModel, List<TurSNAttributeSpec> turSNAttributeSpecList,
                        Locale locale, TurAemSourceContext turAemSourceContext,
                        TurAemSource turAemSource, TurConnectorSession session,
                        boolean standalone) {
                TurSNJobItem turSNJobItem = getTurSNJobItem(aemObject, turSNAttributeSpecList,
                                locale, turAemSourceContext, turAemSource, session,
                                getJobItemAttributes(turAemSourceContext, turAemService.getTargetAttrValueMap(
                                                aemObject, turAemModel, turSNAttributeSpecList,
                                                turAemSourceContext, turAemSource)));
                TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(turSNJobItem,
                                session, aemObject.getDependencies(), standalone);
                turConnectorContext.addJobItem(jobItemWithSession);
        }

        private static @NotNull Map<String, Object> getJobItemAttributes(
                        TurAemSourceContext turAemSourceContext,
                        TurAemTargetAttrValueMap targetAttrValueMap) {
                Map<String, Object> attributes = new HashMap<>();
                String siteName = turAemSourceContext.getSiteName();
                if (StringUtils.isNotBlank(siteName)) {
                        attributes.put(SITE, siteName);
                }
                targetAttrValueMap.entrySet().stream()
                                .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                                .forEach(e -> getJobItemAttribute(e, attributes));
                return attributes;
        }

        private static void getJobItemAttribute(Map.Entry<String, TurMultiValue> entry,
                        Map<String, Object> attributes) {
                String attributeName = entry.getKey();
                entry.getValue().stream().filter(StringUtils::isNotBlank)
                                .forEach(attributeValue -> {
                                        if (attributes.containsKey(attributeName)) {
                                                TurAemCommonsUtils.addItemInExistingAttribute(
                                                                attributeValue, attributes,
                                                                attributeName);
                                        } else {
                                                TurAemCommonsUtils.addFirstItemToAttribute(
                                                                attributeName, attributeValue,
                                                                attributes);
                                        }
                                });
        }

        public void createDeIndexJobAndSendToConnectorQueue(TurConnectorSession session,
                        String contentId, boolean standalone) {
                turConnectorContext.getIndexingItem(contentId, session.getSource(),
                                session.getProviderName()).forEach(indexing -> {
                                        log.info("DeIndex because {} infinity Json file not found.",
                                                        TurAemPluginUtils.getObjectDetailForLogs(
                                                                        contentId, indexing,
                                                                        session));
                                        TurJobItemWithSession turJobItemWithSession = new TurJobItemWithSession(
                                                        deIndexJob(session,
                                                                        indexing),
                                                        session,
                                                        Collections.emptySet(),
                                                        standalone);
                                        turConnectorContext.addJobItem(turJobItemWithSession);
                                });
        }

        public void prepareIndexObject(TurAemModel turAemModel, TurAemObject aemObject,
                        TurAemSourceContext turAemSourceContext,
                        TurConnectorSession turConnectorSession, TurAemSource turAemSource,
                        boolean standalone) {
                List<TurSNAttributeSpec> targetAttrDefinitions = turAemContentDefinitionService
                                .getTargetAttrDefinitions(turAemContentMappingService
                                                .getTurAemContentMapping(turAemSource));
                if (turAemSourceContext.getRootPath() != null
                                && !aemObject.getPath().startsWith(turAemSourceContext.getRootPath())) {
                        log.debug("Skipping object {} as it is outside the root path {}",
                                        aemObject.getPath(), turAemSourceContext.getRootPath());
                        return;
                }
                String type = Objects.requireNonNull(turAemSourceContext.getContentType());
                if (turAemService.isNotValidType(turAemModel, aemObject, type)) {
                        return;
                }
                if (turAemService.isContentFragment(turAemModel, type, aemObject)) {
                        aemObject.setDataPath(DATA_MASTER);
                } else if (turAemService.isStaticFile(turAemModel, type)) {
                        aemObject.setDataPath(METADATA);
                }
                indexObject(aemObject, turAemModel, targetAttrDefinitions, turAemSourceContext,
                                turConnectorSession, turAemSource, standalone);
        }
}
