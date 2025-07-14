package com.viglet.turing.connector.service;

import com.viglet.turing.connector.persistence.model.TurConnectorConfigVarModel;
import com.viglet.turing.connector.persistence.repository.TurConnectorConfigVarRepository;
import org.springframework.stereotype.Service;

@Service
public class TurConnectorConfigVarService {
    private final TurConnectorConfigVarRepository turConnectorConfigVarRepository;
    public static final String FIRST_TIME = "FIRST_TIME";

    public TurConnectorConfigVarService(TurConnectorConfigVarRepository turConnectorConfigVarRepository) {
        this.turConnectorConfigVarRepository = turConnectorConfigVarRepository;
    }

    public boolean hasNotFirstTime() {
        return turConnectorConfigVarRepository.findById(FIRST_TIME).isEmpty();
    }

    public void save(TurConnectorConfigVarModel turConfigVar) {
        this.turConnectorConfigVarRepository.save(turConfigVar);
    }

    public void saveFirstTime() {
        TurConnectorConfigVarModel turConfigVar = new TurConnectorConfigVarModel();
        turConfigVar.setId(FIRST_TIME);
        turConfigVar.setPath("/system");
        turConfigVar.setValue("true");
        save(turConfigVar);
    }
}
