package com.viglet.turing.artemis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.ObjectNameBuilder;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.jms.client.ActiveMQObjectMessage;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.commons.exception.TurRuntimeException;

import jakarta.annotation.Nullable;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;

@Service
public class TurQueueBrowserService {

    private final MBeanServer mbeanServer;
    private final JmsTemplate jmsTemplate;

    public TurQueueBrowserService(MBeanServer mbeanServer, JmsTemplate jmsTemplate) {
        this.mbeanServer = mbeanServer;
        this.jmsTemplate = jmsTemplate;
    }

    public List<Map<String, Object>> listItemsInQueue(String queueName) {
        try {
            SimpleString address = SimpleString.of(queueName);
            SimpleString qName = SimpleString.of(queueName);
            RoutingType routingType = RoutingType.ANYCAST;

            ObjectNameBuilder builder = ObjectNameBuilder.DEFAULT;
            ObjectName queueObjectName = builder.getQueueObjectName(address, qName, routingType);

            QueueControl queueControl = MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
                    queueObjectName, QueueControl.class, false);

            Map<String, Object>[] messages = queueControl.listMessages("");
            return Arrays.asList(messages);
        } catch (Exception e) {
            throw new TurRuntimeException("Error listing items from queue: " + queueName, e);
        }
    }

    public List<String> listAllQueues() {
        try {
            String objectNameString = "org.apache.activemq.artemis:broker=\"localhost\"";
            ObjectName brokerObjectName = new ObjectName(objectNameString);

            ActiveMQServerControl serverControl = MBeanServerInvocationHandler.newProxyInstance(
                    mbeanServer, brokerObjectName, ActiveMQServerControl.class, false);

            return Arrays.asList(serverControl.getQueueNames());
        } catch (Exception e) {
            throw new TurRuntimeException("Error listing all queues from broker.", e);
        }
    }

    public List<String> browseQueueContents(String queueName, int maxMessages) {
        return jmsTemplate.browse(queueName, new BrowserCallback<List<String>>() {
            @Override
            public List<String> doInJms(@Nullable Session session, @Nullable QueueBrowser browser) throws JMSException {
                List<String> contents = new ArrayList<>();

                if (browser == null) {
                    return contents;
                }

                // 1. Pegamos a Enumeration bruta
                Enumeration<?> enumeration = browser.getEnumeration();

                // 2. Convertemos para Iterator (ainda de objetos desconhecidos)
                Iterator<?> iterator = enumeration.asIterator();

                int count = 0;
                // 3. Processamos manualmente para garantir total segurança de tipos
                while (iterator.hasNext() && count < maxMessages) {
                    Object nextObj = iterator.next();

                    // Verificamos se o objeto é realmente uma Message antes do processamento
                    if (nextObj instanceof Message message) {
                        processMessage(message, contents);
                    }
                    count++;
                }

                return contents;
            }

            private void processMessage(Message message, List<String> contents) {
                try {
                    // Uso de Pattern Matching (Java 16+) para evitar cast manual de
                    // ActiveMQObjectMessage
                    if (message instanceof ActiveMQObjectMessage activeMQMsg) {
                        Object obj = activeMQMsg.getObject();
                        if (obj instanceof TurSNJobItems jobItems) {
                            jobItems.getTuringDocuments().forEach(doc -> contents.add(doc.getId()));
                        }
                    } else {
                        contents.add("Unsupported message type: " + message.getClass().getSimpleName());
                    }
                } catch (JMSException e) {
                    contents.add("Error reading message: " + e.getMessage());
                }
            }
        });
    }
}
