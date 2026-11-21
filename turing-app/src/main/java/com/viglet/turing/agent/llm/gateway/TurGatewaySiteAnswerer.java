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
package com.viglet.turing.agent.llm.gateway;

import java.util.List;

import com.viglet.turing.llm.gateway.TurOpenAiWire.WireMessage;
import com.viglet.turing.rag.TurGenAiContext;

/**
 * T1322g — the RAG-grounded answer the gateway returns for a {@code turing-sn/<site>}
 * model, declared by agent and implemented by answer (the SN chat), so agent does
 * not import the module above it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurGatewaySiteAnswerer {

    /** The assistant's answer to the conversation, grounded in the site; never {@code null}. */
    String answer(TurGenAiContext context, List<WireMessage> messages);
}
