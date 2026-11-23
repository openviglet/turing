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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viglet.turing.batch.TurBatchChatRequest;
import com.viglet.turing.batch.TurBatchChatResult;
import com.viglet.turing.batch.TurBatchCompletionHandler;
import com.viglet.turing.batch.TurBatchInferenceService;
import com.viglet.turing.batch.TurBatchState;
import com.viglet.turing.batch.persistence.model.TurBatchJob;
import com.viglet.turing.batch.persistence.repository.TurBatchJobRepository;
import com.viglet.turing.llm.gateway.TurGatewayContext;
import com.viglet.turing.llm.gateway.TurGatewayKeyService;
import com.viglet.turing.llm.gateway.TurOpenAiBatchWire.BatchCreateRequest;
import com.viglet.turing.llm.gateway.TurOpenAiBatchWire.BatchObject;
import com.viglet.turing.llm.gateway.TurOpenAiBatchWire.FileObject;
import com.viglet.turing.llm.gateway.TurOpenAiBatchWire.RequestCounts;
import com.viglet.turing.llm.gateway.TurOpenAiWire.ChatCompletion;
import com.viglet.turing.llm.gateway.TurOpenAiWire.Choice;
import com.viglet.turing.llm.gateway.TurOpenAiWire.Usage;
import com.viglet.turing.llm.gateway.TurOpenAiWire.WireMessage;
import com.viglet.turing.llm.cost.tokenusage.TurLLMTokenUsageService;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import com.viglet.turing.llm.persistence.repository.TurLLMInstanceRepository;
import com.viglet.turing.llm.persistence.model.gateway.TurGatewayBatchFile;
import com.viglet.turing.llm.persistence.model.gateway.TurGatewayKey;
import com.viglet.turing.llm.persistence.repository.gateway.TurGatewayBatchFileRepository;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * T1341 / §CXIII.6 — OpenAI's batch wire on the gateway: a client uploads a JSONL
 * of {@code /v1/chat/completions} requests ({@code POST /v1/files}), batches it
 * ({@code POST /v1/batches}), polls it and reads the output JSONL, and pays the
 * vendor's batch price instead of one synchronous call per line.
 *
 * <p>Behind it is T156's {@link TurBatchInferenceService}: a gateway batch is a
 * {@code batch_job} row with purpose {@value #PURPOSE}, and its output is read
 * back from the vendor when asked for, so only the input file is stored. The
 * key's model allowlist applies to every line, and a line it refuses (or one the
 * batch tier cannot express) is an error line in the output, never a failed
 * batch (T1092).
 *
 * <p>Within one batch every accepted line must resolve to the same LLM
 * instance, and a line carries system messages and one user message: that is
 * what the vendor batch providers send. A vendor without a batch tier is refused
 * for now rather than run line by line.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Service
public class TurGatewayBatchService implements TurBatchCompletionHandler {

    static final String PURPOSE = "gateway";
    static final String CHAT_ENDPOINT = "/v1/chat/completions";
    static final String WINDOW = "24h";
    static final String STAGE_BATCH = "gateway.batch";
    private static final String GATEWAY_USER = "gateway";
    private static final String FILE_PREFIX = "file-";
    private static final String OUTPUT_PREFIX = "file-out-";
    private static final String BATCH_PREFIX = "batch_";
    private static final String BATCH_PURPOSE = "batch";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final TurGatewayBatchFileRepository fileRepository;
    private final TurBatchJobRepository jobRepository;
    private final TurBatchInferenceService batchInferenceService;
    private final TurGatewayKeyService gatewayKeyService;
    private final TurGatewayLlmService gatewayLlmService;
    private final TurLLMInstanceRepository llmInstanceRepository;
    private final TurLLMTokenUsageService tokenUsageService;

    public TurGatewayBatchService(TurGatewayBatchFileRepository fileRepository,
            TurBatchJobRepository jobRepository, TurBatchInferenceService batchInferenceService,
            TurGatewayKeyService gatewayKeyService, TurGatewayLlmService gatewayLlmService,
            TurLLMInstanceRepository llmInstanceRepository, TurLLMTokenUsageService tokenUsageService) {
        this.fileRepository = fileRepository;
        this.jobRepository = jobRepository;
        this.batchInferenceService = batchInferenceService;
        this.gatewayKeyService = gatewayKeyService;
        this.gatewayLlmService = gatewayLlmService;
        this.llmInstanceRepository = llmInstanceRepository;
        this.tokenUsageService = tokenUsageService;
    }

    /** A line of the output that never reached the vendor, and why. */
    record Refused(String customId, String code, String message) {
    }

    /** What a {@code batch_job} row carries for the gateway, in {@code contextJson}. */
    record Context(String keyId, String inputFileId, String endpoint, String completionWindow,
            Map<String, String> metadata, List<Refused> refused) {
    }

    // ---- Files ------------------------------------------------------------

    /** {@code POST /v1/files}: stores a {@code batch} JSONL once every line parses. */
    public FileObject upload(String filename, String purpose, byte[] bytes) {
        if (!BATCH_PURPOSE.equals(purpose)) {
            throw new IllegalArgumentException("purpose must be 'batch'; the gateway stores batch input only");
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        List<JsonNode> lines = parseLines(content);
        TurGatewayBatchFile file = new TurGatewayBatchFile();
        file.setKeyId(currentKeyId());
        file.setFilename(filename);
        file.setPurpose(BATCH_PURPOSE);
        file.setBytes(bytes.length);
        file.setLineCount(lines.size());
        file.setContent(content);
        return toFileObject(fileRepository.save(file));
    }

    /** {@code GET /v1/files/{id}}. */
    public FileObject file(String fileId) {
        if (fileId.startsWith(OUTPUT_PREFIX)) {
            TurBatchJob job = ownedJob(fileId.substring(OUTPUT_PREFIX.length()));
            return new FileObject(fileId, "file", 0, job.getCreatedAt().getEpochSecond(),
                    "batch_output.jsonl", "batch_output");
        }
        return toFileObject(ownedFile(fileId));
    }

    /** {@code GET /v1/files/{id}/content}: the input JSONL, or a completed batch's output. */
    public String fileContent(String fileId) {
        if (fileId.startsWith(OUTPUT_PREFIX)) {
            return output(ownedJob(fileId.substring(OUTPUT_PREFIX.length())));
        }
        return ownedFile(fileId).getContent();
    }

    // ---- Batches ----------------------------------------------------------

    /** {@code POST /v1/batches}. */
    public BatchObject create(BatchCreateRequest request) {
        if (request == null || request.input_file_id() == null) {
            throw new IllegalArgumentException("input_file_id is required");
        }
        if (!CHAT_ENDPOINT.equals(request.endpoint())) {
            throw new IllegalArgumentException("endpoint must be " + CHAT_ENDPOINT);
        }
        if (request.completion_window() != null && !WINDOW.equals(request.completion_window())) {
            throw new IllegalArgumentException("completion_window must be " + WINDOW);
        }
        if (!batchInferenceService.isEnabled()) {
            throw new IllegalArgumentException("The batch tier is off on this Turing (turing.batch.enabled)");
        }
        TurGatewayBatchFile file = ownedFile(request.input_file_id());
        // A batch is one call against the key's rate limit and hard cap, as a
        // synchronous completion is; its lines are charged when it completes.
        TurGatewayKey key = gatewayLlmService.applyPerKeyPreCall();

        List<TurBatchChatRequest> accepted = new ArrayList<>();
        List<Refused> refused = new ArrayList<>();
        TurLLMInstance instance = null;
        for (JsonNode line : parseLines(file.getContent())) {
            String customId = line.path("custom_id").asString();
            JsonNode body = line.path("body");
            String model = body.path("model").asString(null);
            if (key != null && !gatewayKeyService.isModelAllowed(key, model)) {
                refused.add(new Refused(customId, "model_not_permitted", "Model not permitted for this key: " + model));
                continue;
            }
            TurLLMInstance lineInstance = gatewayLlmService.resolveChatInstance(model);
            if (instance == null) {
                instance = lineInstance;
            } else if (!Objects.equals(instance.getId(), lineInstance.getId())) {
                throw new IllegalArgumentException("Every line of a batch must use the same model; '"
                        + customId + "' asks for " + model);
            }
            prompt(customId, body).ifPresentOrElse(accepted::add,
                    () -> refused.add(new Refused(customId, "invalid_request",
                            "A batch line takes system messages and one user message")));
        }
        if (instance == null || accepted.isEmpty()) {
            throw new IllegalArgumentException("No line of " + request.input_file_id() + " can be batched");
        }
        if (!batchInferenceService.isSupported(instance)) {
            throw new IllegalArgumentException("The vendor of " + instance.getId() + " has no batch tier");
        }
        Context context = new Context(key == null ? null : key.getId(), request.input_file_id(),
                CHAT_ENDPOINT, WINDOW, request.metadata(), refused);
        TurBatchJob job = batchInferenceService
                .submit(instance, PURPOSE, accepted, JSON.writeValueAsString(context))
                .orElseThrow(() -> new IllegalStateException("The vendor refused the batch"));
        return toBatchObject(job, context);
    }

    /** {@code GET /v1/batches/{id}}: refreshes an open batch from the vendor first. */
    public BatchObject batch(String batchId) {
        TurBatchJob job = ownedJob(stripBatchPrefix(batchId));
        if (!job.getState().isTerminal()) {
            try {
                batchInferenceService.refresh(job);
            } catch (RuntimeException e) {
                log.warn("[Gateway] could not refresh batch {}: {}", job.getId(), e.getMessage());
            }
        }
        return toBatchObject(job, context(job));
    }

    /** {@code POST /v1/batches/{id}/cancel}. */
    public BatchObject cancel(String batchId) {
        TurBatchJob job = ownedJob(stripBatchPrefix(batchId));
        if (!job.getState().isTerminal()) {
            batchInferenceService.cancel(job);
            batchInferenceService.refresh(job);
        }
        return toBatchObject(job, context(job));
    }

    /**
     * The poller's hand-off for a completed gateway batch. The output is read
     * back from the vendor when the client asks for it, so nothing is collected
     * here.
     */
    @Override
    public String purpose() {
        return PURPOSE;
    }

    /**
     * Charges the completed lines to the key that submitted the batch, at the
     * vendor's batch price ({@link TurLLMTokenUsageService#BATCH_PRICE_FACTOR}), so
     * the key's spend, budget and monthly cap see them.
     */
    @Override
    public void onBatchComplete(TurBatchJob job, List<TurBatchChatResult> results) {
        TurLLMInstance instance = llmInstanceRepository.findById(job.getInstanceId()).orElse(null);
        String keyId = context(job).keyId();
        double cost = 0;
        for (TurBatchChatResult result : results) {
            if (result.success()) {
                cost += tokenUsageService.recordBatchUsage(instance,
                        result.inputTokens() == null ? 0 : result.inputTokens(),
                        result.outputTokens() == null ? 0 : result.outputTokens(),
                        GATEWAY_USER, STAGE_BATCH, keyId);
            }
        }
        log.info("[Gateway] batch {} completed: {} result(s), ${} booked to key {}",
                job.getId(), results.size(), cost, keyId);
    }

    // ---- Internals --------------------------------------------------------

    /** Each non-blank line is a JSON object with a unique {@code custom_id} and a {@code body}. */
    static List<JsonNode> parseLines(String content) {
        List<JsonNode> lines = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        String[] raw = content.split("\\r?\\n");
        for (int i = 0; i < raw.length; i++) {
            if (raw[i].isBlank()) {
                continue;
            }
            JsonNode line;
            try {
                line = JSON.readTree(raw[i]);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + " is not JSON");
            }
            String customId = line.path("custom_id").asString("");
            if (customId.isBlank() || !line.path("body").isObject()) {
                throw new IllegalArgumentException("Line " + (i + 1) + " needs a custom_id and a body");
            }
            if (!ids.add(customId)) {
                throw new IllegalArgumentException("Line " + (i + 1) + " repeats custom_id " + customId);
            }
            String url = line.path("url").asString(CHAT_ENDPOINT);
            if (!CHAT_ENDPOINT.equals(url)) {
                throw new IllegalArgumentException("Line " + (i + 1) + " targets " + url
                        + "; only " + CHAT_ENDPOINT + " is batched");
            }
            lines.add(line);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("The file has no lines");
        }
        return lines;
    }

    /** System messages joined, then exactly one user message, as the vendor batch providers send. */
    static java.util.Optional<TurBatchChatRequest> prompt(String customId, JsonNode body) {
        List<String> system = new ArrayList<>();
        String user = null;
        for (JsonNode message : body.path("messages")) {
            String role = message.path("role").asString();
            String content = message.path("content").asString(null);
            if ("system".equals(role) || "developer".equals(role)) {
                system.add(content);
            } else if ("user".equals(role) && user == null) {
                user = content;
            } else {
                return java.util.Optional.empty();
            }
        }
        if (user == null) {
            return java.util.Optional.empty();
        }
        Double temperature = body.has("temperature") ? body.path("temperature").asDouble() : null;
        Integer maxTokens = body.has("max_tokens") ? body.path("max_tokens").asInt() : null;
        return java.util.Optional.of(new TurBatchChatRequest(customId,
                system.isEmpty() ? null : String.join("\n\n", system), user, null, temperature, maxTokens));
    }

    private String output(TurBatchJob job) {
        if (job.getState() != TurBatchState.COMPLETED) {
            throw new IllegalArgumentException("Batch " + BATCH_PREFIX + job.getId() + " is " + status(job.getState())
                    + "; its output exists once it is completed");
        }
        Context context = context(job);
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (TurBatchChatResult result : batchInferenceService.fetchResults(job)) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("id", "batch_req_" + (++n));
            line.put("custom_id", result.customId());
            if (result.success()) {
                long in = result.inputTokens() == null ? 0 : result.inputTokens();
                long completion = result.outputTokens() == null ? 0 : result.outputTokens();
                ChatCompletion body = new ChatCompletion("chatcmpl-" + UUID.randomUUID(), "chat.completion",
                        job.getCreatedAt().getEpochSecond(), null,
                        List.of(new Choice(0, new WireMessage("assistant", result.content()), "stop")),
                        new Usage(in, completion, in + completion));
                line.put("response", Map.of("status_code", 200, "body", body));
                line.put("error", null);
            } else {
                line.put("response", null);
                line.put("error", Map.of("code", "batch_request_failed",
                        "message", String.valueOf(result.error())));
            }
            out.append(JSON.writeValueAsString(line)).append('\n');
        }
        for (Refused r : context.refused() == null ? List.<Refused>of() : context.refused()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("id", "batch_req_" + (++n));
            line.put("custom_id", r.customId());
            line.put("response", null);
            line.put("error", Map.of("code", r.code(), "message", r.message()));
            out.append(JSON.writeValueAsString(line)).append('\n');
        }
        return out.toString();
    }

    private BatchObject toBatchObject(TurBatchJob job, Context context) {
        long refused = context.refused() == null ? 0 : context.refused().size();
        boolean completed = job.getState() == TurBatchState.COMPLETED;
        return new BatchObject(BATCH_PREFIX + job.getId(), "batch", context.endpoint(), context.inputFileId(),
                context.completionWindow(), status(job.getState()),
                completed ? OUTPUT_PREFIX + job.getId() : null,
                job.getCreatedAt().getEpochSecond(),
                job.getCompletedAt() == null ? null : job.getCompletedAt().getEpochSecond(),
                new RequestCounts(job.getTotalRequests() + refused, job.getCompletedRequests(),
                        job.getFailedRequests() + refused),
                context.metadata());
    }

    static String status(TurBatchState state) {
        return switch (state) {
            case VALIDATING -> "validating";
            case IN_PROGRESS -> "in_progress";
            case COMPLETED -> "completed";
            case FAILED -> "failed";
            case EXPIRED -> "expired";
            case CANCELLED -> "cancelled";
        };
    }

    private Context context(TurBatchJob job) {
        return JSON.readValue(job.getContextJson(), Context.class);
    }

    private TurGatewayBatchFile ownedFile(String fileId) {
        String id = fileId.startsWith(FILE_PREFIX) ? fileId.substring(FILE_PREFIX.length()) : fileId;
        return fileRepository.findById(id)
                .filter(file -> Objects.equals(file.getKeyId(), currentKeyId()))
                .orElseThrow(() -> new NoSuchElementException("No such file: " + fileId));
    }

    private TurBatchJob ownedJob(String jobId) {
        return jobRepository.findById(jobId)
                .filter(job -> PURPOSE.equals(job.getPurpose()))
                .filter(job -> Objects.equals(context(job).keyId(), currentKeyId()))
                .orElseThrow(() -> new NoSuchElementException("No such batch: " + BATCH_PREFIX + jobId));
    }

    private static String stripBatchPrefix(String batchId) {
        return batchId.startsWith(BATCH_PREFIX) ? batchId.substring(BATCH_PREFIX.length()) : batchId;
    }

    private static String currentKeyId() {
        TurGatewayKey key = TurGatewayContext.get();
        return key == null ? null : key.getId();
    }

    private static FileObject toFileObject(TurGatewayBatchFile file) {
        return new FileObject(FILE_PREFIX + file.getId(), "file", file.getBytes(),
                file.getCreatedAt().getEpochSecond(), file.getFilename(), file.getPurpose());
    }
}
