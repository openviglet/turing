package com.viglet.turing.connector.onstartup;

import com.google.inject.Inject;
import com.viglet.turing.connector.persistence.model.TurConnectorConfigVar;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRule;
import com.viglet.turing.connector.persistence.model.TurConnectorIndexingRuleType;
import com.viglet.turing.connector.persistence.repository.TurConnectorConfigVarRepository;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRepository;
import com.viglet.turing.connector.persistence.repository.TurConnectorIndexingRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Transactional
public class TurConnectorIndexingRulesOnStartup {
    private final TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository;

    @Autowired
    public TurConnectorIndexingRulesOnStartup(TurConnectorIndexingRuleRepository turConnectorIndexingRuleRepository) {
        this.turConnectorIndexingRuleRepository = turConnectorIndexingRuleRepository;
    }

    public void createDefaultRows() {

        if (turConnectorIndexingRuleRepository.findAll().isEmpty()) {
            turConnectorIndexingRuleRepository.save(TurConnectorIndexingRule
                    .builder()
                    .name("Indexing Rule Ignore")
                    .description("Indexing Rule Ignore")
                    .ruleType(TurConnectorIndexingRuleType.IGNORE)
                    .source("wknd")
                    .attribute("id")
                    .values(List.of("/content/wknd/us/en/faqs/*",
                            "/content/wknd/us/en/magazine/ski-touring"))
                    .build());
        }
    }
}
