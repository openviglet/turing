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
package com.viglet.turing.llm.nativeapi.openai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseIncludable;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.viglet.turing.llm.logprob.TurLogprobConfidence;
import com.viglet.turing.llm.logprob.TurLogprobConfidence.TokenLogprob;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * T180 — the OpenAI Responses relevance classifier, a provider call with no agent
 * in it. Split from {@code TurOpenAiResponsesService} (T1322c) so the reranker in
 * rag reaches it in llm while the agent-running half of that service lives in
 * agent.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
public class TurOpenAiRelevanceClassifier {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    /**
     * T180 / §X.13.c — one-shot yes/no relevance classification of a snippet
     * against a query, returning the model's calibrated <b>P(relevant)</b> in
     * {@code [0,1]} read from the first answer token's {@code top_logprobs}.
     *
     * <p>The Responses request constrains the model to a single yes/no token
     * ({@code maxOutputTokens=1}) with {@code top_logprobs} on, so the log-prob of
     * {@code yes} vs {@code no} is the model's confidence. Fail-open: any error,
     * missing logprobs, or a non-reasoning failure returns {@link Optional#empty}
     * so the reranker keeps retrieval order. Pure scoring lives in
     * {@link TurLogprobConfidence}.
     */
    public Optional<Double> classifyRelevanceProbability(OpenAIClient client, TurLLMInstance instance,
            String query, String snippet) {
        if (client == null || !StringUtils.hasText(query) || !StringUtils.hasText(snippet)) {
            return Optional.empty();
        }
        try {
            String input = "Question: " + query + "\n\nSnippet:\n" + snippet
                    + "\n\nIs the snippet relevant to answering the question? Answer with only "
                    + "\"yes\" or \"no\".";
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(StringUtils.hasText(instance.getModelName()) ? instance.getModelName() : DEFAULT_MODEL)
                    .instructions("You are a search relevance classifier. Reply with only \"yes\" or "
                            + "\"no\" — no other text.")
                    .input(input)
                    .maxOutputTokens(1L)
                    .topLogprobs(RELEVANCE_TOP_LOGPROBS)
                    .addInclude(ResponseIncludable.MESSAGE_OUTPUT_TEXT_LOGPROBS)
                    .build();
            Response response = client.responses().create(params);
            List<TokenLogprob> alternatives = firstTokenLogprobs(response);
            if (alternatives.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(TurLogprobConfidence.relevanceProbability(alternatives));
        } catch (RuntimeException e) {
            log.debug("[Native][OpenAI-Responses] relevance logprob classify failed: {}",
                    e.getMessage());
            return Optional.empty();
        }
    }

    /** How many token alternatives to request so both {@code yes} and {@code no} are visible. */
    private static final long RELEVANCE_TOP_LOGPROBS = 10L;

    /**
     * The first output-text token's logprob alternatives (the chosen token plus
     * its {@code top_logprobs}), mapped onto the provider-agnostic
     * {@link TokenLogprob}. Empty when the response carried no message text /
     * logprobs (logprobs not returned).
     */
    private static List<TokenLogprob> firstTokenLogprobs(Response response) {
        for (ResponseOutputItem item : response.output()) {
            if (!item.isMessage()) {
                continue;
            }
            for (ResponseOutputMessage.Content content : item.asMessage().content()) {
                if (!content.isOutputText()) {
                    continue;
                }
                Optional<List<ResponseOutputText.Logprob>> logprobs = content.asOutputText().logprobs();
                if (logprobs.isEmpty() || logprobs.get().isEmpty()) {
                    continue;
                }
                ResponseOutputText.Logprob first = logprobs.get().get(0);
                List<TokenLogprob> alternatives = new ArrayList<>();
                alternatives.add(new TokenLogprob(first.token(), first.logprob()));
                for (ResponseOutputText.Logprob.TopLogprob top : first.topLogprobs()) {
                    alternatives.add(new TokenLogprob(top.token(), top.logprob()));
                }
                return alternatives;
            }
        }
        return List.of();
    }
}
