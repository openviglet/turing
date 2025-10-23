package com.viglet.turing.aem.server.core.events;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.AgentManager;
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

	private static final String SERIALIZATION_TYPE_DEFAULT = "Default";
	private static final String SERIALIZATION_TYPE_DISPATCHER_FLUSH = "Dispatcher Flush";
	private static final String FLUSH_AGENT_IDENTIFIER = "flush";

	@Reference
	private TurAemIndexerService turAemIndexerService;

	@Reference
	private AgentManager agentManager;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Override
	public void handleEvent(Event event) {
		ReplicationAction action = ReplicationAction.fromEvent(event);

		if (!isValidReplicationAction(action)) {
			return;
		}

		processReplicationAgent(action);
		processReplicationContent(action);
	}

	private boolean isValidReplicationAction(ReplicationAction action) {
		if (action == null) {
			log.warn("ReplicationAction is null for event");
			return false;
		}

		List<String> paths = Arrays.asList(action.getPaths());
		ReplicationActionType type = action.getType();

		if (paths.isEmpty() || type == null) {
			log.warn("ReplicationAction missing paths or type: {}", action);
			return false;
		}

		return true;
	}

	private void processReplicationAgent(ReplicationAction action) {
		AgentConfig agentConfig = action.getConfig();
		if (agentConfig == null) {
			log.debug("No agent config found for replication action");
			return;
		}

		String agentId = agentConfig.getAgentId();
		if (agentId == null) {
			log.debug("No agent ID found in config");
			return;
		}

		try (ResourceResolver resolver = getServiceResourceResolver()) {
			Agent agent = agentManager.getAgents().get(agentId);

			if (agent != null) {
				logAgentSerializationType(agent, action.getPath(), agentId);
			} else {
				log.warn("Replication agent not found for ID: {}", agentId);
			}

			logAgentType(agentId);

		} catch (LoginException e) {
			log.error("Failed to get service resource resolver: {}", e.getMessage(), e);
		} catch (Exception e) {
			log.error("Error processing replication event: {}", e.getMessage(), e);
		}
	}

	private ResourceResolver getServiceResourceResolver() throws LoginException {
		Map<String, Object> serviceParams = Map.of(
				ResourceResolverFactory.SUBSERVICE, "turing-indexer");
		return resourceResolverFactory.getServiceResourceResolver(serviceParams);
	}

	private void logAgentSerializationType(Agent agent, String path, String agentId) {
		String serializationType = agent.getConfiguration().getSerializationType();

		if (serializationType == null) {
			log.warn("Serialization type not found for agent: {}", agentId);
			return;
		}

		switch (serializationType) {
			case SERIALIZATION_TYPE_DISPATCHER_FLUSH:
				log.info("Dispatcher flush replication. Path: {}, Agent: {}", path, agentId);
				break;
			case SERIALIZATION_TYPE_DEFAULT:
				log.info("Page/content replication. Path: {}, Agent: {}", path, agentId);
				break;
			default:
				log.info("Custom replication type ({}). Path: {}, Agent: {}", serializationType, path, agentId);
		}
	}

	private void logAgentType(String agentId) {
		if (agentId.toLowerCase().contains(FLUSH_AGENT_IDENTIFIER)) {
			log.info("Dispatcher flush event for agent: {}", agentId);
		} else {
			log.info("Content replication event for agent: {}", agentId);
		}
	}

	private void processReplicationContent(ReplicationAction action) {
		List<String> paths = Arrays.asList(action.getPaths());
		ReplicationActionType type = action.getType();

		switch (type) {
			case ACTIVATE:
				log.info("Turing published pages event: {}", paths);
				indexPages(paths, TurAemEvent.PUBLISHING);
				break;
			case DEACTIVATE:
				log.info("Turing unpublished pages event: {}", paths);
				indexPages(paths, TurAemEvent.UNPUBLISHING);
				break;
			default:
				log.info("Unhandled replication action type: {}", type);
		}
	}

	private void indexPages(List<String> paths, TurAemEvent event) {
		try {
			TurAemEventUtils.index(turAemIndexerService.getConfig(), paths, event);
		} catch (Exception e) {
			log.error("Error indexing pages: {}", paths, e);
		}
	}
}
