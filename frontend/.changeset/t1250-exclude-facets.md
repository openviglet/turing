---
"@viglet/turing-sdk": minor
"@viglet/turing-react-sdk": minor
---

`TurSearchRequest.excludeFacets` (T1250) leaves facets out of one search: the engine does not compute them and the response does not carry them, while a filter on an excluded field still applies. The server echoes it, so it stays on every toggle built from the response. Needs a Turing server with the field (2026.3.8 or later); an older one ignores it.
