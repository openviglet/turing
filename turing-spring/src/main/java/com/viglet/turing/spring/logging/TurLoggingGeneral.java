package com.viglet.turing.spring.logging;

import com.viglet.turing.commons.indexing.TurIndexingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Builder
@Getter
@Setter
public class TurLoggingGeneral {
    private Date timestamp;
    private String level;
    private String logger;
    private String message;
}
