package com.viglet.turing.connector.plugin.aem.service;

import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.plugin.aem.api.TurAemPathList;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;

public class TurAemSessionService {
    private final TurAemSourceService turAemSourceService;
    private final TurAemContentDefinitionService turAemContentDefinitionService;
    private final TurAemContentMappingService turAemContentMappingService;

    public TurAemSessionService(TurAemSourceService turAemSourceService,
            TurAemContentDefinitionService turAemContentDefinitionService,
            TurAemContentMappingService turAemContentMappingService) {
        this.turAemSourceService = turAemSourceService;
        this.turAemContentDefinitionService = turAemContentDefinitionService;
        this.turAemContentMappingService = turAemContentMappingService;
    }

    public TurAemSession getTurAemSession(TurAemSource turAemSource) {
        TurConnectorSession session = turAemSourceService.getTurConnectorSession(turAemSource);
        TurAemSession turAemSession = TurAemSession.builder()
                .configuration(turAemSourceService
                        .getTurAemConfiguration(turAemSource))
                .event(TurAemEvent.NONE).standalone(true).indexChildren(true)
                .source(session.getSource())
                .transactionId(session.getTransactionId()).sites(session.getSites())
                .providerName(session.getProviderName()).locale(session.getLocale())
                .attributeSpecs(turAemContentDefinitionService.getAttributeSpec(
                        turAemContentMappingService.getTurAemContentMapping(
                                turAemSource)))
                .contentMapping(turAemContentMappingService
                        .getTurAemContentMapping(turAemSource))
                .build();
        turAemSession.setModel(turAemContentDefinitionService.getModel(turAemSession,
                turAemSource));
        return turAemSession;
    }

    public TurAemSession getTurAemSession(TurAemSource turAemSource,
            TurAemPathList turAemPathList) {
        TurConnectorSession session = turAemSourceService.getTurConnectorSession(turAemSource);
        TurAemSession turAemSession = TurAemSession.builder()
                .configuration(turAemSourceService
                        .getTurAemConfiguration(turAemSource))
                .event(turAemPathList.getEvent()).standalone(true)
                .indexChildren(turAemPathList.getRecursive())
                .source(session.getSource())
                .transactionId(session.getTransactionId()).sites(session.getSites())
                .providerName(session.getProviderName()).locale(session.getLocale())
                .attributeSpecs(turAemContentDefinitionService.getAttributeSpec(
                        turAemContentMappingService.getTurAemContentMapping(
                                turAemSource)))
                .contentMapping(turAemContentMappingService
                        .getTurAemContentMapping(turAemSource))
                .build();
        turAemSession.setModel(turAemContentDefinitionService.getModel(turAemSession,
                turAemSource));
        return turAemSession;
    }

}
