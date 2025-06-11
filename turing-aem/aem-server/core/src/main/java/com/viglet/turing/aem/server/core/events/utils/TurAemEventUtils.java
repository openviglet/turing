package com.viglet.turing.aem.server.core.events.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viglet.turing.aem.server.config.TurAemIndexerConfig;
import com.viglet.turing.aem.server.core.events.beans.TurAemPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class TurAemEventUtils {

    public static final String API_TURING_AEM_REINDEX = "/api/v2/aem/index/";
    public static final String CONTENT_TYPE = "Content-Type";

    public static void index(TurAemIndexerConfig config, String path) {
        index(config, Collections.singletonList(path));
    }

    public static void index(TurAemIndexerConfig config, List<String> pathList) {
        if (!config.enabled()) {
            return;
        }
        try {
            Optional.ofNullable(new ObjectMapper()
                            .writeValueAsString(TurAemPayload
                                    .builder()
                                    .paths(pathList)
                                    .build()))
                    .ifPresent(payload -> indexContent(config, payload));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void indexContent(TurAemIndexerConfig config, String payload) {
        HttpPost post = Optional.of(config.host() + API_TURING_AEM_REINDEX + config.configName())
                .map(HttpPost::new)
                .orElse(null);
        StringEntity entity = new StringEntity(payload, StandardCharsets.UTF_8);
        post.setEntity(entity);
        post.setHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpResponse response = httpClient.execute(post);
            log.error("Response Body: {}", response.getEntity());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
