package com.viglet.turing.sn.web.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.viglet.turing.searchengine.TurSearchEnginePlugin;
import com.viglet.turing.searchengine.TurSearchEnginePluginFactory;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.repository.TurSNSiteRepository;
import com.viglet.turing.sn.api.TurSNSiteRefs;

/** The DSL query reaches the site's engine through the plugin contract (T1310). */
@ExtendWith(MockitoExtension.class)
class TurSNSiteQueryAPITest {

    private MockMvc mockMvc;

    @Mock
    private TurSNSiteRepository turSNSiteRepository;

    @Mock
    private TurSearchEnginePluginFactory pluginFactory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TurSNSiteQueryAPI(turSNSiteRepository, pluginFactory))
                .build();
    }

    @Test
    void testTurSNSiteSearchSelectGet_Found() throws Exception {
        TurSNSite site = new TurSNSite();
        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.nativeJsonQuery(TurSNSiteRefs.toSE(site), Locale.US, "{\"query\":\"test\"}"))
                .thenReturn(Optional.of("{\"result\":\"ok\"}"));

        mockMvc.perform(post("/api/sn/site1/query")
                .param("_setlocale", "en_US")
                .content("{\"query\":\"test\"}")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"result\":\"ok\"}"));
    }

    @Test
    void anEngineWithoutANativeQueryAnswersAnEmptyObject() throws Exception {
        TurSNSite site = new TurSNSite();
        TurSearchEnginePlugin plugin = mock(TurSearchEnginePlugin.class);
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.of(site));
        when(pluginFactory.getPluginForSeInstance(com.viglet.turing.sn.api.TurSNSites.seInstanceIdOf(site))).thenReturn(plugin);
        when(plugin.nativeJsonQuery(any(), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/sn/site1/query")
                .param("_setlocale", "en_US")
                .content("{\"query\":\"test\"}")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void testTurSNSiteSearchSelectGet_NotFound() throws Exception {
        when(turSNSiteRepository.findByNameIgnoreCase("site1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/sn/site1/query")
                .param("_setlocale", "en_US")
                .content("{\"query\":\"test\"}")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));

        verifyNoInteractions(pluginFactory);
    }
}
