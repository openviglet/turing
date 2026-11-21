/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.eval.web;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.agent.chat.web.TurConversationAccess;
import com.viglet.turing.eval.spectator.TurCopilotService;
import com.viglet.turing.eval.spectator.TurCopilotService.ManualTurnResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * T120 — co-pilot "take the wheel": inject an operator-authored assistant turn.
 * Skips the LLM; the operator's {@code assistantMessage} becomes the reply and
 * (when paired with a {@code userMessage}) the flow advances through the same path
 * a normal turn uses, so slots and analytics stay consistent. The reply reaches the
 * visitor and any spectator through the message bus.
 *
 * <p>T1322f — split from {@code TurChatSessionAPI} under the same path, because the
 * co-pilot lives in eval and agent may not import it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/chat/sessions")
@Tag(name = "Chat Session", description = "Chat session inspection API")
public class TurCopilotManualTurnAPI {

    private final TurCopilotService copilotService;
    private final TurConversationAccess conversationAccess;

    public TurCopilotManualTurnAPI(TurCopilotService copilotService, TurConversationAccess conversationAccess) {
        this.copilotService = copilotService;
        this.conversationAccess = conversationAccess;
    }

    @Operation(summary = "T120 — co-pilot manual turn. The operator's text becomes the "
            + "assistant message (LLM skipped); the flow advances and telemetry records "
            + "exactly as a normal turn, keeping slots and analytics consistent.")
    @PostMapping("/{conversationId}/manual-turn")
    @Secured({ "ROLE_ADMIN", "AI_AGENT_EDIT" })
    public ManualTurnResult manualTurn(@PathVariable String conversationId,
            @RequestBody ManualTurnRequest request) {
        conversationAccess.requireVisible(conversationId);
        return copilotService.manualTurn(conversationId,
                request == null ? null : request.agentId(),
                request == null ? null : request.flowId(),
                request == null ? null : request.userMessage(),
                request == null ? null : request.assistantMessage());
    }

    public record ManualTurnRequest(String agentId, String flowId, String userMessage,
            String assistantMessage) {
    }
}
