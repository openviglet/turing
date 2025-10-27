package com.viglet.turing.api.queue;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TurQueueMessageTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();
        
        TurQueueMessage message = TurQueueMessage.builder()
                .messageId("msg-123")
                .content("Test content")
                .timestamp(now)
                .deliveryCount(1)
                .type("TEXT")
                .size(1024)
                .build();

        assertEquals("msg-123", message.getMessageId());
        assertEquals("Test content", message.getContent());
        assertEquals(now, message.getTimestamp());
        assertEquals(1, message.getDeliveryCount());
        assertEquals("TEXT", message.getType());
        assertEquals(1024, message.getSize());
    }

    @Test
    void testNoArgsConstructor() {
        TurQueueMessage message = new TurQueueMessage();
        assertNotNull(message);
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        TurQueueMessage message = new TurQueueMessage("id1", "content1", now, 2, "JSON", 2048);
        
        assertEquals("id1", message.getMessageId());
        assertEquals("content1", message.getContent());
        assertEquals(now, message.getTimestamp());
        assertEquals(2, message.getDeliveryCount());
        assertEquals("JSON", message.getType());
        assertEquals(2048, message.getSize());
    }

    @Test
    void testSettersAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        TurQueueMessage message = new TurQueueMessage();
        
        message.setMessageId("msg-456");
        message.setContent("Updated content");
        message.setTimestamp(now);
        message.setDeliveryCount(5);
        message.setType("XML");
        message.setSize(512);

        assertEquals("msg-456", message.getMessageId());
        assertEquals("Updated content", message.getContent());
        assertEquals(now, message.getTimestamp());
        assertEquals(5, message.getDeliveryCount());
        assertEquals("XML", message.getType());
        assertEquals(512, message.getSize());
    }

    @Test
    void testDeliveryCountIncrement() {
        TurQueueMessage message = TurQueueMessage.builder()
                .deliveryCount(0)
                .build();
        
        assertEquals(0, message.getDeliveryCount());
        
        message.setDeliveryCount(message.getDeliveryCount() + 1);
        assertEquals(1, message.getDeliveryCount());
        
        message.setDeliveryCount(message.getDeliveryCount() + 1);
        assertEquals(2, message.getDeliveryCount());
    }
}
