/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.persistence.repository.sn.metric;

import java.util.regex.Pattern;

/**
 * T1298 — personal data taken out of a search term before it is stored: an e-mail
 * address, a CPF or CNPJ, and any run of seven or more digits (an enrolment number,
 * a phone, a document number), each replaced by {@link #MASK}. What a visitor
 * searched for around it is kept, so "boletim 20231234" is still counted as a
 * search for a report card.
 *
 * <p>Applied where a search's, a click's, an answer share's and an answer signal's
 * query is written, so the search log never holds the data and every report built
 * on it, Search insights included, shows none. On by default; a deployment whose
 * visitors search by long codes it wants to see (an ISBN, an EAN, a SKU) turns it
 * off with {@code turing.search.metrics.mask-personal-data=false}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSearchTermPrivacy {

    /** What a masked span becomes in the stored term. */
    public static final String MASK = "***";

    /** The longest term read; the stores keep at most 2000 characters. */
    private static final int MAX_INPUT = 4096;

    private static final Pattern EMAIL = Pattern.compile("[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.-]+\\.[\\p{L}]{2,}");
    /** CPF and CNPJ as typed with their separators. */
    private static final Pattern DOCUMENT = Pattern.compile(
            "\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b|\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b");
    /** Seven or more digits, which a dot, a dash, a slash or one space may split. */
    private static final Pattern DIGIT_RUN = Pattern.compile("\\d(?:[.\\-/ ]?\\d){6,}");
    /** A date a run may be (2026/11/20, 20-11-2026, 20.11.2026): searched for, never personal. */
    private static final Pattern DATE = Pattern.compile(
            "\\d{4}([./-])\\d{1,2}\\1\\d{1,2}|\\d{1,2}([./-])\\d{1,2}\\2\\d{4}");

    private static volatile boolean enabled = true;

    private TurSearchTermPrivacy() {
    }

    /** Set once at startup from {@code turing.search.metrics.mask-personal-data}. */
    public static void setEnabled(boolean enabled) {
        TurSearchTermPrivacy.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** {@code term} with its personal data masked, or as it is when masking is off. */
    public static String mask(String term) {
        if (!enabled || term == null || term.isEmpty()) {
            return term;
        }
        // Every caller stores at most a few thousand characters; bounding the input
        // keeps the repeated groups below from recursing deep on a pasted wall of text.
        String bounded = term.length() > MAX_INPUT ? term.substring(0, MAX_INPUT) : term;
        String masked = EMAIL.matcher(bounded).replaceAll(MASK);
        masked = DOCUMENT.matcher(masked).replaceAll(MASK);
        return DIGIT_RUN.matcher(masked).replaceAll(run -> DATE.matcher(run.group()).matches()
                ? java.util.regex.Matcher.quoteReplacement(run.group())
                : MASK);
    }
}
