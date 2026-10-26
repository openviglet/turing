package com.viglet.turing.commons.se;

import java.io.Serializable;

/**
 * T1133 — one partition of a shared SN site: the documents whose {@code field}
 * holds {@code value}. Set by the server from the API key a request carries,
 * never read from the request, and applied by every engine as a filter the
 * facet renderer never shows.
 *
 * @param field the field that partitions the site (e.g. {@code site_key})
 * @param value the value the partition's documents carry in it
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public record TurSEPartition(String field, String value) implements Serializable {

    /** The partition as one {@code field:value} filter query. */
    public String filterQuery() {
        return field + ":" + value;
    }

    /** The partition as the search and click logs record it: {@code field=value}. */
    public String key() {
        return field + "=" + value;
    }
}
