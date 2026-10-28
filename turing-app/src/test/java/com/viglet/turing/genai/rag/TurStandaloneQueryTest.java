package com.viglet.turing.genai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.viglet.turing.genai.rag.TurStandaloneQuery.Turn;
import com.viglet.turing.persistence.model.sn.genai.TurFollowupQueryMode;

/**
 * T1157 — the standalone question a follow-up turn's retrieval searches.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurStandaloneQueryTest {

    private static final List<Turn> FOLLOW_UP = List.of(
            new Turn("user", "When do applications for the research grant close?"),
            new Turn("assistant", "Applications close on March 31."),
            new Turn("user", "Where do I send it?"));

    private static UnaryOperator<String> failing(AtomicInteger calls) {
        return prompt -> {
            calls.incrementAndGet();
            throw new IllegalStateException("model down");
        };
    }

    @Test
    void latestOnlyIsTheLatestMessage() {
        AtomicInteger calls = new AtomicInteger();
        assertEquals("Where do I send it?",
                TurStandaloneQuery.resolve(TurFollowupQueryMode.LATEST_ONLY, FOLLOW_UP, failing(calls)));
        assertEquals("Where do I send it?", TurStandaloneQuery.resolve(null, FOLLOW_UP, failing(calls)));
        assertEquals(0, calls.get());
    }

    @Test
    void aFirstTurnIsUnchangedUnderEveryMode() {
        AtomicInteger calls = new AtomicInteger();
        List<Turn> first = List.of(new Turn("user", "When do applications close?"));
        for (TurFollowupQueryMode mode : TurFollowupQueryMode.values()) {
            assertEquals("When do applications close?", TurStandaloneQuery.resolve(mode, first, failing(calls)));
        }
        assertEquals(0, calls.get(), "a first turn never pays for a rewrite");
    }

    @Test
    void joinedCarriesTheAntecedentIntoTheQuery() {
        String query = TurStandaloneQuery.resolve(TurFollowupQueryMode.JOINED, FOLLOW_UP, null);

        assertEquals("When do applications for the research grant close? Where do I send it?", query);
    }

    @Test
    void joinedKeepsOnlyTheRecentUserTurns() {
        List<Turn> history = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            history.add(new Turn("user", "q" + i));
            history.add(new Turn("assistant", "a" + i));
        }

        assertEquals("q3 q4 q5", TurStandaloneQuery.resolve(TurFollowupQueryMode.JOINED, history, null));
    }

    @Test
    void rewriteSearchesTheModelsStandaloneQuestion() {
        List<String> prompts = new ArrayList<>();
        String query = TurStandaloneQuery.resolve(TurFollowupQueryMode.REWRITE, FOLLOW_UP, prompt -> {
            prompts.add(prompt);
            return "\"Where do I send the research grant application?\"\n";
        });

        assertEquals("Where do I send the research grant application?", query);
        assertEquals(1, prompts.size());
        assertTrue(prompts.getFirst().contains("User: When do applications for the research grant close?"));
        assertTrue(prompts.getFirst().contains("Assistant: Applications close on March 31."));
        assertTrue(prompts.getFirst().contains("User: Where do I send it?"));
    }

    @Test
    void aTopicSwitchReturnedUnchangedDoesNotDragTheOldTopicIn() {
        List<Turn> history = List.of(
                new Turn("user", "When do applications for the research grant close?"),
                new Turn("assistant", "Applications close on March 31."),
                new Turn("user", "What are the library opening hours?"));

        String query = TurStandaloneQuery.resolve(TurFollowupQueryMode.REWRITE, history,
                prompt -> "What are the library opening hours?");

        assertEquals("What are the library opening hours?", query);
    }

    @Test
    void aFailedRewriteFallsBackToTheLatestMessage() {
        AtomicInteger calls = new AtomicInteger();

        assertEquals("Where do I send it?",
                TurStandaloneQuery.resolve(TurFollowupQueryMode.REWRITE, FOLLOW_UP, failing(calls)));
        assertEquals(1, calls.get());
    }

    @Test
    void anEmptyOrRunawayRewriteFallsBackToTheLatestMessage() {
        assertEquals("Where do I send it?",
                TurStandaloneQuery.resolve(TurFollowupQueryMode.REWRITE, FOLLOW_UP, prompt -> "  "));
        assertEquals("Where do I send it?",
                TurStandaloneQuery.resolve(TurFollowupQueryMode.REWRITE, FOLLOW_UP, prompt -> null));
        assertEquals("Where do I send it?", TurStandaloneQuery.resolve(TurFollowupQueryMode.REWRITE, FOLLOW_UP,
                prompt -> "x".repeat(TurStandaloneQuery.MAX_REWRITE_CHARS + 1)));
        assertEquals("Where do I send it?",
                TurStandaloneQuery.resolve(TurFollowupQueryMode.REWRITE, FOLLOW_UP, null));
    }

    @Test
    void aConversationWithoutUserTextResolvesToEmpty() {
        assertEquals("", TurStandaloneQuery.resolve(TurFollowupQueryMode.JOINED,
                List.of(new Turn("assistant", "Hello")), null));
        assertEquals("", TurStandaloneQuery.resolve(TurFollowupQueryMode.JOINED, null, null));
    }
}
