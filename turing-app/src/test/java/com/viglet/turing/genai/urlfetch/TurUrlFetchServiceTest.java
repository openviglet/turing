/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.genai.urlfetch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.net.httpserver.HttpServer;
import com.viglet.turing.commons.file.TurFileAttributes;

/**
 * T739 / §XLVIII — unit tests for the mode dispatch + AUTO escalation logic of
 * {@link TurUrlFetchService}, exercised through the package-private
 * {@code dispatch(...)} so no network access or SSRF guard is involved.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@ExtendWith(MockitoExtension.class)
class TurUrlFetchServiceTest {

    @Mock
    private TurUrlFetcher simpleFetcher;
    @Mock
    private TurUrlFetcher headlessFetcher;
    @Mock
    private TurUrlFetchConfigResolver configResolver;

    private TurUrlFetchService service;
    private URL url;

    @BeforeEach
    void setUp() throws Exception {
        when(simpleFetcher.getBackend()).thenReturn(TurUrlFetchMode.SIMPLE);
        when(headlessFetcher.getBackend()).thenReturn(TurUrlFetchMode.HEADLESS);
        service = new TurUrlFetchService(List.of(simpleFetcher, headlessFetcher), configResolver);
        url = URI.create("https://example.com/").toURL();
    }

    private static TurFileAttributes text(String content) {
        return TurFileAttributes.builder().content(content).build();
    }

    private static TurUrlFetchConfig config(TurUrlFetchMode mode, String browserlessUrl) {
        return new TurUrlFetchConfig(mode, browserlessUrl, "", 30, 200);
    }

    @Test
    void simpleModeUsesSimpleAndNeverHeadless() {
        lenient().when(simpleFetcher.fetch(any(), any())).thenReturn(text("plenty of extracted text"));

        TurFileAttributes result = service.dispatch(url, config(TurUrlFetchMode.SIMPLE, ""));

        assertThat(result.getContent()).isEqualTo("plenty of extracted text");
        verify(headlessFetcher, never()).fetch(any(), any());
    }

    @Test
    void headlessModeUsesHeadlessWhenSidecarConfigured() {
        when(headlessFetcher.fetch(any(), any())).thenReturn(text("rendered body"));

        TurFileAttributes result = service.dispatch(url,
                config(TurUrlFetchMode.HEADLESS, "http://browserless:3000"));

        assertThat(result.getContent()).isEqualTo("rendered body");
        verify(simpleFetcher, never()).fetch(any(), any());
    }

    @Test
    void headlessModeFallsBackToSimpleWhenNoSidecar() {
        when(simpleFetcher.fetch(any(), any())).thenReturn(text("http body"));

        TurFileAttributes result = service.dispatch(url, config(TurUrlFetchMode.HEADLESS, ""));

        assertThat(result.getContent()).isEqualTo("http body");
        verify(headlessFetcher, never()).fetch(any(), any());
    }

    @Test
    void autoKeepsSimpleWhenItYieldsEnoughText() {
        String big = "x".repeat(500);
        when(simpleFetcher.fetch(any(), any())).thenReturn(text(big));

        TurFileAttributes result = service.dispatch(url,
                config(TurUrlFetchMode.AUTO, "http://browserless:3000"));

        assertThat(result.getContent()).isEqualTo(big);
        verify(headlessFetcher, never()).fetch(any(), any());
    }

    @Test
    void autoEscalatesToHeadlessWhenSimpleTooShort() {
        when(simpleFetcher.fetch(any(), any())).thenReturn(text("tiny")); // < 200 chars
        String rendered = "y".repeat(800);
        when(headlessFetcher.fetch(any(), any())).thenReturn(text(rendered));

        TurFileAttributes result = service.dispatch(url,
                config(TurUrlFetchMode.AUTO, "http://browserless:3000"));

        assertThat(result.getContent()).isEqualTo(rendered);
        verify(headlessFetcher).fetch(any(), any());
    }

    @Test
    void autoStaysSimpleWhenShortButNoSidecar() {
        when(simpleFetcher.fetch(any(), any())).thenReturn(text("tiny"));

        TurFileAttributes result = service.dispatch(url, config(TurUrlFetchMode.AUTO, ""));

        assertThat(result.getContent()).isEqualTo("tiny");
        verify(headlessFetcher, never()).fetch(any(), any());
    }

    /** A local server where /old answers the given status pointing at /new, and /new serves the body. */
    private static HttpServer redirectingServer(int status, String location) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/old", exchange -> {
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.createContext("/new", exchange -> {
            byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static URI at(HttpServer server, String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Test
    void rawFetchFollowsAMovedPermanentlyRedirect() throws Exception {
        HttpServer server = redirectingServer(301, "/new");
        try {
            String body = service.fetchRawFollowingRedirects(at(server, "/old"), Duration.ofSeconds(5),
                    u -> true);
            assertThat(body).isEqualTo("{\"ok\":true}");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rawFetchRefusesARedirectTheGuardBlocks() throws Exception {
        HttpServer server = redirectingServer(301, "/new");
        try {
            String body = service.fetchRawFollowingRedirects(at(server, "/old"), Duration.ofSeconds(5),
                    u -> u.endsWith("/old"));
            assertThat(body).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rawFetchGivesUpOnARedirectLoop() throws Exception {
        HttpServer server = redirectingServer(302, "/old");
        try {
            String body = service.fetchRawFollowingRedirects(at(server, "/old"), Duration.ofSeconds(5),
                    u -> true);
            assertThat(body).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void autoKeepsSimpleWhenHeadlessReturnsLessText() {
        when(simpleFetcher.fetch(any(), any())).thenReturn(text("short but real")); // 14 chars
        when(headlessFetcher.fetch(any(), any())).thenReturn(text("")); // render failed

        TurFileAttributes result = service.dispatch(url,
                config(TurUrlFetchMode.AUTO, "http://browserless:3000"));

        assertThat(result.getContent()).isEqualTo("short but real");
    }
}
