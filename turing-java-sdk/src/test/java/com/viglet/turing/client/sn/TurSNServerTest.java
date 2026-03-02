package com.viglet.turing.client.sn;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.viglet.turing.client.auth.credentials.TurApiKeyCredentials;
import com.viglet.turing.client.auth.credentials.TurUsernamePasswordCredentials;

class TurSNServerTest {

    @Test
    void shouldInitializeFromUsernamePasswordConstructor() {
        TurUsernamePasswordCredentials credentials = new TurUsernamePasswordCredentials("john", "secret");

        TurSNServer server = new TurSNServer(
                URI.create("http://localhost:2700"),
                "Portal",
                Locale.US,
                credentials,
                "john");

        assertThat(server.getServerURL()).isEqualTo(URI.create("http://localhost:2700"));
        assertThat(server.getSiteName()).isEqualTo("Portal");
        assertThat(server.getLocale()).isEqualTo(Locale.US);
        assertThat(server.getCredentials()).isSameAs(credentials);
        assertThat(server.getProviderName()).isEqualTo("turing-java-sdk");
        assertThat(server.getSnServer()).isEqualTo("http://localhost:2700/api/sn/Portal");
        assertThat(server.getTurSNSitePostParams().getUserId()).isEqualTo("john");
        assertThat(server.getTurSNSitePostParams().isPopulateMetrics()).isTrue();
    }

    @Test
    void shouldInitializeFromApiKeyConstructorAndReturnEmptyLatestSearchesWithoutCredentials() {
        TurApiKeyCredentials apiKeyCredentials = new TurApiKeyCredentials("my-api-key");

        TurSNServer server = new TurSNServer(
                URI.create("http://localhost:2700"),
                "Portal",
                Locale.US,
                apiKeyCredentials,
                "user-id");

        assertThat(server.getApiKey()).isEqualTo("my-api-key");
        assertThat(server.getCredentials()).isNull();
        assertThat(server.getLatestSearches(5)).isEqualTo(Collections.emptyList());
    }

    @Test
    void shouldUseDefaultSiteAndLocaleWhenUsingShortConstructor() {
        TurSNServer server = new TurSNServer(URI.create("http://localhost:2700"), new TurApiKeyCredentials("k"));

        assertThat(server.getSiteName()).isEqualTo("Sample");
        assertThat(server.getLocale()).isEqualTo(Locale.US);
        assertThat(server.getSnServer()).isEqualTo("http://localhost:2700/api/sn/Sample");
    }
}
