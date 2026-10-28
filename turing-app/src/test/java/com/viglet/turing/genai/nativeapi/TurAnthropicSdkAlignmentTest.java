package com.viglet.turing.genai.nativeapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/**
 * The okhttp client {@code turing-app/pom.xml} pins must be the version of the
 * {@code anthropic-java-core} that spring-ai-anthropic resolves. Dependabot leaves
 * the client alone (T1134), so a Spring AI bump that moves the core leaves the two
 * apart, and the mismatch surfaces as a missing SDK class somewhere else.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurAnthropicSdkAlignmentTest {

    private static final Pattern JAR_VERSION = Pattern.compile("-(\\d+\\.\\d+\\.\\d+[^/]*)\\.jar$");

    private static String jarVersion(Class<?> type) {
        String location = type.getProtectionDomain().getCodeSource().getLocation().getPath();
        Matcher matcher = JAR_VERSION.matcher(location);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Test
    void theOkHttpClientMatchesTheCoreSpringAiResolves() {
        String core = jarVersion(AnthropicClient.class);
        String okhttp = jarVersion(AnthropicOkHttpClient.class);
        assertNotNull(core, "anthropic-java-core is not loaded from a versioned jar");
        assertEquals(core, okhttp, "pin anthropic-java-client-okhttp in turing-app/pom.xml to " + core
                + ", the anthropic-java-core spring-ai-anthropic resolves");
    }
}
