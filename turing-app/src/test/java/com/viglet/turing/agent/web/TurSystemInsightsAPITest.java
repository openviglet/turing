package com.viglet.turing.agent.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.viglet.turing.agent.tool.TurSystemInfoToolService;
import com.viglet.turing.llm.TurLlmSummaryService;
import com.viglet.turing.llm.TurLlmSummaryService.SummaryResult;

/** T1311 — the System Information insights, moved out of the system module. */
@ExtendWith(MockitoExtension.class)
class TurSystemInsightsAPITest {

    @Mock
    private TurLlmSummaryService llmSummaryService;
    @Mock
    private TurSystemInfoToolService systemInfoToolService;

    @Test
    void insightsAreGeneratedFromTheSystemStatus() {
        SummaryResult ok = org.mockito.Mockito.mock(SummaryResult.class);
        when(ok.success()).thenReturn(true);
        when(systemInfoToolService.getSystemStatus()).thenReturn("heap: 1G");
        when(llmSummaryService.generate(eq("system-info-insights"), anyString(), anyString(), eq(false)))
                .thenReturn(ok);

        var response = new TurSystemInsightsAPI(llmSummaryService, systemInfoToolService).getInsights(false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ok);
    }

    @Test
    void noDefaultLlmAnswersBadRequest() {
        SummaryResult missing = org.mockito.Mockito.mock(SummaryResult.class);
        when(missing.success()).thenReturn(false);
        when(missing.content()).thenReturn(null);
        when(missing.error()).thenReturn("No default LLM configured");
        when(systemInfoToolService.getSystemStatus()).thenReturn("heap: 1G");
        when(llmSummaryService.generate(eq("system-info-insights"), anyString(), anyString(), eq(true)))
                .thenReturn(missing);

        var response = new TurSystemInsightsAPI(llmSummaryService, systemInfoToolService).getInsights(true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
