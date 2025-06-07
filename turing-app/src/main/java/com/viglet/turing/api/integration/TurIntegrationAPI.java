/*
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.viglet.turing.api.integration;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.viglet.turing.persistence.model.integration.TurIntegrationInstance;
import com.viglet.turing.persistence.repository.integration.TurIntegrationInstanceRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v2/integration/{integrationId}")
@Tag(name = "AEM API", description = "AEM API")
public class TurIntegrationAPI {
    public static final String PUT = "PUT";
    public static final String POST = "POST";
    private final TurIntegrationInstanceRepository turIntegrationInstanceRepository;

    @Inject
    TurIntegrationAPI(TurIntegrationInstanceRepository turIntegrationInstanceRepository) {
        this.turIntegrationInstanceRepository = turIntegrationInstanceRepository;
    }

    @RequestMapping(value = "**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public void indexAnyRequest(HttpServletRequest request, HttpServletResponse response,
                                @PathVariable String integrationId) {
        turIntegrationInstanceRepository.findById(integrationId).ifPresent(turIntegrationInstance ->
                proxy(turIntegrationInstance, request, response));
    }

    public void proxy(TurIntegrationInstance turIntegrationInstance, HttpServletRequest request,
                      HttpServletResponse response) {
        try {
            String endpoint = turIntegrationInstance.getEndpoint() +
                    request.getRequestURI()
                            .replace("/api/v2/integration/" + turIntegrationInstance.getId(), "/api/v2");
            log.info("Executing: {}", endpoint);
            HttpURLConnection connectorEnpoint = (HttpURLConnection) URI.create(endpoint)
                    .toURL().openConnection();
            connectorEnpoint.setRequestMethod(request.getMethod());
            request.getHeaderNames().asIterator().forEachRemaining(headerName ->
                    connectorEnpoint.setRequestProperty(headerName, request.getHeader(headerName)));
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
            connectorEnpoint.getHeaderFields()
                    .forEach((header, values) ->
                            values.forEach(value -> {
                                if (header !=null && !header.equals(HttpHeaders.TRANSFER_ENCODING)) {
                                    log.debug("Header: {} = {}", header, value);
                                    response.setHeader(header, value);
                                }
                            }));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
