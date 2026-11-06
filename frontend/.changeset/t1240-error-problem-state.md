---
"@viglet/turing-sdk": minor
"@viglet/turing-react-sdk": minor
---

The controllers keep a failed call's problem details on `errorProblem`, beside the `error` text (T1240): the answer, chat, search, research, similar and slots controllers, and the React `useTuringAnswer` and `useTuringChat` hooks. A 429 `quota-exhausted` now tells a subscriber `errorProblem.resetsAt`. `problemOf(err)` returns the problem details of any caught error, and is exported from both packages.
