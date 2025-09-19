# Turing JS SDK

A TypeScript SDK for the Turing Semantic Navigation Search API.

## Installation

```bash
npm install @viglet/turing-sdk
```

## Quick Start

### TypeScript/ES Modules
```typescript
import { TurSNSiteSearchService, TurSNFilterQueryOperator } from '@viglet/turing-sdk';

// Initialize the service
const searchService = new TurSNSiteSearchService('http://localhost:2700');

// Perform a basic search
const results = await searchService.search('yourSiteName', {
  q: 'search query',
  rows: 10,
  currentPage: 1,
  localeRequest: 'en_US',
});

console.log(results);
```

### CommonJS
```javascript
const { TurSNSiteSearchService, TurSNFilterQueryOperator } = require('@viglet/turing-sdk');

async function example() {
  const searchService = new TurSNSiteSearchService('http://locahost:2700');
  
  const results = await searchService.search('sample-site', {
    q: 'artificial intelligence',
    rows: 10,
    currentPage: 1,
    fqOperator: TurSNFilterQueryOperator.AND,
    localeRequest: 'en_US',
  });
  
  console.log('Search results:', results);
}

example().catch(console.error);
```

## Configuration

### Basic Configuration

```typescript
const searchService = new TurSNSiteSearchService('http://localhost:2700');
```

### Advanced Configuration

```typescript
import axios from 'axios';

const searchService = new TurSNSiteSearchService('http://localhost:2700', {
  timeout: 5000,
  headers: {
    'User-Agent': 'MyApp/1.0'
  }
});

```

## API Methods

### Search Methods

#### `search(siteName: string, params?: TurSNSearchParams): Promise<TurSNSiteSearch>`

Perform a GET search request.

```typescript
const results = await searchService.search('sample-site', {
  q: 'artificial intelligence',
  rows: 20,
  currentPage: 1,
  sort: 'relevance',
  fqOperator: TurSNFilterQueryOperator.AND,
  localeRequest: 'en_US',
});
```

#### `searchPost(siteName: string, postParams: TurSNSitePostParams, params?: TurSNSearchParams): Promise<TurSNSiteSearch>`

Perform a POST search request with advanced parameters.

```typescript
const results = await searchService.searchPost('sample-site', {
  userId: 'user123',
  query: 'machine learning',
  populateMetrics: true,
  targetingRules: ['rule1', 'rule2']
  localeRequest: 'en_US',
}, {
  rows: 15
});
```

#### `searchList(siteName: string, params?: TurSNSearchParams): Promise<Set<string>>`

Get a list of search result identifiers.

```typescript
const resultIds = await searchService.searchList('sample-site', {
  q: 'data science',
  rows: 50,
  localeRequest: 'en_US',
});
```

### Utility Methods

#### `getLocales(siteName: string): Promise<TurSNSiteLocale[]>`

Get available locales for a site.

```typescript
const locales = await searchService.getLocales('sample-site');
```

#### `getLatestSearches(siteName: string, rows?: number, locale?: string, request?: TurSNSearchLatestRequest): Promise<string[]>`

Get latest search queries.

```typescript
const latestSearches = await searchService.getLatestSearches('sample-site', 10, 'en_US');
```

## Search Parameters

### TurSNSearchParams

| Parameter | Type | Description |
|-----------|------|-------------|
| `q` | `string` | Search query |
| `currentPage` | `number` | Current page number |
| `rows` | `number` | Number of results per page |
| `sort` | `string` | Sort criteria |
| `filterQueriesDefault` | `string[]` | Default filter queries |
| `filterQueriesAnd` | `string[]` | AND filter queries |
| `filterQueriesOr` | `string[]` | OR filter queries |
| `fqOperator` | `TurSNFilterQueryOperator` | Filter query operator |
| `fqItemOperator` | `TurSNFilterQueryOperator` | Filter query item operator |
| `group` | `string` | Group parameter |
| `autoCorrectionDisabled` | `number` | Auto-correction disabled flag |
| `localeRequest` | `string` | Requested locale |

### TurSNSitePostParams

Extended parameters for POST requests including:
- User targeting and personalization
- Advanced filtering options
- Metrics population settings

## Response Types

### TurSNSiteSearch

The main search response containing:
- `pagination`: Pagination information
- `queryContext`: Query context and metadata
- `results`: Search results with documents
- `groups`: Result groupings
- `widget`: Widget configuration

### TurSNSiteSearchDocument

Individual search result document with:
- `source`: Document source
- `elevate`: Elevation flag
- `metadata`: Document metadata
- `fields`: Document fields

## Enums

### TurSNFilterQueryOperator

- `AND`: AND operation
- `OR`: OR operation  
- `NONE`: No operation

### TurSNPaginationType

- `FIRST`: First page
- `LAST`: Last page
- `PREVIOUS`: Previous page
- `NEXT`: Next page
- `PAGE`: Specific page

## Error Handling

```typescript
try {
  const results = await searchService.search('sample-site', { q: 'test', localeRequest: 'en_US' });
} catch (error) {
  if (error.response) {
    // Server responded with error status
    console.error('API Error:', error.response.status, error.response.data);
  } else if (error.request) {
    // No response received
    console.error('Network Error:', error.message);
  } else {
    // Other error
    console.error('Error:', error.message);
  }
}
```

## TypeScript Support

This SDK is built with TypeScript and provides full type definitions for all API responses and parameters.

## License

Apache-2.0

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.