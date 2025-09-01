package com.viglet.turing.connector.onstartup;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.connector.service.TurConnectorConfigVarService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Transactional
public class TurConnectorOnStartup implements ApplicationRunner {
    private final TurConnectorConfigVarService configVarService;

    public TurConnectorOnStartup(TurConnectorConfigVarService configVarService) {
        this.configVarService = configVarService;
    }

    @Override
    public void run(ApplicationArguments arg0) {
        if (configVarService.hasNotFirstTime()) {
            log.info("First Time Configuration ...");
            setFirstTIme();
            log.info("Configuration finished.");
        }
    }

    private void setFirstTIme() {
        configVarService.saveFirstTime();
    }

}
