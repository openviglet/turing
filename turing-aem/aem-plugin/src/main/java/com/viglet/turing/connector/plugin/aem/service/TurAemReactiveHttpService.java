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

import java.time.Duration;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.google.common.net.UrlEscapers;
import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.connector.aem.commons.context.TurAemSourceContext;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

/**
 * Reactive HTTP service for AEM API calls using Spring WebFlux
 * 
 * @author Alexandre Oliveira
 * @since 2025.3
 */
@Slf4j
@Service
public class TurAemReactiveHttpService {

        private final WebClient webClient;

        public TurAemReactiveHttpService() {
                HttpClient httpClient = HttpClient.create().protocol(HttpProtocol.HTTP11);
                this.webClient = WebClient.builder()
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .codecs(configurer -> configurer.defaultCodecs()
                                                .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                                .build();
        }

        /**
         * Fetches response body reactively for the given URL and context
         * 
         * @param url the URL to fetch
         * @param turAemSourceContext the source context containing credentials
         * @return Mono containing the response body if valid JSON, empty otherwise
         */
        public Mono<String> fetchResponseBodyReactive(String url,
                        TurAemSourceContext turAemSourceContext) {
                log.debug("Making reactive HTTP request to: {}", url);

                String escapedUrl = UrlEscapers.urlFragmentEscaper().escape(url);
                String basicAuth = basicAuth(turAemSourceContext.getUsername(),
                                turAemSourceContext.getPassword());

                return webClient.get().uri(escapedUrl).header(HttpHeaders.AUTHORIZATION, basicAuth)
                                .retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(30))
                                .retry(3).filter(TurCommonsUtils::isValidJson)
                                .doOnNext(responseBody -> log.debug("Valid JSON response from: {}",
                                                url))
                                .doOnError(error -> log.error("Error fetching URL {}: {}", url,
                                                error.getMessage()))
                                .onErrorReturn("")
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                                .maxBackoff(Duration.ofSeconds(5)).jitter(0.5)
                                                .filter(throwable -> throwable instanceof SslHandshakeTimeoutException)
                                                .onRetryExhaustedThrow((retryBackoffSpec,
                                                                retrySignal) -> retrySignal
                                                                                .failure()));
        }

        private String basicAuth(String username, String password) {
                return "Basic " + Base64.getEncoder()
                                .encodeToString((username + ":" + password).getBytes());
        }
}
