package com.viglet.turing.aem.server.core.events;

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
    public static final String JCR_CONTENT = "jcr:content";
    @Reference
    private TurAemIndexerService turAemIndexerService;

    @Override
    public void handleEvent(Event event) {
        log.info("Turing Log Resource Event: {}", event);
        Object path = event.getProperty(SlingConstants.PROPERTY_PATH);
        Object resourceType = event.getProperty(SlingConstants.PROPERTY_RESOURCE_TYPE);
        if (path == null || resourceType == null || !isAssetEvent((String) path, (String) resourceType))
            return;
        TurAemEventUtils.index(turAemIndexerService.getConfig(), (String) path);
    }

    protected boolean isAssetEvent(String path, String resourceType) {
        return ((Objects.equals(resourceType, DAM_ASSET) || Objects.equals(resourceType, DAM_ASSET_CONTENT)) &&
                path.startsWith(CONTENT) &&
                !path.contains(JCR_CONTENT));
    }

}
