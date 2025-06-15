package com.viglet.turing.logging;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.spring.logging.TurIndexingLoggingStatus;
import com.viglet.turing.spring.logging.TurLoggingStatus;

import java.util.Date;

public class TurLoggingUtils {
    public static void setLoggingStatus(TurSNJobItem turSNJobItem,
                                         TurIndexingStatus status) {
        TurIndexingLoggingStatus.setStatus(TurLoggingStatus
                .builder()
                .contentId(turSNJobItem.getId())
                .environment(turSNJobItem.getEnvironment())
                .locale(turSNJobItem.getLocale())
                .status(status)
                .transactionId(null)
                .checksum(turSNJobItem.getChecksum())
                .source("Server")
                .modificationDate(new Date())
                .sites(turSNJobItem.getSiteNames())
                .build());
    }
}
