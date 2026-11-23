/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.sn.api;

import java.util.LinkedHashMap;
import java.util.Map;

import com.viglet.turing.searchengine.spi.TurSEIndexField;
import com.viglet.turing.searchengine.spi.TurSESite;
import com.viglet.turing.searchengine.spi.TurSESiteLocale;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteField;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;

/**
 * T1310 — the one place a site, a site locale or a site's fields become what the
 * search-engine plugin contract takes, so the search-engine module reads no sn
 * entity (ADR 0005 §6). Each conversion is {@code null}-safe: {@code null} in,
 * {@code null} out.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSNSiteRefs {

    private TurSNSiteRefs() {
    }

    /** The site as the search engine sees it, or {@code null} for a {@code null} site. */
    public static TurSESite toSE(TurSNSite site) {
        return site == null ? null : new TurSESite(site.getId(), site.getName(), site.getTurSEInstanceId());
    }

    /** The site locale as the search engine sees it, or {@code null} for a {@code null} locale. */
    public static TurSESiteLocale toSE(TurSNSiteLocale siteLocale) {
        return siteLocale == null ? null
                : new TurSESiteLocale(toSE(siteLocale.getTurSNSite()), siteLocale.getLanguage(),
                        siteLocale.getCore());
    }

    /**
     * The site's fields by name, as the search engine reads them when it projects
     * an exported document; {@code null} for a {@code null} map.
     */
    public static Map<String, TurSEIndexField> toSE(Map<String, TurSNSiteField> siteFields) {
        if (siteFields == null) {
            return null;
        }
        Map<String, TurSEIndexField> fields = new LinkedHashMap<>();
        siteFields.forEach((name, field) -> fields.put(name, field == null ? null
                : new TurSEIndexField(field.getName(), field.getType(), field.getMultiValued() == 1)));
        return fields;
    }
}
