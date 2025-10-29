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
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.bean.TurAemTargetAttrValueMap;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.commons.TurConnectorContext;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.commons.domain.TurJobItemWithSession;
import com.viglet.turing.connector.plugin.aem.TurAemContentDefinitionService;
import com.viglet.turing.connector.plugin.aem.TurAemPluginUtils;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;
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
                        TurAemSourceService turAemSourceService, TurAemService turAemService,
                        TurConnectorContext turConnectorContext,
                        TurAemContentDefinitionService turAemContentDefinitionService) {
                this.turAemContentMappingService = turAemContentMappingService;
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
                                                turAemContentMappingService.getTurAemContentMapping(
                                                                turAemSession.getAemSource()))
                                .getTime()));
                jobItem.setEnvironment(
                                turAemSession.getConfiguration().getEnvironment().toString());
                return jobItem;
        }

        public void indexObject(TurAemSession turAemSession, TurAemObject aemObject,
                        TurAemModel turAemModel) {
                indexingAuthor(turAemSession, aemObject, turAemModel);
                indexingPublish(turAemSession, aemObject, turAemModel);
        }

        public void indexingAuthor(TurAemSession turAemSession, TurAemObject aemObject,
                        TurAemModel turAemModel) {
                if (turAemSourceService.isAuthor(turAemSession.getAemSource())) {
                        indexByEnvironment(turAemSession, AUTHOR,
                                        turAemSession.getAemSource().getAuthorSNSite(), aemObject,
                                        turAemModel);
                } ;
        }

        public void indexingPublish(TurAemSession turAemSession, TurAemObject aemObject,
                        TurAemModel turAemModel) {
                if (turAemSourceService.isPublish(turAemSession.getAemSource())) {
                        if (aemObject.isDelivered()) {
                                indexByEnvironment(turAemSession, PUBLISHING,
                                                turAemSession.getAemSource().getPublishSNSite(),
                                                aemObject, turAemModel);
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
                                List.of(turAemSession.getAemSource().getPublishSNSite()),
                                TurAemCommonsUtils.getLocaleFromAemObject(
                                                turAemSession.getConfiguration(), aemObject),
                                aemObject.getPath(), PUBLISHING.toString());
                TurJobItemWithSession turJobItemWithSession = new TurJobItemWithSession(
                                deIndexJobItem, turAemSession, aemObject.getDependencies(), true);
                turConnectorContext.addJobItem(turJobItemWithSession);
                log.info("Forcing deIndex because {} is not publishing.",
                                TurAemPluginUtils.getObjectDetailForLogs(turAemSession, aemObject));
        }

        private void indexByEnvironment(TurAemSession turAemSession, TurAemEnv turAemEnv,
                        String snSite, @NotNull TurAemObject aemObject, TurAemModel turAemModel) {
                // Review - clone turAemSession entire
                TurAemConfiguration turAemSourceContext =
                                new TurAemConfiguration(turAemSession.getConfiguration());
                turAemSourceContext.setEnvironment(turAemEnv);
                turAemSourceContext.setTurSNSite(snSite);
                turAemSession.setSites(Collections.singletonList(snSite));
                createIndexJobAndSendToConnectorQueue(turAemSession, aemObject, turAemModel,
                                TurAemCommonsUtils.getLocaleFromAemObject(turAemSourceContext,
                                                aemObject));
        }

        private void createIndexJobAndSendToConnectorQueue(TurAemSession turAemSession,
                        TurAemObject aemObject, TurAemModel turAemModel, Locale locale) {
                TurSNJobItem turSNJobItem = getTurSNJobItem(turAemSession, aemObject, locale,
                                getJobItemAttributes(turAemSession,
                                                turAemService.getTargetAttrValueMap(turAemSession,
                                                                aemObject, turAemModel)));
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
                turConnectorContext
                                .getIndexingItem(contentId, turAemSession.getSource(),
                                                turAemSession.getProviderName())
                                .forEach(indexing -> {
                                        log.info("DeIndex because {} infinity Json file not found.",
                                                        TurAemPluginUtils.getObjectDetailForLogs(
                                                                        contentId, indexing,
                                                                        turAemSession));
                                        TurJobItemWithSession turJobItemWithSession =
                                                        new TurJobItemWithSession(
                                                                        deIndexJob(turAemSession,
                                                                                        indexing),
                                                                        turAemSession,
                                                                        Collections.emptySet(),
                                                                        turAemSession.isStandalone());
                                        turConnectorContext.addJobItem(turJobItemWithSession);
                                });
        }

        public void prepareIndexObject(TurAemSession turAemSession, TurAemModel turAemModel,
                        TurAemObject aemObject) {

                if (turAemSession.getConfiguration().getRootPath() != null && !aemObject.getPath()
                                .startsWith(turAemSession.getConfiguration().getRootPath())) {
                        log.debug("Skipping object {} as it is outside the root path {}",
                                        aemObject.getPath(),
                                        turAemSession.getConfiguration().getRootPath());
                        return;
                }
                String type = Objects
                                .requireNonNull(turAemSession.getConfiguration().getContentType());
                if (turAemService.isNotValidType(turAemModel, aemObject, type)) {
                        return;
                }
                if (turAemService.isContentFragment(turAemModel, type, aemObject)) {
                        aemObject.setDataPath(DATA_MASTER);
                } else if (turAemService.isStaticFile(turAemModel, type)) {
                        aemObject.setDataPath(METADATA);
                }
                indexObject(turAemSession, aemObject, turAemModel);
        }
}
