package com.viglet.turing.connector.commons.domain;

import com.viglet.turing.commons.indexing.TurIndexingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Builder
@Getter
@Setter
public class TurConnectorIndexing {


    private int id;
    private String objectId;
    private String source;
    private String environment;
    private String transactionId;
    private String checksum;
    private Locale locale;
    private Date created;
    private Date modificationDate;
    private TurIndexingStatus status;
    private List<String> sites;
}
