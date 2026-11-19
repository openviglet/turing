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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * T1330 — a site's time zone is an IANA id or an offset; blank is the server's.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSiteTimeZoneTest {

    @Test
    void anIanaIdOrAnOffsetIsAZone() {
        assertThat(TurSiteTimeZone.of(" America/Sao_Paulo ")).contains(ZoneId.of("America/Sao_Paulo"));
        assertThat(TurSiteTimeZone.of("-03:00")).contains(ZoneId.of("-03:00"));
        assertThat(TurSiteTimeZone.validated("UTC")).isEqualTo("UTC");
    }

    @Test
    void blankIsTheServersAndNonsenseIsRefused() {
        assertThat(TurSiteTimeZone.of(null)).isEmpty();
        assertThat(TurSiteTimeZone.validated("  ")).isNull();
        assertThat(TurSiteTimeZone.of("Mars/Olympus")).isEmpty();
        assertThatThrownBy(() -> TurSiteTimeZone.validated("Mars/Olympus"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
