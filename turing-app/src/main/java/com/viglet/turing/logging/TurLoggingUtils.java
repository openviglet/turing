package com.viglet.turing.logging;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.commons.indexing.TurLoggingStatus;
import com.viglet.turing.commons.logging.TurLoggingIndexingLog;
import com.viglet.turing.commons.logging.TurLoggingIndexing;

import java.util.Date;

public class TurLoggingUtils {

    public static final String URL = "url";
    public static final String SERVER = "Server";

    public static void setLoggingStatus(TurSNJobItem turSNJobItem,
                                        TurIndexingStatus status,
                                        TurLoggingStatus loggingStatus,
                                        String details) {
        TurLoggingIndexingLog.setStatus(TurLoggingIndexing
                .builder()
                .contentId(turSNJobItem.getId())
                .url(turSNJobItem.getStringAttribute(URL))
                .environment(turSNJobItem.getEnvironment())
                .locale(turSNJobItem.getLocale())
                .status(status)
                .transactionId(null)
                .checksum(turSNJobItem.getChecksum())
                .source(SERVER)
                .date(new Date())
                .sites(turSNJobItem.getSiteNames())
                .resultStatus(loggingStatus)
                .details(details)
                .build());
    }

    public static void setSuccessStatus(TurSNJobItem turSNJobItem,
                                        TurIndexingStatus status) {
        setLoggingStatus(turSNJobItem, status, TurLoggingStatus.SUCCESS, null);
    }

    public static void setErrorStatus(TurSNJobItem turSNJobItem,
                                      TurIndexingStatus status, String details) {
        setLoggingStatus(turSNJobItem, status, TurLoggingStatus.ERROR, details);
    }
}
