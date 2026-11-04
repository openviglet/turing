---
"@viglet/turing-sdk": patch
"@viglet/turing-react-sdk": patch
---

The catalog copilot, its kit and nl-filters take `filters` (field to accepted values) that every read of the call stays inside (T1213), for an SN site several sites share: pass it in the request of `postCopilot` and `postCopilotKit`, in the options of the vanilla `postNlFilters`, or as the last argument of the React SDK's `postCopilot` and `postNlFilters`.
