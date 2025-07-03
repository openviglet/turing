package com.viglet.turing.connector.commons.logging;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.commons.indexing.TurLoggingStatus;
import com.viglet.turing.connector.commons.TurConnectorSession;
import com.viglet.turing.commons.logging.TurLoggingIndexing;
import com.viglet.turing.commons.logging.TurLoggingIndexingLog;

import java.util.Date;

public class TurConnectorLoggingUtils {
    public static final String URL = "url";

    public static void setSuccessStatus(TurSNJobItem turSNJobItem, TurConnectorSession session,
                                        TurIndexingStatus status) {
        setSuccessStatus(turSNJobItem, session, status, null);
    }

    public static void setSuccessStatus(TurSNJobItem turSNJobItem, TurConnectorSession session,
                                        TurIndexingStatus status, String details) {
        TurLoggingIndexingLog.setStatus(TurLoggingIndexing
                .builder()
                .contentId(turSNJobItem.getId())
                .url(turSNJobItem.getStringAttribute(URL))
                .environment(turSNJobItem.getEnvironment())
                .locale(turSNJobItem.getLocale())
                .status(status)
                .resultStatus(TurLoggingStatus.SUCCESS)
                .transactionId(session.getTransactionId())
                .checksum(turSNJobItem.getChecksum())
                .source(session.getSource())
                .date(new Date())
                .sites(turSNJobItem.getSiteNames())
                .details(details)
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
                .resultStatus(TurLoggingStatus.SUCCESS)
                .checksum(turSNJobItem.getChecksum())
                .date(new Date())
                .sites(turSNJobItem.getSiteNames())
                .build());
    }

}
