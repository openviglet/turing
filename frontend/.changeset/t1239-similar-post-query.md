---
"@viglet/turing-sdk": minor
"@viglet/turing-react-sdk": minor
---

`fetchSimilar` sends its request as a JSON body to `POST /sn/{site}/search/similar/query` (T1239) instead of a query string, in both SDKs, so a scope of several sites and filters never has to survive a URL. `similarRequestBody` builds that body and is exported. Needs a Turing server with the POST endpoint (2026.3.8 or later).
