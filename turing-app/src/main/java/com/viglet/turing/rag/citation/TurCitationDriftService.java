/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.rag.citation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.viglet.turing.commons.sn.field.TurSNFieldName;
import com.viglet.turing.agent.TurAgentChatExecutor.ChatResponse;
import com.viglet.turing.rag.TurRagSource;
import com.viglet.turing.agent.tool.TurRagSearchToolService;
import com.viglet.turing.persistence.model.agent.TurChatCitationRecord;
import com.viglet.turing.persistence.repository.agent.TurChatCitationRecordRepository;
import com.viglet.turing.properties.TurConfigProperties;
import com.viglet.turing.properties.TurGenAiProperty.TurGenAiCitationDriftProperty;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

/**
 * §X.7.d / T155 — citation drift detection engine.
 *
 * <p>Two responsibilities, both gated by
 * {@code turing.genai.citation-drift.enabled} (default off):
 * <ol>
 *   <li><b>Capture.</b> {@link #tapForPersistence} decorates the native
 *       Anthropic SSE stream so the per-sentence citations Claude returns
 *       (T152–T154) are persisted as {@link TurChatCitationRecord}s the moment
 *       the answer completes, together with the question and the answer
 *       instant.</li>
 *   <li><b>Detect.</b> {@link #runDriftScan} (driven daily by
 *       {@code TurCitationDriftDetectionJob}) re-resolves recent records against
 *       the live index and flips {@code citationStale} per {@link #evaluate}'s
 *       policy — turning provable provenance into a time-aware, auditable
 *       record.</li>
 * </ol>
 *
 * <p>When disabled, capture is a no-op pass-through and the scan returns
 * immediately, so agents that merely stream citations are unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Slf4j
@Service
public class TurCitationDriftService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CITATIONS_EVENT_TYPE = "citations";

    private final TurConfigProperties configProperties;
    private final TurChatCitationRecordRepository repository;
    private final TurRagSearchToolService ragSearchToolService;

    public TurCitationDriftService(TurConfigProperties configProperties,
            TurChatCitationRecordRepository repository,
            TurRagSearchToolService ragSearchToolService) {
        this.configProperties = configProperties;
        this.repository = repository;
        this.ragSearchToolService = ragSearchToolService;
    }

    public boolean isEnabled() {
        return props().isEnabled();
    }

    private TurGenAiCitationDriftProperty props() {
        return configProperties.getGenai().getCitationDrift();
    }

    // ------------------------------------------------------------------
    // Capture
    // ------------------------------------------------------------------

    /**
     * Decorate a native chat SSE flux so its {@code "citations"} event is
     * persisted on completion. A pure pass-through when drift detection is
     * disabled or the turn is stateless ({@code conversationId} blank — there is
     * no conversation to attach provenance to). Best-effort: a persistence
     * failure never propagates onto the answer stream.
     */
    public Flux<ChatResponse> tapForPersistence(Flux<ChatResponse> source, String agentId,
            String conversationId, String question, int topK) {
        if (!isEnabled() || !StringUtils.hasText(conversationId)) {
            return source;
        }
        StringBuilder citationsJson = new StringBuilder();
        return source
                .doOnNext(event -> {
                    if (CITATIONS_EVENT_TYPE.equals(event.type()) && event.content() != null) {
                        citationsJson.setLength(0);
                        citationsJson.append(event.content());
                    }
                })
                .doOnComplete(() -> {
                    if (citationsJson.length() > 0) {
                        try {
                            recordCitations(agentId, conversationId, question, topK,
                                    citationsJson.toString(), Instant.now());
                        } catch (RuntimeException e) {
                            log.warn("[CitationDrift] could not persist citations for conversation '{}': {}",
                                    conversationId, e.getMessage());
                        }
                    }
                });
    }

    /**
     * Parse a {@code "citations"} SSE payload (a JSON array of
     * {@link TurChatCitation}) and persist one {@link TurChatCitationRecord} per
     * distinct {@code (sourceId, citedText)} pair. Citations carrying no
     * {@code sourceId} are skipped (nothing to re-resolve). Returns the number of
     * rows written.
     */
    @Transactional
    public int recordCitations(String agentId, String conversationId, String question, int topK,
            String citationsJson, Instant answerProducedAt) {
        List<TurChatCitation> citations = parseCitations(citationsJson);
        if (citations.isEmpty()) {
            return 0;
        }
        Set<String> seen = new LinkedHashSet<>();
        List<TurChatCitationRecord> rows = new ArrayList<>();
        for (TurChatCitation citation : citations) {
            if (citation == null || !StringUtils.hasText(citation.sourceId())) {
                continue;
            }
            String key = citation.sourceId() + "\0" + safe(citation.citedText());
            if (!seen.add(key)) {
                continue;
            }
            TurChatCitationRecord row = new TurChatCitationRecord();
            row.setConversationId(conversationId);
            row.setAgentId(agentId);
            row.setQuestion(question);
            row.setSourceId(citation.sourceId());
            row.setDocumentTitle(citation.documentTitle());
            row.setUrl(citation.url());
            row.setCitedText(citation.citedText());
            row.setTopK(topK);
            row.setAnswerProducedAt(answerProducedAt);
            row.setCitationStale(false);
            rows.add(row);
        }
        if (rows.isEmpty()) {
            return 0;
        }
        repository.saveAll(rows);
        log.debug("[CitationDrift] persisted {} citation record(s) for conversation '{}'",
                rows.size(), conversationId);
        return rows.size();
    }

    private List<TurChatCitation> parseCitations(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            TurChatCitation[] arr = OBJECT_MAPPER.readValue(json, TurChatCitation[].class);
            return arr == null ? List.of() : List.of(arr);
        } catch (RuntimeException e) {
            log.warn("[CitationDrift] could not parse citations payload: {}", e.getMessage());
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // Detect
    // ------------------------------------------------------------------

    /**
     * Re-resolve the records due this run against the live index and persist
     * their verdicts. Records are grouped by question so each distinct query is
     * retrieved once, then every record citing a source in that result set is
     * evaluated. Returns the number of citations newly flagged stale.
     */
    @Transactional
    public int runDriftScan() {
        if (!isEnabled()) {
            return 0;
        }
        TurGenAiCitationDriftProperty cfg = props();
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds((long) cfg.getCheckWindowDays() * 86_400L);
        Instant recheckBefore = now.minusSeconds((long) cfg.getRecheckIntervalHours() * 3_600L);
        List<TurChatCitationRecord> due = repository.findDueForScan(windowStart, recheckBefore,
                PageRequest.of(0, Math.max(1, cfg.getMaxPerRun())));
        if (due.isEmpty()) {
            return 0;
        }
        Map<String, List<TurChatCitationRecord>> byQuestion = new LinkedHashMap<>();
        for (TurChatCitationRecord row : due) {
            byQuestion.computeIfAbsent(safe(row.getQuestion()), k -> new ArrayList<>()).add(row);
        }
        int flagged = 0;
        for (List<TurChatCitationRecord> group : byQuestion.values()) {
            flagged += checkGroup(group, cfg, now);
        }
        if (flagged > 0) {
            log.info("[CitationDrift] scan flagged {} of {} re-checked citation(s) as stale",
                    flagged, due.size());
        }
        return flagged;
    }

    /**
     * Re-check every record produced by the same question in one retrieval pass.
     * Records with a blank question can't be re-resolved — they are stamped
     * {@code lastCheckedAt} (so they age out of the due set) and left fresh.
     */
    private int checkGroup(List<TurChatCitationRecord> group, TurGenAiCitationDriftProperty cfg, Instant now) {
        TurChatCitationRecord first = group.get(0);
        String question = first.getQuestion();
        if (!StringUtils.hasText(question)) {
            group.forEach(r -> r.setLastCheckedAt(now));
            repository.saveAll(group);
            return 0;
        }
        int effectiveTopK = cfg.getRecheckTopK();
        for (TurChatCitationRecord r : group) {
            if (r.getTopK() != null) {
                effectiveTopK = Math.max(effectiveTopK, r.getTopK());
            }
        }
        Map<String, List<ResolvedChunk>> bySource = resolveCurrentChunks(question, effectiveTopK);
        int flagged = 0;
        for (TurChatCitationRecord r : group) {
            DriftVerdict verdict = evaluate(r.getCitedText(),
                    bySource.getOrDefault(r.getSourceId(), List.of()), r.getAnswerProducedAt());
            r.setLastCheckedAt(now);
            if (verdict.status() == Status.STALE) {
                r.setCitationStale(true);
                r.setStaleReason(verdict.reason());
                r.setDriftDetectedAt(now);
                flagged++;
            }
        }
        repository.saveAll(group);
        return flagged;
    }

    /**
     * Re-retrieve {@code question} and group the live chunks by their resolved
     * {@code sourceId}, carrying each chunk's text and parsed
     * {@code modification_date}. The same default-flags retrieval the citation
     * was produced with (see {@link TurRagSearchToolService#retrieveRawForCitations}).
     */
    private Map<String, List<ResolvedChunk>> resolveCurrentChunks(String question, int topK) {
        List<Document> docs = ragSearchToolService.retrieveRawForCitations(question, topK);
        Map<String, List<ResolvedChunk>> bySource = new LinkedHashMap<>();
        for (Document doc : docs) {
            if (doc == null) {
                continue;
            }
            String sourceId = TurRagSource.fromDocument(doc).sourceId();
            if (!StringUtils.hasText(sourceId)) {
                continue;
            }
            Instant modifiedAt = parseModificationDate(doc.getMetadata().get(TurSNFieldName.MODIFICATION_DATE));
            bySource.computeIfAbsent(sourceId, k -> new ArrayList<>())
                    .add(new ResolvedChunk(doc.getText(), modifiedAt));
        }
        return bySource;
    }

    /**
     * Synchronously re-check every citation of a conversation and persist the
     * verdicts — backs the operator's on-demand "prove it now" drift endpoint.
     * Returns the conversation's records after the re-check (stale flags fresh).
     */
    @Transactional
    public List<TurChatCitationRecord> recheckConversation(String conversationId) {
        List<TurChatCitationRecord> rows =
                repository.findByConversationIdOrderByAnswerProducedAtAsc(conversationId);
        if (rows.isEmpty()) {
            return rows;
        }
        TurGenAiCitationDriftProperty cfg = props();
        Instant now = Instant.now();
        Map<String, List<TurChatCitationRecord>> byQuestion = new LinkedHashMap<>();
        for (TurChatCitationRecord row : rows) {
            // Already-stale rows stay stale (drift is monotonic — a re-index that
            // confirmed drift isn't "un-drifted" by a later identical re-index).
            if (!row.isCitationStale()) {
                byQuestion.computeIfAbsent(safe(row.getQuestion()), k -> new ArrayList<>()).add(row);
            }
        }
        byQuestion.values().forEach(group -> checkGroup(group, cfg, now));
        return repository.findByConversationIdOrderByAnswerProducedAtAsc(conversationId);
    }

    public List<TurChatCitationRecord> findByConversation(String conversationId) {
        return repository.findByConversationIdOrderByAnswerProducedAtAsc(conversationId);
    }

    // ------------------------------------------------------------------
    // Answer-surface freshness (T840)
    // ------------------------------------------------------------------

    /** A cited source to re-check: its id and the passage the answer grounded on. */
    public record FreshnessRef(String sourceId, String citedText) {
    }

    /** Per-source freshness verdict surfaced by the answer's drift badge (T840). */
    public record FreshnessVerdict(String sourceId, Status status, String reason) {
        public boolean stale() {
            return status == Status.STALE;
        }
    }

    /**
     * T840 / §LXII.9 — synchronously re-check the freshness of an answer's cited
     * sources against the live index: a source is {@link Status#STALE} when it was
     * re-indexed after {@code answerProducedAt} or its cited passage is gone. Reuses
     * the same re-resolution + {@link #evaluate} policy the daily drift scan runs,
     * so the answer surface reports exactly what the internal signal already
     * computes. Fail-open: an empty list (no badge) when drift detection is off, the
     * question is blank, or there is nothing to re-check — never a false badge.
     *
     * @param question         the query the answer was produced for (re-retrieved)
     * @param refs             the answer's cited sources (id + grounded passage)
     * @param answerProducedAt when the answer was produced (drift is measured from here)
     * @param topK             the answer's retrieval depth; widened to the recheck floor
     * @return one verdict per distinct source, never {@code null}
     */
    public List<FreshnessVerdict> checkFreshness(String question, List<FreshnessRef> refs,
            Instant answerProducedAt, int topK) {
        if (!isEnabled() || refs == null || refs.isEmpty() || !StringUtils.hasText(question)) {
            return List.of();
        }
        int effectiveTopK = Math.max(topK, props().getRecheckTopK());
        Map<String, List<ResolvedChunk>> bySource = resolveCurrentChunks(question, effectiveTopK);
        List<FreshnessVerdict> verdicts = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (FreshnessRef ref : refs) {
            if (ref == null || !StringUtils.hasText(ref.sourceId()) || !seen.add(ref.sourceId())) {
                continue;
            }
            DriftVerdict verdict = evaluate(ref.citedText(),
                    bySource.getOrDefault(ref.sourceId(), List.of()), answerProducedAt);
            verdicts.add(new FreshnessVerdict(ref.sourceId(), verdict.status(), verdict.reason()));
        }
        return verdicts;
    }

    // ------------------------------------------------------------------
    // Verdict policy (pure — unit tested directly)
    // ------------------------------------------------------------------

    /** Drift status of a single re-resolved citation. */
    public enum Status {
        /** Cited passage still present and the source not re-indexed since the answer. */
        FRESH,
        /** Source re-indexed after the answer, or the cited passage is gone. */
        STALE,
        /** Source no longer retrievable — could not confirm either way. */
        UNVERIFIABLE
    }

    /** Outcome of {@link #evaluate}: a status plus a human-readable reason. */
    public record DriftVerdict(Status status, String reason) {
    }

    /** One live chunk of a source: its text and parsed last-modification instant (nullable). */
    public record ResolvedChunk(String text, Instant modificationDate) {
    }

    /**
     * Decide whether a citation has drifted, given the source's current chunks
     * and the instant the answer was produced. The policy, in order:
     * <ol>
     *   <li><b>Re-indexed since</b> — any current chunk's {@code modification_date}
     *       is after {@code answerProducedAt} → {@link Status#STALE}.</li>
     *   <li><b>Passage gone</b> — chunks exist but none still contains the cited
     *       span (whitespace/case-insensitive) → {@link Status#STALE}.</li>
     *   <li><b>Not retrievable</b> — the source returns no chunks →
     *       {@link Status#UNVERIFIABLE} (conservative: not flagged stale).</li>
     *   <li>Otherwise → {@link Status#FRESH}.</li>
     * </ol>
     */
    public DriftVerdict evaluate(String citedText, List<ResolvedChunk> currentChunks,
            Instant answerProducedAt) {
        if (currentChunks == null || currentChunks.isEmpty()) {
            return new DriftVerdict(Status.UNVERIFIABLE, "source no longer retrievable from the index");
        }
        if (answerProducedAt != null) {
            for (ResolvedChunk chunk : currentChunks) {
                if (chunk.modificationDate() != null
                        && chunk.modificationDate().isAfter(answerProducedAt)) {
                    return new DriftVerdict(Status.STALE,
                            "source re-indexed at " + chunk.modificationDate()
                                    + ", after the answer at " + answerProducedAt);
                }
            }
        }
        if (StringUtils.hasText(citedText)) {
            String needle = normalize(citedText);
            boolean stillPresent = currentChunks.stream()
                    .anyMatch(c -> normalize(c.text()).contains(needle));
            if (!stillPresent) {
                return new DriftVerdict(Status.STALE,
                        "cited passage no longer present in the source's current index");
            }
        }
        return new DriftVerdict(Status.FRESH, null);
    }

    /** Lowercase + whitespace-collapse so re-chunking that only reflows spacing isn't false drift. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /**
     * Parse a {@code modification_date} metadata value into an {@link Instant},
     * tolerating the shapes the index can hold: ISO-8601 string, epoch millis
     * (number or numeric string), or a {@link Date}. {@code null} when absent or
     * unparseable — the caller then falls back to the content-presence test.
     */
    static Instant parseModificationDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ignored) {
            // fall through to the looser attempts below
        }
        try {
            // Solr commonly stores dates as "yyyy-MM-dd'T'HH:mm:ss'Z'" already
            // ISO-parseable above; here we handle a bare local date-time.
            return java.time.LocalDateTime.parse(s).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // not a local date-time either
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(s));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
