package com.viglet.turing.artemis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import com.viglet.turing.commons.exception.TurRuntimeException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.ObjectNameBuilder;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.jms.client.ActiveMQObjectMessage;
import org.jspecify.annotations.NonNull;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import jakarta.jms.JMSException;
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

    public List<String> browseQueueContents(String queueName) {
        return jmsTemplate.browse(queueName, new BrowserCallback<List<String>>() {
            @Override
            public List<String> doInJms(@NonNull Session session, @NonNull QueueBrowser browser) throws JMSException {
                List<String> contents = new ArrayList<>();
                @SuppressWarnings("unchecked")
                Enumeration<jakarta.jms.Message> messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    jakarta.jms.Message message = messages.nextElement();

                    if (message instanceof ActiveMQObjectMessage) {
                        TurSNJobItems obj =
                                (TurSNJobItems) ((ActiveMQObjectMessage) message).getObject();
                        obj.getTuringDocuments().forEach(doc -> contents.add(doc.getId()));
                    } else {
                        contents.add("Unsupported message type.");
                    }
                }
                return contents;
            }
        });
    }
}

