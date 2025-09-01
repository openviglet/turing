/*
 *
 * Copyright (C) 2016-2025 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.genai;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.ABSTRACT;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.ID;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.MODIFICATION_DATE;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.PUBLICATION_DATE;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.SOURCE_APPS;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.TEXT;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.TITLE;
import static com.viglet.turing.commons.sn.field.TurSNFieldName.URL;
import static org.apache.commons.lang3.stream.LangCollectors.joining;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.client.sn.job.TurSNJobItems;
import com.viglet.turing.sn.TurSNSearchProcess;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TurGenAi {
    public static final String SITES = "sites";
    public static final String LOCALE = "locale";
    public static final String QUESTION = "question";
    public static final String INFORMATION = "information";
    private final TurSNSearchProcess turSNSearchProcess;

    public TurGenAi(TurSNSearchProcess turSNSearchProcess) {
        this.turSNSearchProcess = turSNSearchProcess;
    }

    public TurChatMessage assistant(TurGenAiContext context, String q) {
        if (context.isEnabled()) {
            int tokenCounter = new StringTokenizer(q).countTokens();
            if (tokenCounter > 1) {
                return getTurChatMessage(context, q);
            } else {
                if (tokenCounter == 1 && !q.contains("*")) {
                    return getTurChatMessage(context, "what is %s?".formatted(q));
                } else {
                    return TurChatMessage.builder().text(null).enabled(true).build();
                }
            }
        } else {
            return TurChatMessage.builder().text("AI configuration is not enabled").enabled(false).build();
        }
    }

    private TurChatMessage getTurChatMessage(TurGenAiContext context, String q) {
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(context.getEmbeddingModel().embed(q).content())
                .maxResults(10)
                .minScore(0.7)
                .build();
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = context.getChromaEmbeddingStore()
                .search(embeddingSearchRequest).matches();
        PromptTemplate promptTemplate = PromptTemplate.from(context.getSystemPrompt());
        String information = relevantEmbeddings.stream()
                .map(match -> match.embedded().text())
                .collect(joining("\n\n"));

        Map<String, Object> variables = new HashMap<>();
        variables.put(QUESTION, q);
        variables.put(INFORMATION, information);
        Prompt prompt = promptTemplate.apply(variables);
        return TurChatMessage.builder()
                .text(context.getChatLanguageModel().chat(prompt.toUserMessage()).aiMessage().text())
                .enabled(true)
                .build();
    }

    public void addDocuments(TurSNJobItems turSNJobItems) {
        turSNJobItems.getTuringDocuments().forEach(jobItem -> jobItem.getSiteNames()
                .forEach(siteName -> turSNSearchProcess.getSNSite(siteName).ifPresent(turSNSite -> {
                    TurGenAiContext context = new TurGenAiContext(turSNSite.getTurSNSiteGenAi());
                    if (context.isEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        addAttributes(context, jobItem, sb);
                        addDocument(context, sb.toString(), new Metadata(setMetadata(jobItem)));
                    }
                })));
    }

    private void addDocument(TurGenAiContext context, String text, Metadata metadata) {
        Document document = new DefaultDocument(text, metadata);
        DocumentByCharacterSplitter documentByCharacterSplitter = new DocumentByCharacterSplitter(1024, 0);
        EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentByCharacterSplitter)
                .embeddingModel(context.getEmbeddingModel())
                .embeddingStore(context.getChromaEmbeddingStore())
                .build();
        embeddingStoreIngestor.ingest(document);
        log.info("added document to embedding store: {}", metadata.getString(ID));
    }

    private void addAttributes(TurGenAiContext context, TurSNJobItem jobItem, StringBuilder sb) {
        if (context.isEnabled()) {
            String[] allowedAttributes = { TITLE, ABSTRACT, TEXT };
            jobItem.getAttributes().forEach((key, value) -> {
                if (Arrays.asList(allowedAttributes).contains(key))
                    sb.append(value).append(System.lineSeparator());
            });
        }
    }

    @NotNull
    private Map<String, Object> setMetadata(TurSNJobItem jobItem) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ID, jobItem.getId());
        metadata.put(LOCALE, jobItem.getLocale().toString());
        metadata.put(SOURCE_APPS, jobItem.getProviderName());
        metadata.put(SITES, jobItem.getSiteNames().getFirst());
        if (jobItem.getAttributes().containsKey(MODIFICATION_DATE)) {
            metadata.put(MODIFICATION_DATE, jobItem.getAttributes().get(MODIFICATION_DATE));
        }
        if (jobItem.getAttributes().containsKey(PUBLICATION_DATE)) {
            metadata.put(PUBLICATION_DATE, jobItem.getAttributes().get(PUBLICATION_DATE));
        }
        if (jobItem.getAttributes().containsKey(URL)) {
            metadata.put(URL, jobItem.getAttributes().get(URL));
        }
        return metadata;
    }
}
