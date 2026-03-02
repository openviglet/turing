package com.viglet.turing.client.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

class TurOcrTest {

    @Test
    void shouldExposeExpectedConstants() {
        assertThat(TurOcr.TIMEOUT_MINUTES).isEqualTo(5);
        assertThat(TurOcr.API_OCR_URL).contains("/api/ocr/url");
        assertThat(TurOcr.API_OCR_FILE).contains("/api/ocr/file");
        assertThat(TurOcr.FILE).isEqualTo("file");
        assertThat(TurOcr.URL).isEqualTo("url");
    }

    @Test
    void shouldCreateConnectionPoolOnConstruction() throws Exception {
        TurOcr turOcr = new TurOcr();
        Field poolField = TurOcr.class.getDeclaredField("pool");
        poolField.setAccessible(true);

        assertThat(poolField.get(turOcr)).isNotNull();
    }

    @Test
    void shouldBuildJsonRequestEntityFromUrlUsingPrivateHelper() throws Exception {
        Method method = TurOcr.class.getDeclaredMethod("getRequestEntity", URI.class);
        method.setAccessible(true);

        StringEntity entity = (StringEntity) method.invoke(null, URI.create("https://example.org/doc.pdf"));

        assertThat(entity.getContentType()).contains("application/json");
    }

    @Test
    void shouldBuildMultipartRequestEntityFromFileUsingPrivateHelper() throws Exception {
        Method method = TurOcr.class.getDeclaredMethod("getRequestEntity", File.class);
        method.setAccessible(true);

        HttpEntity entity = (HttpEntity) method.invoke(null, new File("example.pdf"));

        assertThat(entity).isNotNull();
        assertThat(entity.getContentType()).isNotNull();
    }
}
