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
    @Reference
    private TurAemIndexerService turAemIndexerService;

    @Override
    public void handleEvent(Event event) {
        if (!isAssetEvent(event)) return;
        TurAemEventUtils.index(turAemIndexerService.getConfig(),
                event.getProperty(SlingConstants.PROPERTY_PATH).toString());
    }

    protected boolean isAssetEvent(Event event) {
        String path = event.getProperty(SlingConstants.PROPERTY_PATH).toString();
        String resourceType = event.getProperty(SlingConstants.PROPERTY_RESOURCE_TYPE).toString();
        return (Objects.equals(resourceType, "dam:Asset") &&
                path.startsWith("/content") &&
                !path.contains("jcr:content"));
    }


}
