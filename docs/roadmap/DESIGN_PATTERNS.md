# Design Patterns Guide for Viglet Turing

## Introduction

This guide identifies specific design patterns to be applied to the Viglet Turing codebase to improve maintainability, scalability, and ease of contribution. Each pattern includes rationale, implementation guidance, and concrete examples from the codebase.

## Table of Contents

1. [Architectural Patterns](#architectural-patterns)
2. [Creational Patterns](#creational-patterns)
3. [Structural Patterns](#structural-patterns)
4. [Behavioral Patterns](#behavioral-patterns)
5. [Enterprise Integration Patterns](#enterprise-integration-patterns)
6. [Spring-Specific Patterns](#spring-specific-patterns)
7. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)

---

## Architectural Patterns

### 1. Layered Architecture (Already Applied, Needs Refinement)

**Current State**: Partially implemented with API, business logic, and persistence layers.

**Target State**: Strictly enforced layers with clear boundaries.

```
┌─────────────────────────────────────┐
│      Presentation Layer (API)       │  ← Controllers, DTOs, Validation
├─────────────────────────────────────┤
│      Application Layer (Service)    │  ← Use cases, orchestration
├─────────────────────────────────────┤
│      Domain Layer (Business Logic)  │  ← Domain models, business rules
├─────────────────────────────────────┤
│      Infrastructure Layer           │  ← Repositories, external services
└─────────────────────────────────────┘
```

**Implementation Guidelines**:

```java
// ❌ BAD: Controller with business logic
@RestController
public class TurSEInstanceAPI {
    public ResponseEntity<TurSEInstance> create(@RequestBody TurSEInstance instance) {
        // Business logic directly in controller - AVOID THIS
        if (instance.getPort() < 1024) {
            throw new IllegalArgumentException("Port must be > 1024");
        }
        instance.setId(UUID.randomUUID().toString());
        return ResponseEntity.ok(repository.save(instance));
    }
}

// ✅ GOOD: Proper layering
@RestController
public class TurSEInstanceAPI {
    private final TurSEInstanceService service;
    
    public ResponseEntity<TurSEInstanceDTO> create(@Valid @RequestBody CreateSEInstanceRequest request) {
        TurSEInstanceDTO created = service.createInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

@Service
public class TurSEInstanceService {
    private final TurSEInstanceRepository repository;
    private final TurSEInstanceValidator validator;
    
    @Transactional
    public TurSEInstanceDTO createInstance(CreateSEInstanceRequest request) {
        validator.validate(request);
        TurSEInstance instance = mapper.toEntity(request);
        instance = repository.save(instance);
        return mapper.toDTO(instance);
    }
}
```

**Benefits**:
- Clear separation of concerns
- Easier testing (mock dependencies)
- Better maintainability
- Reduced coupling

---

### 2. Domain-Driven Design (DDD) - Recommended

**Pattern**: Organize code around business domains rather than technical layers.

**Proposed Structure**:

```
com.viglet.turing/
  ├── search/                    # Search domain
  │   ├── domain/               # Domain models
  │   │   ├── SearchQuery.java
  │   │   └── SearchResult.java
  │   ├── application/          # Use cases
  │   │   └── SearchService.java
  │   ├── infrastructure/       # Technical implementations
  │   │   ├── SolrSearchRepository.java
  │   │   └── ElasticsearchRepository.java
  │   └── api/                  # API endpoints
  │       └── SearchController.java
  │
  ├── indexing/                 # Indexing domain
  │   ├── domain/
  │   ├── application/
  │   ├── infrastructure/
  │   └── api/
  │
  └── integration/              # Integration domain
      ├── domain/
      ├── application/
      ├── infrastructure/
      └── api/
```

**Implementation Example**:

```java
// Domain Model (business logic)
package com.viglet.turing.search.domain;

public class SearchQuery {
    private final String query;
    private final int rows;
    private final List<SearchFilter> filters;
    
    public SearchQuery(String query, int rows, List<SearchFilter> filters) {
        if (query == null || query.trim().isEmpty()) {
            throw new InvalidSearchQueryException("Query cannot be empty");
        }
        if (rows < 1 || rows > 1000) {
            throw new InvalidSearchQueryException("Rows must be between 1 and 1000");
        }
        this.query = query;
        this.rows = rows;
        this.filters = filters;
    }
    
    public boolean hasFilters() {
        return filters != null && !filters.isEmpty();
    }
    
    // Business logic belongs here
    public String buildSolrQuery() {
        // Domain logic for query transformation
    }
}

// Application Service (orchestration)
package com.viglet.turing.search.application;

@Service
public class SearchService {
    private final SearchRepository searchRepository;
    private final SearchAnalytics analytics;
    
    public SearchResult search(SearchQuery query) {
        // Log search request
        analytics.recordSearch(query);
        
        // Execute search
        SearchResult result = searchRepository.search(query);
        
        // Record results
        analytics.recordResults(result);
        
        return result;
    }
}

// Infrastructure (technical implementation)
package com.viglet.turing.search.infrastructure;

@Component
public class SolrSearchRepository implements SearchRepository {
    private final SolrClient solrClient;
    
    @Override
    public SearchResult search(SearchQuery query) {
        SolrQuery solrQuery = new SolrQuery(query.buildSolrQuery());
        // Solr-specific implementation
    }
}
```

**Benefits**:
- Better alignment with business needs
- Easier for domain experts to understand
- Bounded contexts prevent coupling
- More intuitive navigation

---

## Creational Patterns

### 3. Factory Pattern (Apply to Connector Creation)

**Current Issue**: Direct instantiation of connectors makes it hard to add new types.

**Solution**: Factory pattern for plugin/connector creation.

```java
// ✅ GOOD: Factory for connector creation
package com.viglet.turing.integration.factory;

public interface IntegrationConnectorFactory {
    IntegrationConnector createConnector(TurIntegrationType type);
    boolean supports(TurIntegrationType type);
}

@Component
public class CMSConnectorFactory implements IntegrationConnectorFactory {
    
    @Override
    public IntegrationConnector createConnector(TurIntegrationType type) {
        // Example implementation - actual connector classes need to be created
        return switch(type) {
            case AEM -> createAEMConnector();
            case WORDPRESS -> createWordPressConnector();
            case DRUPAL -> createDrupalConnector();
            default -> throw new UnsupportedConnectorException(type);
        };
    }
    
    private IntegrationConnector createAEMConnector() {
        // Implementation for AEM connector
        throw new UnsupportedOperationException("AEM connector not yet implemented");
    }
    
    private IntegrationConnector createWordPressConnector() {
        // Implementation for WordPress connector
        throw new UnsupportedOperationException("WordPress connector not yet implemented");
    }
    
    private IntegrationConnector createDrupalConnector() {
        // Implementation for Drupal connector
        throw new UnsupportedOperationException("Drupal connector not yet implemented");
    }
    
    @Override
    public boolean supports(TurIntegrationType type) {
        return type.isCMSType();
    }
}

@Component
public class IntegrationConnectorRegistry {
    private final List<IntegrationConnectorFactory> factories;
    
    public IntegrationConnector getConnector(TurIntegrationType type) {
        return factories.stream()
            .filter(f -> f.supports(type))
            .findFirst()
            .map(f -> f.createConnector(type))
            .orElseThrow(() -> new UnsupportedConnectorException(type));
    }
}
```

**Benefits**:
- Easy to add new connectors without modifying existing code
- Centralized connector creation logic
- Better testability

---

### 4. Builder Pattern (Apply to Complex Objects)

**Use Case**: Creating complex search queries and index configurations.

```java
// ✅ GOOD: Builder for complex objects
package com.viglet.turing.search.domain;

public class SearchQuery {
    private final String query;
    private final int rows;
    private final int start;
    private final List<SearchFilter> filters;
    private final List<FacetField> facets;
    private final SortOrder sortOrder;
    private final String locale;
    
    private SearchQuery(Builder builder) {
        this.query = builder.query;
        this.rows = builder.rows;
        this.start = builder.start;
        this.filters = builder.filters;
        this.facets = builder.facets;
        this.sortOrder = builder.sortOrder;
        this.locale = builder.locale;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String query;
        private int rows = 10;
        private int start = 0;
        private List<SearchFilter> filters = new ArrayList<>();
        private List<FacetField> facets = new ArrayList<>();
        private SortOrder sortOrder = SortOrder.RELEVANCE;
        private String locale = "en_US";
        
        public Builder query(String query) {
            this.query = query;
            return this;
        }
        
        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }
        
        public Builder addFilter(SearchFilter filter) {
            this.filters.add(filter);
            return this;
        }
        
        public Builder addFacet(FacetField facet) {
            this.facets.add(facet);
            return this;
        }
        
        public Builder sortBy(SortOrder sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }
        
        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }
        
        public SearchQuery build() {
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalStateException("Query is required");
            }
            return new SearchQuery(this);
        }
    }
}

// Usage
SearchQuery query = SearchQuery.builder()
    .query("machine learning")
    .rows(20)
    .addFilter(new CategoryFilter("technology"))
    .addFacet(new FacetField("author"))
    .sortBy(SortOrder.DATE_DESC)
    .locale("en_US")
    .build();
```

**Benefits**:
- Readable, fluent API
- Immutable objects (thread-safe)
- Validation at build time
- Optional parameters handled elegantly

---

### 5. Singleton Pattern (via Spring Beans)

**Current State**: Spring manages singletons automatically.

**Best Practice**: Use Spring's dependency injection rather than manual singleton.

```java
// ❌ BAD: Manual singleton
public class TurConfiguration {
    private static TurConfiguration instance;
    
    private TurConfiguration() {}
    
    public static TurConfiguration getInstance() {
        if (instance == null) {
            synchronized (TurConfiguration.class) {
                if (instance == null) {
                    instance = new TurConfiguration();
                }
            }
        }
        return instance;
    }
}

// ✅ GOOD: Spring-managed singleton
@Configuration
public class TurApplicationConfiguration {
    
    @Bean
    public TurSearchEngineConfig searchEngineConfig() {
        return new TurSearchEngineConfig();
    }
    
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TurCacheManager cacheManager() {
        return new TurCacheManager();
    }
}
```

---

## Structural Patterns

### 6. Adapter Pattern (For Search Engine Abstraction)

**Current State**: Direct coupling to Solr in many places.

**Solution**: Adapter pattern to support multiple search engines.

```java
// Search Engine Abstraction
package com.viglet.turing.search.port;

public interface SearchEnginePort {
    SearchResult search(SearchQuery query);
    void index(Document document);
    void deleteById(String id);
    boolean healthCheck();
}

// Solr Adapter
package com.viglet.turing.search.adapter.solr;

@Component
@ConditionalOnProperty(name = "turing.search.engine", havingValue = "solr")
public class SolrSearchEngineAdapter implements SearchEnginePort {
    private final SolrClient solrClient;
    
    @Override
    public SearchResult search(SearchQuery query) {
        SolrQuery solrQuery = convertToSolrQuery(query);
        QueryResponse response = solrClient.query(solrQuery);
        return convertFromSolrResponse(response);
    }
    
    @Override
    public void index(Document document) {
        SolrInputDocument solrDoc = convertToSolrDocument(document);
        solrClient.add(solrDoc);
        solrClient.commit();
    }
    
    // ... other implementations
}

// Elasticsearch Adapter
package com.viglet.turing.search.adapter.elasticsearch;

@Component
@ConditionalOnProperty(name = "turing.search.engine", havingValue = "elasticsearch")
public class ElasticsearchSearchEngineAdapter implements SearchEnginePort {
    private final RestHighLevelClient esClient;
    
    @Override
    public SearchResult search(SearchQuery query) {
        SearchRequest request = convertToESRequest(query);
        org.elasticsearch.action.search.SearchResponse response = esClient.search(request);
        return convertFromESResponse(response);
    }
    
    @Override
    public void index(Document document) {
        IndexRequest request = convertToIndexRequest(document);
        esClient.index(request);
    }
    
    // ... other implementations
}
```

**Benefits**:
- Swap search engines without changing business logic
- Test with mock search engine
- Support multiple engines simultaneously
- Clean abstraction boundary

---

### 7. Facade Pattern (Simplify Complex Subsystems)

**Use Case**: Simplify interaction with complex indexing pipeline.

```java
// ✅ GOOD: Facade for complex indexing
package com.viglet.turing.indexing.facade;

@Service
public class IndexingFacade {
    private final ContentExtractor contentExtractor;
    private final MetadataProcessor metadataProcessor;
    private final FieldMapper fieldMapper;
    private final SearchEnginePort searchEngine;
    private final IndexingQueue indexingQueue;
    private final IndexingAnalytics analytics;
    
    /**
     * High-level method hiding complex indexing workflow
     */
    public IndexingResult indexContent(ContentSource source) {
        try {
            // Extract content
            RawContent raw = contentExtractor.extract(source);
            
            // Process metadata
            Metadata metadata = metadataProcessor.process(raw);
            
            // Map to index fields
            Document document = fieldMapper.map(raw, metadata);
            
            // Index to search engine
            searchEngine.index(document);
            
            // Record analytics
            analytics.recordIndexing(document);
            
            return IndexingResult.success(document.getId());
            
        } catch (Exception e) {
            // Send to dead letter queue for retry
            indexingQueue.sendToDeadLetter(source, e);
            return IndexingResult.failure(e);
        }
    }
    
    /**
     * Batch indexing with optimized processing
     */
    public List<IndexingResult> indexContentBatch(List<ContentSource> sources) {
        return sources.parallelStream()
            .map(this::indexContent)
            .collect(Collectors.toList());
    }
}
```

**Benefits**:
- Simplified API for common use cases
- Hides complexity from clients
- Centralized orchestration
- Easy to add cross-cutting concerns (logging, metrics)

---

### 8. Decorator Pattern (Add Functionality Dynamically)

**Use Case**: Add caching, logging, monitoring to search without modifying core.

```java
// Base search service
package com.viglet.turing.search.service;

public interface SearchService {
    SearchResult search(SearchQuery query);
}

@Service
@Primary
public class DefaultSearchService implements SearchService {
    private final SearchEnginePort searchEngine;
    
    @Override
    public SearchResult search(SearchQuery query) {
        return searchEngine.search(query);
    }
}

// Decorator: Add caching
@Service
public class CachedSearchService implements SearchService {
    private final SearchService delegate;
    private final Cache<SearchQuery, SearchResult> cache;
    
    public CachedSearchService(@Qualifier("defaultSearchService") SearchService delegate,
                               CacheManager cacheManager) {
        this.delegate = delegate;
        this.cache = cacheManager.getCache("search-results");
    }
    
    @Override
    public SearchResult search(SearchQuery query) {
        return cache.get(query, () -> delegate.search(query));
    }
}

// Decorator: Add analytics
@Service
public class AnalyticsSearchService implements SearchService {
    private final SearchService delegate;
    private final SearchAnalytics analytics;
    
    @Override
    public SearchResult search(SearchQuery query) {
        long startTime = System.currentTimeMillis();
        
        try {
            SearchResult result = delegate.search(query);
            analytics.recordSuccess(query, result, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            analytics.recordFailure(query, e, System.currentTimeMillis() - startTime);
            throw e;
        }
    }
}
```

**Benefits**:
- Add features without modifying existing code
- Compose multiple decorators
- Follow Open-Closed Principle
- Easy to enable/disable features

---

### 9. Proxy Pattern (Lazy Loading, Access Control)

**Use Case**: Lazy load expensive resources, add access control.

```java
// Lazy loading proxy for search engine connection
@Component
public class LazySearchEngineProxy implements SearchEnginePort {
    private SearchEnginePort realSearchEngine;
    private final SearchEngineFactory factory;
    
    @Override
    public SearchResult search(SearchQuery query) {
        ensureInitialized();
        return realSearchEngine.search(query);
    }
    
    private synchronized void ensureInitialized() {
        if (realSearchEngine == null) {
            log.info("Initializing search engine connection...");
            realSearchEngine = factory.createSearchEngine();
        }
    }
    
    @Override
    public boolean healthCheck() {
        return realSearchEngine != null && realSearchEngine.healthCheck();
    }
}

// Security proxy
@Component
public class SecureSearchService implements SearchService {
    private final SearchService delegate;
    private final SecurityManager securityManager;
    
    @Override
    public SearchResult search(SearchQuery query) {
        // Check permissions before executing
        if (!securityManager.hasSearchPermission(query.getSiteId())) {
            throw new AccessDeniedException("User lacks search permission");
        }
        
        SearchResult result = delegate.search(query);
        
        // Filter results based on permissions
        return securityManager.filterResults(result);
    }
}
```

---

## Behavioral Patterns

### 10. Strategy Pattern (Pluggable Algorithms)

**Use Case**: Different ranking strategies, different search strategies.

```java
// Strategy interface
package com.viglet.turing.search.ranking;

public interface RankingStrategy {
    List<SearchResult> rank(List<SearchResult> results, SearchQuery query);
    String getName();
}

// Concrete strategies
@Component
public class RelevanceRankingStrategy implements RankingStrategy {
    @Override
    public List<SearchResult> rank(List<SearchResult> results, SearchQuery query) {
        // Sort by relevance score
        return results.stream()
            .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public String getName() {
        return "relevance";
    }
}

@Component
public class DateRankingStrategy implements RankingStrategy {
    @Override
    public List<SearchResult> rank(List<SearchResult> results, SearchQuery query) {
        // Sort by date, recent first
        return results.stream()
            .sorted(Comparator.comparing(SearchResult::getDate).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public String getName() {
        return "date";
    }
}

@Component
public class PopularityRankingStrategy implements RankingStrategy {
    @Override
    public List<SearchResult> rank(List<SearchResult> results, SearchQuery query) {
        // Sort by click count or popularity metric
        return results.stream()
            .sorted(Comparator.comparingInt(SearchResult::getClickCount).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public String getName() {
        return "popularity";
    }
}

// Context that uses strategy
@Service
public class SearchRankingService {
    private final Map<String, RankingStrategy> strategies;
    
    public SearchRankingService(List<RankingStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(RankingStrategy::getName, Function.identity()));
    }
    
    public List<SearchResult> rankResults(List<SearchResult> results, 
                                          SearchQuery query) {
        String strategyName = query.getSortStrategy();
        RankingStrategy strategy = strategies.getOrDefault(strategyName, 
                                                          strategies.get("relevance"));
        return strategy.rank(results, query);
    }
}
```

**Benefits**:
- Easy to add new ranking algorithms
- Algorithms can be selected at runtime
- Each strategy is independently testable
- Clean separation of concerns

---

### 11. Observer Pattern (Event-Driven Architecture)

**Use Case**: Decouple components via events.

```java
// Event
package com.viglet.turing.indexing.event;

public class DocumentIndexedEvent extends ApplicationEvent {
    private final String documentId;
    private final String siteId;
    private final Instant indexedAt;
    
    public DocumentIndexedEvent(Object source, String documentId, 
                                String siteId, Instant indexedAt) {
        super(source);
        this.documentId = documentId;
        this.siteId = siteId;
        this.indexedAt = indexedAt;
    }
    
    // Getters
}

// Publisher
@Service
public class IndexingService {
    private final ApplicationEventPublisher eventPublisher;
    private final SearchEnginePort searchEngine;
    
    public void indexDocument(Document document) {
        searchEngine.index(document);
        
        // Publish event
        eventPublisher.publishEvent(
            new DocumentIndexedEvent(this, document.getId(), 
                                    document.getSiteId(), Instant.now())
        );
    }
}

// Listeners
@Component
public class IndexingAnalyticsListener {
    
    @EventListener
    @Async
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        // Update analytics
        log.info("Document indexed: {}", event.getDocumentId());
        // Record metrics
    }
}

@Component
public class CacheInvalidationListener {
    private final CacheManager cacheManager;
    
    @EventListener
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        // Invalidate relevant caches
        cacheManager.evictSiteCache(event.getSiteId());
    }
}

@Component
public class NotificationListener {
    private final NotificationService notificationService;
    
    @EventListener
    @Async
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        // Send notifications to subscribers
        notificationService.notifyIndexingComplete(event.getDocumentId());
    }
}
```

**Benefits**:
- Loose coupling between components
- Easy to add new listeners without modifying publishers
- Asynchronous processing out of the box
- Better scalability

---

### 12. Template Method Pattern (Define Algorithm Skeleton)

**Use Case**: Common workflow with customizable steps.

```java
// Abstract template
package com.viglet.turing.integration.template;

public abstract class AbstractIntegrationConnector implements IntegrationConnector {
    
    // Template method - defines the algorithm
    @Override
    public final IntegrationResult sync() {
        try {
            // Step 1: Validate configuration
            validateConfiguration();
            
            // Step 2: Connect to source
            connect();
            
            // Step 3: Fetch content (customizable)
            List<Content> content = fetchContent();
            
            // Step 4: Transform content (customizable)
            List<Document> documents = transformContent(content);
            
            // Step 5: Index documents
            indexDocuments(documents);
            
            // Step 6: Disconnect
            disconnect();
            
            return IntegrationResult.success(documents.size());
            
        } catch (Exception e) {
            handleError(e);
            return IntegrationResult.failure(e);
        }
    }
    
    // Fixed steps - same for all connectors
    private void validateConfiguration() {
        if (getConfiguration() == null) {
            throw new IllegalStateException("Configuration is required");
        }
    }
    
    private void indexDocuments(List<Document> documents) {
        searchEngine.indexBatch(documents);
    }
    
    // Abstract methods - must be implemented by subclasses
    protected abstract void connect();
    protected abstract List<Content> fetchContent();
    protected abstract List<Document> transformContent(List<Content> content);
    protected abstract void disconnect();
    
    // Hook methods - optional override
    protected void handleError(Exception e) {
        log.error("Integration error", e);
    }
}

// Concrete implementation
public class WordPressConnector extends AbstractIntegrationConnector {
    
    @Override
    protected void connect() {
        // WordPress-specific connection logic
        wpClient = new WordPressClient(getConfiguration().getUrl());
        wpClient.authenticate(getConfiguration().getApiKey());
    }
    
    @Override
    protected List<Content> fetchContent() {
        // Fetch posts from WordPress
        return wpClient.getPosts();
    }
    
    @Override
    protected List<Document> transformContent(List<Content> content) {
        // Transform WordPress posts to Turing documents
        return content.stream()
            .map(this::convertPostToDocument)
            .collect(Collectors.toList());
    }
    
    @Override
    protected void disconnect() {
        wpClient.close();
    }
}
```

**Benefits**:
- Reuse common algorithm structure
- Enforce consistent workflow
- Customize only necessary parts
- Easier to maintain common code

---

### 13. Chain of Responsibility Pattern (Processing Pipeline)

**Use Case**: Document processing pipeline with multiple steps.

```java
// Handler interface
package com.viglet.turing.indexing.pipeline;

public interface DocumentProcessor {
    ProcessingResult process(Document document, ProcessingContext context);
    void setNext(DocumentProcessor next);
}

// Abstract handler
public abstract class AbstractDocumentProcessor implements DocumentProcessor {
    private DocumentProcessor next;
    
    @Override
    public void setNext(DocumentProcessor next) {
        this.next = next;
    }
    
    @Override
    public ProcessingResult process(Document document, ProcessingContext context) {
        ProcessingResult result = doProcess(document, context);
        
        if (result.shouldContinue() && next != null) {
            return next.process(document, context);
        }
        
        return result;
    }
    
    protected abstract ProcessingResult doProcess(Document document, 
                                                  ProcessingContext context);
}

// Concrete handlers
@Component
@Order(1)
public class ContentExtractionProcessor extends AbstractDocumentProcessor {
    @Override
    protected ProcessingResult doProcess(Document document, ProcessingContext context) {
        // Extract text from various formats (PDF, DOCX, etc.)
        String extractedText = contentExtractor.extract(document.getRawContent());
        document.setContent(extractedText);
        return ProcessingResult.continueProcessing();
    }
}

@Component
@Order(2)
public class LanguageDetectionProcessor extends AbstractDocumentProcessor {
    @Override
    protected ProcessingResult doProcess(Document document, ProcessingContext context) {
        // Detect language
        String language = languageDetector.detect(document.getContent());
        document.setLanguage(language);
        return ProcessingResult.continueProcessing();
    }
}

@Component
@Order(3)
public class EntityExtractionProcessor extends AbstractDocumentProcessor {
    @Override
    protected ProcessingResult doProcess(Document document, ProcessingContext context) {
        // Extract named entities
        List<Entity> entities = entityExtractor.extract(document.getContent());
        document.setEntities(entities);
        return ProcessingResult.continueProcessing();
    }
}

@Component
@Order(4)
public class ValidationProcessor extends AbstractDocumentProcessor {
    @Override
    protected ProcessingResult doProcess(Document document, ProcessingContext context) {
        // Validate document before indexing
        List<String> errors = validator.validate(document);
        if (!errors.isEmpty()) {
            return ProcessingResult.stopWithErrors(errors);
        }
        return ProcessingResult.continueProcessing();
    }
}

// Pipeline builder
@Configuration
public class DocumentProcessingConfiguration {
    
    @Bean
    public DocumentProcessor processingPipeline(List<AbstractDocumentProcessor> processors) {
        // Sort by @Order annotation
        processors.sort(Comparator.comparingInt(
            p -> p.getClass().getAnnotation(Order.class).value()
        ));
        
        // Chain processors
        for (int i = 0; i < processors.size() - 1; i++) {
            processors.get(i).setNext(processors.get(i + 1));
        }
        
        return processors.get(0);
    }
}
```

**Benefits**:
- Flexible processing pipeline
- Easy to add/remove/reorder processors
- Each processor has single responsibility
- Can short-circuit processing

---

## Enterprise Integration Patterns

### 14. Message Channel Pattern (Async Communication)

**Use Case**: Decouple indexing requests from processing.

```java
// Message producer
@Service
public class IndexingRequestService {
    private final JmsTemplate jmsTemplate;
    
    public void submitIndexingRequest(IndexingRequest request) {
        // Send to queue for async processing
        jmsTemplate.convertAndSend("indexing.requests", request);
    }
    
    public void submitBatchIndexingRequest(List<IndexingRequest> requests) {
        requests.forEach(req -> 
            jmsTemplate.convertAndSend("indexing.requests.batch", req)
        );
    }
}

// Message consumer
@Component
public class IndexingRequestConsumer {
    private final IndexingFacade indexingFacade;
    
    @JmsListener(destination = "indexing.requests", 
                 concurrency = "5-10")
    public void processIndexingRequest(IndexingRequest request) {
        try {
            indexingFacade.indexContent(request.getContentSource());
        } catch (Exception e) {
            // Will be retried automatically by Artemis
            throw new RuntimeException("Indexing failed", e);
        }
    }
    
    @JmsListener(destination = "indexing.requests.batch",
                 concurrency = "2-5")
    public void processBatchIndexingRequest(IndexingRequest request) {
        // Process batch requests with different concurrency
        indexingFacade.indexContentBatch(request.getContentSources());
    }
}

// Dead Letter Queue handler
@Component
public class IndexingDeadLetterHandler {
    
    @JmsListener(destination = "DLQ.indexing.requests")
    public void handleFailedIndexing(IndexingRequest request) {
        // Log failure
        log.error("Indexing failed after retries: {}", request);
        
        // Store for manual intervention
        failedRequestRepository.save(request);
        
        // Notify administrators
        notificationService.notifyIndexingFailure(request);
    }
}
```

---

### 15. Content-Based Router Pattern (Route by Type)

**Use Case**: Route different content types to appropriate processors.

```java
@Component
public class ContentRouter {
    private final Map<ContentType, ContentProcessor> processors;
    
    public void route(Content content) {
        ContentType type = detectContentType(content);
        ContentProcessor processor = processors.get(type);
        
        if (processor == null) {
            throw new UnsupportedContentTypeException(type);
        }
        
        processor.process(content);
    }
    
    private ContentType detectContentType(Content content) {
        String mimeType = content.getMimeType();
        return ContentType.fromMimeType(mimeType);
    }
}
```

---

## Spring-Specific Patterns

### 16. Repository Pattern with Specifications

**Use Case**: Complex, reusable queries.

```java
// Specification for complex queries
package com.viglet.turing.persistence.specification;

public class TurSNSiteSpecifications {
    
    public static Specification<TurSNSite> hasName(String name) {
        return (root, query, cb) -> 
            cb.equal(root.get("name"), name);
    }
    
    public static Specification<TurSNSite> isEnabled() {
        return (root, query, cb) -> 
            cb.equal(root.get("enabled"), 1);
    }
    
    public static Specification<TurSNSite> belongsToUser(String userId) {
        return (root, query, cb) -> 
            cb.equal(root.get("owner").get("id"), userId);
    }
    
    public static Specification<TurSNSite> createdAfter(LocalDateTime date) {
        return (root, query, cb) -> 
            cb.greaterThan(root.get("createdDate"), date);
    }
}

// Usage
@Service
public class TurSNSiteService {
    private final TurSNSiteRepository repository;
    
    public List<TurSNSite> findUserSites(String userId) {
        return repository.findAll(
            Specification.where(isEnabled())
                .and(belongsToUser(userId))
                .and(createdAfter(LocalDateTime.now().minusMonths(6)))
        );
    }
}
```

---

### 17. DTO Pattern with MapStruct

**Use Case**: Clean separation between domain and API models.

```java
// Domain model
@Entity
public class TurSEInstance {
    private String id;
    private String title;
    private String description;
    private String host;
    private int port;
    private TurSEVendor vendor;
    // Complex internal structure
}

// API DTO
public class TurSEInstanceDTO {
    private String id;
    private String title;
    private String description;
    private String endpoint; // Computed from host:port
    private String vendorName;
    // Simplified structure for API
}

// Mapper
@Mapper(componentModel = "spring")
public interface TurSEInstanceMapper {
    
    @Mapping(target = "endpoint", expression = "java(buildEndpoint(entity))")
    @Mapping(target = "vendorName", source = "vendor.name")
    TurSEInstanceDTO toDTO(TurSEInstance entity);
    
    @Mapping(target = "host", expression = "java(extractHost(dto.getEndpoint()))")
    @Mapping(target = "port", expression = "java(extractPort(dto.getEndpoint()))")
    TurSEInstance toEntity(TurSEInstanceDTO dto);
    
    default String buildEndpoint(TurSEInstance entity) {
        return entity.getHost() + ":" + entity.getPort();
    }
    
    default String extractHost(String endpoint) {
        return endpoint.split(":")[0];
    }
    
    default int extractPort(String endpoint) {
        return Integer.parseInt(endpoint.split(":")[1]);
    }
}
```

---

## Anti-Patterns to Avoid

### 1. God Object / God Class

**Problem**: Classes with too many responsibilities.

```java
// ❌ BAD: God class doing everything
public class TuringManager {
    public void search() { }
    public void index() { }
    public void configure() { }
    public void analyze() { }
    public void backup() { }
    public void restore() { }
    // 50 more methods...
}

// ✅ GOOD: Separate concerns
public class SearchService { }
public class IndexingService { }
public class ConfigurationService { }
public class AnalyticsService { }
public class BackupService { }
```

---

### 2. Anemic Domain Model

**Problem**: Domain objects with no behavior, only getters/setters.

```java
// ❌ BAD: Anemic model
public class SearchQuery {
    private String query;
    private int rows;
    // Only getters and setters
}

// Service does all the work
public class SearchService {
    public boolean isValid(SearchQuery query) {
        return query.getQuery() != null && query.getRows() > 0;
    }
}

// ✅ GOOD: Rich domain model
public class SearchQuery {
    private String query;
    private int rows;
    
    public boolean isValid() {
        return query != null && query.trim().length() > 0 && rows > 0;
    }
    
    public boolean requiresPagination() {
        return rows < getTotalResults();
    }
    
    public SearchQuery withExpandedQuery() {
        // Business logic belongs in domain
        return new SearchQuery(expandSynonyms(query), rows);
    }
}
```

---

### 3. Circular Dependencies

**Problem**: Classes depend on each other, creating tight coupling.

```java
// ❌ BAD: Circular dependency
public class IndexingService {
    @Autowired
    private SearchService searchService;
}

public class SearchService {
    @Autowired
    private IndexingService indexingService;
}

// ✅ GOOD: Extract shared functionality
public class IndexingService {
    @Autowired
    private DocumentRepository documentRepository;
}

public class SearchService {
    @Autowired
    private DocumentRepository documentRepository;
}
```

---

### 4. Service Layer with No Business Logic

**Problem**: Service layer that just delegates to repository.

```java
// ❌ BAD: Useless service layer
@Service
public class TurSEInstanceService {
    private final TurSEInstanceRepository repository;
    
    public List<TurSEInstance> findAll() {
        return repository.findAll();
    }
    
    public TurSEInstance save(TurSEInstance instance) {
        return repository.save(instance);
    }
}

// ✅ GOOD: Service with business logic
@Service
public class TurSEInstanceService {
    private final TurSEInstanceRepository repository;
    private final TurSEInstanceValidator validator;
    private final TurSolrInstanceProcess solrProcess;
    
    @Transactional
    public TurSEInstance createInstance(CreateInstanceRequest request) {
        // Validation
        validator.validate(request);
        
        // Business logic
        TurSEInstance instance = mapper.toEntity(request);
        instance.setStatus(InstanceStatus.PENDING);
        
        // Save
        instance = repository.save(instance);
        
        // Initialize search engine
        solrProcess.initialize(instance);
        
        // Update status
        instance.setStatus(InstanceStatus.ACTIVE);
        repository.save(instance);
        
        return instance;
    }
}
```

---

## Implementation Priorities

### Phase 1: Critical Patterns (Months 1-2)
1. **Layered Architecture** - Enforce strict boundaries
2. **Repository Pattern** - Clean data access
3. **DTO Pattern** - Separate API from domain
4. **Adapter Pattern** - Abstract search engine

### Phase 2: Structural Improvements (Months 3-4)
5. **Factory Pattern** - Connector creation
6. **Builder Pattern** - Complex objects
7. **Facade Pattern** - Simplify subsystems
8. **Strategy Pattern** - Pluggable algorithms

### Phase 3: Advanced Patterns (Months 5-6)
9. **Template Method** - Connector workflows
10. **Chain of Responsibility** - Processing pipeline
11. **Observer Pattern** - Event-driven architecture
12. **Decorator Pattern** - Add features dynamically

---

## Conclusion

Applying these design patterns systematically will:
- **Improve maintainability**: Easier to understand and modify
- **Enhance scalability**: Better structure for growth
- **Increase testability**: Smaller, focused components
- **Ease contribution**: Clear patterns to follow
- **Reduce coupling**: Independent components
- **Enable reuse**: Common patterns across codebase

Each pattern should be applied where it makes sense, not dogmatically. The goal is to improve the codebase, not to use every pattern possible.

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-04  
**Next Review**: 2026-04-04  
**Maintainer**: Viglet Team
