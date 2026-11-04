---
"@viglet/turing-react-sdk": patch
---

The `@viglet/turing-react-sdk` root no longer needs the optional `react-markdown` peer (T1216), so a project that uses only the hooks need not install it. `TuringMarkdown` and `TuringAnswer` moved off the root: import them from `@viglet/turing-react-sdk/TuringMarkdown` and `@viglet/turing-react-sdk/TuringAnswer`. Their prop types stay on the root. The package now declares an `exports` map with those two subpaths.
