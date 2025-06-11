package com.viglet.turing.aem.server.core.events;

import lombok.extern.slf4j.Slf4j;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

@Slf4j
@Component(service = EventHandler.class, immediate = true, property = {
Constants.SERVICE_DESCRIPTION + "=Listen to all org node changes",
EventConstants.EVENT_TOPIC + "=org/*" })
public class TurAemAllOrgEventHandler implements EventHandler {
    @Override
    public void handleEvent(Event event) {
        log.info("Turing Log All Org Event: {}", event);
    }
}
