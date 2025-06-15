package com.viglet.turing.spring.logging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurLoggingIndexingLog {

   public static void setStatus(TurLoggingIndexing status) {
      log.info("{}", status);

   }
}
