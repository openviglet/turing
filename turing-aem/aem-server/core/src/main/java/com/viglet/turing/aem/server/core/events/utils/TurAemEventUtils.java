package com.viglet.turing.aem.server.core.events.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viglet.turing.aem.server.core.events.beans.TurAemPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class TurAemEventUtils {
    public static void index(String path) {
        index(Collections.singletonList(path));
    }
    public static void index(List<String> pathList) {
        try {
            Optional.ofNullable(new ObjectMapper()
                            .writeValueAsString(TurAemPayload
                                    .builder()
                                    .paths(pathList)
                                    .build()))
                    .ifPresent(TurAemEventUtils::indexContent);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
    public static void indexContent(String payload) {
        HttpPost post = Optional.of("http://localhost:30130/api/v2/aem/reindex/WKND-AUTHOR")
                .map(HttpPost::new)
                .orElse(null);
        StringEntity entity = new StringEntity(payload, "UTF-8");
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/json");
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpResponse response = httpClient.execute(post);
            log.error("Response Body: {}", response.getEntity().toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
