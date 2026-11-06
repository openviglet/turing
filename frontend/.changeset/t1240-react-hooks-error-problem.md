---
"@viglet/turing-react-sdk": minor
---

Every hook that returns an `error` now returns `errorProblem` beside it, and so does the search provider's store (T1240): `useTuringSearch`, `useTuringSimilar`, `useTuringCopilot`, `useTuringLlmChat`, `useTuringSemanticChat`, `useTuringSlots` and the rest. The React SDK's `problemOf(err)` also reads the problem body of a call made through axios, so a 429 `quota-exhausted` on a JSON call carries `resetsAt` too.
