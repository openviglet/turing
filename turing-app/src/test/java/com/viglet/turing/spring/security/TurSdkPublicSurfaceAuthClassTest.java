package com.viglet.turing.spring.security;

import static com.viglet.turing.spring.security.TurSecurityConfigProduction.publicMatchers;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

import com.viglet.turing.spring.security.auth.TurSNSiteApiKeyGateFilter;

/**
 * T1118 — every Semantic Navigation endpoint the vanilla SDK calls is anonymous
 * in a {@code PUBLIC} site and reached by the per-site API-key gate, unless it is
 * named below as deliberately authenticated.
 *
 * <p>{@code related-terms} shipped in the SDK beside search and spell-check and
 * answered a browser 401, because nothing compared the SDK's paths with the
 * filter chain. This reads the paths out of the SDK's own source, so a call added
 * there without its matcher fails here.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSdkPublicSurfaceAuthClassTest {

    private static final Path SDK_API = Path.of("../frontend/packages/sdk/src/api.ts");

    /** {@code `/sn/${site}/...`}, up to the query string or the closing backtick. */
    private static final Pattern SDK_PATH = Pattern.compile("`(/sn/\\$\\{[^}]+}[^`?]*)");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}]+}");

    /** Calls the SDK makes on the visitor's behalf that must carry a session or a key. */
    private static final Set<String> AUTHENTICATED = Set.of(
            // T889 — minting a permalink is a write: an anonymous one is a spam surface.
            "/api/sn/x/answer/share",
            "/api/sn/x/answer/share/x",
            // T1119 — search insights are admin metrics, called with the site's key.
            "/api/sn/x/metrics/failed-searches",
            "/api/sn/x/metrics/content-gaps");

    private static Set<String> sdkPaths;

    @BeforeAll
    static void readSdk() throws IOException {
        assertThat(SDK_API).as("the vanilla SDK's API module must be where this test looks").exists();
        String source = Files.readString(SDK_API, StandardCharsets.UTF_8);
        sdkPaths = new TreeSet<>();
        Matcher m = SDK_PATH.matcher(source);
        while (m.find()) {
            sdkPaths.add("/api" + PLACEHOLDER.matcher(m.group(1)).replaceAll("x"));
        }
        assertThat(sdkPaths).as("the SDK's SN calls must be found").contains("/api/sn/x/x/related-terms");
    }

    @Test
    void everySdkCallIsAnonymousInAPublicSite() {
        PathPatternParser parser = new PathPatternParser();
        assertThat(sdkPaths).filteredOn(path -> !AUTHENTICATED.contains(path))
                .allSatisfy(path -> assertThat(publicMatchers())
                        .as("%s is called by the SDK without a key, so it must be permitted", path)
                        .anySatisfy(matcher -> assertThat(parser.parse(matcher)
                                .matches(PathContainer.parsePath(path))).isTrue()));
    }

    @Test
    void everySdkCallIsGatedForAnApiKeySite() {
        assertThat(sdkPaths).filteredOn(path -> !AUTHENTICATED.contains(path))
                .allSatisfy(path -> assertThat(TurSNSiteApiKeyGateFilter.gates(path))
                        .as("%s must be checked against an API_KEY-mode site's key", path).isTrue());
    }
}
