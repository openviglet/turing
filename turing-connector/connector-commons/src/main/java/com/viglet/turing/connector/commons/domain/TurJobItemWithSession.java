package com.viglet.turing.connector.commons.domain;

import java.util.Set;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.commons.TurConnectorSession;

public record TurJobItemWithSession(TurSNJobItem turSNJobItem, TurConnectorSession session,
        Set<String> dependencies, boolean standalone) {

}
