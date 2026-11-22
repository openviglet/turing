package com.viglet.turing.agent.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viglet.turing.agent.persistence.dto.TurAIAgentDto;
import com.viglet.turing.agent.persistence.mapper.TurAIAgentMapper;
import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;
import com.viglet.turing.llm.mcp.persistence.repository.TurMcpServerRepository;
import com.viglet.turing.sn.api.TurSNSiteGenAis;

/**
 * Tests for TurAIAgentAPI.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
@ExtendWith(MockitoExtension.class)
class TurAIAgentAPITest {

    private MockMvc mockMvc;

    @Mock
    private TurAIAgentRepository turAIAgentRepository;

    @Spy
    private TurAIAgentMapper turAIAgentMapper = Mappers.getMapper(TurAIAgentMapper.class);

    @Mock
    private TurLLMInstanceRepository turLLMInstanceRepository;

    @Mock
    private TurMcpServerRepository turMcpServerRepository;

    @Mock
    private TurSNSiteGenAis turSNSiteGenAiRepository;

    @Mock
    private com.viglet.turing.tenant.TurTenantQuotaService turTenantQuotaService;

    @Mock
    private com.viglet.turing.persona.persistence.repository.TurPersonaRepository turPersonaRepository;

    @InjectMocks
    private TurAIAgentAPI turAIAgentAPI;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(turAIAgentAPI)
                .setControllerAdvice(new com.viglet.turing.system.web.exception.TurGlobalExceptionHandler())
                .build();
    }

    @Test
    void testTurAIAgentList() throws Exception {
        TurAIAgent agent1 = new TurAIAgent();
        agent1.setId("1");
        agent1.setTitle("Agent 1");

        TurAIAgent agent2 = new TurAIAgent();
        agent2.setId("2");
        agent2.setTitle("Agent 2");

        when(turAIAgentRepository.findAll(any(Sort.class))).thenReturn(Arrays.asList(agent1, agent2));

        mockMvc.perform(get("/api/ai-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].title").value("Agent 1"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].title").value("Agent 2"));

        verify(turAIAgentRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    void testTurAIAgentStructure() throws Exception {
        mockMvc.perform(get("/api/ai-agent/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void testTurAIAgentGetFound() throws Exception {
        TurAIAgent agent = new TurAIAgent();
        agent.setId("1");
        agent.setTitle("Test Agent");
        agent.setEnabled(1);

        when(turAIAgentRepository.findById("1")).thenReturn(Optional.of(agent));

        mockMvc.perform(get("/api/ai-agent/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("Test Agent"))
                .andExpect(jsonPath("$.enabled").value(1));

        verify(turAIAgentRepository, times(1)).findById("1");
    }

    @Test
    void testTurAIAgentGetNotFound() throws Exception {
        when(turAIAgentRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ai-agent/missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());

        verify(turAIAgentRepository, times(1)).findById("missing");
    }

    @Test
    void testTurAIAgentUpdateFound() throws Exception {
        TurAIAgent existingAgent = new TurAIAgent();
        existingAgent.setId("1");
        existingAgent.setTitle("Old Title");

        TurAIAgentDto updatedDto = new TurAIAgentDto();
        updatedDto.setTitle("New Title");
        updatedDto.setDescription("New Description");
        updatedDto.setEnabled(1);

        when(turAIAgentRepository.findById("1")).thenReturn(Optional.of(existingAgent));
        when(turAIAgentRepository.save(any(TurAIAgent.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/ai-agent/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.description").value("New Description"));

        verify(turAIAgentRepository, times(1)).save(any(TurAIAgent.class));
    }

    /** T1319 — a save made from an older version is refused, not applied over the newer one. */
    @Test
    void aSaveFromAnOlderVersionIsRefusedWithAConflict() throws Exception {
        TurAIAgent existingAgent = new TurAIAgent();
        existingAgent.setId("1");
        existingAgent.setTitle("Changed by someone else");
        existingAgent.setVersion(4L);
        when(turAIAgentRepository.findById("1")).thenReturn(Optional.of(existingAgent));

        mockMvc.perform(put("/api/ai-agent/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Mine\",\"enabled\":1,\"version\":3}"))
                .andExpect(status().isConflict());

        verify(turAIAgentRepository, times(0)).save(any(TurAIAgent.class));
    }

    /** T1319 — the response carries the version of the saved copy, for the next save. */
    @Test
    void aSaveFromTheCurrentVersionAnswersTheSavedVersion() throws Exception {
        TurAIAgent existingAgent = new TurAIAgent();
        existingAgent.setId("1");
        existingAgent.setVersion(4L);
        when(turAIAgentRepository.findById("1")).thenReturn(Optional.of(existingAgent));
        when(turAIAgentRepository.save(any(TurAIAgent.class))).thenAnswer(inv -> {
            TurAIAgent merged = new TurAIAgent();
            merged.setId("1");
            merged.setTitle(((TurAIAgent) inv.getArgument(0)).getTitle());
            merged.setVersion(5L);
            return merged;
        });

        mockMvc.perform(put("/api/ai-agent/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Mine\",\"enabled\":1,\"version\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5));
    }

    @Test
    void testTurAIAgentUpdateNotFound() throws Exception {
        TurAIAgentDto dto = new TurAIAgentDto();
        dto.setTitle("New Title");

        when(turAIAgentRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/ai-agent/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void testTurAIAgentDelete() throws Exception {
        mockMvc.perform(delete("/api/ai-agent/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // T906 — deleteById loads the entity, so @PostRemove fires and the snapshot
        // eviction listener runs; the bulk delete this replaces fired neither.
        verify(turAIAgentRepository, times(1)).deleteById("1");
    }

    @Test
    void testTurAIAgentAdd() throws Exception {
        TurAIAgentDto newAgent = new TurAIAgentDto();
        newAgent.setTitle("New Agent");
        newAgent.setEnabled(1);

        mockMvc.perform(post("/api/ai-agent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAgent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Agent"));

        verify(turAIAgentRepository, times(1)).save(any(TurAIAgent.class));
        // T963 — the plan is consulted on every create, not only when it bites.
        verify(turTenantQuotaService, times(1)).checkCanCreateAgent();
    }

    /** T1315 — a default persona outside the agent's catalog is refused. */
    @Test
    void aDefaultPersonaOutsideTheCatalogIsRefused() throws Exception {
        when(turPersonaRepository.existsById("p1")).thenReturn(true);

        mockMvc.perform(post("/api/ai-agent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"A\",\"enabled\":1,\"defaultPersonaId\":\"p1\"}"))
                .andExpect(status().isBadRequest());

        verify(turAIAgentRepository, times(0)).save(any(TurAIAgent.class));
    }

    /** T1315 — a default persona id no persona has is dropped, not stored. */
    @Test
    void anUnknownDefaultPersonaIdIsDropped() throws Exception {
        when(turPersonaRepository.existsById("ghost")).thenReturn(false);

        mockMvc.perform(post("/api/ai-agent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"A\",\"enabled\":1,\"defaultPersonaId\":\"ghost\"}"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<TurAIAgent> saved = org.mockito.ArgumentCaptor.forClass(TurAIAgent.class);
        verify(turAIAgentRepository).save(saved.capture());
        org.assertj.core.api.Assertions.assertThat(saved.getValue().getDefaultPersonaId()).isNull();
    }

    /**
     * T963 / §LXXII — a tenant at its agent limit is refused before anything is
     * written. The refusal itself is T959's: the exception carries the counts
     * and the exit, and this asserts only that nothing was created.
     */
    @Test
    void testTurAIAgentAddRefusedAtThePlanLimit() throws Exception {
        org.mockito.Mockito.doThrow(new com.viglet.turing.shared.exception.TurQuotaExhaustedException(
                org.springframework.http.HttpStatus.PAYMENT_REQUIRED, "agents", 3L, 3L, null, null,
                "Plan limit reached for 'agents': 3 of 3 used."))
                .when(turTenantQuotaService).checkCanCreateAgent();

        TurAIAgentDto newAgent = new TurAIAgentDto();
        newAgent.setTitle("One Too Many");

        try {
            mockMvc.perform(post("/api/ai-agent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newAgent)));
        } catch (Exception expected) {
            // standaloneSetup has no @ControllerAdvice, so the exception escapes
            // the dispatcher rather than being rendered as the 402 it maps to.
        }

        verify(turAIAgentRepository, org.mockito.Mockito.never()).save(any(TurAIAgent.class));
    }
}
