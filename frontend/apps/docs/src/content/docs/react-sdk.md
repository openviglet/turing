---
title: "React SDK"
description: "Headless hooks and unstyled UI components for building custom search experiences with Viglet Turing."
---
The **@viglet/turing-react-sdk** provides headless hooks and unstyled UI components for building custom search experiences on top of the Viglet Turing Semantic Navigation API.

- **Headless architecture**: zero built-in styles, full render control via render props
- **Two modes**: manual (programmatic) or URL-synced (URL as source of truth)
- **TypeScript-first**: full type safety for all hooks, components, and API responses
- **React 18 & 19** compatible

> **Storybook**: Interactive demos and live examples at [turing.viglet.org/react-sdk](https://turing.viglet.org/react-sdk)

---

## Installation

```bash
npm install @viglet/turing-react-sdk
```

**Peer dependencies:** `react` (^18 or ^19) and `axios` (^1.0).

---

## Quick Start

```tsx
import { TuringProvider, useTuringSearch } from "@viglet/turing-react-sdk";

function App() {
  return (
    <TuringProvider config={{ site: "my-site", locale: "en_US" }}>
      <SearchPage />
    </TuringProvider>
  );
}

function SearchPage() {
  const { documents, searchQuery, status } = useTuringSearch();

  return (
    <div>
      <input
        placeholder="Search..."
        onKeyDown={(e) => {
          if (e.key === "Enter") searchQuery(e.currentTarget.value);
        }}
      />
      {status === "loading" && <p>Loading...</p>}
      {documents.map((doc) => (
        <div key={doc.url}>
          <h3>{doc.title}</h3>
          <p>{doc.description}</p>
        </div>
      ))}
    </div>
  );
}
```

---

## TuringProvider

The root context provider that makes Turing config and search state available to all hooks.

```tsx
import { TuringProvider } from "@viglet/turing-react-sdk";
```

### Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `config` | `TuringConfig` | Yes | Site configuration |
| `urlSync` | `{ searchParams, setSearchParams }` | No | Enable URL synchronization |
| `children` | `ReactNode` | Yes | Child components |

### TuringConfig

```typescript
{
  site: string;    // Turing site name (required)
  locale?: string; // Default locale (e.g., "en_US")
  sort?: string;   // Default sort (e.g., "relevance")
}
```

### URL Sync Mode

When `urlSync` is provided, the URL becomes the single source of truth. Changes to the URL trigger automatic searches, and searches update the URL automatically.

```tsx
import { useSearchParams } from "react-router-dom";

function App() {
  const [searchParams, setSearchParams] = useSearchParams();
  return (
    <TuringProvider
      config={{ site: "my-site", locale: "pt_BR" }}
      urlSync={{ searchParams, setSearchParams }}
    >
      <SearchPage />
    </TuringProvider>
  );
}
```

---

## Hooks

### useTuringSearch

Core search hook for **manual (non-URL)** mode. Manages the full lifecycle: query, results, pagination, facets, locale, and sort.

```tsx
const {
  status,        // "idle" | "loading" | "success" | "error"
  data,          // Raw TurSearchResponse
  chat,          // AI chat response (null for wildcard queries)
  documents,     // Resolved documents with mapped default fields
  groups,        // Resolved groups (when group param used)
  error,         // Error message
  params,        // Current search params
  structured,    // Link-free response (POST-native; authoritative)
  request,       // The request that will be sent next
  search,        // Execute with given params
  searchRequest, // Execute from an explicit structured request
  searchQuery,   // New text query (resets page to 1)
  toggleFacet,   // Toggle a structured facet item, or a result's own facet chip
  clearFacet,    // Clear one facet
  clearAllFilters,
  applySpellCheck, // Accept (or refuse) "did you mean"
  changeLocale,  // Change locale
  changeSort,    // Change sort
  goToPage,      // Go to specific page
} = useTuringSearch(initialParams?);
```

The hook is **POST-native** by default: it calls the
[POST-native search endpoint](/docs/rest-api/#post-native-search) and asks for the
link-free response, so `structured` is authoritative and every action above is a
change to `request` rather than a Turing href to parse. That is what makes a
filter value containing `&`, `=`, `+`, `#`, `%` or a space safe.

`data` (the legacy link-bearing body) is `null` in this mode. Pass
`{ postNative: false }` to go back to the GET contract and `data`, and render its
hrefs as real anchors. The deprecated `navigate(href)` still works on that path
but logs a one-time console notice and is **scheduled for removal in the next
major**; the same applies to `onNavigate` on `TuringPagination`.

### useTuringAutoComplete

Autocomplete hook that calls the Turing `/ac` endpoint with built-in debouncing.

```tsx
const {
  suggestions, // string[]: completion suggestions
  isLoading,   // Fetching state
  fetch,       // Trigger autocomplete (min 2 chars)
  clear,       // Clear suggestions
} = useTuringAutoComplete(debounceMs?: number); // default: 300
```

### useTuringFacets

Provides facet data with toggle/clear actions. Requires `urlSync` mode.

```tsx
const {
  facetGroups, // Groups with toggle/clear actions
  hasSelected, // Any facet selected?
  clearAll,    // Remove all selections
  raw,         // Raw API facet data
} = useTuringFacets();
```

Each `facetGroup` contains:

```typescript
{
  name: string;        // Group identifier
  label: string;       // Localized label
  multivalued: boolean;
  hasSelected: boolean;
  clear: () => void;   // Clear group selections
  facets: [{
    count: number;
    label: string;
    selected: boolean;
    toggle: () => void; // Toggle this facet
    link?: string;      // Legacy mode only (see below)
  }];
}
```

`link` is the server-rendered href for a facet value and exists **only** in
legacy mode (`postNative: false`). The default POST-native mode returns a
link-free response, so it is `undefined` there: use `toggle()`, and if you need
a crawlable anchor, handle the absence explicitly rather than rendering
`<a href={facet.link}>` (which would link to the current page).

### useTuringPagination

Provides pagination data with navigation helpers. Requires `urlSync` mode.

```tsx
const {
  pages,       // All pages with action helpers
  currentPage, // Current page number
  pageCount,   // Total pages
  totalCount,  // Total results
  hasPages,    // Multiple pages?
} = useTuringPagination();
```

Each `page` contains:

```typescript
{
  page: number;
  label: string;       // "1", "2", "Next", etc.
  type: string;        // PAGE | CURRENT | FIRST | PREVIOUS | NEXT | LAST | ELLIPSIS
  isCurrent: boolean;
  isEllipsis: boolean;
  select: () => void;  // Navigate to this page
}
```

### useTuringDocument

Typed, normalized access to document custom fields, eliminating `Array.isArray()` checks.

```tsx
const {
  getString,  // (field, fallback?) => string
  getArray,   // (field) => string[]
  getNumber,  // (field, fallback?) => number
  getBoolean, // (field) => boolean
  getDate,    // (field) => Date | null
  getRaw,     // <T>(field) => T | undefined
  fields,     // Direct field access
} = useTuringDocument(document);
```

### useTuringTabs

Manages tab-based search with filter switching and grouping. Keeps URLs clean using implicit params.

```tsx
const {
  activeIndex, // Active tab index
  activeTab,   // Active tab definition
  tabs,        // All tabs with select() action
  isGrouped,   // Active tab uses grouping?
  documents,   // Docs for active tab
  groups,      // Groups (if grouped)
} = useTuringTabs({
  attribute: "templateName",
  items: [
    { label: "All" },
    { label: "Courses", filter: "templateName:cursos", group: "category" },
    { label: "News", filter: "templateName:noticias", rows: 5 },
  ],
});
```

### useTuringSortOptions

Fetches available sort options (built-in + custom from Turing admin console).

```tsx
const {
  sortOptions, // { value: string, label: string }[]
  isLoading,
  error,
  refresh,     // Re-fetch options
} = useTuringSortOptions();
```

### useTuringSearchHistory

Persists search terms in IndexedDB, scoped per site + domain.

```tsx
const {
  history, // Recent terms (newest first, deduplicated)
  save,    // Save a term
  remove,  // Remove a term
  clear,   // Clear all for current site
} = useTuringSearchHistory(maxItems?: number); // default: 50
```

### useTuringUrlSearch

Returns the shared search store from TuringProvider when `urlSync` is configured.

```tsx
const store = useTuringUrlSearch();
// Same shape as TuringProvider's internal store:
// status, structured, data, documents, groups, params, inputValue,
// submitSearch, toggleFacet, goToPage, navigate, setLocale, setSort, showAll, etc.
```

`structured` holds the link-free response (the default). `data` holds the legacy
link-bearing one and is `null` unless you set `urlSync.postNative: false`. Prefer
the derived hooks above (and `useSearchView` below) over reading either directly.

### useSearchView

Projects whichever response arrived onto one view model, so a component never
learns which shape it got. It covers the parts the other hooks do not (the site
icon, the locales, the applied-filter row and the spell-check offer) and resolves
each **action** with its state: removing an applied filter subtracts a filter query
in the default mode and follows an href on the legacy transport, through the same
`remove()` call.

```tsx
const view = useSearchView();
if (!view.ready) return <Spinner />;   // the render gate, either shape

return (
  <>
    {view.siteIcon ? <Icon icon={view.siteIcon} /> : <YourLogo />}
    {view.appliedFilters.map((filter) => (
      <button key={filter.key} onClick={filter.remove}>{filter.label} ✕</button>
    ))}
    {view.spellCheck?.correctedText && (
      <button onClick={view.spellCheck.applyCorrection}>
        Did you mean {view.spellCheck.correctedTextValue}?
      </button>
    )}
    <FacetRail multiSelect={view.multiSelectFacetItems} />
  </>
);
```

| Field | Description |
|---|---|
| `source` | `"structured"` \| `"legacy"` \| `"none"`: which shape backed the view |
| `ready` | `true` once a response of either shape arrived |
| `siteIcon` | The site's Iconify name, or `null`: fall back to your own logo |
| `locales` | `[{ locale }]` the site can be searched in |
| `queryContext` | Counts, paging and default field names |
| `appliedFilters` | `[{ key, label, remove() }]`: the "applied filters" row |
| `spellCheck` | `{ correctedTextValue, applyCorrection(), applyOriginal() }` or `null` |
| `multiSelectFacetItems` | `true` when the site's facets multi-select (draw checkboxes) |

The projection itself is `toSearchView(store)` in
[`@viglet/turing-sdk`](/docs/javascript-sdk/#tosearchview), the same function, so a
plain-JS host and a React one resolve a response identically. This hook is the
React wrapper.

---

### useTuringChat

Streams a conversation with the site's chat (`/sn/{site}/chat/conversation`) inside a `TuringProvider`. It returns `messages`, `status`, `isStreaming`, `error` and `send(text, overrides?)`, plus `stop()` and `reset()`.

To answer within the visitor's current search, pass `filters` (field → values, the shape [`postNlFilters`](/docs/rest-api/#natural-language-filters) returns); `send`'s second argument replaces them for one turn. `onFilters` receives the filters the server read from a list-style question, ahead of the answer:

```tsx
const { messages, send } = useTuringChat({
  filters: { uf: [currentUf] },
  onFilters: (parsed) => setSuggestedFq(parsed.fq),
});

await send("Quais são remotas?");
await send("E no Rio?", { filters: { uf: ["RJ"] } }); // this turn only
```

## UI Components

All components are **unstyled** (headless). You control the look via render props and CSS.

### TuringSearchBar

Simple search bar with customizable input/button via render props.

```tsx
<TuringSearchBar
  onSearch={(query) => console.log(query)}
  placeholder="Search..."
  defaultQuery=""
  renderInput={(props) => <input {...props} className="my-input" />}
  renderButton={({ onClick }) => <button onClick={onClick}>Go</button>}
/>
```

| Prop | Type | Description |
|------|------|-------------|
| `onSearch` | `(query: string) => void` | Callback on Enter or button click |
| `defaultQuery` | `string` | Pre-fill value |
| `placeholder` | `string` | Input placeholder |
| `renderInput` | `(props) => ReactNode` | Custom input renderer |
| `renderButton` | `(props) => ReactNode` | Custom button renderer |

### TuringSearchField

Advanced compound component with autocomplete, search history, and dropdown.

```tsx
<TuringSearchField turing={urlSearchStore} debounce={300} maxHistory={8}>
  <TuringSearchField.Input placeholder="Search..." className="my-input" />
  <TuringSearchField.Button className="my-btn">Search</TuringSearchField.Button>
  <TuringSearchField.Dropdown
    className="my-dropdown"
    historyLabel="Recent searches"
    clearAllLabel="Clear all"
    renderSuggestion={(term, onClick) => (
      <div onClick={onClick}>{term}</div>
    )}
    renderHistory={(term, onClick, onRemove) => (
      <div>
        <span onClick={onClick}>{term}</span>
        <button onClick={onRemove}>x</button>
      </div>
    )}
  />
</TuringSearchField>
```

### TuringResultList

Renders search results using the slot/render-prop pattern.

```tsx
<TuringResultList
  documents={documents}
  isLoading={status === "loading"}
  itemComponent={({ document, index }) => (
    <div key={document.url}>
      <h3>{document.title}</h3>
      <p>{document.description}</p>
    </div>
  )}
  emptyComponent={() => <p>No results found.</p>}
  loadingComponent={() => <p>Loading...</p>}
/>
```

| Prop | Type | Description |
|------|------|-------------|
| `documents` | `ResolvedDocument[]` | Documents to render |
| `itemComponent` | `(props) => ReactNode` | Item renderer (required) |
| `emptyComponent` | `() => ReactNode` | No-results state |
| `loadingComponent` | `() => ReactNode` | Loading state |
| `isLoading` | `boolean` | Show loading component |
| `onResultClick` | `(document, index) => void` | Fires on a row click: wire it to `trackResultClick` |
| `onToggleFacet` | `(target) => void` | Toggles a result's facet chip; forwarded to `itemComponent` |
| `onNavigate` | `(href) => void` | Legacy: follows a chip's href; forwarded the same way |

### TuringDocumentChips

Renders a result's facet chips (`document.raw.metadata`) as **actions**. A chip
arrives in one of two shapes (a legacy `href` or a link-free `filterQuery`), and
this narrows that for you: a filter query becomes an `aria-pressed` button, an
href becomes an anchor that still carries the URL (for middle-click and crawlers)
but calls your handler, and a chip you gave no handler for renders as plain text
rather than as a control that does nothing.

```tsx
<TuringResultList
  documents={documents}
  onToggleFacet={toggleFacet}
  itemComponent={({ document, raw, onToggleFacet }) => (
    <article>
      <h3>{document.title}</h3>
      <TuringDocumentChips
        chips={raw.metadata}
        onToggleFacet={onToggleFacet}
        chipClassName="chip"
        selectedChipClassName="chip--on"
      />
    </article>
  )}
/>
```

| Prop | Type | Description |
|------|------|-------------|
| `chips` | `TuringDocumentChip[]` | The document's `metadata` array, either shape |
| `onToggleFacet` | `(target) => void` | Omit it and every chip renders inert |
| `onNavigate` | `(href) => void` | Legacy chips only |
| `itemComponent` | `(props) => ReactNode` | Own markup; receives `chip`, `isActionable`, `isSelected`, `onClick` |
| `chipClassName` / `selectedChipClassName` | `string` | Skin the default renderer |

Also exported from `@viglet/turing-react-ui` for hosts that consume the headless
package directly.

### TuringPagination

Renders pagination items with navigation actions.

```tsx
<TuringPagination
  items={structured?.pagination ?? []}
  onSelectPage={(page) => goToPage(page)}
  itemComponent={({ item, isCurrent, isEllipsis, onClick, label }) => (
    <button
      onClick={onClick}
      disabled={isCurrent}
      style={{ fontWeight: isCurrent ? "bold" : "normal" }}
    >
      {isEllipsis ? "..." : label}
    </button>
  )}
/>
```

---

## Data Types

### ResolvedDocument

Documents returned by hooks have default fields already mapped:

```typescript
{
  url: string;         // Document URL
  title: string;       // Document title
  description: string; // Document description
  date: string;        // Document date
  image: string;       // Image URL
  text: string;        // Full text
  raw: TurDocument;    // Full document with all custom fields
}
```

### SearchParams

```typescript
{
  q: string;           // Query string
  p?: string;          // Page number
  _setlocale?: string; // Locale
  sort?: string;       // Sort field
  fq?: string[];       // Facet queries (filters)
  group?: string;      // Group field name
  rows?: string;       // Results per page
}
```

---

## Full Example: Search Page with Facets

```tsx
import {
  TuringProvider,
  useTuringUrlSearch,
  useTuringFacets,
  useTuringPagination,
  TuringResultList,
  TuringSearchBar,
  TuringPagination,
} from "@viglet/turing-react-sdk";
import { useSearchParams } from "react-router-dom";

function App() {
  const [searchParams, setSearchParams] = useSearchParams();
  return (
    <TuringProvider
      config={{ site: "my-site", locale: "en_US" }}
      urlSync={{ searchParams, setSearchParams }}
    >
      <SearchPage />
    </TuringProvider>
  );
}

function SearchPage() {
  const store = useTuringUrlSearch();
  const { facetGroups, clearAll } = useTuringFacets();
  const { pages } = useTuringPagination();

  return (
    <div>
      <TuringSearchBar onSearch={(q) => store.searchQuery(q)} />

      <aside>
        {facetGroups.map((group) => (
          <div key={group.name}>
            <h4>{group.label}</h4>
            {group.facets.map((facet) => (
              <label key={facet.label}>
                <input
                  type="checkbox"
                  checked={facet.selected}
                  onChange={facet.toggle}
                />
                {facet.label} ({facet.count})
              </label>
            ))}
          </div>
        ))}
      </aside>

      <main>
        <TuringResultList
          documents={store.documents}
          isLoading={store.status === "loading"}
          itemComponent={({ document }) => (
            <article>
              <h3><a href={document.url}>{document.title}</a></h3>
              <p>{document.description}</p>
            </article>
          )}
          emptyComponent={() => <p>No results found.</p>}
        />

        <TuringPagination
          items={store.structured?.pagination ?? []}
          onSelectPage={store.goToPage}
        />
      </main>
    </div>
  );
}
```

---

## Links

- [GitHub Repository](https://github.com/openviglet/turing)
- [npm Package](https://www.npmjs.com/package/@viglet/turing-react-sdk)
- [Storybook (Live Demos)](https://turing.viglet.org/react-sdk)
- [REST API Reference](/docs/rest-api/)

---

## Related Pages

| Page | Description |
|---|---|
| [Developer Guide](/docs/developer-guide/) | Dev environment, stack, contributing |
| [Client Tools](/docs/client-tools/) | Live tool activity, client tools, generative UI |
| [Reference showcase](/docs/showcase/) | Atlas Store: a full app built on this SDK |
| [Chat](/docs/chat/) | The chat surface the hooks drive |
| [REST API Reference](/docs/rest-api/) | The endpoints behind the hooks |

