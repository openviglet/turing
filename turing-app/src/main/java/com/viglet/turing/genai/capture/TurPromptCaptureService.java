/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.genai.capture;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.viglet.turing.api.asset.TurAssetItem;
import com.viglet.turing.genai.TurAgentChatExecutor.ChatMessageItem;
import com.viglet.turing.persistence.model.agent.TurAIAgent;
import com.viglet.turing.service.storage.TurStorageService;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * T618 / §XXXIV.6 — captures the exact assembled prompt per conversation turn at
 * send time and reads it back for true (verbatim) replay in the Live Preview.
 *
 * <p>A thin façade over the pluggable {@link TurStorageService} (the same seam
 * the T111 workspace uses), so captures never touch the primary DB — important
 * because writes land on the chat hot path. Each turn is stored under the T78
 * multi-tenant path
 * {@code tenants/{agentId}/{conversationId}/prompt-captures/turn-{turnIndex}.json}.
 *
 * <h2>Bounded, opt-in</h2>
 * Capture only runs when the agent opted in ({@code promptCaptureEnabled}) and
 * storage is enabled. Growth is bounded three ways (see
 * {@link TurPromptCaptureProperties}): a per-conversation turn ring pruned on
 * every write, a per-entry byte cap that skips oversized captures, and a
 * scheduled retention sweep ({@link TurPromptCaptureCleanupJob}).
 *
 * <h2>Fail-open</h2>
 * A capture is pure observability: every write path swallows its own errors so a
 * storage hiccup never fails the user's turn. Reads degrade to "no captures".
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Service
public class TurPromptCaptureService {

    static final String TENANTS_DIR = "tenants";
    static final String CAPTURES_DIR = "prompt-captures";
    private static final String TURN_PREFIX = "turn-";
    private static final String TURN_SUFFIX = ".json";
    /** T1157 — the searched-query sidecar's prefix; {@link #parseTurnIndex} ignores it. */
    private static final String SEARCHED_PREFIX = "searched-";
    private static final String SEARCHED_QUERY_FIELD = "query";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TurStorageService storageService;
    private final TurPromptCaptureProperties properties;

    public TurPromptCaptureService(TurStorageService storageService,
            TurPromptCaptureProperties properties) {
        this.storageService = storageService;
        this.properties = properties;
    }

    /**
     * True when this agent opted into capture <em>and</em> object storage is
     * available. Callers on the hot path should gate on this before assembling
     * any capture payload.
     */
    public boolean isEnabled(TurAIAgent agent) {
        return agent != null && agent.isPromptCaptureEnabled() && storageService.isEnabled();
    }

    // ---------------------------------------------------------------------
    // Capture (write) — the chat hot path
    // ---------------------------------------------------------------------

    /**
     * Captures the Spring-AI assembled list (system message at index 0, then the
     * fanned-out history + the turn's user message) as handed to the {@code
     * ChatModel}. No-op when disabled / no conversation id.
     */
    public void captureSpringAi(TurAIAgent agent, String conversationId, List<Message> messages) {
        captureSpringAi(agent, conversationId, messages, 0);
    }

    /**
     * T1166 — as {@link #captureSpringAi(TurAIAgent, String, List)}, numbered by
     * {@code clientTurns}, the user messages in the history the client sent. The
     * assembled list also carries memory turns (the T115 summary, T30 recalled
     * turns) as {@code user} messages, and counting those would number the turn
     * differently from every other writer keyed by it. {@code 0} counts the list.
     */
    public void captureSpringAi(TurAIAgent agent, String conversationId, List<Message> messages,
            int clientTurns) {
        if (!isEnabled(agent) || !StringUtils.hasText(conversationId) || messages == null) {
            return;
        }
        try {
            String systemPrompt = "";
            List<TurPromptCapture.Message> captured = new ArrayList<>(messages.size());
            int userTurns = 0;
            for (Message message : messages) {
                String role = roleOf(message);
                String content = message.getText() == null ? "" : message.getText();
                if (message instanceof SystemMessage && systemPrompt.isEmpty()) {
                    systemPrompt = content;
                }
                if ("user".equals(role)) {
                    userTurns++;
                }
                captured.add(new TurPromptCapture.Message(role, content));
            }
            store(agent.getId(), conversationId, turnIndex(clientTurns, userTurns), systemPrompt, captured);
        } catch (RuntimeException e) {
            log.debug("[PromptCapture] Spring-AI capture skipped for conv '{}': {}",
                    conversationId, e.getMessage());
        }
    }

    /**
     * Captures the native-path assembled turn — the system prompt string + the
     * flat history the vendor SDK receives — reassembled into one message list
     * (system first) so it reads identically to the Spring-AI capture. No-op when
     * disabled / no conversation id.
     */
    public void captureNative(TurAIAgent agent, String conversationId,
            String systemPrompt, List<ChatMessageItem> history) {
        captureNative(agent, conversationId, systemPrompt, history, 0);
    }

    /** T1166 — as {@link #captureNative(TurAIAgent, String, String, List)}, numbered by {@code clientTurns}. */
    public void captureNative(TurAIAgent agent, String conversationId,
            String systemPrompt, List<ChatMessageItem> history, int clientTurns) {
        if (!isEnabled(agent) || !StringUtils.hasText(conversationId)) {
            return;
        }
        try {
            String system = systemPrompt == null ? "" : systemPrompt;
            List<TurPromptCapture.Message> captured = new ArrayList<>();
            captured.add(new TurPromptCapture.Message("system", system));
            int userTurns = 0;
            if (history != null) {
                for (ChatMessageItem item : history) {
                    String role = item.role() == null ? "user" : item.role();
                    if ("user".equals(role)) {
                        userTurns++;
                    }
                    captured.add(new TurPromptCapture.Message(role,
                            item.content() == null ? "" : item.content()));
                }
            }
            store(agent.getId(), conversationId, turnIndex(clientTurns, userTurns), system, captured);
        } catch (RuntimeException e) {
            log.debug("[PromptCapture] native capture skipped for conv '{}': {}",
                    conversationId, e.getMessage());
        }
    }

    /**
     * T1157 — records the query a turn's retrieval searched when it differs from
     * the visitor's message (a follow-up rewritten as a standalone question), so
     * the Live Preview can show it beside the captured turn. Stored next to the
     * turn, not inside it: the capture is written later, by the executor that
     * sends the prompt. No-op when capture is disabled or the query is blank.
     */
    public void recordSearchedQuery(TurAIAgent agent, String conversationId, int turnIndex, String query) {
        if (!isEnabled(agent) || !StringUtils.hasText(conversationId) || !StringUtils.hasText(query)) {
            return;
        }
        try {
            byte[] body = MAPPER.writeValueAsBytes(java.util.Map.of(SEARCHED_QUERY_FIELD, query));
            try (ByteArrayInputStream in = new ByteArrayInputStream(body)) {
                storageService.uploadStream(searchedKey(agent.getId(), conversationId, Math.max(turnIndex, 1)),
                        in, body.length, "application/json");
            }
        } catch (Exception e) {
            log.debug("[PromptCapture] searched query skipped for conv '{}': {}", conversationId, e.getMessage());
        }
    }

    /** T1157 — the query turn {@code turnIndex} searched, when one was recorded. */
    public Optional<String> getSearchedQuery(String agentId, String conversationId, int turnIndex) {
        if (!storageService.isEnabled() || !StringUtils.hasText(agentId)
                || !StringUtils.hasText(conversationId)) {
            return Optional.empty();
        }
        try (var in = storageService.downloadObject(searchedKey(agentId, conversationId, turnIndex))) {
            Object query = MAPPER.readValue(in.readAllBytes(), java.util.Map.class).get(SEARCHED_QUERY_FIELD);
            return query instanceof String text && StringUtils.hasText(text) ? Optional.of(text) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void store(String agentId, String conversationId, int turnIndex,
            String systemPrompt, List<TurPromptCapture.Message> messages) {
        TurPromptCapture capture = new TurPromptCapture(conversationId, turnIndex,
                System.currentTimeMillis(), agentId, systemPrompt, messages);
        byte[] body = MAPPER.writeValueAsBytes(capture);
        if (body.length > properties.getMaxBytesPerEntry()) {
            log.debug("[PromptCapture] turn {} of conv '{}' is {} bytes (> cap {}), skipping",
                    turnIndex, conversationId, body.length, properties.getMaxBytesPerEntry());
            return;
        }
        String objectName = turnKey(agentId, conversationId, turnIndex);
        try (ByteArrayInputStream in = new ByteArrayInputStream(body)) {
            storageService.uploadStream(objectName, in, body.length, "application/json");
        } catch (Exception e) {
            log.debug("[PromptCapture] failed to write turn {} of conv '{}': {}",
                    turnIndex, conversationId, e.getMessage());
            return;
        }
        pruneRing(agentId, conversationId);
    }

    /** Drops the oldest turns once a conversation exceeds the ring cap. */
    private void pruneRing(String agentId, String conversationId) {
        int cap = properties.getMaxTurnsPerConversation();
        if (cap <= 0) {
            return;
        }
        try {
            List<TurPromptCaptureTurnRef> turns = listTurns(agentId, conversationId);
            if (turns.size() <= cap) {
                return;
            }
            // listTurns returns newest-first; delete everything past the cap.
            for (TurPromptCaptureTurnRef stale : turns.subList(cap, turns.size())) {
                storageService.deleteObject(turnKey(agentId, conversationId, stale.turnIndex()));
                deleteQuietly(searchedKey(agentId, conversationId, stale.turnIndex()));
            }
        } catch (Exception e) {
            log.debug("[PromptCapture] ring prune no-op for conv '{}': {}",
                    conversationId, e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Replay (read) — the Live Preview
    // ---------------------------------------------------------------------

    /** Lists the captured turns for a conversation, newest first (empty when none). */
    public List<TurPromptCaptureTurnRef> listTurns(String agentId, String conversationId) {
        if (!storageService.isEnabled() || !StringUtils.hasText(agentId)
                || !StringUtils.hasText(conversationId)) {
            return List.of();
        }
        String prefix = capturesRoot(agentId, conversationId);
        List<TurPromptCaptureTurnRef> refs = new ArrayList<>();
        try {
            for (TurAssetItem item : storageService.listObjects(prefix)) {
                if (item.directory()) {
                    continue;
                }
                Integer turnIndex = parseTurnIndex(item.name());
                if (turnIndex != null) {
                    refs.add(new TurPromptCaptureTurnRef(turnIndex,
                            TurPromptCaptureTimes.toEpochMillis(item.lastModified())));
                }
            }
        } catch (Exception e) {
            log.debug("[PromptCapture] listTurns no-op for conv '{}': {}",
                    conversationId, e.getMessage());
            return List.of();
        }
        refs.sort(Comparator.comparingInt(TurPromptCaptureTurnRef::turnIndex).reversed());
        return refs;
    }

    /** Loads one captured turn verbatim, or empty when it does not exist. */
    public Optional<TurPromptCapture> getCapture(String agentId, String conversationId, int turnIndex) {
        if (!storageService.isEnabled() || !StringUtils.hasText(agentId)
                || !StringUtils.hasText(conversationId)) {
            return Optional.empty();
        }
        String objectName = turnKey(agentId, conversationId, turnIndex);
        try (var in = storageService.downloadObject(objectName)) {
            byte[] bytes = in.readAllBytes();
            return Optional.of(MAPPER.readValue(bytes, TurPromptCapture.class));
        } catch (Exception e) {
            log.debug("[PromptCapture] getCapture miss for conv '{}' turn {}: {}",
                    conversationId, turnIndex, e.getMessage());
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------------
    // Path scoping + helpers
    // ---------------------------------------------------------------------

    static String capturesRoot(String agentId, String conversationId) {
        return TENANTS_DIR + "/" + sanitizeSegment(agentId) + "/"
                + sanitizeSegment(conversationId) + "/" + CAPTURES_DIR + "/";
    }

    static String turnKey(String agentId, String conversationId, int turnIndex) {
        return capturesRoot(agentId, conversationId) + TURN_PREFIX + turnIndex + TURN_SUFFIX;
    }

    /**
     * T1166 — the user messages in a client history: the one turn number every
     * writer keyed by turn uses (the capture, the T1157 searched-query sidecar).
     */
    public static int userTurns(List<ChatMessageItem> history) {
        if (history == null) {
            return 0;
        }
        return (int) history.stream().filter(m -> m != null && "user".equals(m.role())).count();
    }

    /** The client's count when known, else the assembled list's; never below 1. */
    static int turnIndex(int clientTurns, int assembledUserTurns) {
        return Math.max(clientTurns > 0 ? clientTurns : assembledUserTurns, 1);
    }

    /** T1157 — the sidecar holding the query turn {@code turnIndex} searched. */
    static String searchedKey(String agentId, String conversationId, int turnIndex) {
        return capturesRoot(agentId, conversationId) + SEARCHED_PREFIX + turnIndex + TURN_SUFFIX;
    }

    /** Deletes a sidecar that may never have been written. */
    private void deleteQuietly(String objectName) {
        try {
            storageService.deleteObject(objectName);
        } catch (Exception e) {
            log.debug("[PromptCapture] no sidecar '{}' to delete: {}", objectName, e.getMessage());
        }
    }

    /** Extracts the turn index from a {@code .../turn-N.json} object name, or null. */
    static Integer parseTurnIndex(String objectName) {
        if (objectName == null) {
            return null;
        }
        String name = objectName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        String file = slash >= 0 ? name.substring(slash + 1) : name;
        if (!file.startsWith(TURN_PREFIX) || !file.endsWith(TURN_SUFFIX)) {
            return null;
        }
        String digits = file.substring(TURN_PREFIX.length(), file.length() - TURN_SUFFIX.length());
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String roleOf(Message message) {
        if (message instanceof SystemMessage) {
            return "system";
        }
        if (message instanceof AssistantMessage) {
            return "assistant";
        }
        return "user";
    }

    /**
     * Rejects path-traversal in an id used as a single path segment. Ids are
     * server-minted UUIDs; anything else is coerced/rejected (mirrors
     * {@code TurAgentWorkspaceService.sanitizeSegment}).
     */
    static String sanitizeSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Prompt-capture tenant id must not be blank");
        }
        String safe = raw.replaceAll("[^a-zA-Z0-9._-]", "");
        if (safe.isEmpty() || ".".equals(safe) || "..".equals(safe)) {
            throw new IllegalArgumentException("Invalid prompt-capture tenant id: " + raw);
        }
        return safe;
    }
}
