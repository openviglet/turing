/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.api.sn.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viglet.turing.commons.sn.bean.TurSNFilterParams;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.sn.TurSNSimilarMode;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * T1239 â€” the JSON body of {@code POST /api/sn/{site}/search/similar/query} and
 * {@code POST /api/sn/{site}/search/duplicates/query}. The filter fields carry the
 * names and aliases of {@link com.viglet.turing.commons.sn.bean.TurSNSearchRequest},
 * so a client builds the scope of a similar call the way it builds a search, without
 * a hand-escaped query string.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurSNSimilarRequest {

    /** The default number of documents answered, as on the GET. */
    public static final int DEFAULT_ROWS = 10;

    /** The default duplicate threshold, as on the GET. */
    public static final double DEFAULT_THRESHOLD = 0.92;

    /** The seed document id. */
    private String id;

    /** Similar documents only: {@code VECTOR} or {@code MLT}; auto-selected when omitted. */
    private TurSNSimilarMode mode;

    /** Duplicates only: minimum similarity for a document to count as a duplicate. */
    private Double threshold;

    /** Number of documents answered (similar) or extra members (duplicates). */
    private Integer rows;

    /** Locale tag, e.g. {@code en_US}. Falls back to the site's default locale. */
    private String locale;

    /** Default filter queries, each a raw {@code field:value} pair. */
    private List<String> fq = new ArrayList<>();

    /** Filter queries combined with AND. */
    @JsonAlias("fq.and")
    private List<String> fqAnd = new ArrayList<>();

    /** Filter queries combined with OR. */
    @JsonAlias("fq.or")
    private List<String> fqOr = new ArrayList<>();

    /** Operator applied between filter-query groups. */
    @JsonAlias("fq.op")
    private TurSNFilterQueryOperator fqOperator;

    /** Operator applied between the values of one field. */
    @JsonAlias({"fqi.op", "fqItemOp"})
    private TurSNFilterQueryOperator fqItemOperator;

    /** The rows asked for, or {@link #DEFAULT_ROWS}. */
    public int rowsOrDefault() {
        return rows == null ? DEFAULT_ROWS : rows;
    }

    /** The threshold asked for, or {@link #DEFAULT_THRESHOLD}. */
    public double thresholdOrDefault() {
        return threshold == null ? DEFAULT_THRESHOLD : threshold;
    }

    /** The locale asked for, or {@code null} so the service uses the site default. */
    public Locale resolvedLocale() {
        return StringUtils.isBlank(locale) ? null : LocaleUtils.toLocale(locale);
    }

    /** The filters, with the search's semantics. */
    public TurSNFilterParams filters() {
        return TurSNFilterParams.builder().defaultValues(emptyToNull(fq)).and(emptyToNull(fqAnd))
                .or(emptyToNull(fqOr)).operator(orNone(fqOperator)).itemOperator(orNone(fqItemOperator))
                .build();
    }

    private static List<String> emptyToNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values;
    }

    private static TurSNFilterQueryOperator orNone(TurSNFilterQueryOperator operator) {
        return operator == null ? TurSNFilterQueryOperator.NONE : operator;
    }
}
