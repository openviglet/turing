package com.viglet.turing.storage.asset;

/**
 * Event published when an asset is uploaded or deleted in MinIO.
 *
 * <p>T1324 — it carries the tenant it was published under ({@code null} when no tenant
 * is bound), so a listener runs in that tenant even when the event publication registry
 * delivers it again after a restart, with no request around it.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
public record TurAssetEvent(Type type, String objectName, String contentType, long size, String tenantId) {

    public enum Type { UPLOADED, DELETED }

    public static TurAssetEvent uploaded(String objectName, String contentType, long size, String tenantId) {
        return new TurAssetEvent(Type.UPLOADED, objectName, contentType, size, tenantId);
    }

    public static TurAssetEvent deleted(String objectName, String tenantId) {
        return new TurAssetEvent(Type.DELETED, objectName, "", 0, tenantId);
    }
}
