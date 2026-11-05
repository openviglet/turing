package com.viglet.turing.sn.facet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.contentai.TurContentAiQueryBuilder;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.properties.TurContentAiProperty;

/**
 * T1233 — a negative filter query is read the same way everywhere: a leading
 * {@code -} excludes the field's value, quoted or not; Content AI, which cannot
 * exclude, refuses it instead of matching nothing.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
class TurSNFilterExclusionTest {

    @Test
    void aLeadingHyphenExcludesTheFieldsValue() {
        assertThat(TurSNFilterExclusion.of("-source_apps:wordpress"))
                .contains(new TurSNFilterExclusion("source_apps", "wordpress"));
        assertThat(TurSNFilterExclusion.of("-type:\"Blog post\""))
                .contains(new TurSNFilterExclusion("type", "Blog post"));
        assertThat(TurSNFilterExclusion.of("-type:\"Blog post\"").orElseThrow().positive())
                .isEqualTo("type:Blog post");
    }

    @Test
    void anythingElseIsNoExclusion() {
        assertThat(TurSNFilterExclusion.isExclusion("source_apps:wordpress")).isFalse();
        assertThat(TurSNFilterExclusion.isExclusion("-nocolon")).isFalse();
        assertThat(TurSNFilterExclusion.isExclusion("-:value")).isFalse();
        assertThat(TurSNFilterExclusion.isExclusion("-field:")).isFalse();
        assertThat(TurSNFilterExclusion.isExclusion(null)).isFalse();
    }

    @Test
    void exclusionsAreGatheredFromEveryList() {
        assertThat(TurSNFilterExclusion.in(List.of("site_key:x", "-a:1"), null, List.of("-b:2")))
                .containsExactly(new TurSNFilterExclusion("a", "1"), new TurSNFilterExclusion("b", "2"));
    }

    @Test
    void contentAiRefusesAnExclusion() {
        TurSNSearchParams searchParams = new TurSNSearchParams();
        searchParams.setQ("*");
        searchParams.setFq(List.of("-source_apps:wordpress"));
        TurSEParameters params = new TurSEParameters(searchParams);
        params.setTurSNFilterParams(TurSNFilterParams.builder()
                .defaultValues(List.of("-source_apps:wordpress")).build());

        assertThatThrownBy(() -> new TurContentAiQueryBuilder().buildSearchRequest("aem", new TurSNSite(),
                params, List.of(), null, 10, new TurContentAiProperty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-source_apps:wordpress");
    }
}
