package com.viglet.turing.api.queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.jmx.enabled=true")
class TurQueueManagementServiceTest {

    @Autowired
    private TurQueueManagementService turQueueManagementService;

    @Test
    void testGetAllQueues() {
        List<TurQueueInfo> queues = turQueueManagementService.getAllQueues();
        assertNotNull(queues);
    }

    @Test
    void testGetQueueMessages() {
        // Get messages from a non-existent queue should return empty list
        List<TurQueueMessage> messages = turQueueManagementService.getQueueMessages("non-existent-queue", 10);
        assertNotNull(messages);
    }

    @Test
    void testPauseQueue() {
        // Pausing a non-existent queue should return false
        boolean result = turQueueManagementService.pauseQueue("non-existent-queue");
        assertFalse(result);
    }

    @Test
    void testResumeQueue() {
        // Resuming a non-existent queue should return false
        boolean result = turQueueManagementService.resumeQueue("non-existent-queue");
        assertFalse(result);
    }

    @Test
    void testClearQueue() {
        // Clearing a non-existent queue should return false
        boolean result = turQueueManagementService.clearQueue("non-existent-queue");
        assertFalse(result);
    }
}
