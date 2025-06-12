package com.viglet.turing.connector.commons.plugin.dto;

import com.viglet.turing.connector.commons.plugin.TurConnectorStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Builder
@Getter
@Setter
public class TurConnectorIndexingDTO {


    private int id;
    private String objectId;
    private String name;
    private String environment;
    private String transactionId;
    private String checksum;
    private Locale locale;
    private Date created;
    private Date modificationDate;
    private TurConnectorStatus status;
    private List<String> sites;
}
