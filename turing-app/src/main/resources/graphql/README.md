# GraphQL Search API

This directory contains the GraphQL schema and documentation for the Turing Semantic Navigation search API.

## GraphQL Endpoint

The GraphQL endpoint is available at: `/graphql`

## Example Queries

### Basic Search Query

```graphql
query {
  siteSearch(
    siteName: "your-site-name"
    searchParams: {
      q: "your search query"
      rows: 10
      p: 1
      sort: "relevance"
    }
    locale: "en"
  ) {
    queryContext {
      count
      page
      pageCount
      responseTime
    }
    results {
      numFound
      start
      document {
        fields {
          title
          text
          url
          date
        }
        source
      }
    }
    pagination {
      text
      href
      page
      current
    }
  }
}
```

### Advanced Search with Filters

```graphql
query {
  siteSearch(
    siteName: "your-site-name"
    searchParams: {
      q: "technology"
      rows: 20
      p: 1
      sort: "date"
      fq: ["type:article", "status:published"]
      fqOp: "AND"
    }
    locale: "en"
  ) {
    queryContext {
      count
      limit
      offset
      responseTime
      defaultFields {
        title
        description
        url
        date
      }
    }
    results {
      numFound
      document {
        fields {
          id
          title
          text
          description
          url
          date
          image
        }
        metadata {
          name
          value
        }
        source
      }
      facet {
        name
        label {
          lang
          text
        }
        facets {
          count
          label
          facet
        }
      }
    }
    groups {
      name
      count
      page
      pageCount
      results {
        numFound
        document {
          fields {
            title
            url
            date
          }
        }
      }
    }
  }
}
```

### Search with Grouping

```graphql
query {
  siteSearch(
    siteName: "your-site-name"
    searchParams: {
      q: "*"
      group: "category"
      rows: 5
      nfpr: 3
    }
    locale: "en"
  ) {
    groups {
      name
      count
      page
      pageCount
      pageStart
      pageEnd
      limit
      results {
        numFound
        document {
          fields {
            title
            description
            url
          }
        }
      }
      pagination {
        text
        href
        page
        current
      }
    }
  }
}
```

## Search Parameters

The `searchParams` input supports the following fields:

- `q`: Query string (default: "*")
- `p`: Page number (default: 1)
- `rows`: Number of results per page (default: -1 for all)
- `sort`: Sort order ("relevance", "date", "title", etc.)
- `group`: Field to group results by
- `nfpr`: Number of facets per group (default: 1)
- `fq`: Filter queries (array of strings)
- `fqAnd`: AND filter queries (array of strings)
- `fqOr`: OR filter queries (array of strings)
- `fqOp`: Filter query operator ("AND", "OR", "NONE")
- `fqiOp`: Filter query item operator ("AND", "OR", "NONE")
- `locale`: Locale for the search
- `fl`: Fields to return (array of strings)

## Response Structure

The search response includes:

- `pagination`: Array of pagination links
- `queryContext`: Search metadata (count, timing, etc.)
- `results`: Main search results with documents and facets
- `groups`: Grouped results (when using group parameter)
- `widget`: Custom widget data

## Error Handling

If a site is not found or doesn't support the specified locale, an empty response will be returned with default values.

## Integration

This GraphQL API uses the same underlying search infrastructure as the REST API (`TurSNSiteSearchAPI`), ensuring consistency between both interfaces.