package com.viglet.turing.commons.logging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurLoggingIndexingLog {
   private TurLoggingIndexingLog() {
      throw new IllegalStateException("Log Ingestion Utility");
   }
   public static void setStatus(TurLoggingIndexing status) {
      log.info("{}", status);

   }
}
