# Usage Examples for Turing JS SDK

This document provides practical examples of how to use the Turing JS SDK to interact with the Turing Semantic Navigation Search API.

## Installation and Setup

```bash
cd turing-js-sdk
npm install
npm run build
```

## Basic Usage

### Import and Initialize

```typescript
import { TurSNSiteSearchService, TurSNFilterQueryOperator } from './dist/index.js';

const searchService = new TurSNSiteSearchService('https://your-turing-instance.com');
searchService.setAuth('your-bearer-token'); // if authentication is required
```

### Simple Search

```typescript
async function basicSearch() {
  try {
    const results = await searchService.search('yourSiteName', {
      q: 'machine learning',
      rows: 10,
      currentPage: 1
    });
    
    console.log('Total results:', results.queryContext?.count);
    console.log('Documents:', results.results?.document);
  } catch (error) {
    console.error('Search failed:', error.message);
  }
}
```

### Advanced Search with Filters

```typescript
async function advancedSearch() {
  const results = await searchService.search('yourSiteName', {
    q: 'artificial intelligence',
    rows: 20,
    currentPage: 1,
    filterQueriesAnd: ['category:technology', 'status:published'],
    filterQueriesOr: ['tag:ai', 'tag:ml'],
    fqOperator: TurSNFilterQueryOperator.AND,
    sort: 'date desc',
    localeRequest: 'en_US'
  });
  
  return results;
}
```

### POST Search with Targeting

```typescript
async function targetedSearch() {
  const postParams = {
    userId: 'user123',
    query: 'data science',
    populateMetrics: true,
    targetingRules: ['audience:technical', 'level:advanced'],
    locale: 'en_US',
    fqOperator: TurSNFilterQueryOperator.AND
  };
  
  const results = await searchService.searchPost('yourSiteName', postParams, {
    rows: 15
  });
  
  return results;
}
```

### Search List (Get Document IDs)

```typescript
async function getSearchIds() {
  const ids = await searchService.searchList('yourSiteName', {
    q: 'neural networks',
    rows: 100
  });
  
  console.log('Document IDs:', Array.from(ids));
  return ids;
}
```

### Get Available Locales

```typescript
async function getAvailableLocales() {
  const locales = await searchService.getLocales('yourSiteName');
  
  locales.forEach(locale => {
    console.log(`Locale: ${locale.locale}, Link: ${locale.link}`);
  });
  
  return locales;
}
```

### Get Latest Searches

```typescript
async function getRecentSearches() {
  const latestSearches = await searchService.getLatestSearches(
    'yourSiteName',
    10, // rows
    'en_US', // locale
    { userId: 'user123' } // optional request body
  );
  
  console.log('Recent searches:', latestSearches);
  return latestSearches;
}
```

## Error Handling

```typescript
async function searchWithErrorHandling() {
  try {
    const results = await searchService.search('yourSiteName', {
      q: 'test query',
      rows: 10
    });
    return results;
  } catch (error) {
    if (error.response) {
      // Server responded with error status
      console.error('API Error:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data
      });
    } else if (error.request) {
      // No response received
      console.error('Network Error - no response received');
    } else {
      // Other error
      console.error('Error:', error.message);
    }
    throw error;
  }
}
```

## Working with Search Results

```typescript
async function processSearchResults() {
  const results = await searchService.search('yourSiteName', {
    q: 'artificial intelligence',
    rows: 10
  });
  
  // Access query context
  const { queryContext } = results;
  if (queryContext) {
    console.log(`Found ${queryContext.count} results in ${queryContext.responseTime}ms`);
    console.log(`Page ${queryContext.page} of ${queryContext.pageCount}`);
  }
  
  // Process documents
  const documents = results.results?.document || [];
  documents.forEach((doc, index) => {
    console.log(`Document ${index + 1}:`);
    console.log(`  Source: ${doc.source}`);
    console.log(`  Elevated: ${doc.elevate}`);
    
    // Access custom fields
    if (doc.fields) {
      Object.entries(doc.fields).forEach(([key, value]) => {
        console.log(`  ${key}: ${value}`);
      });
    }
  });
  
  // Handle pagination
  const pagination = results.pagination || [];
  pagination.forEach(page => {
    console.log(`${page.type}: ${page.text} (${page.href})`);
  });
}
```

## Configuration Examples

### Custom Axios Configuration

```typescript
const searchService = new TurSNSiteSearchService('https://your-turing-instance.com', {
  timeout: 10000,
  headers: {
    'User-Agent': 'MyApp/1.0',
    'Custom-Header': 'custom-value'
  }
});
```

### Environment-based Configuration

```typescript
const baseURL = process.env.TURING_BASE_URL || 'https://localhost:2700';
const authToken = process.env.TURING_AUTH_TOKEN;

const searchService = new TurSNSiteSearchService(baseURL);

if (authToken) {
  searchService.setAuth(authToken);
}
```

## Integration Examples

### React Component

```typescript
import React, { useState, useEffect } from 'react';
import { TurSNSiteSearchService, TurSNSiteSearch } from '@openviglet/turing-js-sdk';

const SearchComponent: React.FC = () => {
  const [results, setResults] = useState<TurSNSiteSearch | null>(null);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState('');

  const searchService = new TurSNSiteSearchService('https://your-turing-instance.com');

  const handleSearch = async () => {
    if (!query.trim()) return;

    setLoading(true);
    try {
      const searchResults = await searchService.search('yourSiteName', {
        q: query,
        rows: 10
      });
      setResults(searchResults);
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Enter search query..."
      />
      <button onClick={handleSearch} disabled={loading}>
        {loading ? 'Searching...' : 'Search'}
      </button>
      
      {results && (
        <div>
          <h3>Results ({results.queryContext?.count})</h3>
          {results.results?.document?.map((doc, index) => (
            <div key={index}>
              <h4>{doc.fields?.title}</h4>
              <p>{doc.fields?.description}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
```

### Node.js CLI Tool

```typescript
#!/usr/bin/env node

import { TurSNSiteSearchService } from '@openviglet/turing-js-sdk';

async function main() {
  const [,, siteName, query] = process.argv;
  
  if (!siteName || !query) {
    console.log('Usage: node search-cli.js <siteName> <query>');
    process.exit(1);
  }

  const searchService = new TurSNSiteSearchService('https://your-turing-instance.com');
  
  try {
    const results = await searchService.search(siteName, { q: query });
    
    console.log(`Found ${results.queryContext?.count || 0} results:`);
    
    results.results?.document?.forEach((doc, index) => {
      console.log(`${index + 1}. ${doc.fields?.title || 'No title'}`);
      if (doc.fields?.description) {
        console.log(`   ${doc.fields.description}`);
      }
      console.log('');
    });
  } catch (error) {
    console.error('Search failed:', error.message);
    process.exit(1);
  }
}

main();
```

These examples demonstrate the full capabilities of the Turing JS SDK for integrating Turing Semantic Navigation search functionality into your applications.