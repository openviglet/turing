package com.viglet.turing.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.jmx.enabled=true")
@AutoConfigureMockMvc
class TurAPITest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testInfoEndpoint() throws Exception {
        mockMvc.perform(get("/api/v2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void testInfoEndpointReturnsMap() throws Exception {
        mockMvc.perform(get("/api/v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }
}
