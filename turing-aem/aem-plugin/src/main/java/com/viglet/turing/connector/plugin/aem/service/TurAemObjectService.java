package com.viglet.turing.connector.plugin.aem.service;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;

@Service
public class TurAemObjectService {
    public List<TurAemObject> getTurAemObjects(TurAemConfiguration turAemConfiguration, String path,
            JSONObject infinityJson, TurAemEvent turAemEvent) {

        if (turAemConfiguration == null || path == null || infinityJson == null
                || turAemEvent == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }

        List<TurAemObject> turAemObjects = new ArrayList<>();

        if (turAemConfiguration.isAuthor()) {
            turAemObjects
                    .add(createTurAemObject(path, infinityJson, turAemEvent, TurAemEnv.AUTHOR));
        }

        if (turAemConfiguration.isPublish()) {
            turAemObjects
                    .add(createTurAemObject(path, infinityJson, turAemEvent, TurAemEnv.PUBLISHING));
        }

        return turAemObjects;
    }

    public List<TurAemObject> getTurAemObjects(TurAemConfiguration turAemConfiguration, String path,
            JSONObject infinityJson) {
        return getTurAemObjects(turAemConfiguration, path, infinityJson, TurAemEvent.NONE);
    }

    private TurAemObject createTurAemObject(String path, JSONObject infinityJson,
            TurAemEvent turAemEvent, TurAemEnv environment) {
        return new TurAemObject(path, infinityJson, turAemEvent, environment);
    }
}
