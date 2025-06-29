package com.viglet.turing.connector.api;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Locale;

@Getter
@Setter
public class TurSNSiteLocale implements Serializable {
    private Locale language;
    private String core;
    private TurSNSite turSNSite;
}