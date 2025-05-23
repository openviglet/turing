package com.viglet.turing.connector.plugin.aem.export.bean;

import lombok.*;

import java.util.Locale;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TurAemSourceLocalePathExchange {
    private String snSite;
    private Locale locale;
    private String path;
}
