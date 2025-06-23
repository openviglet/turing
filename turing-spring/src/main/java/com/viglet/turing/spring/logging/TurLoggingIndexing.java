package com.viglet.turing.spring.logging;

import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.commons.indexing.TurLoggingStatus;
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
public class TurLoggingIndexing {
    private Date timestamp;
    private TurIndexingStatus status;
    private String source;
    private String contentId;
    private String url;
    private List<String> sites;
    private String environment;
    private Locale locale;
    private String transactionId;
    private String checksum;
    private TurLoggingStatus resultStatus;
    private String details;

}
