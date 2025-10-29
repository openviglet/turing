/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.plugin.aem.service;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.JSON;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.viglet.turing.connector.aem.commons.context.TurAemConfiguration;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Reactive utilities for AEM JSON processing using Spring WebFlux
 * 
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@Slf4j
@Component
public class TurAemReactiveUtils {

    private final TurAemReactiveHttpService reactiveHttpService;

    public TurAemReactiveUtils(TurAemReactiveHttpService reactiveHttpService) {
        this.reactiveHttpService = reactiveHttpService;
    }

    /**
     * Reactively gets infinity JSON for the given URL and context
     * 
     * @param url                 the URL path
     * @param turAemSourceContext the source context
     * @return Mono containing JSONObject or empty if not found/invalid
     */
    public Mono<JSONObject> getInfinityJsonReactive(String url, TurAemConfiguration turAemSourceContext) {
        String infinityJsonUrl = String.format(url.endsWith(JSON) ? "%s%s" : "%s%s.infinity.json",
                turAemSourceContext.getUrl(), url);

        log.debug("Getting infinity JSON reactively for: {}", infinityJsonUrl);

        return reactiveHttpService.fetchResponseBodyReactive(infinityJsonUrl, turAemSourceContext)
                .filter(StringUtils::isNotBlank)
                .flatMap(responseBody -> {
                    try {
                        if (isResponseBodyJSONArray(responseBody) && !url.endsWith(JSON)) {
                            // Handle JSON array response by recursively getting first item
                            JSONArray jsonArray = new JSONArray(responseBody);
                            if (!jsonArray.isEmpty()) {
                                String firstItem = jsonArray.toList().getFirst().toString();
                                return getInfinityJsonReactive(firstItem, turAemSourceContext);
                            }
                        } else if (isResponseBodyJSONObject(responseBody)) {
                            return Mono.just(new JSONObject(responseBody));
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        log.warn("Error parsing JSON response from {}: {}", infinityJsonUrl, e.getMessage());
                        return Mono.empty();
                    }
                })
                .doOnNext(jsonObject -> log.debug("Successfully got infinity JSON for: {}", infinityJsonUrl))
                .doOnError(error -> log.warn("Request failed for {}: {}", infinityJsonUrl, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Request not found or failed: {}", infinityJsonUrl);
                    return Mono.empty();
                });
    }

    private static boolean isResponseBodyJSONArray(String responseBody) {
        try {
            new JSONArray(responseBody);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isResponseBodyJSONObject(String responseBody) {
        try {
            new JSONObject(responseBody);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}