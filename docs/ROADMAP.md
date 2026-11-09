# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BV — The quality gate stops at the package boundary

## Block CJ — Releases ship from the repository the README names

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

## Block CL — SN sites for research and memory archives

## Block CF — What the SN site manifest cannot declare

## Block CT — The WordPress plugin (Viglet Turing Search)

- 📋 **T1260** (deps: T1253 ✅, T1255 ✅, WordPress.org: first review approved and SVN access granted) **A release tag publishes the zip on GitHub only, so the plugin cannot be found or updated from wp-admin** — Sites update from the directory; a manual SVN commit per release gets skipped (ex-Dumont D227). → §CLXXXII.6
- 📋 **T1274** (deps: —) **The WordPress plugin still shows the bare name in its block title and setup labels** — Rename the visible plugin strings to Viglet Turing so the admin UI and the docs that quote it follow the hub naming rule. → §CLXXXII.15

## Block AB — Public site & top-of-funnel conversion (`turing.viglet.org`, §XXVII)

- 📋 **T1270** (deps: T1268 ✅) **Getting started and resource links do not express the same journey as the hub** — Give evaluation, installation, documentation and release history explicit destinations and a reliable route back to the portfolio. → §XXVII.7
- 📋 **T1271** (deps: T1268 ✅) **The home page repeats technical detail before visitors can choose their next step** — Shorten the evaluation path and move depth into existing pages without removing evidence, stable URLs or useful technical documentation. → §XXVII.8
- 📋 **T1272** (deps: —) **The home presents broad provider and delivery-time claims without bounded evidence** — Make supported configurations and outcomes checkable so the product site follows the hub rule against invented or universal claims. → §XXVII.9
- 📋 **T1273** (deps: T1268 ✅, T1269 ✅, T1270, T1271, T1272) **No acceptance check compares the product site with the revised hub contract** — Validate the rendered brand and journeys across routes, themes and small screens so future site changes cannot silently undo the alignment. → §XXVII.10

## Done when — T1184

- **An evaluator sees the admin console without installing** A read-only guest console
  on the demo host (browse, no save, reset on a schedule) or a recorded walk-through on
  the site, linked from the README.

## Done when — T1270

- **Evidence and boundary are verified** Re-read the cited source and rendered routes,
  satisfy the acceptance checks in the design, and preserve technical identifiers and
  existing URLs.
- **The hub contract survives the change** Review full names, relevant light/dark and
  mobile layouts, and links to installation, documentation and the portfolio; retain the
  product-specific behavior.

## Done when — T1271

- **Evidence and boundary are verified** Re-read the cited source and rendered routes,
  satisfy the acceptance checks in the design, and preserve technical identifiers and
  existing URLs.
- **The hub contract survives the change** Review full names, relevant light/dark and
  mobile layouts, and links to installation, documentation and the portfolio; retain the
  product-specific behavior.

## Done when — T1272

- **Evidence and boundary are verified** Re-read the cited source and rendered routes,
  satisfy the acceptance checks in the design, and preserve technical identifiers and
  existing URLs.
- **The hub contract survives the change** Review full names, relevant light/dark and
  mobile layouts, and links to installation, documentation and the portfolio; retain the
  product-specific behavior.

## Done when — T1273

- **Evidence and boundary are verified** Re-read the cited source and rendered routes,
  satisfy the acceptance checks in the design, and preserve technical identifiers and
  existing URLs.
- **The hub contract survives the change** Review full names, relevant light/dark and
  mobile layouts, and links to installation, documentation and the portfolio; retain the
  product-specific behavior.

## Non-goals (do NOT add as tasks)

Binding constraints — see [IMPROVEMENTS.md](IMPROVEMENTS.md) §I.6 (What NOT to
refactor) and §I.7 (Restrictions) for the full text. Summary:

- Don't refactor the auto-trigger router, continuation logic, A/B assignment,
  sub-flow descent/ascent, SSE bus, defense-in-depth checks, silent telemetry,
  anti-injection / CPF-CNPJ / yes-no normalization, `TurToolCallbackPipeline.decorate`,
  or `summarizeVariables` cap — these are product, not patches.
- Structured output (LLM → JSON) is **not** a path — use provider-side tool-calling schemas.
- Don't rewrite the chat-flow DSL (`ChatFlowGraph` / `ChatFlowNode` shape) — only execution.
- Keep Spring AI `ChatModel.call()` + `internalToolExecutionEnabled(true)` (no framework swap).
- Heuristic and LlmJudge strategies coexist.
- No "auto-detect" middleware guessing node intent (toolFree, captureMode) — that's authorship.
- No in-process ONNX/DJL Whisper transcription runtime (the retired `LOCAL_ONNX` backend, ex-T694). The supported fully-local path is `OPENAI_COMPATIBLE` pointed at a self-hosted server (e.g. faster-whisper-server) — the platform won't bundle a fragile in-process Whisper dependency (T193 Jlama discipline).
