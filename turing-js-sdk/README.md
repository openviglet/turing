# Turing JS SDK

A TypeScript SDK for the Turing Semantic Navigation Search API.

## Installation

```bash
npm install @openviglet/turing-js-sdk
```

## Quick Start

### TypeScript/ES Modules
```typescript
import { TurSNSiteSearchService, TurSNFilterQueryOperator } from '@openviglet/turing-js-sdk';

// Initialize the service
const searchService = new TurSNSiteSearchService('https://your-turing-instance.com');

// Perform a basic search
const results = await searchService.search('yourSiteName', {
  q: 'search query',
  rows: 10,
  currentPage: 1
});

console.log(results);
```

### CommonJS
```javascript
const { TurSNSiteSearchService, TurSNFilterQueryOperator } = require('@openviglet/turing-js-sdk');

async function example() {
  const searchService = new TurSNSiteSearchService('https://your-turing-instance.com');
  
  const results = await searchService.search('yourSiteName', {
    q: 'artificial intelligence',
    rows: 10,
    currentPage: 1,
    fqOperator: TurSNFilterQueryOperator.AND
  });
  
  console.log('Search results:', results);
}

example().catch(console.error);
```

## Configuration

### Basic Configuration

```typescript
const searchService = new TurSNSiteSearchService('https://your-turing-instance.com');
```

### Advanced Configuration

```typescript
import axios from 'axios';

const searchService = new TurSNSiteSearchService('https://your-turing-instance.com', {
  timeout: 5000,
  headers: {
    'User-Agent': 'MyApp/1.0'
  }
});

// Set authentication
searchService.setAuth('your-token-here');
```

## API Methods

### Search Methods

#### `search(siteName: string, params?: TurSNSearchParams): Promise<TurSNSiteSearch>`

Perform a GET search request.

```typescript
const results = await searchService.search('mysite', {
  q: 'artificial intelligence',
  rows: 20,
  currentPage: 1,
  sort: 'relevance',
  fqOperator: TurSNFilterQueryOperator.AND
});
```

#### `searchPost(siteName: string, postParams: TurSNSitePostParams, params?: TurSNSearchParams): Promise<TurSNSiteSearch>`

Perform a POST search request with advanced parameters.

```typescript
const results = await searchService.searchPost('mysite', {
  userId: 'user123',
  query: 'machine learning',
  populateMetrics: true,
  targetingRules: ['rule1', 'rule2']
}, {
  rows: 15
});
```

#### `searchList(siteName: string, params?: TurSNSearchParams): Promise<Set<string>>`

Get a list of search result identifiers.

```typescript
const resultIds = await searchService.searchList('mysite', {
  q: 'data science',
  rows: 50
});
```

### Utility Methods

#### `getLocales(siteName: string): Promise<TurSNSiteLocale[]>`

Get available locales for a site.

```typescript
const locales = await searchService.getLocales('mysite');
```

#### `getLatestSearches(siteName: string, rows?: number, locale?: string, request?: TurSNSearchLatestRequest): Promise<string[]>`

Get latest search queries.

```typescript
const latestSearches = await searchService.getLatestSearches('mysite', 10, 'en');
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
  const results = await searchService.search('mysite', { q: 'test' });
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