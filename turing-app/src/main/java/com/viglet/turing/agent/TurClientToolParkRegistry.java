/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.viglet.turing.persistence.model.persona.TurPersona;

import lombok.extern.slf4j.Slf4j;

/**
 * T438 / §XXII.3 — in-memory store of chat turns parked awaiting a frontend
 * ("client") tool result.
 *
 * <p>When the model calls a declared client tool, the dispatcher catches the
 * {@link com.viglet.turing.agent.clienttool.TurClientToolParkException}, parks
 * everything needed to resume here, and emits a {@code client_tool_call} SSE
 * event. The browser runs the tool and POSTs the result to
 * {@code /api/v2/chat/client-tool-result}; the dispatcher {@link #take(String, String)}s
 * the park and re-dispatches with the tool response appended.
 *
 * <p><b>Single-node MVP.</b> Like the analytics in-flight bag and the T427
 * tool-call trace, this is process-local: a parked turn does not survive a
 * restart and is not visible to other cluster nodes. Bounded by count and a TTL
 * sweep so an unanswered call can't wedge the conversation or leak memory. A
 * durable (persisted) park is a documented follow-up.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Service
public class TurClientToolParkRegistry {

    /** Hard cap on concurrently-parked turns; the oldest is evicted past it. */
    static final int MAX_PARKED = 2000;
    /** Default time a park is held before the timeout sweep abandons it. */
    static final long DEFAULT_TTL_MILLIS = 5 * 60 * 1000L;

    /**
     * A parked turn: the SSE-facing call info plus everything {@code dispatchCall}
     * needs to resume (single-node — live Spring AI objects held by reference).
     */
    public record ParkedClientTurn(
            String conversationId,
            String callId,
            String toolName,
            String arguments,
            TurChatTurnContext turn,
            ChatModel chatModel,
            ChatOptions initialOptions,
            TurAgentChatFlowContext flowContext,
            TurPersona persona,
            List<Message> promptMessages,
            AssistantMessage assistantMessage,
            long expiresAtMillis) {
    }

    private final Map<String, ParkedClientTurn> parked = new ConcurrentHashMap<>();

    private static String key(String conversationId, String callId) {
        return conversationId + "\0" + callId;
    }

    /** Park a turn; returns the TTL deadline stamped on it. */
    public void park(ParkedClientTurn parkedTurn) {
        if (parkedTurn == null || parkedTurn.callId() == null) {
            return;
        }
        if (parked.size() >= MAX_PARKED) {
            evictOldest();
        }
        parked.put(key(parkedTurn.conversationId(), parkedTurn.callId()), parkedTurn);
        log.info("[ClientTool] parked conv={} callId={} tool={}",
                parkedTurn.conversationId(), parkedTurn.callId(), parkedTurn.toolName());
    }

    /**
     * Atomically removes and returns the park for {@code (conversationId, callId)},
     * or {@code null} when unknown / already resumed / expired.
     */
    public ParkedClientTurn take(String conversationId, String callId) {
        if (conversationId == null || callId == null) {
            return null;
        }
        ParkedClientTurn removed = parked.remove(key(conversationId, callId));
        if (removed != null && removed.expiresAtMillis() < System.currentTimeMillis()) {
            log.info("[ClientTool] callId={} resumed after its deadline — abandoning", callId);
            return null;
        }
        return removed;
    }

    /** Best-effort current parked count (diagnostics/tests). */
    public int size() {
        return parked.size();
    }

    /** Sweep expired parks so an unanswered client call can't leak memory. */
    @Scheduled(fixedDelayString = "${turing.genai.client-tools.sweep-ms:60000}")
    public void sweepExpired() {
        long now = System.currentTimeMillis();
        int before = parked.size();
        parked.values().removeIf(p -> p.expiresAtMillis() < now);
        int removed = before - parked.size();
        if (removed > 0) {
            log.info("[ClientTool] swept {} expired parked turn(s); {} remaining", removed, parked.size());
        }
    }

    private void evictOldest() {
        parked.entrySet().stream()
                .min((a, b) -> Long.compare(a.getValue().expiresAtMillis(), b.getValue().expiresAtMillis()))
                .ifPresent(e -> parked.remove(e.getKey()));
    }
}
