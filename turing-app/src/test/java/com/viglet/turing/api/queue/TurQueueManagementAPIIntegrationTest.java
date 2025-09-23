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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test class for TurQueueManagementAPI.
 *
 * @author Alexandre Oliveira
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TurQueueManagementAPIIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    private static final String API_URL = "/api/artemis";
    
    @BeforeAll
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Test
    @Order(1)
    @DisplayName("List all queues - should return JSON array")
    void testListQueues() throws Exception {
        MvcResult result = mockMvc.perform(get(API_URL)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        String content = result.getResponse().getContentAsString();
        log.info("Queues response: {}", content);
        
        // Parse response to ensure it's a valid JSON array
        List<?> queues = objectMapper.readValue(content, List.class);
        Assertions.assertNotNull(queues);
        log.info("Found {} queues", queues.size());
    }
    
    @Test
    @Order(2)
    @DisplayName("Get messages from indexing.queue")
    void testGetQueueMessages() throws Exception {
        MvcResult result = mockMvc.perform(get(API_URL + "/indexing.queue/messages")
                .param("maxMessages", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        String content = result.getResponse().getContentAsString();
        log.info("Queue messages response: {}", content);
        
        // Parse response to ensure it's a valid JSON array
        List<?> messages = objectMapper.readValue(content, List.class);
        Assertions.assertNotNull(messages);
        log.info("Found {} messages in queue", messages.size());
    }
    
    @Test
    @Order(3)
    @DisplayName("Test pause and resume queue operations")
    void testPauseResumeQueue() throws Exception {
        // Test pause
        MvcResult pauseResult = mockMvc.perform(post(API_URL + "/indexing.queue/pause")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        
        String pauseContent = pauseResult.getResponse().getContentAsString();
        log.info("Pause response: {}", pauseContent);
        
        // Test resume
        MvcResult resumeResult = mockMvc.perform(post(API_URL + "/indexing.queue/resume")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        
        String resumeContent = resumeResult.getResponse().getContentAsString();
        log.info("Resume response: {}", resumeContent);
    }
    
    @Test
    @Order(4)
    @DisplayName("Get specific queue info")
    void testGetQueueInfo() throws Exception {
        // This test might return 404 if the queue doesn't exist, which is fine
        MvcResult result = mockMvc.perform(get(API_URL + "/indexing.queue")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Accept both 200 (OK) and 404 (Not Found) as valid responses
        int status = result.getResponse().getStatus();
        Assertions.assertTrue(status == 200 || status == 404, 
                "Expected status 200 or 404, but got " + status);
        
        String content = result.getResponse().getContentAsString();
        log.info("Queue info response (status: {}): {}", 
                 result.getResponse().getStatus(), content);
    }
    
    @Test
    @Order(5)
    @DisplayName("Test clear queue operation")
    void testClearQueue() throws Exception {
        MvcResult result = mockMvc.perform(delete(API_URL + "/indexing.queue/messages")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        
        String content = result.getResponse().getContentAsString();
        log.info("Clear queue response: {}", content);
    }
}