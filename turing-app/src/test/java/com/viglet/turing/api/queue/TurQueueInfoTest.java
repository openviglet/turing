package com.viglet.turing.api.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurQueueInfoTest {

    @Test
    void testBuilder() {
        TurQueueInfo queueInfo = TurQueueInfo.builder()
                .name("test-queue")
                .messageCount(10)
                .consumerCount(2)
                .paused(false)
                .status("ACTIVE")
                .temporary(false)
                .address("test.address")
                .build();

        assertEquals("test-queue", queueInfo.getName());
        assertEquals(10, queueInfo.getMessageCount());
        assertEquals(2, queueInfo.getConsumerCount());
        assertFalse(queueInfo.isPaused());
        assertEquals("ACTIVE", queueInfo.getStatus());
        assertFalse(queueInfo.isTemporary());
        assertEquals("test.address", queueInfo.getAddress());
    }

    @Test
    void testNoArgsConstructor() {
        TurQueueInfo queueInfo = new TurQueueInfo();
        assertNotNull(queueInfo);
    }

    @Test
    void testAllArgsConstructor() {
        TurQueueInfo queueInfo = new TurQueueInfo("queue1", 5, 1, true, "PAUSED", true, "addr1");
        
        assertEquals("queue1", queueInfo.getName());
        assertEquals(5, queueInfo.getMessageCount());
        assertEquals(1, queueInfo.getConsumerCount());
        assertTrue(queueInfo.isPaused());
        assertEquals("PAUSED", queueInfo.getStatus());
        assertTrue(queueInfo.isTemporary());
        assertEquals("addr1", queueInfo.getAddress());
    }

    @Test
    void testSettersAndGetters() {
        TurQueueInfo queueInfo = new TurQueueInfo();
        
        queueInfo.setName("new-queue");
        queueInfo.setMessageCount(15);
        queueInfo.setConsumerCount(3);
        queueInfo.setPaused(true);
        queueInfo.setStatus("PAUSED");
        queueInfo.setTemporary(true);
        queueInfo.setAddress("new.address");

        assertEquals("new-queue", queueInfo.getName());
        assertEquals(15, queueInfo.getMessageCount());
        assertEquals(3, queueInfo.getConsumerCount());
        assertTrue(queueInfo.isPaused());
        assertEquals("PAUSED", queueInfo.getStatus());
        assertTrue(queueInfo.isTemporary());
        assertEquals("new.address", queueInfo.getAddress());
    }
}
