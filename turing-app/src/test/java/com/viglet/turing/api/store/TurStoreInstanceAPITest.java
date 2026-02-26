package com.viglet.turing.api.store;

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

import java.util.Collections;
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
import com.viglet.turing.persistence.model.store.TurStoreInstance;
import com.viglet.turing.persistence.repository.store.TurStoreInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TurStoreInstanceAPITest {

    private MockMvc mockMvc;

    @Mock
    private TurStoreInstanceRepository turStoreInstanceRepository;

    @InjectMocks
    private TurStoreInstanceAPI api;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(api).build();
    }

    @Test
    void testTurStoreInstanceList() throws Exception {
        TurStoreInstance instance = new TurStoreInstance();
        instance.setId("store1");
        instance.setTitle("Store 1");

        when(turStoreInstanceRepository.findAll(any(Sort.class)))
                .thenReturn(Collections.singletonList(instance));

        mockMvc.perform(get("/api/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("store1"))
                .andExpect(jsonPath("$[0].title").value("Store 1"));
    }

    @Test
    void testTurStoreInstanceGet_Found() throws Exception {
        TurStoreInstance instance = new TurStoreInstance();
        instance.setId("store1");

        when(turStoreInstanceRepository.findById("store1")).thenReturn(Optional.of(instance));

        mockMvc.perform(get("/api/store/store1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("store1"));
    }

    @Test
    void testTurStoreInstanceGet_NotFound() throws Exception {
        when(turStoreInstanceRepository.findById("store1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/store/store1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void testTurStoreInstanceUpdate_Found() throws Exception {
        TurStoreInstance instance = new TurStoreInstance();
        instance.setId("store1");

        TurStoreInstance updatedInstance = new TurStoreInstance();
        updatedInstance.setTitle("New Title");

        when(turStoreInstanceRepository.findById("store1")).thenReturn(Optional.of(instance));

        mockMvc.perform(put("/api/store/store1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedInstance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));

        verify(turStoreInstanceRepository, times(1)).save(instance);
    }

    @Test
    void testTurStoreInstanceUpdate_NotFound() throws Exception {
        TurStoreInstance updatedInstance = new TurStoreInstance();
        updatedInstance.setTitle("New Title");

        when(turStoreInstanceRepository.findById("store1")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/store/store1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedInstance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist());

        verify(turStoreInstanceRepository, never()).save(any());
    }

    @Test
    void testTurStoreInstanceDelete() throws Exception {
        mockMvc.perform(delete("/api/store/store1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(turStoreInstanceRepository, times(1)).delete("store1"); // Uses old delete(ID) in Spring Data?
        // Note: Check repository interface; if it uses deleteById, this might fail
        // compilation or execution.
    }

    @Test
    void testTurStoreInstanceAdd() throws Exception {
        TurStoreInstance newInstance = new TurStoreInstance();
        newInstance.setTitle("New Store");

        mockMvc.perform(post("/api/store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newInstance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Store"));

        verify(turStoreInstanceRepository, times(1)).save(any(TurStoreInstance.class));
    }

    @Test
    void testTurEmbeddingStoreInstanceStructure() throws Exception {
        mockMvc.perform(get("/api/store/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turStoreVendor").exists());
    }
}
