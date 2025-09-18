# GraphQL Implementation for Turing Semantic Navigation Search

This document summarizes the complete GraphQL implementation that was added to the Turing project.

## Overview

This implementation adds GraphQL support to the Turing Semantic Navigation search functionality, providing a modern, type-safe alternative to the existing REST API while reusing the same underlying search infrastructure.

## Files Created/Modified

### 1. Dependencies
- **Modified**: `turing-app/pom.xml`
  - Added `spring-boot-starter-graphql` dependency

### 2. GraphQL Schema
- **Created**: `turing-app/src/main/resources/graphql/schema.graphqls`
  - Complete GraphQL schema definition
  - Types for search results, pagination, facets, groups, etc.
  - Input types for search parameters

### 3. Java Classes
- **Created**: `turing-app/src/main/java/com/viglet/turing/api/sn/graphql/TurSNSearchParamsInput.java`
  - GraphQL input class for search parameters
  - Maps to existing `TurSNSearchParams` structure

- **Created**: `turing-app/src/main/java/com/viglet/turing/api/sn/graphql/TurSNSiteSearchGraphQLController.java`
  - Main GraphQL controller with `@QueryMapping`
  - Converts GraphQL inputs to internal search parameters
  - Creates proper search context using `TurSNUtils`
  - Handles mock HTTP requests for GraphQL context

- **Created**: `turing-app/src/main/java/com/viglet/turing/api/sn/graphql/GraphQLConfig.java`
  - Spring Boot GraphQL configuration class
  - Runtime wiring configuration

### 4. Configuration
- **Modified**: `turing-app/src/main/resources/application.yaml`
  - Added GraphQL configuration
  - Enabled GraphiQL interface at `/graphiql`
  - Configured CORS for GraphQL endpoint

### 5. Documentation
- **Created**: `turing-app/src/main/resources/graphql/README.md`
  - Comprehensive documentation
  - Query examples (basic, advanced, grouped search)
  - Parameter descriptions
  - Integration guidelines

- **Created**: `turing-app/src/main/resources/graphql/sample-queries.graphql`
  - Ready-to-use GraphQL queries for testing
  - Various search scenarios and use cases

### 6. Tests
- **Created**: `turing-app/src/test/java/com/viglet/turing/api/sn/graphql/TurSNSiteSearchGraphQLControllerTest.java`
  - Unit tests for input parameter handling
  - Basic validation tests

## Key Features

### 1. Complete GraphQL Schema
- Covers all search result types from the existing REST API
- Type-safe queries with proper input validation
- Supports all search parameters (filtering, grouping, pagination)

### 2. Reuses Existing Infrastructure
- Uses `TurSNSearchProcess.search()` for consistent behavior
- Leverages `TurSNUtils.getTurSNSiteSearchContext()` for proper context creation
- Compatible with existing caching and security mechanisms
- Returns same `TurSNSiteSearchBean` objects as REST API

### 3. Proper Request Context Handling
- Creates mock `HttpServletRequest` for GraphQL queries
- Maintains compatibility with URI-based context creation
- Handles both web and GraphQL request contexts gracefully

### 4. Developer Experience
- GraphiQL interface for interactive query development
- Comprehensive documentation with examples
- Sample queries for common use cases
- Type-safe schema with detailed field descriptions

## API Endpoints

- **GraphQL API**: `POST /graphql`
- **GraphiQL Interface**: `GET /graphiql`

## Example Usage

```graphql
query {
  siteSearch(
    siteName: "your-site-name"
    searchParams: {
      q: "technology"
      rows: 10
      p: 1
      sort: "relevance"
    }
    locale: "en"
  ) {
    queryContext {
      count
      responseTime
    }
    results {
      numFound
      document {
        fields {
          title
          text
          url
        }
      }
    }
  }
}
```

## Integration Benefits

1. **Type Safety**: Strong typing prevents runtime errors
2. **Flexible Queries**: Clients can request exactly the data they need
3. **Single Endpoint**: All search operations through one GraphQL endpoint
4. **Backward Compatibility**: Existing REST API remains unchanged
5. **Interactive Development**: GraphiQL interface for query development
6. **Consistent Results**: Uses same search engine and processing as REST API

## Deployment Notes

The GraphQL implementation is ready for deployment and will be automatically available once the application starts. No additional configuration is required beyond what's included in the application.yaml file.

The implementation follows Spring Boot GraphQL best practices and maintains compatibility with the existing Turing architecture.