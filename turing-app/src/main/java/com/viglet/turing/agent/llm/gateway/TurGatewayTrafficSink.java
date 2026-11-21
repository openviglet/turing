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
import com.viglet.turing.llm.persistence.model.TurLLMInstance;

/**
 * T1322f — where the gateway hands a completed turn for capture, declared by agent
 * and implemented by eval (which turns it into an eval-dataset row), so agent does
 * not import eval.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurGatewayTrafficSink {

    /** Whether captured turns are being kept. */
    boolean isEnabled();

    /** The dataset captured turns go to, or {@code null} before the first capture. */
    String currentDatasetId();

    /** Keeps one completed gateway turn. Fail-open: never throws. */
    void capture(String model, List<WireMessage> messages, String answer, TurLLMInstance instance,
            String keyId);
}
