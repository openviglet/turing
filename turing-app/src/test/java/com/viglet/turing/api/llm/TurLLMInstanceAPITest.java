package com.viglet.turing.api.llm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.persistence.repository.llm.TurLLMInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TurLLMInstanceAPITest {

    private MockMvc mockMvc;

    @Mock
    private TurLLMInstanceRepository turLLMInstanceRepository;

    @InjectMocks
    private TurLLMInstanceAPI turLLMInstanceAPI;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(turLLMInstanceAPI).build();
    }

    @Test
    void testTurLLMInstanceList() throws Exception {
        TurLLMInstance instance1 = new TurLLMInstance();
        instance1.setId("1");
        instance1.setTitle("Instance 1");

        TurLLMInstance instance2 = new TurLLMInstance();
        instance2.setId("2");
        instance2.setTitle("Instance 2");

        List<TurLLMInstance> instances = Arrays.asList(instance1, instance2);
        when(turLLMInstanceRepository.findAll(any(Sort.class))).thenReturn(instances);

        mockMvc.perform(get("/api/llm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].title").value("Instance 1"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].title").value("Instance 2"));

        verify(turLLMInstanceRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    void testTurLLMInstanceStructure() throws Exception {
        mockMvc.perform(get("/api/llm/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.turLLMVendor").exists());
    }

    @Test
    void testTurLLMInstanceGet_Found() throws Exception {
        TurLLMInstance instance = new TurLLMInstance();
        instance.setId("1");
        instance.setTitle("Instance 1");

        when(turLLMInstanceRepository.findById("1")).thenReturn(Optional.of(instance));

        mockMvc.perform(get("/api/llm/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("Instance 1"));

        verify(turLLMInstanceRepository, times(1)).findById("1");
    }

    @Test
    void testTurLLMInstanceGet_NotFound() throws Exception {
        when(turLLMInstanceRepository.findById("1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/llm/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());

        verify(turLLMInstanceRepository, times(1)).findById("1");
    }

    @Test
    void testTurLLMInstanceUpdate_Found() throws Exception {
        TurLLMInstance existingInstance = new TurLLMInstance();
        existingInstance.setId("1");
        existingInstance.setTitle("Old Title");

        TurLLMInstance updatedInstance = new TurLLMInstance();
        updatedInstance.setTitle("New Title");
        updatedInstance.setDescription("New Desc");
        updatedInstance.setUrl("http://newurl");
        updatedInstance.setEnabled(1);
        updatedInstance.setModelName("llama2");

        when(turLLMInstanceRepository.findById("1")).thenReturn(Optional.of(existingInstance));

        mockMvc.perform(put("/api/llm/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedInstance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.description").value("New Desc"))
                .andExpect(jsonPath("$.url").value("http://newurl"))
                .andExpect(jsonPath("$.enabled").value(1))
                .andExpect(jsonPath("$.modelName").value("llama2"));

        verify(turLLMInstanceRepository, times(1)).findById("1");
        verify(turLLMInstanceRepository, times(1)).save(any(TurLLMInstance.class));
    }

    @Test
    void testTurLLMInstanceUpdate_NotFound() throws Exception {
        TurLLMInstance updatedInstance = new TurLLMInstance();
        updatedInstance.setTitle("New Title");

        when(turLLMInstanceRepository.findById("1")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/llm/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedInstance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());

        verify(turLLMInstanceRepository, times(1)).findById("1");
        verify(turLLMInstanceRepository, never()).save(any(TurLLMInstance.class));
    }

    @Test
    void testTurLLMInstanceDelete() throws Exception {
        mockMvc.perform(delete("/api/llm/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(turLLMInstanceRepository, times(1)).delete("1");
    }

    @Test
    void testTurLLMInstanceAdd() throws Exception {
        TurLLMInstance newInstance = new TurLLMInstance();
        newInstance.setTitle("New Instance");

        mockMvc.perform(post("/api/llm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newInstance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Instance"));

        verify(turLLMInstanceRepository, times(1)).save(any(TurLLMInstance.class));
    }
}
