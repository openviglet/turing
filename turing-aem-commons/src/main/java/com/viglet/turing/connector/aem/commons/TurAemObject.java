package com.viglet.turing.connector.aem.commons;

import org.json.JSONObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEnv;
import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
public class TurAemObject extends TurAemObjectGeneric {
    private TurAemEnv environment;

    public TurAemObject(String nodePath, JSONObject jcrNode, TurAemEnv environment) {
        super(nodePath, jcrNode);
        this.environment = environment;
    }

    public TurAemObject(TurAemObjectGeneric turAemObjectGeneric,
            TurAemEnv environment) {
        super(turAemObjectGeneric.getPath(), turAemObjectGeneric.getJcrNode());
        this.environment = environment;
    }

    public String getUrlPrefix(TurAemConfiguration configuration) {
        return getEnvironment().equals(TurAemEnv.AUTHOR) ? configuration.getAuthorURLPrefix()
                : configuration.getPublishURLPrefix();
    }

    public String getSNSite(TurAemConfiguration configuration) {
        return getEnvironment().equals(TurAemEnv.AUTHOR) ? configuration.getAuthorSNSite()
                : configuration.getPublishSNSite();
    }

}
