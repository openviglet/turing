package com.viglet.turing.connector.plugin.aem.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import com.viglet.turing.connector.aem.commons.TurAemObjectGeneric;
import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;

@Service
public class TurAemObjectService {

    public TurAemObjectGeneric getTurAemObjectGeneric(String path, JSONObject infinityJson) {
        return getTurAemObjectGeneric(path, infinityJson, TurAemEvent.NONE);
    }

    public TurAemObjectGeneric getTurAemObjectGeneric(String path, JSONObject infinityJson,
            TurAemEvent turAemEvent) {
        return new TurAemObjectGeneric(path, infinityJson, turAemEvent);
    }

}
