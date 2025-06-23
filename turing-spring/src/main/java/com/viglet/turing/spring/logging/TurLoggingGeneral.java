package com.viglet.turing.spring.logging;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Date;

@Slf4j
@Builder
@Getter
@Setter
public class TurLoggingGeneral implements Serializable {
    private String clusterNode;
    private String level;
    private String logger;
    private String message;
    private String stackTrace;
    @JsonSerialize(using = IsoDateSerializer.class)
    private Date date;
}
