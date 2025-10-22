package com.viglet.turing.aem.server.core.events;

import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.viglet.turing.aem.server.core.events.beans.TurAemEvent;
import com.viglet.turing.aem.server.core.events.utils.TurAemEventUtils;
import com.viglet.turing.aem.server.core.services.TurAemIndexerService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = EventHandler.class, immediate = true, property = {
		"event.topics=" + ReplicationAction.EVENT_TOPIC
})
public class TurAemPageReplicationEventHandler implements EventHandler {
	@Reference
	private TurAemIndexerService turAemIndexerService;

	@Override
	public void handleEvent(Event event) {
		ReplicationAction action = ReplicationAction.fromEvent(event);
		if (action == null) {
			log.warn("ReplicationAction is null for event: {}", event);
			return;
		}

		List<String> paths = Arrays.asList(action.getPaths());
		ReplicationActionType type = action.getType();

		if (paths == null || type == null) {
			log.warn("ReplicationAction missing paths or type: {}", action);
			return;
		}

		switch (type) {
			case ACTIVATE:
				log.info("Turing published pages event: {}", paths.toString());
				indexPage(paths, TurAemEvent.PUBLISHING);
				break;
			case DEACTIVATE:
				log.info("Turing unpublished pages event: {}", paths.toString());
				indexPage(paths, TurAemEvent.UNPUBLISHING);
				break;
			default:
				log.debug("Unhandled replication action type: {}", type);
		}
	}

	private void indexPage(List<String> paths, TurAemEvent event) {
		try {
			TurAemEventUtils.index(turAemIndexerService.getConfig(), paths, event);
		} catch (Exception e) {
			log.error("Error indexing pages: {}", paths.toString(), e);
		}
	}
}
