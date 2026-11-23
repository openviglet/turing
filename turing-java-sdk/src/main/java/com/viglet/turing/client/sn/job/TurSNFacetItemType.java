package com.viglet.turing.client.sn.job;

/**
 * T1223 — how a search combines two or more values picked in the same facet
 * field: {@link #AND} keeps documents that carry every value, {@link #OR} keeps
 * documents that carry any of them, {@link #DEFAULT} defers to the site's
 * setting. Sent with a manifest field as {@code facetItemType}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public enum TurSNFacetItemType {
    DEFAULT,
    AND,
    OR
}
