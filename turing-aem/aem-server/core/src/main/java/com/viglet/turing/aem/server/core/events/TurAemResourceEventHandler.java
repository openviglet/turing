package com.viglet.turing.aem.server.core.events;

import com.viglet.turing.aem.server.core.events.beans.TurAemEvent;
import com.viglet.turing.aem.server.core.events.utils.TurAemEventUtils;
import com.viglet.turing.aem.server.core.services.TurAemIndexerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import java.util.*;

@Slf4j
@Component(service = EventHandler.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=Listen to the assets changes",
        EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/*"
})
public class TurAemResourceEventHandler implements EventHandler {
    public static final String DAM_ASSET = "dam:Asset";
    public static final String DAM_ASSET_CONTENT = "dam:AssetContent";
    public static final String CONTENT = "/content";
    public static final String JCR_CONTENT = "/jcr:content";
    @Reference
    private TurAemIndexerService turAemIndexerService;

    @Override
    public void handleEvent(Event event) {
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        String resourceType = (String) event.getProperty(SlingConstants.PROPERTY_RESOURCE_TYPE);
        log.info("Turing Log Resource Event: path={}, resourceType={}", path, resourceType);
        if (path == null || resourceType == null) {
            return;
        }
        if (path.contains(JCR_CONTENT)) {
            path = path.replace(JCR_CONTENT, "");
        }
        if (!isAssetEvent(path, resourceType))
            return;
        // Index the asset path to update the search engine with the latest changes in DAM assets.
        TurAemEventUtils.index(turAemIndexerService.getConfig(), path, TurAemEvent.NONE);
    }

    protected boolean isAssetEvent(String path, String resourceType) {
        return ((Objects.equals(resourceType, DAM_ASSET) || Objects.equals(resourceType, DAM_ASSET_CONTENT)) &&
                path.startsWith(CONTENT));
    }

}
