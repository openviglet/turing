package com.viglet.turing.api.llm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import com.viglet.turing.genai.provider.llm.TurGenAiLlmProvider;
import com.viglet.turing.genai.provider.llm.TurGenAiLlmProviderFactory;
import com.viglet.turing.persistence.model.llm.TurLLMInstance;
import com.viglet.turing.persistence.repository.llm.TurLLMInstanceRepository;
import com.viglet.turing.system.security.TurSecretCryptoService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequestMapping("/api/v2/llm/{id}/chat")
@Tag(name = "LLM Chat", description = "Chat directly with a Language Model instance")
public class TurLLMChatAPI {

    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp");

    private static final String SYSTEM_PROMPT = """
            You are a helpful AI assistant with access to the internet, weather data, financial markets, \
            and a Python code interpreter. You can fetch web pages, check weather, look up stocks, \
            and execute Python code to solve problems.

            You have access to these tools:

            WEB:
            1. fetch_webpage — Fetches a web page by URL and returns its text content.
            2. extract_links — Extracts links from a web page, optionally filtered by keyword.

            WEATHER:
            3. get_weather — Gets current weather and forecast for any city (powered by Open-Meteo).

            FINANCE:
            4. get_stock_quote — Gets current stock price, change, and price history for a ticker symbol. \
            Common symbols: AAPL (Apple), GOOGL (Google), PETR4.SA (Petrobras), ^BVSP (Bovespa), \
            ^GSPC (S&P 500), ^DJI (Dow Jones), BTC-USD (Bitcoin), BRL=X (USD/BRL).
            5. search_ticker — Searches for a ticker symbol by company name.

            CODE INTERPRETER:
            6. execute_python — Executes Python code and returns the output. Use for:
               - Math calculations, statistics, data analysis
               - Processing CSV/JSON/Excel files
               - Generating charts with matplotlib (save to 'output.png', do NOT call plt.show())
               - Any computation that needs real code execution
               When generating charts, always use: plt.savefig('output.png', dpi=150, bbox_inches='tight')
               Generated files are served via URL that will be included in the output.

            RULES:
            1. When the user provides a URL, use fetch_webpage — do NOT make up content.
            2. For weather questions, use get_weather with the city name.
            3. For stock/market questions, use search_ticker first if you don't know the symbol, \
            then use get_stock_quote with the correct ticker.
            4. For math, data processing, or chart requests, use execute_python. Write clean, \
            complete Python code. Always use print() for output.
            5. When execute_python returns a file URL, include it in your response so the user can access it.
            6. After fetching data, summarize clearly and highlight key information.
            7. If the user asks in a specific language, respond in that same language.
            8. For general questions that don't need tools, respond directly.
            """;

    private final TurLLMInstanceRepository turLLMInstanceRepository;
    private final TurGenAiLlmProviderFactory llmProviderFactory;
    private final TurSecretCryptoService turSecretCryptoService;
    private final TurWebCrawlerToolService webCrawlerToolService;
    private final TurWeatherToolService weatherToolService;
    private final TurFinanceToolService financeToolService;
    private final TurCodeInterpreterToolService codeInterpreterToolService;

    public TurLLMChatAPI(TurLLMInstanceRepository turLLMInstanceRepository,
            TurGenAiLlmProviderFactory llmProviderFactory,
            TurSecretCryptoService turSecretCryptoService,
            TurWebCrawlerToolService webCrawlerToolService,
            TurWeatherToolService weatherToolService,
            TurFinanceToolService financeToolService,
            TurCodeInterpreterToolService codeInterpreterToolService) {
        this.turLLMInstanceRepository = turLLMInstanceRepository;
        this.llmProviderFactory = llmProviderFactory;
        this.turSecretCryptoService = turSecretCryptoService;
        this.webCrawlerToolService = webCrawlerToolService;
        this.weatherToolService = weatherToolService;
        this.financeToolService = financeToolService;
        this.codeInterpreterToolService = codeInterpreterToolService;
    }

    public record ChatRequest(List<ChatMessageItem> messages) {
    }

    public record ChatMessageItem(String role, String content) {
    }

    public record ChatResponse(String role, String content) {
    }

    public record ContextInfoResponse(int contextWindow, String source) {
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE,
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<ChatResponse> chat(
            @PathVariable String id,
            @RequestPart("request") ChatRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return doChat(id, request, files);
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE,
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ChatResponse> chatJson(
            @PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody ChatRequest request) {
        return doChat(id, request, null);
    }

    @GetMapping("/context-info")
    public ContextInfoResponse contextInfo(@PathVariable String id) {
        TurLLMInstance turLLMInstance = turLLMInstanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("LLM instance not found: " + id));

        TurGenAiLlmProvider provider = llmProviderFactory.getProvider(turLLMInstance);
        String decryptedApiKey = turSecretCryptoService.decrypt(turLLMInstance.getApiKeyEncrypted());

        OptionalInt fetched = provider.fetchContextWindow(turLLMInstance, decryptedApiKey);
        if (fetched.isPresent()) {
            return new ContextInfoResponse(fetched.getAsInt(), "provider");
        }

        int stored = turLLMInstance.getContextWindow() != null
                ? turLLMInstance.getContextWindow()
                : 128000;
        return new ContextInfoResponse(stored, "config");
    }

    private Flux<ChatResponse> doChat(String id, ChatRequest request, List<MultipartFile> files) {
        TurLLMInstance turLLMInstance = turLLMInstanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("LLM instance not found: " + id));

        TurGenAiLlmProvider provider = llmProviderFactory.getProvider(turLLMInstance);
        String decryptedApiKey = turSecretCryptoService.decrypt(turLLMInstance.getApiKeyEncrypted());
        ChatModel chatModel = provider.createChatModel(turLLMInstance, decryptedApiKey);

        List<Message> springMessages = new ArrayList<>();
        springMessages.add(new SystemMessage(SYSTEM_PROMPT));
        springMessages.addAll(buildMessages(request.messages(), files));

        ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(webCrawlerToolService, weatherToolService, financeToolService,
                        codeInterpreterToolService)
                .build()
                .getToolCallbacks();

        log.info("[Chat] Registered {} tool callbacks for LLM instance {}", toolCallbacks.length, id);

        var chatOptions = DefaultToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(true)
                .build();

        Prompt prompt = new Prompt(springMessages, chatOptions);

        // Use call() instead of stream() to ensure tool calling works reliably.
        // Spring AI's stream() + internalToolExecutionEnabled(true) does not execute
        // tools correctly with some providers (e.g. Anthropic CONTENT_BLOCK_STOP issue).
        return Mono.fromCallable(() -> {
                    log.info("[Chat] Calling LLM with tool support for instance {}", id);
                    var response = chatModel.call(prompt);
                    String text = response.getResult() != null
                            && response.getResult().getOutput() != null
                            && response.getResult().getOutput().getText() != null
                                    ? response.getResult().getOutput().getText()
                                    : "";
                    log.info("[Chat] LLM response: {} chars for instance {}", text.length(), id);
                    return new ChatResponse("assistant", text);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(err -> log.error("[Chat] Call error: {}", err.getMessage(), err))
                .filter(response -> !response.content().isEmpty())
                .flux();
    }

    private List<Message> buildMessages(List<ChatMessageItem> items, List<MultipartFile> files) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ChatMessageItem item = items.get(i);
            if ("assistant".equals(item.role())) {
                messages.add(new AssistantMessage(item.content()));
            } else {
                boolean isLast = (i == items.size() - 1);
                if (isLast && files != null && !files.isEmpty()) {
                    messages.add(buildUserMessageWithFiles(item.content(), files));
                } else {
                    messages.add(new UserMessage(item.content()));
                }
            }
        }
        return messages;
    }

    private UserMessage buildUserMessageWithFiles(String text, List<MultipartFile> files) {
        List<Media> imageMedia = new ArrayList<>();
        StringBuilder extractedText = new StringBuilder();

        for (MultipartFile file : files) {
            String contentType = file.getContentType() != null
                    ? file.getContentType() : "application/octet-stream";

            // Always extract text from all files (works for PDF, DOCX, and images via OCR)
            String content = extractTextFromFile(file);
            if (!content.isBlank()) {
                extractedText.append("\n\n--- File: ")
                        .append(file.getOriginalFilename())
                        .append(" ---\n")
                        .append(content);
            }

            // Also attach images as Media for vision-capable models
            if (IMAGE_MIME_TYPES.contains(contentType)) {
                try {
                    imageMedia.add(Media.builder()
                            .mimeType(MimeType.valueOf(contentType))
                            .data(new ByteArrayResource(file.getBytes()))
                            .name(file.getOriginalFilename())
                            .build());
                } catch (IOException e) {
                    log.warn("Failed to read image file: {}", file.getOriginalFilename(), e);
                }
            }
        }

        String fullText = extractedText.isEmpty()
                ? text
                : text + extractedText;

        if (!imageMedia.isEmpty()) {
            return UserMessage.builder()
                    .text(fullText)
                    .media(imageMedia)
                    .build();
        }
        return new UserMessage(fullText);
    }

    private String extractTextFromFile(MultipartFile file) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBytes())) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_TYPE, file.getContentType());
            new AutoDetectParser().parse(inputStream, handler, metadata, new ParseContext());
            return handler.toString().strip();
        } catch (IOException | TikaException | SAXException e) {
            log.warn("Failed to extract text from file: {}", file.getOriginalFilename(), e);
            return "";
        }
    }
}
