package com.viglet.turing.connector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TurRestClientConfig {
    public static final String KEY = "Key";
    private final String turingUrl;
    private final String turingApiKey;

    public TurRestClientConfig(@Value("${turing.url:http://localhost:2700}") String turingUrl,
                               @Value("${turing.apiKey}") String turingApiKey) {
        this.turingUrl = turingUrl;
        this.turingApiKey = turingApiKey;
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .baseUrl(turingUrl)
                .defaultHeader(KEY, turingApiKey)
                .build();
    }
}