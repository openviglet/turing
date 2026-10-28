/*
 *
 * Copyright (C) 2016-2025 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.genai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.viglet.turing.persistence.model.sn.genai.TurFollowupQueryMode;

import lombok.extern.slf4j.Slf4j;

/**
 * T1157 — the standalone question a chat turn's retrieval searches. A follow-up
 * ("Where do I send it?") names nothing its antecedent named, so searching the
 * latest message alone retrieves nothing and the refusal answers. The SN
 * streaming chat, the blocking SN chat and the catalog copilot all call this
 * before retrieval; the answer keeps the visitor's own words.
 *
 * <p>A first turn is returned as it is under every mode, and a failed or empty
 * rewrite falls back to the latest message, so the worst a mode can do is the
 * behaviour before it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Slf4j
public final class TurStandaloneQuery {

    /** How many user turns {@link TurFollowupQueryMode#JOINED} joins, the latest included. */
    static final int JOINED_TURNS = 3;
    /** How many turns of the conversation the rewrite prompt sees, the latest included. */
    static final int REWRITE_TURNS = 6;
    /** A rewrite longer than this is not a question; the latest message is used. */
    static final int MAX_REWRITE_CHARS = 500;
    private static final int MAX_TURN_CHARS = 600;

    /** One conversation turn, as each caller's own message record maps to it. */
    public record Turn(String role, String content) {
        boolean isUser() {
            return !"assistant".equalsIgnoreCase(role) && !"system".equalsIgnoreCase(role);
        }
    }

    private TurStandaloneQuery() {
    }

    /**
     * The query to search for the latest user turn of {@code history}.
     *
     * @param mode     the site's mode; {@code null} is {@link TurFollowupQueryMode#LATEST_ONLY}
     * @param history  the conversation so far, oldest first
     * @param rewriter runs one prompt and returns the model's text; read only by
     *                 {@link TurFollowupQueryMode#REWRITE}, and may be {@code null}
     *                 elsewhere
     * @return the query, or {@code ""} when the conversation has no user text
     */
    public static String resolve(TurFollowupQueryMode mode, List<Turn> history,
            UnaryOperator<String> rewriter) {
        List<String> userTurns = userTurns(history);
        if (userTurns.isEmpty()) {
            return "";
        }
        String latest = userTurns.getLast();
        if (userTurns.size() == 1 || mode == null || mode == TurFollowupQueryMode.LATEST_ONLY) {
            return latest;
        }
        return switch (mode) {
            case JOINED -> String.join(" ",
                    userTurns.subList(Math.max(0, userTurns.size() - JOINED_TURNS), userTurns.size()));
            case REWRITE -> rewrite(history, latest, rewriter);
            default -> latest;
        };
    }

    private static List<String> userTurns(List<Turn> history) {
        List<String> turns = new ArrayList<>();
        if (history == null) {
            return turns;
        }
        for (Turn turn : history) {
            if (turn != null && turn.isUser() && turn.content() != null && !turn.content().isBlank()) {
                turns.add(turn.content().strip());
            }
        }
        return turns;
    }

    private static String rewrite(List<Turn> history, String latest, UnaryOperator<String> rewriter) {
        if (rewriter == null) {
            return latest;
        }
        try {
            String raw = rewriter.apply(rewritePrompt(history));
            String question = clean(raw);
            if (question.isEmpty() || question.length() > MAX_REWRITE_CHARS) {
                log.debug("Standalone query rewrite unusable ('{}'), searching the latest message", raw);
                return latest;
            }
            log.debug("Standalone query rewrite: '{}' -> '{}'", latest, question);
            return question;
        } catch (RuntimeException e) {
            log.warn("Standalone query rewrite failed, searching the latest message: {}", e.getMessage());
            return latest;
        }
    }

    static String rewritePrompt(List<Turn> history) {
        List<Turn> recent = new ArrayList<>();
        for (Turn turn : history) {
            if (turn != null && turn.content() != null && !turn.content().isBlank()) {
                recent.add(turn);
            }
        }
        recent = recent.subList(Math.max(0, recent.size() - REWRITE_TURNS), recent.size());
        StringBuilder conversation = new StringBuilder();
        for (Turn turn : recent) {
            String content = turn.content().strip().replaceAll("\\s+", " ");
            if (content.length() > MAX_TURN_CHARS) {
                content = content.substring(0, MAX_TURN_CHARS) + "…";
            }
            conversation.append(turn.isUser() ? "User: " : "Assistant: ").append(content).append('\n');
        }
        return """
                Rewrite the user's LAST message as one standalone search question that \
                can be understood without the conversation: replace pronouns and \
                references ("it", "that one", "there") with what they refer to. If the \
                last message is already self-contained or changes the topic, return it \
                unchanged. Keep the user's language. Reply with the question only, no \
                preamble and no quotes.

                Conversation:
                %s""".formatted(conversation);
    }

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String line = raw.strip();
        int newline = line.indexOf('\n');
        if (newline >= 0) {
            line = line.substring(0, newline).strip();
        }
        line = line.replaceFirst("(?i)^(standalone question|question)\\s*:\\s*", "");
        if (line.length() >= 2 && (line.startsWith("\"") && line.endsWith("\"")
                || line.startsWith("“") && line.endsWith("”"))) {
            line = line.substring(1, line.length() - 1).strip();
        }
        return line;
    }
}
