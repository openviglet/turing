package com.viglet.turing.api.system;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.domain.sn.TurSNSiteDomain;
import com.viglet.turing.domain.sn.TurSNSiteRepositoryPort;
import com.viglet.turing.system.TurLlmSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/summary")
@Tag(name = "Summary", description = "Generic AI-powered summary generation")
@RequiredArgsConstructor
public class TurSummaryAPI {

    private final TurLlmSummaryService llmSummaryService;
    private final TurSNSiteRepositoryPort turSNSiteRepositoryPort;

    @Operation(summary = "Generate an AI summary from provided data")
    @PostMapping
    public ResponseEntity<TurLlmSummaryService.SummaryResult> generate(
            @RequestBody SummaryRequest request,
            @RequestParam(defaultValue = "false") boolean regenerate) {

        TurLlmSummaryService.SummaryResult result = llmSummaryService.generate(
                request.cacheKey(), request.data(), request.systemPrompt(), regenerate, null,
                siteName(request.site()));

        return ResponseEntity.ok(result);
    }

    /**
     * T1150 — the site the spend is recorded against, by the name the cost summary
     * reads. The lookup is tenant-scoped, so a key can only name a site of its own
     * tenant; a partitioned key never reaches this endpoint. A name that resolves
     * to nothing is refused rather than dropped, or the spend would vanish.
     */
    private String siteName(String site) {
        if (site == null || site.isBlank()) {
            return null;
        }
        return turSNSiteRepositoryPort.findByName(site.trim())
                .map(TurSNSiteDomain::name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown SN site '" + site.trim() + "'"));
    }

    /**
     * @param site T1150 — optional SN site name the call's spend is recorded
     *             against, as {@code GET /api/v2/llm/cost/summary?site=} reads it
     */
    public record SummaryRequest(String cacheKey, String data, String systemPrompt, String site) {
    }
}
