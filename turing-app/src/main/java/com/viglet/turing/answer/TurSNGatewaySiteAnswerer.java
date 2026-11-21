/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.answer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.viglet.turing.agent.TurChatMessage;
import com.viglet.turing.agent.llm.gateway.TurGatewaySiteAnswerer;
import com.viglet.turing.llm.gateway.TurOpenAiWire.WireMessage;
import com.viglet.turing.rag.TurGenAiContext;

/**
 * T1322g — the SN chat behind the gateway's {@code turing-sn/<site>} models.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSNGatewaySiteAnswerer implements TurGatewaySiteAnswerer {

    private final TurSNGenAi snGenAi;

    public TurSNGatewaySiteAnswerer(TurSNGenAi snGenAi) {
        this.snGenAi = snGenAi;
    }

    @Override
    public String answer(TurGenAiContext context, List<WireMessage> messages) {
        List<TurSNGenAi.ConversationMessage> history = messages == null ? List.of()
                : messages.stream()
                        .map(m -> new TurSNGenAi.ConversationMessage(m.role(), m.content()))
                        .toList();
        TurChatMessage answer = snGenAi.assistantConversation(context, history);
        return (answer == null || answer.getText() == null) ? "" : answer.getText();
    }
}
