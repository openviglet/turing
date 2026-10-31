/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.plugins.se;

import java.util.ArrayList;
import java.util.List;

import com.viglet.turing.commons.se.result.spellcheck.TurSESpellCheckResult;

/**
 * T1199 / §CLXII.8 — the guard every spell-check producer (Lucene's
 * {@code DirectSpellChecker}, Solr's {@code /tur_spell} collation) passes its
 * correction through. A spell-checker knows words, not prices: asked about
 * {@code R$ 2.200} it happily answers {@code r$ 2.100}, and a client that
 * searches the correction searches a number the shopper never named.
 *
 * <p>Aligned token by token with what was typed, a token holding a digit or a
 * symbol ({@code 2.200}, {@code R$}, {@code 45L}) is never replaced, and a token
 * the checker only re-cased keeps the case it was typed in. A correction left
 * identical to the query is no correction. When the two disagree on the token
 * count (a word-break collation) the alignment is unknown, so the correction
 * is kept as the engine gave it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
public final class TurSpellCheckGuard {

    private TurSpellCheckGuard() {
    }

    /** The guarded result for {@code corrected}, the engine's answer to {@code typed}. */
    public static TurSESpellCheckResult guard(String typed, String corrected) {
        String guarded = guardedText(typed, corrected);
        return guarded == null ? new TurSESpellCheckResult() : new TurSESpellCheckResult(true, guarded);
    }

    /**
     * The correction with protected tokens restored, or {@code null} when nothing
     * is left of it.
     */
    static String guardedText(String typed, String corrected) {
        if (typed == null || corrected == null || corrected.isBlank()) {
            return null;
        }
        String[] typedTokens = typed.trim().split("\\s+");
        String[] correctedTokens = corrected.trim().split("\\s+");
        if (typedTokens.length != correctedTokens.length) {
            return corrected.equals(typed) ? null : corrected;
        }
        List<String> out = new ArrayList<>(typedTokens.length);
        boolean changed = false;
        for (int i = 0; i < typedTokens.length; i++) {
            String original = typedTokens[i];
            String suggestion = correctedTokens[i];
            if (isProtected(original) || suggestion.equalsIgnoreCase(original)) {
                out.add(original);
            } else {
                out.add(suggestion);
                changed = true;
            }
        }
        return changed ? String.join(" ", out) : null;
    }

    /**
     * Whether a typed token is out of a spell-checker's reach: it holds a digit,
     * a currency, math or other symbol, or no letter at all.
     */
    static boolean isProtected(String token) {
        boolean hasLetter = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c)) {
                return true;
            }
            int type = Character.getType(c);
            if (type == Character.CURRENCY_SYMBOL || type == Character.MATH_SYMBOL
                    || type == Character.OTHER_SYMBOL || type == Character.MODIFIER_SYMBOL) {
                return true;
            }
            hasLetter |= Character.isLetter(c);
        }
        return !hasLetter;
    }
}
