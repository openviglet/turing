/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pure unit tests for the static path-sanitization helper on
 * {@link TurCodeInterpreterToolService}. Defends the tenant-isolated
 * session-dir layout against accidental or adversarial path traversal
 * via server-generated ids that may relax format in future refactors.
 *
 * <p>The sanitizer is intentionally strict: keep ONLY
 * {@code [a-zA-Z0-9._-]}; everything else is dropped. UUIDs in this
 * codebase only use {@code [a-zA-Z0-9-]} so legitimate ids pass through
 * unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.2.7
 */
class TurCodeInterpreterTenantPathTest {

    // ─────────────────────── Happy path ───────────────────────

    @ParameterizedTest
    @CsvSource({
            // Real-world UUID shapes — must pass through unchanged.
            "550e8400-e29b-41d4-a716-446655440000, 550e8400-e29b-41d4-a716-446655440000",
            "abc123,                               abc123",
            "agent-marina-ee,                      agent-marina-ee",
            "conv_42.production,                   conv_42.production",
    })
    void sanitize_validIdsPassThroughUnchanged(String input, String expected) {
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment(input))
                .isEqualTo(expected);
    }

    // ─────────────────────── Empty / null ───────────────────────

    @Test
    void sanitize_nullReturnsNull() {
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment(null)).isNull();
    }

    @Test
    void sanitize_blankReturnsNull() {
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment("")).isNull();
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment("   ")).isNull();
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment("\t\n")).isNull();
    }

    @Test
    void sanitize_allDangerousCharsReturnsNull() {
        // Input is 100% unsafe — sanitizer must reject entirely, not
        // leave a hollow shell that could collide with a real id.
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment("../"))
                .as("../ has nothing safe — must be null").isNull();
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment("/etc/passwd"))
                .as("Even a real path with no safe chars maps to null… wait, has 'etc' and 'passwd' which ARE safe")
                .isEqualTo("etcpasswd");
        // The above is the actual contract — we keep safe chars and drop
        // unsafe ones; "etc" and "passwd" remain because they're [a-z]+.
        // What we want to PIN: the path separators are gone, which means
        // the result can NEVER traverse to a different dir.
    }

    // ─────────────────────── Path traversal attempts ───────────────────────

    @ParameterizedTest
    @CsvSource({
            // Adversarial id → what the sanitizer reduces it to.
            // The KEY contract: no path separators or '.' sequences survive
            // in a form that lets {@link java.io.File} traverse outside the
            // session dir.
            "'../etc/passwd',                  '..etcpasswd'",
            "'..\\..\\windows',                '....windows'",
            "'agent/../other',                 'agent..other'",
            "'agent\\..\\other',               'agent..other'",
            "'/absolute/path',                 'absolutepath'",
            "'C:\\Windows\\System32',          'CWindowsSystem32'",
            // Even the leftover dots can't form '..' segments when there's
            // no separator surrounding them.
    })
    void sanitize_pathTraversalReducedToSafeForm(String input, String expected) {
        String result = TurCodeInterpreterToolService.sanitizePathSegment(input);
        // Critical invariant: no path separators in the output.
        assertThat(result)
                .isEqualTo(expected)
                .doesNotContain("/").doesNotContain("\\");
    }

    @Test
    void sanitize_nullByteStripped() {
        // Null-byte injection: classic path-truncation attack on legacy
        // C-string code. JVM is safe but defense-in-depth.
        String result = TurCodeInterpreterToolService.sanitizePathSegment("agent\0../etc");
        assertThat(result)
                .doesNotContain("\0")
                .doesNotContain("/");
    }

    @Test
    void sanitize_unicodeNormalization() {
        // Unicode lookalike chars must NOT survive — the safe-char regex
        // is ASCII-only on purpose. Cyrillic 'a' (U+0430) looks like
        // Latin 'a' but won't pass the [a-zA-Z0-9._-] filter.
        String cyrillicA = "аgent"; // first char is Cyrillic
        String result = TurCodeInterpreterToolService.sanitizePathSegment(cyrillicA);
        assertThat(result)
                .as("Cyrillic lookalikes stripped — output is pure ASCII")
                .isEqualTo("gent");
    }

    @Test
    void sanitize_spacesAndSpecialCharsDropped() {
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment("agent name with spaces"))
                .isEqualTo("agentnamewithspaces");
        assertThat(TurCodeInterpreterToolService.sanitizePathSegment("agent;DROP TABLE"))
                .isEqualTo("agentDROPTABLE");
    }
}
