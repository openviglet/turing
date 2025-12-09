# Search Engine Plugin System

## Overview

The Turing Search Engine Plugin System allows you to switch between different search engine implementations (Solr, Elasticsearch, etc.) through configuration. This provides flexibility to choose the best search engine for your use case without changing application code.

## Architecture

The plugin system consists of:

1. **TurSearchEnginePlugin Interface** - Defines the contract for search engine implementations
2. **Plugin Implementations** - Concrete implementations for different search engines:
   - `TurSolrSearchEnginePlugin` - Apache Solr implementation (fully functional)
   - `TurElasticsearchSearchEnginePlugin` - Elasticsearch stub (for future implementation)
3. **TurSearchEnginePluginFactory** - Factory that manages and provides plugin instances based on configuration

## Configuration

Configure the search engine type in `application.yaml`:

```yaml
turing:
  search:
    engine:
      type: solr  # Options: solr, elasticsearch
```

### Default Configuration

If not specified, the system defaults to `solr`.

## Using Different Search Engines

### Apache Solr (Default)

```yaml
turing:
  search:
    engine:
      type: solr
  solr:
    timeout: 30000
    cloud: false
    commit:
      within: 10000
      enabled: false
```

### Elasticsearch (Coming Soon)

```yaml
turing:
  search:
    engine:
      type: elasticsearch
  elasticsearch:
    # Elasticsearch-specific configuration will go here
```

**Note:** Elasticsearch support is currently a stub implementation for future development.

## Plugin Interface

All search engine plugins must implement the `TurSearchEnginePlugin` interface:

```java
public interface TurSearchEnginePlugin {
    Optional<TurSEResults> retrieveSearchResults(TurSNSiteSearchContext context);
    Optional<TurSEResults> retrieveFacetResults(TurSNSiteSearchContext context, String facetName);
    String getPluginType();
}
```

## Creating a New Plugin

To add support for a new search engine:

1. Create a new package under `com.viglet.turing.plugins.se` (e.g., `opensearch`)

2. Implement the `TurSearchEnginePlugin` interface:

```java
@Slf4j
@Component
public class TurOpenSearchSearchEnginePlugin implements TurSearchEnginePlugin {
    
    @Override
    public Optional<TurSEResults> retrieveSearchResults(TurSNSiteSearchContext context) {
        // Implement search logic for OpenSearch
    }

    @Override
    public Optional<TurSEResults> retrieveFacetResults(TurSNSiteSearchContext context, String facetName) {
        // Implement facet retrieval logic for OpenSearch
    }

    @Override
    public String getPluginType() {
        return "opensearch";
    }
}
```

3. Register your plugin by annotating it with `@Component` (Spring will auto-discover it)

4. Update configuration to use your new plugin:

```yaml
turing:
  search:
    engine:
      type: opensearch
```

## Plugin Factory

The `TurSearchEnginePluginFactory` automatically discovers and registers all `TurSearchEnginePlugin` implementations via Spring's dependency injection.

### Usage in Code

```java
@Autowired
private TurSearchEnginePluginFactory pluginFactory;

public void performSearch() {
    // Get the configured default plugin
    TurSearchEnginePlugin plugin = pluginFactory.getDefaultPlugin();
    
    // Or get a specific plugin
    TurSearchEnginePlugin solrPlugin = pluginFactory.getPlugin("solr");
}
```

## Benefits

1. **Flexibility** - Switch search engines without code changes
2. **Extensibility** - Easy to add new search engine implementations
3. **Maintainability** - Clear separation of concerns between search engines
4. **Testability** - Mock plugins for testing
5. **Configuration-driven** - Control search engine selection through configuration

## Current Status

- ✅ **Solr Plugin** - Fully implemented and functional
- ⚠️ **Elasticsearch Plugin** - Stub implementation (returns empty results with warning logs)
- 🔄 **Future Plugins** - Can be added as needed (OpenSearch, Algolia, etc.)

## Migration from Direct Solr Usage

The plugin system maintains backward compatibility. Existing Solr-based installations will continue to work without any configuration changes, as Solr is the default search engine.

## Logging

The plugin system provides informative logs:
- Plugin registration and initialization
- Default engine selection
- Warnings for unimplemented plugins

Example logs:
```
INFO  TurSearchEnginePluginFactory - Initialized TurSearchEnginePluginFactory with 2 plugins. Default engine: solr
INFO  TurSearchEnginePluginFactory - Registered plugin: solr
INFO  TurSearchEnginePluginFactory - Registered plugin: elasticsearch
```
