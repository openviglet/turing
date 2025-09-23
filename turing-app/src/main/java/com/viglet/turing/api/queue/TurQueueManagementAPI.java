/*
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.api.queue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for managing Artemis queues.
 *
 * @author Alexandre Oliveira
 */
@Slf4j
@RestController
@RequestMapping("/api/artemis")
@Tag(name = "Artemis Queue Management", description = "Artemis Queue Management API")
public class TurQueueManagementAPI {
    
    private final TurQueueManagementService queueManagementService;
    
    public TurQueueManagementAPI(TurQueueManagementService queueManagementService) {
        this.queueManagementService = queueManagementService;
    }
    
    @Operation(summary = "List all queues", 
               description = "Get information about all existing Artemis queues")
    @GetMapping
    public ResponseEntity<List<TurQueueInfo>> listQueues() {
        try {
            List<TurQueueInfo> queues = queueManagementService.getAllQueues();
            return ResponseEntity.ok(queues);
        } catch (Exception e) {
            log.error("Error listing queues", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(summary = "Get queue messages", 
               description = "Get messages from a specific queue")
    @GetMapping("/{queueName}/messages")
    public ResponseEntity<List<TurQueueMessage>> getQueueMessages(
            @Parameter(description = "Queue name") @PathVariable String queueName,
            @Parameter(description = "Maximum number of messages to retrieve") 
            @RequestParam(defaultValue = "50") int maxMessages) {
        try {
            List<TurQueueMessage> messages = queueManagementService.getQueueMessages(queueName, maxMessages);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting messages from queue {}", queueName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(summary = "Pause queue", 
               description = "Pause message consumption for a specific queue")
    @PostMapping("/{queueName}/pause")
    public ResponseEntity<Map<String, Object>> pauseQueue(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        try {
            boolean success = queueManagementService.pauseQueue(queueName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Queue paused successfully" : "Failed to pause queue");
            response.put("queueName", queueName);
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error pausing queue {}", queueName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error pausing queue: " + e.getMessage());
            response.put("queueName", queueName);
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(summary = "Resume queue", 
               description = "Resume message consumption for a specific queue")
    @PostMapping("/{queueName}/resume")
    public ResponseEntity<Map<String, Object>> resumeQueue(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        try {
            boolean success = queueManagementService.resumeQueue(queueName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Queue resumed successfully" : "Failed to resume queue");
            response.put("queueName", queueName);
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error resuming queue {}", queueName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error resuming queue: " + e.getMessage());
            response.put("queueName", queueName);
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(summary = "Start queue", 
               description = "Start/resume message consumption for a specific queue (alias for resume)")
    @PostMapping("/{queueName}/start")
    public ResponseEntity<Map<String, Object>> startQueue(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        return resumeQueue(queueName);
    }
    
    @Operation(summary = "Stop queue", 
               description = "Stop/pause message consumption for a specific queue (alias for pause)")
    @PostMapping("/{queueName}/stop")
    public ResponseEntity<Map<String, Object>> stopQueue(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        return pauseQueue(queueName);
    }
    
    @Operation(summary = "Clear queue", 
               description = "Remove all messages from a specific queue")
    @DeleteMapping("/{queueName}/messages")
    public ResponseEntity<Map<String, Object>> clearQueue(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        try {
            boolean success = queueManagementService.clearQueue(queueName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Queue cleared successfully" : "Failed to clear queue");
            response.put("queueName", queueName);
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error clearing queue {}", queueName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error clearing queue: " + e.getMessage());
            response.put("queueName", queueName);
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(summary = "Get queue info", 
               description = "Get detailed information about a specific queue")
    @GetMapping("/{queueName}")
    public ResponseEntity<TurQueueInfo> getQueueInfo(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        try {
            List<TurQueueInfo> queues = queueManagementService.getAllQueues();
            TurQueueInfo queueInfo = queues.stream()
                    .filter(queue -> queueName.equals(queue.getName()))
                    .findFirst()
                    .orElse(null);
            
            if (queueInfo != null) {
                return ResponseEntity.ok(queueInfo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting queue info for {}", queueName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}