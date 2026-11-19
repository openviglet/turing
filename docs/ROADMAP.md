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

- 📋 **T1296** (deps: —) **The SN manifest never converges facet, facetName, description or mandatory on a field that exists** — A mapping turned from facet on to off, or relabelled, reports provisioned while visitors keep the old facet until someone fixes it in the console. → §CLXIX.4

## Block CT — The WordPress plugin (Viglet Turing Search)

- 📋 **T1260** (deps: T1253 ✅, T1255 ✅, WordPress.org: first review approved and SVN access granted) **A release tag publishes the zip on GitHub only, so the plugin cannot be found or updated from wp-admin** — Sites update from the directory; a manual SVN commit per release gets skipped (ex-Dumont D227). → §CLXXXII.6
- 📋 **T1298** (deps: —) **Search insights list searches holding an e-mail, CPF or student number verbatim, and Turing stores them** — A site cannot tell visitors its search log is anonymous, and every editor opening Search insights sees students' personal numbers. → §CLXXXII.18
- 📋 **T1301** (deps: —) **The answer's context is not numbered and no prompt asks for [n], so answers carry no footnotes on any provider** — A visitor either gets an answer with no source or one citing unrelated pages, which is the trust a site's AI answer cannot lose. → §CLXXXII.19
- 📋 **T1303** (deps: —) **A card shows an event whose local time lands on midnight UTC as a date with no time, on the wrong day** — A 21h event on a UTC-3 site reads as an all-day event on the next day in the cards and the instant search. → §CLXXXII.20

## Block CW — Chat retrieval recalls what the site's search finds

- 📋 **T1302** (deps: —) **The single-turn SN chat and the non-streaming conversation still retrieve vector-only, without the site's lexical hits** — GET /chat?q= keeps the T1294 gap: one off-topic page and a refusal where the site's own search finds the answer. → §CLXXXVI.2

## Block CX — Synonyms that mix single words and phrases

- 📋 **T1297** (deps: —) **On Lucene, a synonym group of one word and a phrase blows the word's query past the clause limit, so it finds nothing** — A site that adds its acronym as a synonym of its full name loses every result for the acronym, the word visitors type most. → §CLXXXVII.1

## Block CY — Filters from a sentence say what visitors say

- 📋 **T1299** (deps: —) **Filters from a sentence drop a bare month and every future phrase: "em novembro" and "semana que vem" filter nothing** — On an events calendar the date is the word the visitor cared about, and the search silently ignores it. → §CLXXXVIII.1
- 📋 **T1300** (deps: —) **Filters from a sentence drop a value that is part of a facet value: "Casarão" never matches "Casarão FESPSP"** — Visitors say the short name and editors file the long one, so the filter the model found right is thrown away. → §CLXXXVIII.2

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
