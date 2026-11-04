---
"@viglet/turing-react-ui": patch
"@viglet/turing-react-sdk": patch
---

The `@viglet/turing-react-ui` root no longer needs the optional `react-markdown` peer (T1215): a project with only `react` and `react-dom` can import it. `TuringMarkdown` is no longer exported from the root; import it from `@viglet/turing-react-ui/TuringMarkdown` (or from `@viglet/turing-react-sdk`, which still re-exports it). `resolveSandboxUrl` and the `TuringMarkdownProps` type stay on the root.
