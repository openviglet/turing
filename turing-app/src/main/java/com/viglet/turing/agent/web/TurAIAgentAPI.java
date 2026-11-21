package com.viglet.turing.agent.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.exchange.agent.TurAIAgentExportService;
import com.viglet.turing.agent.persistence.dto.TurAIAgentDto;
import com.viglet.turing.agent.persistence.mapper.TurAIAgentMapper;
import com.viglet.turing.agent.persistence.model.TurAIAgent;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.agent.persistence.repository.customtool.TurCustomToolRepository;
import com.viglet.turing.llm.persistence.repository.embedding.TurEmbeddingModelRepository;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;
import com.viglet.turing.mcp.persistence.repository.TurMcpServerRepository;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;
import com.viglet.turing.shared.exception.TurConflictException;
import com.viglet.turing.sn.api.TurSNSiteGenAis;
import com.viglet.turing.llm.persistence.repository.store.TurStoreInstanceRepository;
import com.viglet.turing.spring.utils.TurPersistenceUtils;
import com.viglet.turing.system.TurGlobalSettingsService;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ai-agent")
@Tag(name = "AI Agent", description = "AI Agent API")
public class TurAIAgentAPI {
    private final TurAIAgentRepository turAIAgentRepository;
    private final TurAIAgentMapper turAIAgentMapper;
    private final TurLLMInstanceRepository turLLMInstanceRepository;
    private final TurMcpServerRepository turMcpServerRepository;
    private final TurCustomToolRepository turCustomToolRepository;
    private final TurEmbeddingModelRepository turEmbeddingModelRepository;
    private final TurStoreInstanceRepository turStoreInstanceRepository;
    private final com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository turSEInstanceRepository;
    private final TurPersonaRepository turPersonaRepository;
    private final TurSNSiteGenAis turSNSiteGenAiRepository;
    private final TurGlobalSettingsService turGlobalSettingsService;
    private final TurAIAgentExportService turAIAgentExportService;
    private final com.viglet.turing.tenant.TurTenantQuotaService turTenantQuotaService;

    public TurAIAgentAPI(TurAIAgentRepository turAIAgentRepository,
            TurAIAgentMapper turAIAgentMapper,
            TurLLMInstanceRepository turLLMInstanceRepository,
            TurMcpServerRepository turMcpServerRepository,
            TurCustomToolRepository turCustomToolRepository,
            TurEmbeddingModelRepository turEmbeddingModelRepository,
            TurStoreInstanceRepository turStoreInstanceRepository,
            com.viglet.turing.searchengine.persistence.repository.TurSEInstanceRepository turSEInstanceRepository,
            TurPersonaRepository turPersonaRepository,
            TurSNSiteGenAis turSNSiteGenAiRepository,
            TurGlobalSettingsService turGlobalSettingsService,
            TurAIAgentExportService turAIAgentExportService,
            com.viglet.turing.tenant.TurTenantQuotaService turTenantQuotaService) {
        this.turAIAgentRepository = turAIAgentRepository;
        this.turAIAgentMapper = turAIAgentMapper;
        this.turLLMInstanceRepository = turLLMInstanceRepository;
        this.turMcpServerRepository = turMcpServerRepository;
        this.turCustomToolRepository = turCustomToolRepository;
        this.turEmbeddingModelRepository = turEmbeddingModelRepository;
        this.turStoreInstanceRepository = turStoreInstanceRepository;
        this.turSEInstanceRepository = turSEInstanceRepository;
        this.turPersonaRepository = turPersonaRepository;
        this.turSNSiteGenAiRepository = turSNSiteGenAiRepository;
        this.turGlobalSettingsService = turGlobalSettingsService;
        this.turAIAgentExportService = turAIAgentExportService;
        this.turTenantQuotaService = turTenantQuotaService;
    }

    @Operation(summary = "AI Agent List")
    @GetMapping
    @Secured({"ROLE_ADMIN", "AI_AGENT_VIEW"})
    public List<TurAIAgentDto> turAIAgentList() {
        return turAIAgentMapper
                .toDtoList(this.turAIAgentRepository.findAll(TurPersistenceUtils.orderByTitleIgnoreCase()));
    }

    @Operation(summary = "Export an AI Agent as a ZIP bundle",
            description = "Produces a ZIP containing agent.json (TurExchange envelope), one " +
                    "chat-flows/{slug}.chat-flow.json per chat flow, and one tools/{slug}.groovy " +
                    "per custom tool. API keys (LLM apiKey/apiKeyEncrypted) are never serialized.")
    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Secured({"ROLE_ADMIN", "AI_AGENT_VIEW"})
    public StreamingResponseBody turAIAgentExport(@PathVariable String id,
            HttpServletResponse response) {
        TurAIAgent agent = turAIAgentRepository.findById(id).orElse(null);
        if (agent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Agent not found: " + id);
        }
        Path zipPath = turAIAgentExportService.exportAgentToZip(id);
        String filename = "agent-" + safeFilenamePart(agent.getTitle(), id) + ".zip";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);
        return output -> {
            try {
                Files.copy(zipPath, output);
                output.flush();
            } catch (IOException e) {
                log.error("Failed to stream agent export for {}: {}", id, e.getMessage(), e);
            } finally {
                turAIAgentExportService.cleanup(zipPath);
            }
        };
    }

    private static String safeFilenamePart(String name, String id) {
        String base = (name == null ? "untitled" : name)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-)|(-$)", "");
        if (base.isEmpty()) base = "untitled";
        return base + "-" + (id == null ? "" : id.substring(0, Math.min(8, id.length())));
    }

    @Operation(summary = "AI Agent structure")
    @GetMapping("/structure")
    @Secured({"ROLE_ADMIN", "AI_AGENT_VIEW"})
    public TurAIAgentDto turAIAgentStructure() {
        return turAIAgentMapper.toDto(new TurAIAgent());
    }

    @Operation(summary = "Show an AI Agent")
    @GetMapping("/{id}")
    @Secured({"ROLE_ADMIN", "AI_AGENT_VIEW"})
    public TurAIAgentDto turAIAgentGet(@PathVariable String id) {
        return turAIAgentMapper
                .toDto(this.turAIAgentRepository.findById(id).orElse(new TurAIAgent()));
    }

    @Operation(summary = "Update an AI Agent")
    @PutMapping("/{id}")
    @Secured({"ROLE_ADMIN", "AI_AGENT_EDIT"})
    public TurAIAgentDto turAIAgentUpdate(@PathVariable String id,
            @RequestBody TurAIAgentDto turAIAgentDto) {
        TurAIAgent turAIAgent = turAIAgentMapper.toEntity(turAIAgentDto);
        return turAIAgentRepository.findById(id).map(agentEdit -> {
            // T1319 — a save made from an older version would overwrite a change
            // the caller never saw; it is refused instead.
            if (turAIAgent.getVersion() != null && agentEdit.getVersion() != null
                    && !turAIAgent.getVersion().equals(agentEdit.getVersion())) {
                throw new TurConflictException(
                        "This agent was changed by someone else since you opened it. Reload it and try again.");
            }
            agentEdit.setTitle(turAIAgent.getTitle());
            agentEdit.setDescription(turAIAgent.getDescription());
            agentEdit.setIcon(turAIAgent.getIcon());
            agentEdit.setSystemPrompt(turAIAgent.getSystemPrompt());
            agentEdit.setSystemPromptMetaPrompt(turAIAgent.getSystemPromptMetaPrompt());
            agentEdit.setEnabled(turAIAgent.getEnabled());
            agentEdit.setNativeTools(turAIAgent.getNativeTools());
            // T433 / §X.18.b — per-agent provider-native capability selection.
            agentEdit.setNativeCapabilities(turAIAgent.getNativeCapabilities());
            // T435 / §X.18.d — per-agent REQUEST_OPTION selections.
            agentEdit.setRequestOptionsJson(turAIAgent.getRequestOptionsJson());
            agentEdit.setLlmInstanceIds(existingLlmInstanceIds(turAIAgent.getLlmInstanceIds()));
            // T1315 — the MCP servers are held by id; keep only the ones that exist.
            agentEdit.setMcpServerIds(existingMcpServerIds(turAIAgent.getMcpServerIds()));
            agentEdit.setCustomTools(resolveRelations(
                    turAIAgent.getCustomTools(), turCustomToolRepository));
            agentEdit.setEmbeddingModelInstanceId(existingId(turAIAgent.getEmbeddingModelInstanceId(),
                    turEmbeddingModelRepository));
            agentEdit.setStoreInstanceId(existingId(turAIAgent.getStoreInstanceId(),
                    turStoreInstanceRepository));
            // T28 / §III.5 Phase C — per-agent SE binding for the analytics
            // intent index. Null clears the binding so the indexer falls back
            // to the global default (property / first registered SE).
            agentEdit.setAnalyticsSeInstanceId(existingSeInstanceId(turAIAgent.getAnalyticsSeInstanceId()));
            // Personas: catalog + default, both by id (T1315). The default, when
            // set, must belong to the catalog — otherwise the runtime resolver
            // would silently fall back to null on every turn. We enforce it
            // here once instead of every chat request.
            Set<String> personaCatalog = existingPersonaIds(turAIAgent.getPersonaIds());
            agentEdit.setPersonaIds(personaCatalog);
            agentEdit.setDefaultPersonaId(
                    validDefaultPersonaId(turAIAgent.getDefaultPersonaId(), personaCatalog));
            agentEdit.setRagEnabled(canEnableRag(turAIAgent.isRagEnabled(),
                    agentEdit.getLlmInstanceIds(),
                    agentEdit.getEmbeddingModelInstanceId(),
                    agentEdit.getStoreInstanceId()));
            // Rich-content rendering opt-in (```html / ```d2 live preview).
            // Field-by-field copy like the other toggles — omitting it would
            // silently drop the switch on save.
            agentEdit.setRichContentEnabled(turAIAgent.isRichContentEnabled());
            agentEdit.setDiscloseKnowledgeCutoff(turAIAgent.isDiscloseKnowledgeCutoff());
            // T442 — answer-as-an-app generative-UI client tools (opt-in).
            agentEdit.setAnswerAsAppEnabled(turAIAgent.isAnswerAsAppEnabled());
            // T443 — co-browse: agent drives the host page's real search UI (opt-in).
            agentEdit.setCoBrowseEnabled(turAIAgent.isCoBrowseEnabled());
            // T445 — ambient / proactive copilot (opt-in) + its signal threshold.
            agentEdit.setProactiveEnabled(turAIAgent.isProactiveEnabled());
            agentEdit.setProactiveThresholdSignals(turAIAgent.getProactiveThresholdSignals());
            // T446 — cross-conversation personal memory (opt-in).
            agentEdit.setUserMemoryEnabled(turAIAgent.isUserMemoryEnabled());
            // T447 — self-tuning loop (opt-in; opens human-approved suggestions).
            agentEdit.setSelfTuningEnabled(turAIAgent.isSelfTuningEnabled());
            // T448 — agent-to-agent handoff (opt-in) + the specialist allowlist.
            agentEdit.setAgentHandoffEnabled(turAIAgent.isAgentHandoffEnabled());
            agentEdit.setSpecialistAgentIds(turAIAgent.getSpecialistAgentIds());
            // T450 — embeddable action widget (opt-in host-action client tools).
            agentEdit.setActionWidgetEnabled(turAIAgent.isActionWidgetEnabled());
            // T323 — skills opt-in (run_skill delegation to the Default LLM).
            // Field-by-field copy like the other toggles.
            agentEdit.setSkillsEnabled(turAIAgent.isSkillsEnabled());
            // T145 — federate the agent's MCP servers to the native vendor.
            agentEdit.setMcpNativeFederation(turAIAgent.isMcpNativeFederation());
            // T436 — live tool-call events on the chat SSE (opt-in).
            agentEdit.setToolCallEventsEnabled(turAIAgent.isToolCallEventsEnabled());
            // T618 — capture the assembled prompt per turn for verbatim replay (opt-in).
            agentEdit.setPromptCaptureEnabled(turAIAgent.isPromptCaptureEnabled());
            // T438 — frontend ("client") tools (opt-in) + their declarations.
            agentEdit.setClientToolsEnabled(turAIAgent.isClientToolsEnabled());
            agentEdit.setClientToolsJson(turAIAgent.getClientToolsJson());
            // Chat memory configuration — silently dropped before this fix,
            // which is why toggling the UI switch had no effect after save.
            agentEdit.setChatMemoryEnabled(turAIAgent.isChatMemoryEnabled());
            agentEdit.setChatMemoryFlushIntervalMinutes(
                    Math.max(1, turAIAgent.getChatMemoryFlushIntervalMinutes()));
            agentEdit.setChatMemoryMaxMessages(
                    Math.max(1, turAIAgent.getChatMemoryMaxMessages()));
            // Agent-specific Python deps addendum — unioned with the global
            // GLOBAL_PYTHON_REQUIREMENTS by the Code Interpreter dep service
            // before pip install. NULL = global-only, the safe default that
            // preserves pre-2026.2.7 behavior for agents that don't need
            // anything extra.
            agentEdit.setPythonRequirements(turAIAgent.getPythonRequirements());
            // Chat mode (CALL / STREAM). Same field-by-field copy pattern as
            // the rest of this update: if the assignment is missing, Hibernate
            // silently keeps the previously-persisted value and the toggle in
            // the form has no effect after save. NPE-safe — keep current value
            // when the DTO omits the field (older clients).
            if (turAIAgent.getChatMode() != null) {
                agentEdit.setChatMode(turAIAgent.getChatMode());
            }
            // T66 / §VII.6.g — submission retention policy. Same field-by-field
            // copy contract: null DTO value keeps the previously-persisted mode
            // (older clients). The days threshold is copied verbatim (null is a
            // valid "no threshold" state for RETAIN_DAYS).
            if (turAIAgent.getSubmissionRetention() != null) {
                agentEdit.setSubmissionRetention(turAIAgent.getSubmissionRetention());
            }
            agentEdit.setSubmissionRetentionDays(turAIAgent.getSubmissionRetentionDays());
            // T166 / §X.9.d — memory tool scope (CONVERSATION / USER). Same
            // field-by-field copy contract: a null DTO value keeps the
            // previously-persisted scope (older clients).
            if (turAIAgent.getMemoryScope() != null) {
                agentEdit.setMemoryScope(turAIAgent.getMemoryScope());
            }
            // Grounding policy (OPEN / STRICT_RAG). Same field-by-field copy
            // contract: a null DTO value keeps the previously-persisted mode
            // (older clients), never nulling a non-null column.
            if (turAIAgent.getGroundingMode() != null) {
                agentEdit.setGroundingMode(turAIAgent.getGroundingMode());
            }
            // T19/T24 reposition (2026.2.7) — ragBm25Fallback and
            // ragHybridSearch moved to TurSNSiteGenAi; no longer copied
            // here. See TurSNSiteGenAi field javadoc + TurSNSiteAPI for
            // the persistence side, and the SN site GenAI settings form
            // for the UI.
            // T1319 — the merged copy carries the incremented version the next save sends back.
            return turAIAgentMapper.toDto(this.turAIAgentRepository.save(agentEdit));
        }).orElse(new TurAIAgentDto());
    }

    @Transactional
    @Operation(summary = "Delete an AI Agent")
    @DeleteMapping("/{id}")
    @Secured({"ROLE_ADMIN", "AI_AGENT_DELETE"})
    public boolean turAIAgentDelete(@PathVariable String id) {
        // SN site GenAI configs may point at this agent via the
        // fk_sn_site_genai_ai_agent FK. Detach them first (null out agent_id)
        // so the site keeps its GenAI config but no longer references a
        // deleted agent — otherwise the FK rejects the agent delete.
        //
        // saveAndFlush (vs. save) is required because the agent delete on the
        // next line is a `@Modifying` JPQL bulk query that bypasses the JPA
        // persistence context: a plain save() schedules the UPDATE but the
        // database doesn't see it until flush, so the FK constraint check
        // fires against the still-old agent_id and rejects the delete.
        var attachedSites = this.turSNSiteGenAiRepository.findByTurAIAgentId(id);
        for (var siteGenAi : attachedSites) {
            siteGenAi.setTurAIAgentId(null);
            this.turSNSiteGenAiRepository.saveAndFlush(siteGenAi);
        }
        // T906 / §LXVIII.13 — deleteById loads the entity, so @PostRemove fires and
        // TurSNSiteSnapshotEvictionListener drops the snapshot that still names this
        // agent. The bulk delete this replaces fired nothing and compensated with
        // nothing, so a deleted agent stayed in the snapshot cache indefinitely.
        this.turAIAgentRepository.deleteById(id);
        return true;
    }

    @Operation(summary = "Create an AI Agent")
    @PostMapping
    @Secured({"ROLE_ADMIN", "AI_AGENT_CREATE"})
    public TurAIAgentDto turAIAgentAdd(@RequestBody TurAIAgentDto turAIAgentDto) {
        // T963 / §LXXII — the plan's agent limit, enforced here at the HTTP
        // boundary rather than in the repository beneath it. That placement is
        // the exemption: the startup seeders and the import path create agents
        // through the repository directly, and neither is a tenant spending
        // their plan — a gate one layer down would refuse a fresh tenant's own
        // provisioning, or need a flag threaded through to say "not this one".
        turTenantQuotaService.checkCanCreateAgent();
        TurAIAgent turAIAgent = turAIAgentMapper.toEntity(turAIAgentDto);
        turAIAgent.setLlmInstanceIds(existingLlmInstanceIds(turAIAgent.getLlmInstanceIds()));
        turAIAgent.setMcpServerIds(existingMcpServerIds(turAIAgent.getMcpServerIds()));
        turAIAgent.setCustomTools(resolveRelations(
                turAIAgent.getCustomTools(), turCustomToolRepository));
        turAIAgent.setEmbeddingModelInstanceId(existingId(turAIAgent.getEmbeddingModelInstanceId(),
                turEmbeddingModelRepository));
        turAIAgent.setStoreInstanceId(existingId(turAIAgent.getStoreInstanceId(),
                turStoreInstanceRepository));
        // T28 / §III.5 Phase C — same per-agent SE binding propagation on create.
        turAIAgent.setAnalyticsSeInstanceId(existingSeInstanceId(turAIAgent.getAnalyticsSeInstanceId()));
        Set<String> personaCatalog = existingPersonaIds(turAIAgent.getPersonaIds());
        turAIAgent.setPersonaIds(personaCatalog);
        turAIAgent.setDefaultPersonaId(
                validDefaultPersonaId(turAIAgent.getDefaultPersonaId(), personaCatalog));
        turAIAgent.setRagEnabled(canEnableRag(turAIAgent.isRagEnabled(),
                turAIAgent.getLlmInstanceIds(),
                turAIAgent.getEmbeddingModelInstanceId(),
                turAIAgent.getStoreInstanceId()));
        this.turAIAgentRepository.save(turAIAgent);
        return turAIAgentMapper.toDto(turAIAgent);
    }

    /**
     * T1315 — the default persona is held by id. An id no persona has is
     * dropped; one that exists outside the agent's catalog is refused, since
     * the runtime reads the default from the catalog.
     */
    private java.util.Set<String> existingMcpServerIds(java.util.Set<String> requested) {
        java.util.Set<String> existing = new java.util.HashSet<>();
        if (requested != null && !requested.isEmpty()) {
            turMcpServerRepository.findAllById(requested).forEach(server -> existing.add(server.getId()));
        }
        return existing;
    }

    /** T1315 — the LLM instance ids that name an existing instance; the rest are dropped. */
    private Set<String> existingLlmInstanceIds(Set<String> requested) {
        Set<String> existing = new java.util.HashSet<>();
        if (requested != null && !requested.isEmpty()) {
            turLLMInstanceRepository.findAllById(requested).forEach(llm -> existing.add(llm.getId()));
        }
        return existing;
    }

    /** T1315 — the persona ids that name an existing persona; the rest are dropped. */
    private Set<String> existingPersonaIds(Set<String> requested) {
        Set<String> existing = new java.util.HashSet<>();
        if (requested != null && !requested.isEmpty()) {
            turPersonaRepository.findAllById(requested).forEach(persona -> existing.add(persona.getId()));
        }
        return existing;
    }

    private String validDefaultPersonaId(String requestedId, Set<String> personaCatalog) {
        if (requestedId == null || !turPersonaRepository.existsById(requestedId)) {
            return null;
        }
        if (!personaCatalog.contains(requestedId)) {
            throw new IllegalArgumentException(
                    "Default persona must be one of the agent's allowed personas");
        }
        return requestedId;
    }

    /**
     * Resolves a single ManyToOne relation. If the incoming entity has an id,
     * fetches the managed instance from the repo. Returns null if not found or
     * if the input was null.
     */
    /** T1315 — the id when that row exists in {@code repo}, else null: never a dangling id. */
    private static String existingId(String id,
            org.springframework.data.jpa.repository.JpaRepository<?, String> repo) {
        return id != null && !id.isBlank() && repo.existsById(id) ? id : null;
    }

    /**
     * T1315 — the SE instance belongs to the searchengine module, so the agent holds
     * it by id: the id when that instance exists, else null, never a dangling id.
     */
    private String existingSeInstanceId(String id) {
        return id != null && !id.isBlank() && turSEInstanceRepository.existsById(id) ? id : null;
    }

    private <T> T resolveSingleRelation(
            T incoming,
            org.springframework.data.jpa.repository.JpaRepository<T, String> repo) {
        if (incoming == null) {
            return null;
        }
        if (incoming instanceof org.springframework.data.domain.Persistable) {
            var persistable = (org.springframework.data.domain.Persistable<?>) incoming;
            String id = (String) persistable.getId();
            if (id == null) {
                return null;
            }
            return repo.findById(id).orElse(null);
        }
        return incoming;
    }

    /**
     * RAG can only be enabled on the agent when (a) at least one LLM instance
     * is bound — without an LLM there is nothing to generate the answer; and
     * (b) an embedding model and an embedding store are resolvable, either
     * set on the agent itself or via the global RAG defaults configured in
     * Global Settings. Silently flipping {@code ragEnabled} back to false
     * here keeps the data model consistent so {@code TurSNSiteGenAiAPI}'s
     * diagnostic walk never has to backtrack — but the admin frontend should
     * still block the toggle on its side for a useful error message.
     */
    private boolean canEnableRag(boolean requested,
            Set<String> llmInstanceIds,
            String embeddingModelId,
            String storeInstanceId) {
        if (!requested) {
            return false;
        }
        boolean hasLlm = llmInstanceIds != null && !llmInstanceIds.isEmpty();
        boolean modelOk = embeddingModelId != null
                || StringUtils.hasText(turGlobalSettingsService.getDefaultEmbeddingModelId());
        boolean storeOk = storeInstanceId != null
                || StringUtils.hasText(turGlobalSettingsService.getDefaultEmbeddingStoreId());
        return hasLlm && modelOk && storeOk;
    }

    private <T> Set<T> resolveRelations(
            Set<T> incoming,
            org.springframework.data.jpa.repository.JpaRepository<T, String> repo) {
        if (incoming == null || incoming.isEmpty()) {
            return new HashSet<>();
        }
        return incoming.stream()
                .flatMap(entity -> {
                    if (entity instanceof org.springframework.data.domain.Persistable) {
                        var persistable = (org.springframework.data.domain.Persistable<?>) entity;
                        String id = (String) persistable.getId();
                        return repo.findById(id).stream();
                    }
                    return java.util.stream.Stream.of(entity);
                })
                .collect(Collectors.toSet());
    }
}
