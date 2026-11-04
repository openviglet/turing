/**
 * T1216 — `@viglet/turing-react-sdk/TuringMarkdown`: the markdown renderer, on its
 * own subpath because it imports the optional `react-markdown` peer statically and
 * the package root must load without it. The same component as
 * `@viglet/turing-react-ui/TuringMarkdown`, offered here so an SDK consumer keeps
 * one import source.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
export { TuringMarkdown, resolveSandboxUrl } from "@viglet/turing-react-ui/TuringMarkdown";
export type { TuringMarkdownProps } from "@viglet/turing-react-ui/TuringMarkdown";
