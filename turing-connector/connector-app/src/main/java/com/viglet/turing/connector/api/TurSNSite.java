package com.viglet.turing.connector.api;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TurSNSite implements Serializable {
    private String name;
    private TurSEInstance turSEInstance;
}
