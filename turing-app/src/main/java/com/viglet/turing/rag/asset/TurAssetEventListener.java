package com.viglet.turing.rag.asset;

import com.viglet.core.tenancy.VigletTenantContext;
import com.viglet.turing.system.TurGlobalSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import com.viglet.turing.storage.asset.TurAssetEvent;

/**
 * Listens for asset upload/delete events and triggers automatic
 * embedding indexing/deindexing when RAG is enabled.
 *
 * <p>T1324 — an {@link ApplicationModuleListener}: it runs asynchronously after the
 * publishing transaction commits, and the event publication registry keeps the event
 * until this method returns, so an asset uploaded just before a crash is still indexed
 * when {@code TurEventPublicationJob} delivers it again. A failure leaves the
 * publication incomplete for that retry. The work runs in the tenant the event was
 * published under.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
@Component
public class TurAssetEventListener {

    private static final Logger log = LoggerFactory.getLogger(TurAssetEventListener.class);

    private final TurGlobalSettingsService globalSettingsService;
    private final TurAssetTrainingService trainingService;
    private final VigletTenantContext tenantContext;

    public TurAssetEventListener(TurGlobalSettingsService globalSettingsService,
                                  TurAssetTrainingService trainingService,
                                  VigletTenantContext tenantContext) {
        this.globalSettingsService = globalSettingsService;
        this.trainingService = trainingService;
        this.tenantContext = tenantContext;
    }

    @ApplicationModuleListener
    public void handleAssetEvent(TurAssetEvent event) {
        String previous = tenantContext.getCurrentTenant();
        if (event.tenantId() != null) {
            tenantContext.setCurrentTenant(event.tenantId());
        }
        try {
            handle(event);
        } finally {
            if (previous != null) {
                tenantContext.setCurrentTenant(previous);
            } else {
                tenantContext.clear();
            }
        }
    }

    private void handle(TurAssetEvent event) {
        if (!globalSettingsService.isRagEnabled()) {
            log.debug("[AssetEvent] RAG disabled, ignoring {} event for: {}", event.type(), event.objectName());
            return;
        }

        if (event.type() == TurAssetEvent.Type.UPLOADED) {
            log.info("[AssetEvent] File uploaded, indexing: {}", event.objectName());
            trainingService.indexSingleFile(event.objectName(), event.contentType(), event.size());
        } else if (event.type() == TurAssetEvent.Type.DELETED) {
            log.info("[AssetEvent] File deleted, deindexing: {}", event.objectName());
            trainingService.deindexSingleFile(event.objectName());
        }
    }
}
