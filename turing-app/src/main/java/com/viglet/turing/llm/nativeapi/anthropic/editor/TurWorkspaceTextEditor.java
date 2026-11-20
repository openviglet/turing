/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.llm.nativeapi.anthropic.editor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.agent.workspace.TurAgentWorkspace;

import lombok.extern.slf4j.Slf4j;

/**
 * T142 / §X.4.d — executes the Anthropic {@code text_editor_20250728}
 * ({@code str_replace_based_edit_tool}) commands against a
 * {@link TurAgentWorkspace}, so Claude authors/edits markdown (or any text) that
 * persists in the per-conversation workspace instead of a real filesystem.
 *
 * <p>Each tool call carries a {@code command} ({@code view} / {@code create} /
 * {@code str_replace} / {@code insert} / {@code undo_edit}) and a {@code path};
 * the path becomes a workspace key (leading slashes stripped, traversal
 * rejected by the workspace). This class is deliberately free of any Anthropic
 * SDK type — {@link TurAnthropicEditorService} hands it the decoded input map —
 * so the edit semantics are unit-testable against an in-memory workspace.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Component
public class TurWorkspaceTextEditor {

    /** The fixed tool name Anthropic uses for {@code text_editor_20250728}. */
    public static final String TOOL_NAME = "str_replace_based_edit_tool";

    /** Undo sentinel meaning "the file did not exist before this edit — delete it". */
    private static final String DELETE_ON_UNDO = "\0__turing_delete_on_undo__";

    private final TurAgentWorkspace workspace;

    public TurWorkspaceTextEditor(TurAgentWorkspace workspace) {
        this.workspace = workspace;
    }

    /** Outcome of one editor command: the {@code tool_result} text + error flag. */
    public record EditResult(String output, boolean error) {

        static EditResult ok(String output) {
            return new EditResult(output, false);
        }

        static EditResult error(String output) {
            return new EditResult(output, true);
        }
    }

    /**
     * Execute one decoded text_editor command.
     *
     * @param undo per-loop map of {@code key -> previous content} used by
     *             {@code undo_edit}; mutated here on edits.
     */
    public EditResult execute(String agentId, String conversationId, Map<?, ?> input,
            Map<String, String> undo) {
        String command = str(input.get("command"));
        if (command == null) {
            return EditResult.error("Missing 'command'.");
        }
        String path = str(input.get("path"));
        if (path == null) {
            return EditResult.error("Missing 'path'.");
        }
        String key;
        try {
            key = toKey(path);
        } catch (IllegalArgumentException e) {
            return EditResult.error("Invalid path '" + path + "': " + e.getMessage());
        }

        return switch (command.toLowerCase(Locale.ROOT)) {
            case "view" -> view(agentId, conversationId, key, path, input);
            case "create" -> create(agentId, conversationId, key, path, input, undo);
            case "str_replace" -> strReplace(agentId, conversationId, key, path, input, undo);
            case "insert" -> insert(agentId, conversationId, key, path, input, undo);
            case "undo_edit" -> undo(agentId, conversationId, key, path, undo);
            default -> EditResult.error("Unsupported command '" + command + "'.");
        };
    }

    private EditResult view(String agentId, String conversationId, String key, String path,
            Map<?, ?> input) {
        Optional<String> content = readText(agentId, conversationId, key);
        if (content.isEmpty()) {
            return EditResult.error("File not found: " + path);
        }
        List<String> lines = splitLines(content.get());
        int[] range = viewRange(input.get("view_range"), lines.size());
        StringBuilder sb = new StringBuilder();
        for (int i = range[0]; i <= range[1]; i++) {
            sb.append(i + 1).append('\t').append(lines.get(i)).append('\n');
        }
        return EditResult.ok(sb.toString());
    }

    private EditResult create(String agentId, String conversationId, String key, String path,
            Map<?, ?> input, Map<String, String> undo) {
        String fileText = str(input.get("file_text"));
        if (fileText == null) {
            fileText = "";
        }
        rememberForUndo(agentId, conversationId, key, undo);
        writeText(agentId, conversationId, key, fileText);
        return EditResult.ok("File created: " + path);
    }

    private EditResult strReplace(String agentId, String conversationId, String key, String path,
            Map<?, ?> input, Map<String, String> undo) {
        Optional<String> existing = readText(agentId, conversationId, key);
        if (existing.isEmpty()) {
            return EditResult.error("File not found: " + path);
        }
        String oldStr = str(input.get("old_str"));
        if (oldStr == null || oldStr.isEmpty()) {
            return EditResult.error("Missing 'old_str'.");
        }
        String newStr = str(input.get("new_str"));
        if (newStr == null) {
            newStr = "";
        }
        String content = existing.get();
        int occurrences = countOccurrences(content, oldStr);
        if (occurrences == 0) {
            return EditResult.error("No match for 'old_str' in " + path + ".");
        }
        if (occurrences > 1) {
            return EditResult.error("Found " + occurrences + " matches for 'old_str' in " + path
                    + "; it must be unique. Add more surrounding context.");
        }
        undo.put(key, content);
        writeText(agentId, conversationId, key, content.replace(oldStr, newStr));
        return EditResult.ok("Edited " + path + ".");
    }

    private EditResult insert(String agentId, String conversationId, String key, String path,
            Map<?, ?> input, Map<String, String> undo) {
        Optional<String> existing = readText(agentId, conversationId, key);
        if (existing.isEmpty()) {
            return EditResult.error("File not found: " + path);
        }
        Integer insertLine = intOrNull(input.get("insert_line"));
        if (insertLine == null) {
            return EditResult.error("Missing 'insert_line'.");
        }
        // Anthropic uses "new_str" for the inserted text on the 2025-07-28 tool;
        // tolerate the older "insert_text" alias too.
        String text = str(input.get("new_str"));
        if (text == null) {
            text = str(input.get("insert_text"));
        }
        if (text == null) {
            text = "";
        }
        List<String> lines = splitLines(existing.get());
        if (insertLine < 0 || insertLine > lines.size()) {
            return EditResult.error("insert_line " + insertLine + " is out of range (0.."
                    + lines.size() + ") for " + path + ".");
        }
        undo.put(key, existing.get());
        lines.add(insertLine, text);
        writeText(agentId, conversationId, key, String.join("\n", lines));
        return EditResult.ok("Inserted text after line " + insertLine + " in " + path + ".");
    }

    private EditResult undo(String agentId, String conversationId, String key, String path,
            Map<String, String> undo) {
        if (!undo.containsKey(key)) {
            return EditResult.error("No edit to undo for " + path + ".");
        }
        String previous = undo.remove(key);
        if (DELETE_ON_UNDO.equals(previous)) {
            workspace.delete(agentId, conversationId, key);
            return EditResult.ok("Reverted creation of " + path + " (deleted).");
        }
        writeText(agentId, conversationId, key, previous);
        return EditResult.ok("Reverted last edit to " + path + ".");
    }

    private void rememberForUndo(String agentId, String conversationId, String key,
            Map<String, String> undo) {
        undo.put(key, readText(agentId, conversationId, key).orElse(DELETE_ON_UNDO));
    }

    private Optional<String> readText(String agentId, String conversationId, String key) {
        return workspace.get(agentId, conversationId, key)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    private void writeText(String agentId, String conversationId, String key, String text) {
        workspace.put(agentId, conversationId, key,
                text.getBytes(StandardCharsets.UTF_8), contentType(key));
    }

    /** Map a model-supplied path to a workspace key (strip leading slashes / {@code ./}). */
    static String toKey(String path) {
        String trimmed = path.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.startsWith("./")) {
            trimmed = trimmed.substring(2);
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("empty path");
        }
        if (trimmed.contains("..")) {
            throw new IllegalArgumentException("path traversal not allowed");
        }
        return trimmed;
    }

    private static String contentType(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "text/markdown";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html";
        }
        if (lower.endsWith(".csv")) {
            return "text/csv";
        }
        return "text/plain";
    }

    /** Split on newlines preserving empty trailing segments; never returns null. */
    private static List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content.isEmpty()) {
            return lines;
        }
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines.add(content.substring(start, i));
                start = i + 1;
            }
        }
        lines.add(content.substring(start));
        return lines;
    }

    /** Resolve a 1-based inclusive {@code [start, end]} view range to 0-based bounds. */
    private static int[] viewRange(Object rangeValue, int lineCount) {
        int lastIndex = Math.max(0, lineCount - 1);
        if (rangeValue instanceof List<?> list && list.size() >= 2) {
            Integer start = intOrNull(list.get(0));
            Integer end = intOrNull(list.get(1));
            int from = start == null ? 1 : Math.max(1, start);
            int to = (end == null || end < 0) ? lineCount : Math.min(lineCount, end);
            return new int[] { Math.min(from - 1, lastIndex), Math.min(Math.max(to - 1, from - 1), lastIndex) };
        }
        return new int[] { 0, lastIndex };
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer intOrNull(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s && StringUtils.hasText(s)) {
            try {
                return (int) Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
