# Scalability Guide for Viglet Turing

## Introduction

This guide provides comprehensive strategies for scaling Viglet Turing to handle enterprise-scale workloads. It covers horizontal scaling, vertical scaling, performance optimization, and infrastructure considerations.

## Table of Contents

1. [Scalability Fundamentals](#scalability-fundamentals)
2. [Application Layer Scaling](#application-layer-scaling)
3. [Database Scaling](#database-scaling)
4. [Search Engine Scaling](#search-engine-scaling)
5. [Message Queue Scaling](#message-queue-scaling)
6. [Caching Strategies](#caching-strategies)
7. [Performance Optimization](#performance-optimization)
8. [Monitoring and Observability](#monitoring-and-observability)
9. [Capacity Planning](#capacity-planning)

---

## Scalability Fundamentals

### Current Capacity Baseline

**Single Instance Capacity**:
- **Queries per second**: ~100-200 QPS
- **Documents indexed**: ~10M documents
- **Concurrent users**: ~500 users
- **Response time**: P95 < 500ms, P99 < 1s

**Target Capacity** (Enterprise Scale):
- **Queries per second**: 10,000+ QPS
- **Documents indexed**: 1B+ documents
- **Concurrent users**: 100,000+ users
- **Response time**: P95 < 200ms, P99 < 500ms

### Scalability Principles

1. **Stateless Design**: Application instances should not store session state
2. **Horizontal Scalability**: Add more instances rather than bigger instances
3. **Async Processing**: Long-running tasks should be asynchronous
4. **Database Optimization**: Proper indexing and query optimization
5. **Caching**: Cache aggressively at multiple levels
6. **Load Balancing**: Distribute load evenly across instances

---

## Application Layer Scaling

### 1. Stateless Application Design

**Current Challenge**: Session state may be stored in memory.

**Solution**: Externalize session storage.

```yaml
# application.yml
spring:
  session:
    store-type: redis
    redis:
      flush-mode: on_save
      namespace: spring:session
      
redis:
  host: redis-cluster.example.com
  port: 6379
  cluster:
    nodes:
      - redis-1.example.com:6379
      - redis-2.example.com:6379
      - redis-3.example.com:6379
```

```java
// Configuration
@Configuration
@EnableRedisHttpSession
public class SessionConfiguration {
    
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration(Arrays.asList(
                "redis-1.example.com:6379",
                "redis-2.example.com:6379",
                "redis-3.example.com:6379"
            ));
        
        return new LettuceConnectionFactory(clusterConfig);
    }
}
```

### 2. Load Balancing Configuration

**Kubernetes Deployment**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: turing-app
spec:
  replicas: 5  # Start with 5 replicas
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
        image: viglet/turing:latest
        resources:
          requests:
            cpu: "1"
            memory: "2Gi"
          limits:
            cpu: "2"
            memory: "4Gi"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mariadb://mariadb-cluster:3306/turing"
        livenessProbe:
          httpGet:
            path: /actuator/health
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
apiVersion: v1
kind: Service
metadata:
  name: turing-app-service
spec:
  selector:
    app: turing-app
  ports:
  - port: 80
    targetPort: 2700
  type: LoadBalancer
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: turing-app-hpa
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
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
```

### 3. Connection Pool Optimization

```java
// HikariCP configuration
@Configuration
public class DatabaseConfiguration {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        // Pool sizing
        config.setMaximumPoolSize(20); // Per instance
        config.setMinimumIdle(5);
        
        // Connection timeout
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        
        // Performance optimizations
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);
        
        // Leak detection
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        return new HikariDataSource(config);
    }
}
```

### 4. Thread Pool Configuration

```yaml
# application.yml
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 1000
        keep-alive: 60s
    scheduling:
      pool:
        size: 5

server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 10000
    accept-count: 100
    connection-timeout: 20000
```

---

## Database Scaling

### 1. Read Replica Configuration

**Setup**:

```yaml
# application-production.yml
spring:
  datasource:
    master:
      url: jdbc:mariadb://master-db:3306/turing
      username: turing_master
      password: ${MASTER_DB_PASSWORD}
      hikari:
        maximum-pool-size: 20
        
    replica:
      url: jdbc:mariadb://replica-1-db:3306,replica-2-db:3306,replica-3-db:3306/turing
      username: turing_replica
      password: ${REPLICA_DB_PASSWORD}
      hikari:
        maximum-pool-size: 50
        read-only: true
```

```java
@Configuration
public class DataSourceConfiguration {
    
    @Bean
    @Primary
    public DataSource dataSource() {
        return new RoutingDataSource();
    }
    
    @Bean
    public DataSource masterDataSource() {
        return DataSourceBuilder.create()
            .url(masterUrl)
            .username(masterUsername)
            .password(masterPassword)
            .build();
    }
    
    @Bean
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create()
            .url(replicaUrl)
            .username(replicaUsername)
            .password(replicaPassword)
            .build();
    }
}

public class RoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() 
            ? "replica" : "master";
    }
}

// Usage
@Service
public class TurSNSiteService {
    
    @Transactional(readOnly = true)
    public List<TurSNSite> findAll() {
        // Routes to replica
        return repository.findAll();
    }
    
    @Transactional
    public TurSNSite create(TurSNSite site) {
        // Routes to master
        return repository.save(site);
    }
}
```

### 2. Database Sharding Strategy

**Shard by Site/Tenant**:

```java
@Configuration
public class ShardingConfiguration {
    
    @Bean
    public DataSource shardedDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        
        // Create data source for each shard
        for (int i = 0; i < SHARD_COUNT; i++) {
            targetDataSources.put(
                "shard-" + i,
                createDataSource("db-shard-" + i + ".example.com")
            );
        }
        
        ShardingDataSource dataSource = new ShardingDataSource();
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(targetDataSources.get("shard-0"));
        
        return dataSource;
    }
}

public class ShardingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        String siteId = ShardingContext.getCurrentSiteId();
        int shardIndex = getShardIndex(siteId);
        return "shard-" + shardIndex;
    }
    
    private int getShardIndex(String siteId) {
        // Consistent hashing
        return Math.abs(siteId.hashCode() % SHARD_COUNT);
    }
}
```

### 3. Database Indexing Strategy

```sql
-- Essential indexes for performance

-- Search site queries
CREATE INDEX idx_sn_site_name ON sn_site(name);
CREATE INDEX idx_sn_site_enabled ON sn_site(enabled);
CREATE INDEX idx_sn_site_owner ON sn_site(owner_id);

-- Search engine instances
CREATE INDEX idx_se_instance_vendor ON se_instance(se_vendor_id);
CREATE INDEX idx_se_instance_enabled ON se_instance(enabled);

-- Integration instances
CREATE INDEX idx_integration_type ON integration_instance(integration_type_id);
CREATE INDEX idx_integration_enabled ON integration_instance(enabled);

-- Composite indexes for common queries
CREATE INDEX idx_sn_site_enabled_owner ON sn_site(enabled, owner_id);
CREATE INDEX idx_se_instance_vendor_enabled ON se_instance(se_vendor_id, enabled);

-- Full-text search on relevant fields
CREATE FULLTEXT INDEX idx_sn_site_name_desc ON sn_site(name, description);
CREATE FULLTEXT INDEX idx_se_instance_title ON se_instance(title, description);
```

### 4. Query Optimization

```java
// ❌ BAD: N+1 query problem
public List<TurSNSiteDTO> getAllSites() {
    List<TurSNSite> sites = repository.findAll();
    return sites.stream()
        .map(site -> {
            TurSNSiteDTO dto = mapper.toDTO(site);
            // This triggers a query for each site!
            dto.setFieldCount(site.getFields().size());
            return dto;
        })
        .collect(Collectors.toList());
}

// ✅ GOOD: Fetch with join
public interface TurSNSiteRepository extends JpaRepository<TurSNSite, String> {
    
    @Query("SELECT s FROM TurSNSite s " +
           "LEFT JOIN FETCH s.fields " +
           "LEFT JOIN FETCH s.searchEngine " +
           "WHERE s.enabled = 1")
    List<TurSNSite> findAllWithDetails();
}

// ✅ GOOD: Use DTO projection
public interface TurSNSiteRepository extends JpaRepository<TurSNSite, String> {
    
    @Query("SELECT new com.viglet.turing.dto.TurSNSiteDTO(" +
           "s.id, s.name, s.description, COUNT(f.id)) " +
           "FROM TurSNSite s LEFT JOIN s.fields f " +
           "WHERE s.enabled = 1 " +
           "GROUP BY s.id, s.name, s.description")
    List<TurSNSiteDTO> findAllSiteSummaries();
}
```

---

## Search Engine Scaling

### 1. Solr Cluster Configuration

**SolrCloud Setup**:

```yaml
# solr-cloud.yml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: solr
spec:
  serviceName: solr-headless
  replicas: 5  # 5 Solr nodes
  selector:
    matchLabels:
      app: solr
  template:
    metadata:
      labels:
        app: solr
    spec:
      containers:
      - name: solr
        image: solr:9.4
        resources:
          requests:
            cpu: "2"
            memory: "8Gi"
          limits:
            cpu: "4"
            memory: "16Gi"
        env:
        - name: ZK_HOST
          value: "zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181/solr"
        - name: SOLR_JAVA_MEM
          value: "-Xms8g -Xmx8g"
        - name: GC_TUNE
          value: "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:G1HeapRegionSize=8m"
        volumeMounts:
        - name: data
          mountPath: /var/solr
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 500Gi
```

**Collection Configuration**:

```bash
# Create collection with 10 shards, 3 replicas each
bin/solr create -c turing_documents \
  -shards 10 \
  -replicationFactor 3 \
  -confname turing_config \
  -confdir /opt/solr/configs/turing
```

### 2. Shard Distribution Strategy

```java
@Configuration
public class SolrShardingConfiguration {
    
    /**
     * Route documents to shards based on site ID
     */
    public String calculateRouteKey(Document document) {
        String siteId = document.getSiteId();
        // Use composite routing: siteId!documentId
        return siteId + "!" + document.getId();
    }
    
    /**
     * Query routing for tenant isolation
     */
    public SolrQuery createRoutedQuery(String siteId, String query) {
        SolrQuery solrQuery = new SolrQuery(query);
        // Route query to specific shards
        solrQuery.set("_route_", siteId);
        return solrQuery;
    }
}
```

### 3. Solr Performance Tuning

```xml
<!-- solrconfig.xml optimizations -->
<config>
  <!-- Query result cache -->
  <queryResultCache 
    class="solr.LRUCache"
    size="512"
    initialSize="512"
    autowarmCount="256"/>
  
  <!-- Document cache -->
  <documentCache
    class="solr.LRUCache"
    size="16384"
    initialSize="16384"
    autowarmCount="8192"/>
  
  <!-- Filter cache -->
  <filterCache
    class="solr.LRUCache"
    size="512"
    initialSize="512"
    autowarmCount="256"/>
  
  <!-- Query settings -->
  <query>
    <maxBooleanClauses>10240</maxBooleanClauses>
    <filterCache class="solr.FastLRUCache" 
                 size="512" 
                 initialSize="512"/>
    <useFilterForSortedQuery>true</useFilterForSortedQuery>
    <queryResultWindowSize>200</queryResultWindowSize>
    <queryResultMaxDocsCached>2000</queryResultMaxDocsCached>
  </query>
  
  <!-- Autocommit settings -->
  <autoCommit>
    <maxTime>60000</maxTime> <!-- 1 minute -->
    <maxDocs>10000</maxDocs>
    <openSearcher>false</openSearcher>
  </autoCommit>
  
  <autoSoftCommit>
    <maxTime>10000</maxTime> <!-- 10 seconds -->
  </autoSoftCommit>
</config>
```

### 4. Search Query Optimization

```java
@Service
public class OptimizedSearchService {
    
    /**
     * Parallel search across multiple collections
     */
    public SearchResult parallelSearch(SearchQuery query, List<String> collections) {
        List<CompletableFuture<SearchResult>> futures = collections.stream()
            .map(collection -> CompletableFuture.supplyAsync(() -> 
                searchInCollection(query, collection), executorService))
            .collect(Collectors.toList());
        
        // Wait for all and merge results
        List<SearchResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        return mergeResults(results);
    }
    
    /**
     * Use cursor-based pagination for large result sets
     */
    public SearchResult deepPagination(SearchQuery query) {
        SolrQuery solrQuery = buildSolrQuery(query);
        
        if (query.getStart() > 10000) {
            // Use cursor for deep pagination
            solrQuery.set("cursorMark", query.getCursorMark());
            solrQuery.setSort("id", SolrQuery.ORDER.asc);
        }
        
        return executeQuery(solrQuery);
    }
}
```

---

## Message Queue Scaling

### 1. Artemis Cluster Configuration

```xml
<!-- broker.xml -->
<configuration>
  <core>
    <!-- Cluster configuration -->
    <cluster-connections>
      <cluster-connection name="turing-cluster">
        <connector-ref>netty-connector</connector-ref>
        <retry-interval>500</retry-interval>
        <use-duplicate-detection>true</use-duplicate-detection>
        <message-load-balancing>ON_DEMAND</message-load-balancing>
        <max-hops>1</max-hops>
        <discovery-group-ref discovery-group-name="turing-discovery"/>
      </cluster-connection>
    </cluster-connections>
    
    <!-- High availability -->
    <ha-policy>
      <replication>
        <master>
          <check-for-live-server>true</check-for-live-server>
        </master>
      </replication>
    </ha-policy>
    
    <!-- Address settings -->
    <address-settings>
      <address-setting match="indexing.#">
        <max-delivery-attempts>3</max-delivery-attempts>
        <redelivery-delay>10000</redelivery-delay>
        <redelivery-multiplier>2.0</redelivery-multiplier>
        <max-redelivery-delay>60000</max-redelivery-delay>
        <dead-letter-address>DLQ</dead-letter-address>
        <expiry-address>ExpiryQueue</expiry-address>
      </address-setting>
    </address-settings>
  </core>
</configuration>
```

### 2. Message Processing Optimization

```java
@Configuration
public class JmsConfiguration {
    
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = 
            new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // Concurrent consumers for parallel processing
        factory.setConcurrency("10-50");
        
        // Prefetch for better throughput
        factory.setSessionTransacted(true);
        factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
        
        // Error handler
        factory.setErrorHandler(new IndexingErrorHandler());
        
        return factory;
    }
}

@Component
public class IndexingMessageConsumer {
    
    /**
     * Process messages in batches for better throughput
     */
    @JmsListener(destination = "indexing.requests", 
                 concurrency = "20-40")
    public void processBatch(List<IndexingRequest> requests) {
        // Batch processing is more efficient
        indexingService.indexBatch(requests);
    }
}
```

---

## Caching Strategies

### 1. Multi-Level Caching

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        return new CompositeCacheManager(
            localCacheManager(),
            distributedCacheManager()
        );
    }
    
    /**
     * Level 1: Local cache (fast, limited size)
     */
    @Bean
    public CacheManager localCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("search-results-local", 
                CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build()
                    .asMap(),
                false)
        ));
        return cacheManager;
    }
    
    /**
     * Level 2: Distributed cache (larger, shared)
     */
    @Bean
    public CacheManager distributedCacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(config)
            .build();
    }
}

@Service
public class SearchService {
    
    @Cacheable(value = "search-results", 
               key = "#query.getCacheKey()",
               unless = "#result.isEmpty()")
    public SearchResult search(SearchQuery query) {
        return searchEngine.search(query);
    }
    
    @CacheEvict(value = "search-results", allEntries = true)
    public void clearSearchCache() {
        // Evict all cached search results
    }
}
```

### 2. Cache Warming Strategy

```java
@Component
public class CacheWarmingService {
    
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void warmCache() {
        List<String> popularQueries = analyticsService.getTopQueries(100);
        
        popularQueries.parallelStream()
            .forEach(query -> {
                try {
                    searchService.search(new SearchQuery(query));
                } catch (Exception e) {
                    log.error("Failed to warm cache for query: {}", query, e);
                }
            });
    }
}
```

---

## Performance Optimization

### 1. Database Query Optimization

```java
// Use batch operations
@Service
public class BatchIndexingService {
    
    @Transactional
    public void indexBatch(List<Document> documents) {
        int batchSize = 100;
        
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(
                i, Math.min(i + batchSize, documents.size())
            );
            
            // Batch insert
            repository.saveAll(batch);
            
            // Flush and clear to free memory
            if (i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}
```

### 2. Async Processing

```java
@Service
public class AsyncSearchService {
    
    @Async("searchExecutor")
    public CompletableFuture<SearchResult> searchAsync(SearchQuery query) {
        SearchResult result = searchEngine.search(query);
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * Parallel search across multiple sites
     */
    public List<SearchResult> searchMultipleSites(SearchQuery query, 
                                                   List<String> siteIds) {
        List<CompletableFuture<SearchResult>> futures = siteIds.stream()
            .map(siteId -> searchAsync(query.forSite(siteId)))
            .collect(Collectors.toList());
        
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
}

@Configuration
public class AsyncConfiguration {
    
    @Bean(name = "searchExecutor")
    public Executor searchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("search-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }
}
```

---

## Monitoring and Observability

### 1. Metrics Collection

```java
@Component
public class SearchMetrics {
    private final MeterRegistry registry;
    
    public void recordSearch(SearchQuery query, long durationMs) {
        // Counter for total searches
        Counter.builder("search.requests.total")
            .tag("site", query.getSiteId())
            .register(registry)
            .increment();
        
        // Timer for latency
        Timer.builder("search.duration")
            .tag("site", query.getSiteId())
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
        
        // Gauge for cache hit rate
        Gauge.builder("search.cache.hit.rate", this::calculateCacheHitRate)
            .register(registry);
    }
}
```

### 2. Health Checks

```java
@Component
public class SearchEngineHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            boolean healthy = searchEngine.healthCheck();
            if (healthy) {
                return Health.up()
                    .withDetail("solr", "Available")
                    .withDetail("shards", getShardStatus())
                    .build();
            } else {
                return Health.down()
                    .withDetail("solr", "Unavailable")
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

---

## Capacity Planning

### Resource Estimation

**Per 100M documents**:
- **Disk Space**: ~500GB (depends on document size)
- **RAM**: 64GB for Solr, 32GB for application
- **CPU**: 16 cores for Solr, 8 cores for application
- **Network**: 1Gbps minimum

**Per 1000 QPS**:
- **Application Instances**: 5-10 instances
- **Solr Nodes**: 3-5 nodes
- **Database**: Read replica pool of 3-5
- **Load Balancer**: 2 for redundancy

---

## Conclusion

Scaling Viglet Turing requires a holistic approach:
1. **Horizontal scaling** of application and search engine
2. **Database optimization** with sharding and replication
3. **Aggressive caching** at multiple levels
4. **Async processing** for long-running operations
5. **Continuous monitoring** and capacity planning

This guide provides the foundation for enterprise-scale deployments.

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-04  
**Maintainer**: Viglet Team
