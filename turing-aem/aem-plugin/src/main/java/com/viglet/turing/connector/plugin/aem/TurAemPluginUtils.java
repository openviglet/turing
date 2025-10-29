package com.viglet.turing.connector.plugin.aem;

import static com.viglet.turing.connector.aem.commons.bean.TurAemEnv.PUBLISHING;
import com.viglet.turing.connector.aem.commons.TurAemCommonsUtils;
import com.viglet.turing.connector.aem.commons.TurAemObject;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.connector.commons.domain.TurConnectorIndexing;
import com.viglet.turing.connector.plugin.aem.context.TurAemSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurAemPluginUtils {
    public static String getObjectDetailForLogs(TurAemSession turAemSession,
            TurAemObject aemObject) {
        return "%s object (%s - %s - %s: %s)".formatted(aemObject.getPath(),
                turAemSession.getConfiguration().getId(), PUBLISHING, TurAemCommonsUtils
                        .getLocaleFromAemObject(turAemSession.getConfiguration(), aemObject),
                turAemSession.getTransactionId());
    }

    public static String getObjectDetailForLogs(String contentId, TurConnectorIndexing indexing,
            TurConnectorSession session) {
        return "%s object (%s - %s - %s: %s)".formatted(contentId, session.getSource(),
                indexing.getEnvironment(), indexing.getLocale(), session.getTransactionId());
    }
}
