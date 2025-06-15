package com.viglet.turing.logging;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.spring.logging.TurLoggingIndexingLog;
import com.viglet.turing.spring.logging.TurLoggingIndexing;

import java.util.Date;

public class TurLoggingUtils {
    public static void setLoggingStatus(TurSNJobItem turSNJobItem,
                                         TurIndexingStatus status) {
        TurLoggingIndexingLog.setStatus(TurLoggingIndexing
                .builder()
                .contentId(turSNJobItem.getId())
                .environment(turSNJobItem.getEnvironment())
                .locale(turSNJobItem.getLocale())
                .status(status)
                .transactionId(null)
                .checksum(turSNJobItem.getChecksum())
                .source("Server")
                .timestamp(new Date())
                .sites(turSNJobItem.getSiteNames())
                .build());
    }
}
