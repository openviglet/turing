/**
 * T1216 — `@viglet/turing-react-sdk/TuringAnswer`: the fused answer surface, on its
 * own subpath because it renders markdown through the optional `react-markdown`
 * peer and the package root must load without it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
export { TuringAnswer } from "./ui/TuringAnswer.js";
export type { TuringAnswerProps, TuringAnswerClassNames, TuringAnswerLabels } from "./ui/TuringAnswer.js";
