/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.insights;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.viglet.turing.sn.persistence.repository.metric.TurSearchTermPrivacy;

import lombok.extern.slf4j.Slf4j;

/**
 * T1298 — hands {@code turing.search.metrics.mask-personal-data} to
 * {@link TurSearchTermPrivacy}, which the metric entities' setters call and so
 * cannot have it injected.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
public class TurSearchTermPrivacyConfiguration {

    public TurSearchTermPrivacyConfiguration(
            @Value("${turing.search.metrics.mask-personal-data:true}") boolean maskPersonalData) {
        TurSearchTermPrivacy.setEnabled(maskPersonalData);
        if (!maskPersonalData) {
            log.info("Search metrics keep personal data in search terms (turing.search.metrics.mask-personal-data=false)");
        }
    }
}
