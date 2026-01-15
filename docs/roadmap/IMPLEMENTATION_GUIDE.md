# Implementation Quick Reference

This quick reference provides actionable steps for implementing the strategic improvements outlined in the roadmap.

## Priority Implementation Guide

### Week 1-2: Foundation Setup

**Immediate Actions**:

1. **Review Documentation** ✅
   - [x] ARCHITECTURE.md created
   - [x] ROADMAP.md created
   - [x] DESIGN_PATTERNS.md created
   - [x] SCALABILITY.md created
   - [x] COMMUNITY.md created
   - [x] CONTRIBUTING.md updated

2. **Team Alignment**
   - [ ] Share documentation with core team
   - [ ] Schedule architecture review meeting
   - [ ] Prioritize Phase 1 items
   - [ ] Assign owners to initiatives

3. **Community Setup**
   - [ ] Set up Discord server
   - [ ] Create mailing lists
   - [ ] Label "good first issue" on GitHub
   - [ ] Plan first community call

### Month 1: Quick Wins (Phase 1)

**Priority 1: Code Quality**

```bash
# Set up code quality gates
# Add to .github/workflows/validate.yml

- name: SonarCloud Quality Gate
  run: |
    ./mvnw sonar:sonar \
      -Dsonar.qualitygate.wait=true \
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# Require 70% test coverage
# Set in pom.xml
<jacoco-maven-plugin>
  <configuration>
    <rules>
      <rule>
        <element>BUNDLE</element>
        <limits>
          <limit>
            <counter>LINE</counter>
            <value>COVEREDRATION</value>
            <minimum>0.70</minimum>
          </limit>
        </limits>
      </rule>
    </rules>
  </configuration>
</jacoco-maven-plugin>
```

**Priority 2: Developer Tools**

```yaml
# Create .devcontainer/devcontainer.json
{
  "name": "Turing Development",
  "dockerComposeFile": "docker-compose.dev.yml",
  "service": "app",
  "workspaceFolder": "/workspace",
  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "vmware.vscode-spring-boot",
        "gabrielbb.vscode-lombok",
        "redhat.vscode-yaml"
      ]
    }
  },
  "postCreateCommand": "mvn clean install -DskipTests"
}
```

**Priority 3: Performance - Caching**

```java
// Add to turing-app/pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

// Add to application.yml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
    
// Enable caching in TuringES.java
@EnableCaching
public class TuringES {
    // existing code
}

// Add caching to search service
@Service
public class TurSNSearchService {
    
    @Cacheable(value = "search-results", 
               key = "#siteId + '-' + #query",
               unless = "#result == null")
    public SearchResult search(String siteId, String query) {
        // existing search logic
    }
}
```

### Month 2-3: Architecture Improvements

**Apply Repository Pattern with Specifications**

```java
// Create specification class
package com.viglet.turing.persistence.specification;

import org.springframework.data.jpa.domain.Specification;
import com.viglet.turing.persistence.model.sn.TurSNSite;

public class TurSNSiteSpecifications {
    
    public static Specification<TurSNSite> isEnabled() {
        return (root, query, cb) -> cb.equal(root.get("enabled"), 1);
    }
    
    public static Specification<TurSNSite> hasName(String name) {
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("name")), 
            "%" + name.toLowerCase() + "%"
        );
    }
    
    public static Specification<TurSNSite> createdAfter(LocalDateTime date) {
        return (root, query, cb) -> cb.greaterThan(root.get("createdDate"), date);
    }
}

// Update repository
public interface TurSNSiteRepository extends JpaRepository<TurSNSite, String>,
                                             JpaSpecificationExecutor<TurSNSite> {
    // existing methods
}

// Use in service
@Service
public class TurSNSiteService {
    
    public List<TurSNSite> findActiveSites(String nameFilter) {
        return repository.findAll(
            Specification.where(TurSNSiteSpecifications.isEnabled())
                .and(TurSNSiteSpecifications.hasName(nameFilter))
        );
    }
}
```

**Apply Factory Pattern for Connectors**

```java
// Create factory interface
package com.viglet.turing.plugins.factory;

public interface ConnectorFactory {
    Connector createConnector(String type);
    boolean supports(String type);
}

// Implement factory
@Component
public class CMSConnectorFactory implements ConnectorFactory {
    
    @Override
    public Connector createConnector(String type) {
        return switch(type) {
            case "AEM" -> new AEMConnector();
            case "WORDPRESS" -> new WordPressConnector();
            case "DRUPAL" -> new DrupalConnector();
            default -> throw new UnsupportedConnectorException(type);
        };
    }
    
    @Override
    public boolean supports(String type) {
        return List.of("AEM", "WORDPRESS", "DRUPAL").contains(type);
    }
}

// Registry
@Component
public class ConnectorRegistry {
    private final List<ConnectorFactory> factories;
    
    public ConnectorRegistry(List<ConnectorFactory> factories) {
        this.factories = factories;
    }
    
    public Connector getConnector(String type) {
        return factories.stream()
            .filter(f -> f.supports(type))
            .findFirst()
            .map(f -> f.createConnector(type))
            .orElseThrow(() -> new UnsupportedConnectorException(type));
    }
}
```

### Month 4-6: Scalability (Phase 2)

**Database Read Replicas**

```yaml
# application-production.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
  jpa:
    properties:
      hibernate:
        connection:
          provider_disables_autocommit: true

# Add routing configuration
turing:
  datasource:
    master:
      url: ${MASTER_DB_URL}
      username: ${MASTER_DB_USER}
      password: ${MASTER_DB_PASSWORD}
    replica:
      url: ${REPLICA_DB_URL}
      username: ${REPLICA_DB_USER}
      password: ${REPLICA_DB_PASSWORD}
```

**Kubernetes Deployment**

```yaml
# k8s/deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: turing-app
  namespace: turing
spec:
  replicas: 5
  selector:
    matchLabels:
      app: turing-app
  template:
    metadata:
      labels:
        app: turing-app
    spec:
      containers:
      - name: turing-app
        image: viglet/turing:2026.1.7
        ports:
        - containerPort: 2700
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: production
        resources:
          requests:
            memory: "2Gi"
            cpu: "1"
          limits:
            memory: "4Gi"
            cpu: "2"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 2700
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 2700
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: turing-app-hpa
  namespace: turing
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: turing-app
  minReplicas: 5
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Month 7-9: Developer Experience (Phase 3)

**Improve API Documentation**

```java
// Add comprehensive OpenAPI annotations
@RestController
@RequestMapping("/api/v2/search")
@Tag(name = "Search API", description = "Search operations")
public class SearchController {
    
    @Operation(
        summary = "Search documents",
        description = "Performs a search across indexed documents with filters and facets",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Search completed successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SearchResponse.class),
                    examples = @ExampleObject(
                        name = "Basic search",
                        value = "{ \"query\": \"machine learning\", \"results\": [...] }"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid search parameters"
            )
        }
    )
    @GetMapping("/{siteId}")
    public ResponseEntity<SearchResponse> search(
        @Parameter(description = "Site identifier", required = true)
        @PathVariable String siteId,
        
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,
        
        @Parameter(description = "Number of results per page", example = "10")
        @RequestParam(defaultValue = "10") int rows
    ) {
        // implementation
    }
}
```

**Create Migration Guides**

```markdown
<!-- docs/migration/v2.md -->
# Migration Guide: v1.x to v2.0

## Breaking Changes

### 1. API Endpoint Changes

**Old:**
```
GET /api/sn/{site}/search?q={query}
```

**New:**
```
GET /api/v2/search/{site}?q={query}
```

**Migration:**
```java
// Old code
String url = "/api/sn/" + site + "/search?q=" + query;

// New code
String url = "/api/v2/search/" + site + "?q=" + query;
```

### 2. Configuration Changes

**Old (application.properties):**
```
turing.solr.url=http://localhost:8983/solr
```

**New (application.yml):**
```yaml
turing:
  search-engine:
    type: solr
    solr:
      url: http://localhost:8983/solr
```

## New Features in v2.0

1. Multi-search engine support (Solr + Elasticsearch)
2. Improved caching with Redis
3. GraphQL API
4. Enhanced security
```

### Month 10-12: AI Integration (Phase 4)

**Implement RAG Pipeline**

```java
// RAG service
@Service
public class RAGSearchService {
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;
    
    public RAGResponse search(String query) {
        // 1. Generate embedding for query
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // 2. Search for similar documents
        List<Document> relevantDocs = vectorStore.findRelevant(
            queryEmbedding, 
            10
        );
        
        // 3. Build context from documents
        String context = buildContext(relevantDocs);
        
        // 4. Generate response with LLM
        String prompt = String.format(
            "Based on the following context, answer the question.\n\n" +
            "Context: %s\n\n" +
            "Question: %s\n\n" +
            "Answer:",
            context, query
        );
        
        String answer = chatModel.generate(prompt);
        
        return new RAGResponse(answer, relevantDocs);
    }
}
```

## Testing Strategy

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {
    
    @Mock
    private SearchEnginePort searchEngine;
    
    @InjectMocks
    private SearchService searchService;
    
    @Test
    void shouldReturnResults_whenValidQuery() {
        // Given
        SearchQuery query = SearchQuery.builder()
            .query("test")
            .rows(10)
            .build();
        SearchResult expected = new SearchResult(/* ... */);
        when(searchEngine.search(query)).thenReturn(expected);
        
        // When
        SearchResult result = searchService.search(query);
        
        // Then
        assertThat(result).isEqualTo(expected);
        verify(searchEngine).search(query);
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class SearchIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldPerformSearch() {
        // Given
        String url = "/api/v2/search/test-site?q=machine%20learning";
        
        // When
        ResponseEntity<SearchResponse> response = restTemplate.getForEntity(
            url, 
            SearchResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResults()).isNotEmpty();
    }
}
```

## Performance Testing

```bash
# Load testing with Apache JMeter
jmeter -n -t load-test.jmx -l results.jtl -e -o report/

# Or with k6
k6 run --vus 100 --duration 30s load-test.js
```

## Monitoring Setup

```yaml
# Prometheus monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      
# Custom metrics
@Component
public class SearchMetrics {
    private final Counter searchCounter;
    private final Timer searchTimer;
    
    public SearchMetrics(MeterRegistry registry) {
        this.searchCounter = Counter.builder("search.requests")
            .description("Total search requests")
            .register(registry);
        this.searchTimer = Timer.builder("search.duration")
            .description("Search duration")
            .register(registry);
    }
}
```

## Deployment Checklist

- [ ] All tests pass
- [ ] Code coverage > 70%
- [ ] SonarCloud quality gate passes
- [ ] API documentation updated
- [ ] Migration guide created (if breaking changes)
- [ ] Performance tested
- [ ] Security scan completed
- [ ] Backup plan ready
- [ ] Rollback plan documented
- [ ] Monitoring configured
- [ ] Release notes written

## Getting Help

- 📖 Read the full [ROADMAP.md](ROADMAP.md)
- 🏗️ Review [ARCHITECTURE.md](ARCHITECTURE.md)
- 🎨 Check [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md)
- 💬 Ask in GitHub Discussions
- 📧 Email: opensource@viglet.com

---

**Last Updated**: 2026-01-04
**Version**: 1.0
