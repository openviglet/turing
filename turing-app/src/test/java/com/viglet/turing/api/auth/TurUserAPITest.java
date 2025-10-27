package com.viglet.turing.api.auth;

import com.viglet.turing.persistence.model.auth.TurUser;
import com.viglet.turing.persistence.repository.auth.TurUserRepository;
import com.viglet.turing.persistence.repository.auth.TurGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.jmx.enabled=true")
@AutoConfigureMockMvc
class TurUserAPITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TurUserRepository turUserRepository;

    @Autowired
    private TurGroupRepository turGroupRepository;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testTurUserList() throws Exception {
        mockMvc.perform(get("/api/v2/user"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testTurUserStructure() throws Exception {
        mockMvc.perform(get("/api/v2/user/model"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testTurUserCurrent() throws Exception {
        mockMvc.perform(get("/api/v2/user/current"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testTurUserEdit() throws Exception {
        // Check if admin user exists
        TurUser adminUser = turUserRepository.findByUsername("admin");
        if (adminUser != null) {
            mockMvc.perform(get("/api/v2/user/admin"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCannotDeleteAdminUser() throws Exception {
        mockMvc.perform(delete("/api/v2/user/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
