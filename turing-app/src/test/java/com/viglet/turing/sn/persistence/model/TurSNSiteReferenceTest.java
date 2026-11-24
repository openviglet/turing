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

    /** T1355 — a request body's site (an id, no version) becomes a reference; else the path's. */
    @Test
    void referenceToPrefersTheBodySiteThenTheFallbackAndElseNull() {
        TurSNSite fromBody = new TurSNSite();
        fromBody.setId("body-site");

        TurSNSite named = TurSNSite.referenceTo(fromBody, "path-site");
        assertThat(named.getId()).isEqualTo("body-site");
        assertThat(named.getVersion()).isNotNull();
        assertThat(TurSNSite.referenceTo(null, "path-site").getId()).isEqualTo("path-site");
        assertThat(TurSNSite.referenceTo(new TurSNSite(), "path-site").getId()).isEqualTo("path-site");
        assertThat(TurSNSite.referenceTo(null, null)).isNull();
    }

    /** T1355 — an update payload's site never replaces the managed site of a locale or field. */
    @Test
    void anUpdateKeepsTheLocaleAndFieldOwnSite() {
        TurSNSite managed = TurSNSite.reference("managed-site");
        TurSNSite fromBody = new TurSNSite();
        fromBody.setId("body-site");

        var locale = new com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale();
        locale.setTurSNSite(managed);
        var localePayload = new com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale();
        localePayload.setTurSNSite(fromBody);
        org.mapstruct.factory.Mappers
                .getMapper(com.viglet.turing.sn.persistence.mapper.locale.TurSNSiteLocaleMapper.class)
                .updateEntity(localePayload, locale);
        assertThat(locale.getTurSNSite()).isSameAs(managed);

        var field = new com.viglet.turing.sn.persistence.model.field.TurSNSiteField();
        field.setTurSNSite(managed);
        var fieldPayload = new com.viglet.turing.sn.persistence.model.field.TurSNSiteField();
        fieldPayload.setTurSNSite(fromBody);
        org.mapstruct.factory.Mappers
                .getMapper(com.viglet.turing.sn.persistence.mapper.field.TurSNSiteFieldMapper.class)
                .updateEntity(fieldPayload, field);
        assertThat(field.getTurSNSite()).isSameAs(managed);
    }

    @Test
    void aSiteBuiltForSavingStillStartsWithoutAVersion() {
        // T1319 — the wrapper's null is what makes an imported site persist, not merge.
        assertThat(new TurSNSite().getVersion()).isNull();
    }
}
