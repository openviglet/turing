package com.viglet.turing.rag.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.viglet.turing.rag.urlfetch.TurUrlFetchService;
import com.viglet.turing.rag.urlfetch.TurUrlFetchService.TurBrowserlessStatus;

/** T1311 — the probe kept its Global Settings path when it moved into rag. */
@ExtendWith(MockitoExtension.class)
class TurUrlFetchStatusAPITest {

    @Mock
    private TurUrlFetchService turUrlFetchService;

    @Test
    void theBrowserlessProbeAnswersAtItsGlobalSettingsPath() throws Exception {
        when(turUrlFetchService.checkBrowserless()).thenReturn(new TurBrowserlessStatus(false, null, "down"));

        MockMvcBuilders.standaloneSetup(new TurUrlFetchStatusAPI(turUrlFetchService)).build()
                .perform(get("/api/system/global-settings/url-fetch/browserless/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
