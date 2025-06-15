package com.viglet.turing.spring.logging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TurIndexingLoggingStatus {

   public static void setStatus(TurLoggingStatus status) {
      log.info("{}", status);

   }
}
