/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.nl;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.document.Document;

/**
 * T1363 — rewrites the UTC instants in chunk text ({@code 2026-11-27T22:00:00Z}, the
 * form the search engine stores every date in) into a site's wall clock
 * ({@code 2026-11-27 19:00}), offset dropped, so a model reading the chunk announces
 * the time the visitor's clock shows. Applied where chunk text enters a prompt, not at
 * indexing: no reindex, and a changed site zone takes effect at once. Metadata and
 * filters stay UTC.
 *
 * <p>A value at midnight UTC is a date-only field stored as an instant: it becomes the
 * bare date, its own day, where converting it would land on the day before west of
 * Greenwich.</p>
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSiteWallClock {

    private static final Pattern UTC_INSTANT =
            Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2})(?:\\.\\d{1,9})?Z\\b");
    private static final String MIDNIGHT = "00:00:00";
    private static final DateTimeFormatter WALL_CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TurSiteWallClock() {
    }

    /** {@code text} with each UTC instant in {@code zone}'s wall clock; as it is with no zone. */
    public static String localize(String text, ZoneId zone) {
        if (text == null || zone == null || text.indexOf('Z') < 0) {
            return text;
        }
        Matcher matcher = UTC_INSTANT.matcher(text);
        StringBuilder out = new StringBuilder(text.length());
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(wallClock(matcher, zone)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** {@code document} with its text in {@code zone}'s wall clock; itself when nothing changes. */
    public static Document localize(Document document, ZoneId zone) {
        if (document == null || zone == null || document.getText() == null) {
            return document;
        }
        String text = localize(document.getText(), zone);
        if (text.equals(document.getText())) {
            return document;
        }
        return Document.builder()
                .id(document.getId())
                .text(text)
                .metadata(document.getMetadata())
                .score(document.getScore())
                .build();
    }

    /** Each of {@code documents} in {@code zone}'s wall clock; the list itself with no zone. */
    public static List<Document> localize(List<Document> documents, ZoneId zone) {
        if (documents == null || zone == null) {
            return documents;
        }
        return documents.stream().map(document -> localize(document, zone)).toList();
    }

    private static String wallClock(Matcher matcher, ZoneId zone) {
        String date = matcher.group(1);
        String time = matcher.group(2);
        if (MIDNIGHT.equals(time)) {
            return date;
        }
        try {
            Instant instant = LocalDateTime.parse(date + "T" + time).toInstant(ZoneOffset.UTC);
            return WALL_CLOCK.format(instant.atZone(zone));
        } catch (DateTimeException e) {
            return matcher.group();
        }
    }
}
