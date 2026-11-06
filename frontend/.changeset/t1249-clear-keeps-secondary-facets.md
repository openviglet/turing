---
"@viglet/turing-sdk": minor
"@viglet/turing-react-sdk": minor
---

`clearAllFilters` keeps the selections of the response's secondary facets (T1249), usually rendered as tabs, in both SDKs, so clearing the side filters no longer drops the tab the visitor is on. `keepSecondaryFilterQueries` computes what it keeps and is exported. The server's `cleanUpFacets` link keeps them the same way.
