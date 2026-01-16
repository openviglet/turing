/*
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.api.integration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Paths;

import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.viglet.turing.persistence.model.integration.TurIntegrationInstance;
import com.viglet.turing.persistence.repository.integration.TurIntegrationInstanceRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v2/integration/{integrationId}")
@Tag(name = "AEM API", description = "AEM API")
public class TurIntegrationAPI {
    public static final String PUT = "PUT";
    public static final String POST = "POST";
    private final TurIntegrationInstanceRepository turIntegrationInstanceRepository;

    TurIntegrationAPI(TurIntegrationInstanceRepository turIntegrationInstanceRepository) {
        this.turIntegrationInstanceRepository = turIntegrationInstanceRepository;
    }

    @RequestMapping(value = "**", method = { RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public void indexAnyRequest(HttpServletRequest request, HttpServletResponse response,
            @PathVariable String integrationId) {
        turIntegrationInstanceRepository.findById(integrationId).ifPresent(
                turIntegrationInstance -> proxy(turIntegrationInstance, request, response));
    }

    public void proxy(TurIntegrationInstance turIntegrationInstance, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String endpoint = turIntegrationInstance.getEndpoint() + request.getRequestURI()
                    .replace("/api/v2/integration/" + turIntegrationInstance.getId(), "/api/v2");
            log.debug("Executing: {}", endpoint);
            URI baseUri = URI.create(turIntegrationInstance.getEndpoint());
            URI fullUri = URI.create(endpoint);
            // SSRF Mitigation: Only allow requests to the same host and scheme as the
            // registered
            // endpoint
            if (!baseUri.getHost().equalsIgnoreCase(fullUri.getHost())
                    || !baseUri.getScheme().equalsIgnoreCase(fullUri.getScheme())) {
                log.warn("Blocked SSRF attempt: attempted host={}, scheme={}", fullUri.getHost(),
                        fullUri.getScheme());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Forbidden proxy target\"}");
                return;
            }
            // Validate that the path is safe and does not contain traversal or forbidden
            // segments
            if (!isValidProxyPath(fullUri.getPath())) {
                log.warn("Blocked SSRF attempt: invalid or unauthorized path: {}",
                        fullUri.getPath());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Forbidden proxy path\"}");
                return;
            }
            HttpURLConnection connectorEnpoint = (HttpURLConnection) fullUri.toURL().openConnection();
            connectorEnpoint.setRequestMethod(request.getMethod());
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> connectorEnpoint
                    .setRequestProperty(headerName, request.getHeader(headerName)));
            String method = request.getMethod();
            if (method.equals(PUT) || method.equals(POST)) {
                connectorEnpoint.setDoOutput(true);
                OutputStream outputStream = connectorEnpoint.getOutputStream();
                outputStream.write(CharStreams.toString(request.getReader()).getBytes());
                outputStream.flush();
                outputStream.close();
            }
            response.setStatus(connectorEnpoint.getResponseCode());
            ByteStreams.copy(connectorEnpoint.getInputStream(), response.getOutputStream());
            connectorEnpoint.getHeaderFields().forEach((header, values) -> values.forEach(value -> {
                if (header != null && !header.equals(HttpHeaders.TRANSFER_ENCODING)) {
                    log.debug("Header: {} = {}", header, value);
                    response.setHeader(header, value);
                }
            }));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Validates that the proxy path is safe: normalized, does not contain directory
     * traversal, and
     * starts with the expected API prefix.
     */
    private boolean isValidProxyPath(String path) {
        if (path == null) {
            return false;
        }
        // Normalize the path first to eliminate any ./ or ../ segments
        String normalized = Paths.get(path).normalize().toString().replace('\\', '/');

        // Ensure the normalized path still starts with the expected API prefix.
        // This prevents proxying to arbitrary internal endpoints.
        if (!normalized.startsWith("/api/")) {
            return false;
        }

        // Disallow any attempts at directory traversal in the normalized path
        if (normalized.contains("..")) {
            return false;
        }

        // Optionally, restrict characters in the path to a safe subset
        // (alphanumeric, slash, dash, underscore, dot).
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '/' || c == '-' || c == '_' || c == '.')) {
                return false;
            }
        }

        return true;
    }
}