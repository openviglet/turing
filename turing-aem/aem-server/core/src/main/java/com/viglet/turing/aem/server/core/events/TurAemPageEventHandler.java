package com.viglet.turing.aem.server.core.events;

import com.day.cq.wcm.api.PageEvent;
import com.day.cq.wcm.api.PageModification;
import com.viglet.turing.aem.server.core.events.utils.TurAemEventUtils;
import com.viglet.turing.aem.server.core.services.TurAemIndexerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component(service = EventHandler.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=Listen to page node changes",
        EventConstants.EVENT_TOPIC + "=" + PageEvent.EVENT_TOPIC
})
public class TurAemPageEventHandler implements EventHandler {
    @Reference
    private TurAemIndexerService turAemIndexerService;
    @Override
    public void handleEvent(Event event) {
        PageEvent pageEvent = PageEvent.fromEvent(event);
        List<String> paths = IteratorUtils
                .toList(pageEvent.getModifications())
                .stream()
                .map(PageModification::getPath)
                .collect(Collectors.toList());
        TurAemEventUtils.index(turAemIndexerService.getConfig(), paths);
    }
}
