package com.viglet.turing.sn.persistence.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T1353 — a site that stands for a row by id must not read as unsaved: Hibernate
 * treats a null {@code @Version} as a new entity and refuses it as a query
 * parameter, which failed every SN search.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNSiteReferenceTest {

    @Test
    void aReferenceCarriesItsIdAndAVersionSoItIsNotUnsaved() {
        TurSNSite site = TurSNSite.reference("site-1");

        assertThat(site.getId()).isEqualTo("site-1");
        assertThat(site.getVersion()).isNotNull();
    }

    @Test
    void aSiteBuiltForSavingStillStartsWithoutAVersion() {
        // T1319 — the wrapper's null is what makes an imported site persist, not merge.
        assertThat(new TurSNSite().getVersion()).isNull();
    }
}
