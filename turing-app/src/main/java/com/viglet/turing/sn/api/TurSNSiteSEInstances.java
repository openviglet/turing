/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.api;

import org.springframework.stereotype.Component;

import com.viglet.turing.searchengine.persistence.model.TurSEInstance;
import com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository;
import com.viglet.turing.sn.persistence.model.TurSNSite;

/**
 * T1315 — the SE instance an SN site names. The site holds it by id (the
 * instance belongs to this module); this loads the row. {@code null} when the
 * site names none or the instance is gone.
 *
 * <p>T1310z — lives in sn's api, since it reads an sn entity: searchengine no
 * longer imports sn for it (ADR 0005 section 6).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSNSiteSEInstances {

    private final TurSEInstanceRepository seInstanceRepository;

    public TurSNSiteSEInstances(TurSEInstanceRepository seInstanceRepository) {
        this.seInstanceRepository = seInstanceRepository;
    }

    public TurSEInstance of(TurSNSite site) {
        if (site == null || site.getTurSEInstanceId() == null) {
            return null;
        }
        return seInstanceRepository.findById(site.getTurSEInstanceId()).orElse(null);
    }
}
