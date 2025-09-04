package com.viglet.turing.connector.commons.domain;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.connector.commons.TurConnectorSession;

public record TurJobItemWithSession(TurSNJobItem turSNJobItem, TurConnectorSession session,
                boolean standalone) {

}
