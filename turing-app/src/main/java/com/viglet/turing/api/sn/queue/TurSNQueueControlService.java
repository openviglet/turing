package com.viglet.turing.api.sn.queue;

import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.viglet.turing.sn.TurSNConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TurSNQueueControlService {
    private final JmsListenerEndpointRegistry registry;
    private boolean suspendedQueue = false;
    private boolean solrServiceAvailable = true;

    public TurSNQueueControlService(JmsListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    public void suspendQueue(String listenerId) {
        registry.getListenerContainerIds().forEach(log::info);
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container != null && container.isRunning()) {
            container.stop();
            log.error("Queue listener '" + listenerId + "' SUSPENDED.");
            startMonitoring(listenerId);
        }
    }

    public void startQueue(String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container != null && !container.isRunning()) {
            container.start();
            log.info("Queue listener '" + listenerId + "' STARTED.");
        }
    }

    public void startMonitoring(String listenerId) {
        this.suspendedQueue = true;
    }

    @Scheduled(fixedDelay = 60000)
    public void checkTuring() {
        if (suspendedQueue) {
            if (solrServiceAvailable) {
                startQueue(TurSNConstants.INDEXING_QUEUE_LISTENER);
                suspendedQueue = false;
            }
        }
    }

}
