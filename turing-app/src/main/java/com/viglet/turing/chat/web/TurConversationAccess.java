/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.chat.web;

import org.springframework.stereotype.Component;

import com.viglet.turing.agent.persistence.model.TurChatCitationRecord;
import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.agent.persistence.repository.TurChatCitationRecordRepository;
import com.viglet.turing.agent.persistence.repository.TurChatFlowStateRepository;
import com.viglet.turing.agent.persistence.repository.TurChatFlowSubmissionRepository;
import com.viglet.turing.shared.exception.TurNotFoundException;
import com.viglet.turing.tenant.TurTenantContext;

/**
 * T1334 — whether the caller may read a conversation by its id.
 *
 * <p>A conversation id is a cookie value, and the rows stored under it (slot
 * audit, citation records, chat memory) carry no tenant column. With tenancy on,
 * a conversation is the caller's only when a tenant-scoped record proves it: a
 * chat flow state or a flow submission for it (both {@code @TenantId}, so
 * another tenant's are invisible here), or a citation record whose agent the
 * caller can load. With none, the conversation answers as not found, rather than
 * opening another tenant's conversation to anyone who has its id. With tenancy
 * off there is one tenant and nothing to check.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurConversationAccess {

    private final TurTenantContext tenantContext;
    private final TurChatFlowStateRepository flowStateRepository;
    private final TurChatFlowSubmissionRepository submissionRepository;
    private final TurChatCitationRecordRepository citationRecordRepository;
    private final TurAIAgentRepository agentRepository;

    public TurConversationAccess(TurTenantContext tenantContext,
            TurChatFlowStateRepository flowStateRepository,
            TurChatFlowSubmissionRepository submissionRepository,
            TurChatCitationRecordRepository citationRecordRepository,
            TurAIAgentRepository agentRepository) {
        this.tenantContext = tenantContext;
        this.flowStateRepository = flowStateRepository;
        this.submissionRepository = submissionRepository;
        this.citationRecordRepository = citationRecordRepository;
        this.agentRepository = agentRepository;
    }

    /** Throws {@link TurNotFoundException} when the caller cannot see the conversation. */
    public void requireVisible(String conversationId) {
        if (!isVisible(conversationId)) {
            throw TurNotFoundException.of("Conversation", conversationId);
        }
    }

    public boolean isVisible(String conversationId) {
        if (!tenantContext.isTenancyEnabled()) {
            return true;
        }
        if (conversationId == null || conversationId.isBlank()) {
            return false;
        }
        if (!flowStateRepository.findByConversationId(conversationId).isEmpty()
                || !submissionRepository.findByConversationIdOrderByCompletedAtDesc(conversationId).isEmpty()) {
            return true;
        }
        return citationRecordRepository.findByConversationIdOrderByAnswerProducedAtAsc(conversationId).stream()
                .map(TurChatCitationRecord::getAgentId)
                .filter(agentId -> agentId != null && !agentId.isBlank())
                .distinct()
                .anyMatch(agentRepository::existsById);
    }
}
