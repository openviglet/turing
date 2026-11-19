/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.nl;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.util.StringUtils;

/**
 * T1330 — an SN site's time zone, the zone "today", "tomorrow" and "this weekend"
 * are resolved in. Stored as an IANA id ({@code America/Sao_Paulo}); blank means the
 * server's zone.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSiteTimeZone {

    private TurSiteTimeZone() {
    }

    /** The zone {@code id} names, or empty when it is blank or names none. */
    public static Optional<ZoneId> of(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZoneId.of(id.trim()));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    /**
     * {@code id} trimmed, {@code null} when blank.
     *
     * @throws IllegalArgumentException when it names no time zone (HTTP 400)
     */
    public static String validated(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        return of(id).map(zone -> id.trim())
                .orElseThrow(() -> new IllegalArgumentException("Unknown time zone: " + id.trim()));
    }
}
