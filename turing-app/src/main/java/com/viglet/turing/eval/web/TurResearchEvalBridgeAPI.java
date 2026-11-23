/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.eval.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.agent.persistence.dto.TurEvalDatasetDto;
import com.viglet.turing.agent.persistence.model.TurEvalDataset;
import com.viglet.turing.eval.TurResearchEvalBridgeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Studies to eval bridge (T725): promotes a research study's completed
 * interviews into an eval dataset. T1322 — split out of answer's
 * {@code TurResearchStudyAPI} with the bridge it calls, since eval sits above
 * answer (ADR 0005 section 6); the path is unchanged, and a missing study is
 * still a 404, now answered by the bridge.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/research-study")
@Tag(name = "Research Study", description = "Synthetic user research: cohort interviews & insights")
public class TurResearchEvalBridgeAPI {

    private final TurResearchEvalBridgeService evalBridgeService;

    public TurResearchEvalBridgeAPI(TurResearchEvalBridgeService evalBridgeService) {
        this.evalBridgeService = evalBridgeService;
    }

    @Operation(summary = "Promote the study's completed interviews into a reusable eval "
            + "dataset (agent-QA harness); the studio deep-links the returned id into /bento/eval")
    @PostMapping("/{id}/promote-to-dataset")
    public TurEvalDatasetDto promoteToDataset(@PathVariable String id,
            @RequestParam(required = false) String name) {
        TurEvalDataset dataset = evalBridgeService.promoteToDataset(id, name);
        return TurEvalDatasetDto.summary(dataset, dataset.getRows().size());
    }
}
