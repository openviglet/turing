package com.viglet.turing.connector.logging;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.connector.commons.plugin.TurConnectorSession;
import com.viglet.turing.spring.logging.TurLoggingIndexing;
import com.viglet.turing.spring.logging.TurLoggingIndexingLog;

import java.util.Date;

public class TurConnectorLoggingUtils {
    public static final String URL = "url";
    public static void setSuccessStatus(TurSNJobItem turSNJobItem, TurConnectorSession session,
                                         TurIndexingStatus status) {
        TurLoggingIndexingLog.setStatus(TurLoggingIndexing
                .builder()
                .contentId(turSNJobItem.getId())
                .url(turSNJobItem.getStringAttribute(URL))
                .environment(turSNJobItem.getEnvironment())
                .locale(turSNJobItem.getLocale())
                .status(status)
                .transactionId(session.getTransactionId())
                .checksum(turSNJobItem.getChecksum())
                .source(session.getSource())
                .timestamp(new Date())
                .sites(turSNJobItem.getSiteNames())
                .build());
    }

    public static void setSuccessStatus(TurSNJobItem turSNJobItem,
                                        TurIndexingStatus status) {
        TurLoggingIndexingLog.setStatus(TurLoggingIndexing
                .builder()
                .contentId(turSNJobItem.getId())
                .url(turSNJobItem.getStringAttribute(URL))
                .environment(turSNJobItem.getEnvironment())
                .locale(turSNJobItem.getLocale())
                .status(status)
                .checksum(turSNJobItem.getChecksum())
                .timestamp(new Date())
                .sites(turSNJobItem.getSiteNames())
                .build());
    }

}
