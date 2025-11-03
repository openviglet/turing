package com.viglet.turing.connector.plugin.aem.service;

import static com.viglet.turing.client.sn.TurSNConstants.ID_ATTR;
import static com.viglet.turing.client.sn.TurSNConstants.SOURCE_APPS_ATTR;
import static com.viglet.turing.client.sn.job.TurSNJobAction.CREATE;
import static com.viglet.turing.client.sn.job.TurSNJobAction.DELETE;
import static com.viglet.turing.commons.indexing.TurIndexingStatus.DEINDEXED;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DATA_MASTER;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.METADATA;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.SITE;
import static com.viglet.turing.connector.aem.commons.bean.TurAemEnv.PUBLISHING;
import static com.viglet.turing.connector.commons.logging.TurConnectorLoggingUtils.setSuccessStatus;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import com.viglet.turing.client.sn.TurMultiValue;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
import com.viglet.turing.connector.plugin.aem.TurAemPluginUtils;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TurAemJobService {
        private final TurAemSourceService turAemSourceService;
        private final TurAemService turAemService;
        private final TurConnectorContext turConnectorContext;
        private final TurAemContentDefinitionService turAemContentDefinitionService;

        public TurAemJobService(TurAemContentMappingService turAemContentMappingService,
                        TurAemSourceService turAemSourceService, TurAemService turAemService,
                        TurConnectorContext turConnectorContext,
                        TurAemContentDefinitionService turAemContentDefinitionService) {
                this.turAemSourceService = turAemSourceService;
                this.turAemService = turAemService;
                this.turConnectorContext = turConnectorContext;
                this.turAemContentDefinitionService = turAemContentDefinitionService;
        }

        public TurSNJobItem deIndexJob(TurAemSession turAemSession, List<String> sites,
                        Locale locale, String objectId, String environment) {
                TurSNJobItem turSNJobItem = new TurSNJobItem(DELETE, sites, locale, Map.of(ID_ATTR,
                                objectId, SOURCE_APPS_ATTR, turAemSession.getProviderName()));
                turSNJobItem.setEnvironment(environment);
                setSuccessStatus(turSNJobItem, turAemSession, DEINDEXED);
                return turSNJobItem;
        }

        public TurSNJobItem deIndexJob(TurAemSession turAemSession,
                        TurConnectorIndexing turConnectorIndexingDTO) {
                return deIndexJob(turAemSession, turConnectorIndexingDTO.getSites(),
                                turConnectorIndexingDTO.getLocale(),
                                turConnectorIndexingDTO.getObjectId(),
                                turConnectorIndexingDTO.getEnvironment());
        }

        public @NotNull TurSNJobItem getTurSNJobItem(TurAemSession turAemSession,
                        TurAemObject aemObject, Locale locale, Map<String, Object> attributes) {
                TurSNJobItem jobItem = new TurSNJobItem(CREATE,
                                turAemSession.getSites().stream().toList(), locale, attributes,
                                TurAemCommonsUtils.castSpecToJobSpec(
                                                TurAemCommonsUtils.getDefinitionFromModel(
                                                                turAemSession.getAttributeSpecs(),
                                                                attributes)));
                jobItem.setChecksum(String.valueOf(turAemContentDefinitionService
                                .getDeltaDate(aemObject, turAemSession.getConfiguration(),
                                                turAemSession.getContentMapping())
                                .getTime()));
                jobItem.setEnvironment(
                                aemObject.getEnvironment().toString());
                return jobItem;
        }

        public void indexObject(TurAemSession turAemSession, TurAemObject aemObject) {
                indexingAuthor(turAemSession, aemObject);
                indexingPublish(turAemSession, aemObject);
        }

        public void indexingAuthor(TurAemSession turAemSession, TurAemObject aemObject) {
                if (aemObject.getEnvironment().equals(TurAemEnv.AUTHOR)) {
                        indexByEnvironment(turAemSession, aemObject);
                } ;
        }

        public void indexingPublish(TurAemSession turAemSession, TurAemObject aemObject) {
                if (aemObject.getEnvironment().equals(TurAemEnv.PUBLISHING)) {
                        if (aemObject.isDelivered()) {
                                indexByEnvironment(turAemSession, aemObject);
                        } else if (turAemSession.isStandalone()) {
                                forcingDeIndex(turAemSession, aemObject);
                        } else {
                                ignoringDeIndexLog(turAemSession, aemObject);
                        }
                }
        }

        private void ignoringDeIndexLog(TurAemSession turAemSession, TurAemObject aemObject) {
                log.info("Ignoring deIndex because {} is not publishing.",
                                TurAemPluginUtils.getObjectDetailForLogs(turAemSession, aemObject));
        }

        private void forcingDeIndex(TurAemSession turAemSession, TurAemObject aemObject) {
                TurSNJobItem deIndexJobItem = deIndexJob(turAemSession,
                                List.of(turAemSession.getConfiguration().getPublishSNSite()),
                                TurAemCommonsUtils.getLocaleFromAemObject(
                                                turAemSession.getConfiguration(), aemObject),
                                aemObject.getPath(), PUBLISHING.toString());
                TurJobItemWithSession turJobItemWithSession = new TurJobItemWithSession(
                                deIndexJobItem, turAemSession, aemObject.getDependencies(), true);
                turConnectorContext.addJobItem(turJobItemWithSession);
                log.info("Forcing deIndex because {} is not publishing.",
                                TurAemPluginUtils.getObjectDetailForLogs(turAemSession, aemObject));
        }

        private void indexByEnvironment(TurAemSession turAemSession,
                        @NotNull TurAemObject aemObject) {
                turAemSession.setSites(Collections.singletonList(aemObject.getSNSite(
                                turAemSession.getConfiguration())));
                createIndexJobAndSendToConnectorQueue(turAemSession, aemObject,
                                TurAemCommonsUtils.getLocaleFromAemObject(
                                                turAemSession.getConfiguration(),
                                                aemObject));
        }

        private void createIndexJobAndSendToConnectorQueue(TurAemSession turAemSession,
                        TurAemObject aemObject, Locale locale) {
                TurSNJobItem turSNJobItem = getTurSNJobItem(turAemSession, aemObject, locale,
                                getJobItemAttributes(turAemSession,
                                                turAemService.getTargetAttrValueMap(turAemSession,
                                                                aemObject)));
                TurJobItemWithSession jobItemWithSession = new TurJobItemWithSession(turSNJobItem,
                                turAemSession, aemObject.getDependencies(),
                                turAemSession.isStandalone());
                turConnectorContext.addJobItem(jobItemWithSession);
        }

        private static @NotNull Map<String, Object> getJobItemAttributes(
                        TurAemSession turAemSession, TurAemTargetAttrValueMap targetAttrValueMap) {
                Map<String, Object> attributes = new HashMap<>();
                String siteName = turAemSession.getConfiguration().getSiteName();
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

        public void createDeIndexJobAndSendToConnectorQueue(TurAemSession turAemSession,
                        String contentId) {
                List<TurConnectorIndexing> indexingItems = turConnectorContext
                                .getIndexingItem(contentId, turAemSession.getSource(),
                                                turAemSession.getProviderName());

                if (CollectionUtils.isEmpty(indexingItems)) {
                        log.debug("No indexing items found for contentId: {} in source: {}",
                                        contentId, turAemSession.getProviderName());
                        return;
                }

                indexingItems.forEach(indexing -> {
                        try {
                                log.info("DeIndex initiated for {} - infinity Json file not found.",
                                                TurAemPluginUtils.getObjectDetailForLogs(contentId,
                                                                indexing,
                                                                turAemSession));

                                TurSNJobItem deIndexJobItem = deIndexJob(turAemSession, indexing);
                                TurJobItemWithSession turJobItemWithSession =
                                                new TurJobItemWithSession(
                                                                deIndexJobItem, turAemSession,
                                                                Collections.emptySet(),
                                                                turAemSession.isStandalone());
                                turConnectorContext.addJobItem(turJobItemWithSession);
                                log.debug("DeIndex job successfully queued for contentId: {}",
                                                contentId);

                        } catch (Exception e) {
                                log.error("Failed to create deIndex job for contentId: {} in session: {}. Error: {}",
                                                contentId, turAemSession.getProviderName(),
                                                e.getMessage(), e);
                        }
                });
        }

        public void prepareIndexObject(TurAemSession turAemSession, TurAemObject aemObject) {

                if (!isObjectEligibleForIndexing(turAemSession, aemObject)) {
                        return;
                }
                configureObjectDataPath(turAemSession, aemObject);
                indexObject(turAemSession, aemObject);

        }

        private boolean isObjectEligibleForIndexing(TurAemSession turAemSession,
                        TurAemObject aemObject) {
                TurAemConfiguration config = turAemSession.getConfiguration();
                if (!isWithinRootPath(aemObject, config)) {
                        return false;
                }
                String contentType = config.getContentType();
                if (contentType == null) {
                        log.warn("Content type is null for session {}",
                                        turAemSession.getProviderName());
                        return false;
                }
                return !turAemService.isNotValidType(turAemSession.getModel(), aemObject,
                                contentType);
        }

        private boolean isWithinRootPath(TurAemObject aemObject, TurAemConfiguration config) {
                String rootPath = config.getRootPath();
                if (rootPath != null && !aemObject.getPath().startsWith(rootPath)) {
                        log.debug("Skipping object {} as it is outside the root path {}",
                                        aemObject.getPath(), rootPath);
                        return false;
                }
                return true;
        }

        private void configureObjectDataPath(TurAemSession turAemSession, TurAemObject aemObject) {
                String contentType = turAemSession.getConfiguration().getContentType();
                if (turAemService.isContentFragment(turAemSession.getModel(), contentType,
                                aemObject)) {
                        aemObject.setDataPath(DATA_MASTER);
                } else if (turAemService.isStaticFile(turAemSession.getModel(), contentType)) {
                        aemObject.setDataPath(METADATA);
                }
        }
}
