package com.viglet.turing.connector.plugin.aem.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.viglet.turing.client.sn.job.TurSNAttributeSpec;
import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import com.viglet.turing.connector.aem.commons.mappers.TurAemContentMapping;
import com.viglet.turing.connector.aem.commons.mappers.TurAemModel;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.plugin.aem.api.TurAemPathList;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;
import com.viglet.turing.connector.plugin.aem.persistence.model.TurAemSource;

@Service
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

        public TurAemSession getTurAemSession(TurAemSource turAemSource,
                        TurAemPathList turAemPathList) {
                // Retrieve content mapping once and reuse
                TurAemContentMapping turAemContentMapping =
                                turAemContentMappingService.getTurAemContentMapping(turAemSource);

                // Get attribute specifications
                List<TurSNAttributeSpec> attributeSpecs =
                                turAemContentDefinitionService
                                                .getAttributeSpec(turAemContentMapping);

                // Get connector session and configuration
                TurConnectorSession session =
                                turAemSourceService.getTurConnectorSession(turAemSource);
                TurAemConfiguration turAemConfiguration =
                                turAemSourceService.getTurAemConfiguration(turAemSource);

                // Get optional model
                TurAemModel model = turAemContentDefinitionService
                                .getModel(turAemConfiguration, turAemSource).orElse(null);

                // Extract event and recursion settings with null-safe operations
                TurAemEvent event = Optional.ofNullable(turAemPathList)
                                .map(TurAemPathList::getEvent)
                                .orElse(TurAemEvent.NONE);

                boolean indexChildren = Optional.ofNullable(turAemPathList)
                                .map(TurAemPathList::getRecursive)
                                .orElse(true);

                // Build and return session
                return TurAemSession.builder()
                                .configuration(turAemConfiguration)
                                .event(event)
                                .standalone(true)
                                .indexChildren(indexChildren)
                                .source(session.getSource())
                                .transactionId(session.getTransactionId())
                                .sites(session.getSites())
                                .providerName(session.getProviderName())
                                .locale(session.getLocale())
                                .attributeSpecs(attributeSpecs)
                                .contentMapping(turAemContentMapping)
                                .model(model)
                                .build();
        }

        public TurAemSession getTurAemSession(TurAemSource turAemSource) {
                return getTurAemSession(turAemSource, (TurAemPathList) null);

        }
}
