---
"@viglet/turing-sdk": minor
"@viglet/turing-react-sdk": patch
---

`TuringHttpError` carries the problem-details body of an error answer on `problem` (T1234), typed with the RFC 7807 members and the quota fields: a 429 `quota-exhausted` now tells a page `problem.resetsAt`, when AI answers come back. The `TuringProblem` type is exported. `problem` is `undefined` when the error body is not JSON.
