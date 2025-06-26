package com.viglet.turing.connector.plugin.aem;

import com.viglet.turing.client.sn.TurSNConstants;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import com.viglet.turing.connector.aem.commons.mappers.*;
import com.viglet.turing.connector.commons.plugin.TurConnectorContext;
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.connector.plugin.aem.conf.AemPluginHandlerConfiguration;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;
import com.viglet.turing.connector.plugin.aem.persistence.repository.TurAemSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.*;

@Slf4j
@Component
public class TurAemPluginSingleProcess {
    private final TurConnectorContext turConnectorContext;
    private final TurAemSourceRepository turAemSourceRepository;
    private final TurAemPluginService turAemPluginService;
    public TurAemPluginSingleProcess(TurConnectorContext turConnectorContext,
                                     TurAemSourceRepository turAemSourceRepository,
                                     TurAemPluginService turAemPluginService) {
        this.turConnectorContext = turConnectorContext;
        this.turAemSourceRepository = turAemSourceRepository;
        this.turAemPluginService = turAemPluginService;
    }

    public Optional<TurSNJobItem> getTurSNJobItem(String objectId, String source, String environment, Locale locale) {
        return turAemSourceRepository.findByName(source).map(turAemSource -> {
            TurConnectorSession turConnectorSession = TurAemPluginProcess.getTurConnectorSession(turAemSource);
            return indexContentId(turConnectorSession, turAemSource, objectId, environment, locale);
        }).orElse(Optional.empty());
    }

    public Optional<TurSNJobItem> indexContentId(TurConnectorSession session, TurAemSource turAemSource, String contentId,
                                                 String environment, Locale locale) {
        TurAemSourceContext turAemSourceContext = turAemPluginService.getTurAemSourceContext(
                new AemPluginHandlerConfiguration(turAemSource));
        return TurAemCommonsUtils.getInfinityJson(contentId, turAemSourceContext, false)
                .map(infinityJson -> {
                    turAemSourceContext.setContentType(infinityJson.getString(JCR_PRIMARY_TYPE));
                    return getNodeFromJson(contentId, infinityJson, turAemSourceContext, session, turAemSource,
                            new TurAemContentDefinitionProcess(turAemPluginService.getTurAemContentMapping(turAemSource)),
                            environment, locale);
                }).orElseGet(() -> sendToTuringToBeDeIndexed(session, contentId, environment, locale));
    }

    private Optional<TurSNJobItem> getNodeFromJson(String nodePath, JSONObject jsonObject,
                                                   TurAemSourceContext turAemSourceContext,
                                                   TurConnectorSession session,
                                                   TurAemSource turAemSource,
                                                   TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                                   String environment, Locale locale) {
        if (TurAemCommonsUtils.isTypeEqualContentType(jsonObject, turAemSourceContext)) {
            return turAemContentDefinitionProcess.findByNameFromModelWithDefinition(turAemSourceContext.getContentType())
                    .flatMap(model -> prepareIndexObject(model, new TurAemObject(nodePath, jsonObject),
                            turAemContentDefinitionProcess.getTargetAttrDefinitions(),
                            turAemSourceContext, session, turAemSource,
                            turAemContentDefinitionProcess, environment, locale));
        }
        return Optional.empty();
    }

    private Optional<TurSNJobItem> prepareIndexObject(TurAemModel turAemModel, TurAemObject aemObject,
                                                      List<TurSNAttributeSpec> targetAttrDefinitions,
                                                      TurAemSourceContext turAemSourceContext,
                                                      TurConnectorSession turConnectorSession,
                                                      TurAemSource turAemSource,
                                                      TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                                      String environment, Locale locale) {
        String type = Objects.requireNonNull(turAemSourceContext.getContentType());
        if (TurAemPluginUtils.isNotValidType(turAemModel, aemObject, type)) {
            return Optional.empty();
        }
        if (TurAemPluginUtils.isContentFragment(turAemModel, type, aemObject)) {
            aemObject.setDataPath(DATA_MASTER);
        } else if (TurAemPluginUtils.isStaticFile(turAemModel, type)) {
            aemObject.setDataPath(METADATA);
        }
        return indexObject(aemObject, turAemModel, targetAttrDefinitions, turAemSourceContext,
                turConnectorSession, turAemSource, turAemContentDefinitionProcess, environment, locale);
    }

    private Optional<TurSNJobItem> indexObject(@NotNull TurAemObject aemObject, TurAemModel turAemModel,
                                               List<TurSNAttributeSpec> turSNAttributeSpecList,
                                               TurAemSourceContext turAemSourceContext,
                                               TurConnectorSession turConnectorSession,
                                               TurAemSource turAemSource,
                                               TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                               String environment, Locale locale) {
        if (environment.equals(TurAemEnv.AUTHOR.toString())) {
            return indexByEnvironment(TurAemEnv.AUTHOR, turAemSource.getAuthorSNSite(),
                    aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                    turConnectorSession, turAemContentDefinitionProcess, locale);
        }
        if (environment.equals(TurAemEnv.PUBLISHING.toString())) {
            if (aemObject.isDelivered()) {
                return indexByEnvironment(TurAemEnv.PUBLISHING, turAemSource.getPublishSNSite(),
                        aemObject, turAemModel, turSNAttributeSpecList, turAemSourceContext,
                        turConnectorSession, turAemContentDefinitionProcess, locale);
            } else {
                log.debug("Ignoring because {} object ({}) is not publishing. transactionId = {}",
                        aemObject.getPath(), turAemSourceContext.getId(), turConnectorSession.getTransactionId());
            }
        }
        return Optional.empty();
    }

    private Optional<TurSNJobItem> indexByEnvironment(TurAemEnv turAemEnv, String snSite,
                                                      @NotNull TurAemObject aemObject, TurAemModel turAemModel,
                                                      List<TurSNAttributeSpec> turSNAttributeSpecList,
                                                      TurAemSourceContext turAemSourceContext,
                                                      TurConnectorSession turConnectorSession,
                                                      TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                                      Locale locale) {
        turAemSourceContext.setEnvironment(turAemEnv);
        turConnectorSession.setSites(Collections.singletonList(snSite));
        return getTurSNJobItem(
                aemObject,
                turSNAttributeSpecList,
                locale,
                turAemSourceContext,
                turConnectorSession,
                turAemContentDefinitionProcess,
                TurAemPluginUtils.getJobItemAttributes(
                        turAemSourceContext,
                        TurAemPluginUtils.getTargetAttrValueMap(
                                aemObject,
                                turAemModel,
                                turSNAttributeSpecList,
                                turAemSourceContext,
                                turAemContentDefinitionProcess)));
    }


    private Optional<TurSNJobItem> sendToTuringToBeDeIndexed(TurConnectorSession session, String contentId,
                                                             String environment, Locale locale) {
        return turConnectorContext.getIndexingItem(contentId, session.getSource(), environment, locale)
                .map(indexing -> new TurSNJobItem(
                        TurSNJobAction.DELETE,
                        indexing.getSites(),
                        indexing.getLocale(),
                        Map.of(
                                TurSNConstants.ID_ATTR, indexing.getObjectId(),
                                TurSNConstants.SOURCE_APPS_ATTR, session.getProviderName())));
    }

    private static @NotNull Optional<TurSNJobItem> getTurSNJobItem(TurAemObject aemObject,
                                                                   List<TurSNAttributeSpec> turSNAttributeSpecList,
                                                                   Locale locale,
                                                                   TurAemSourceContext turAemSourceContext,
                                                                   TurConnectorSession turConnectorSession,
                                                                   TurAemContentDefinitionProcess turAemContentDefinitionProcess,
                                                                   Map<String, Object> attributes) {
        TurSNJobItem turSNJobItem = new TurSNJobItem(
                TurSNJobAction.CREATE,
                turConnectorSession.getSites().stream().toList(),
                locale,
                attributes,
                TurAemCommonsUtils.castSpecToJobSpec(
                        TurAemCommonsUtils.getDefinitionFromModel(turSNAttributeSpecList, attributes)));
        turSNJobItem.setChecksum(String.valueOf(TurAemCommonsUtils
                .getDeltaDate(aemObject, turAemSourceContext, turAemContentDefinitionProcess).getTime()));
        turSNJobItem.setEnvironment(turAemSourceContext.getEnvironment().toString());
        return Optional.of(turSNJobItem);
    }

}
