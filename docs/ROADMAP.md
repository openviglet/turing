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

## Block AE — Documentation parity & didactic refresh, post-T410 (§XXIX)

- 📋 **T1280** (deps: —) **integrations/wordpress/index.md is 9.8k words: install tutorial, admin how-tos and WP-CLI reference on one page** — A WordPress admin looking for one setting reads the whole plugin history; the page needs splitting by Diátaxis type. → §CLXXXIV.2
- 📋 **T1281** (deps: —) **semantic-navigation.md is 8.5k words with repeated headings ("What they are", "REST API") and Title Case throughout** — The core SN page is the one most readers land on, and its duplicate anchors make links to a section ambiguous. → §CLXXXIV.2
- 📋 **T1282** (deps: —) **rest-api.md is 7.9k words: endpoint reference interleaved with feature explanations and release-relative prose** — A developer cannot scan it as a reference; the explanations belong on feature pages and the endpoints in one consistent table shape. → §CLXXXIV.2
- 📋 **T1283** (deps: T1279 ✅) **personas.md is 6.8k words overlapping persona-book.md, with internal block labels in its prose** — Two persona pages explain the same objects differently; the rewrite decides which page owns which type and cross-links them. → §CLXXXIV.2
- 📋 **T1284** (deps: —) **94 code fences on 32 docs pages name no language, so Starlight renders them unhighlighted** — The cheapest docs-style debt: a mechanical pass shrinks the baseline before the rewrites, and a reader gets highlighting and a copy label. → §CLXXXIV.3
- 📋 **T1285** (deps: —) **39 docs pages have a frontmatter description outside 70 to 160 characters** — The description is the search-result and link-preview text; too short says nothing, too long gets cut mid-sentence. → §CLXXXIV.3
- 📋 **T1286** (deps: —) **549 docs headings on 57 pages are Title Case instead of sentence case** — Mixed heading case is the most visible inconsistency between pages, and the largest single rule in the docs-style baseline. → §CLXXXIV.3
- 📋 **T1287** (deps: —) **Docs pages carry 25 release-relative phrases, 14 internal T-ids/Block labels, duplicate headings and skipped levels** — "Is now" and "T1233" only make sense to someone who read the changelog; a reader arriving today needs the present state. → §CLXXXIV.3
- 📋 **T1288** (deps: —) **85 of 126 turing.* properties and 4 @ConfigurationProperties prefixes are named on no docs page** — configuration-reference.md implies it is the list of settings, so an operator cannot tell an undocumented knob from one that does not exist. → §CLXXXIV.4
- 📋 **T1289** (deps: —) **The turing match CLI command is not documented on cli.md** — The CLI dispatches it and turing help lists it, but a user reading the docs cannot find what it does or which flags it takes. → §CLXXXIV.4
- 📋 **T1290** (deps: —) **138 of 237 public /api/sn and /api/v2 endpoints are named on no docs page, with no verdict on which are public** — Without a per-family verdict the report's advisory list never shrinks, and a real gap in the public API docs hides among console-only routes. → §CLXXXIV.4
- 📋 **T1291** (deps: —) **The Pages deploy builds the docs but never runs the docs-style lint, so a regression ships if nobody ran it locally** — publish-github-pages.yml runs build:site (links) but not pnpm test:docs, so the docs-style baseline holds only when a session remembers it. → §CLXXXIV.5

## Done when — T1184

- **An evaluator sees the admin console without installing** A read-only guest console
  on the demo host (browse, no save, reset on a schedule) or a recorded walk-through on
  the site, linked from the README.

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
