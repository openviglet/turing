package com.viglet.turing.searchengine.lucene;

import java.util.Arrays;
import java.util.List;

import com.viglet.turing.searchengine.spi.TurSESearchField;
import com.viglet.turing.searchengine.spi.TurSESiteSettings;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.ranking.TurSNSiteSearchConfigProvider;

/**
 * T1310 — reads the site and field fixtures the Lucene tests build as sn entities
 * the way the search SPI hands them to the engine.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurLuceneSpiFixtures {

    private TurLuceneSpiFixtures() {
    }

    /** The site's settings as the SPI answers them. */
    public static TurSESiteSettings settings(TurSNSite site) {
        return site == null ? null : TurSNSiteSearchConfigProvider.toSettings(site);
    }

    /** The fields as the SPI answers them. */
    public static List<TurSESearchField> fields(TurSNSiteFieldExt... fields) {
        return fields(Arrays.asList(fields));
    }

    /** The fields as the SPI answers them. */
    public static List<TurSESearchField> fields(List<TurSNSiteFieldExt> fields) {
        return fields.stream().map(TurSNSiteSearchConfigProvider::toSearchField).toList();
    }

    /** The fields as the SPI answers them, each one highlighted. */
    public static List<TurSESearchField> hlFields(TurSNSiteFieldExt... fields) {
        return fields(fields).stream()
                .map(field -> new TurSESearchField(field.name(), field.type(), field.facet(), 1,
                        field.facetPosition(), field.facetItemType(), field.searchWeight()))
                .toList();
    }
}
