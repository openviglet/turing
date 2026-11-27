# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

- 📋 **T1376** (deps: —) **The site routes only English paths, so no page can be served in Portuguese under /br/** — Every translated page needs one routing rule shared by the prerender and the client, with the locale read from the path. → §CXV.10
- 📋 **T1377** (deps: T1376) **Page copy is English literals in site-content.ts and in JSX, so a translation has no shape to fill** — A typed dictionary per locale, with English as the type, turns a missing Portuguese string into a compile error instead of an English sentence on a /br/ page. → §CXV.11
- 📋 **T1378** (deps: T1377) **About 200 English sentences are written directly in the page and section components, out of reach of any dictionary** — Solution pages and demo sections keep their copy inline; translating them starts by moving it. → §CXV.12
- 📋 **T1379** (deps: T1376) **Nothing tells search engines that an English page and its /br/ twin are one page in two languages** — Without reciprocal hreflang, Google may treat them as duplicates or show English to Brazilian searches. → §CXV.13
- 📋 **T1380** (deps: T1376) **A visitor reading one language has no way to switch to the other on the same page** — A visitor on the wrong language needs one click to the same page in the other. → §CXV.14
- 📋 **T1381** (deps: T1377, T1379, T1380) **/br/ has no Portuguese home or feature pages, so a Brazilian visitor meets the site in English** — The home and feature pages are where most visitors arrive and decide. → §CXV.15
- 📋 **T1382** (deps: T1378, T1381) **The solution pages exist only in English, so a /br/ visitor following the menu falls back to it** — They are where a buyer judges fit, and each needs its Portuguese twin. → §CXV.16
- 📋 **T1383** (deps: T1376, T1379) **The education page lives at /educacao/, outside the /br/ tree every other Portuguese page will share** — One Portuguese tree keeps switcher and hreflang rules uniform; published links must keep working. → §CXV.17
- 📋 **T1384** (deps: T1381) **Link previews of a /br/ page show the English share image and title card** — A Portuguese page shared on WhatsApp or LinkedIn should preview in Portuguese. → §CXV.18
- 📋 **T1385** (deps: T1381) **The /br/ playground suggests English queries and its search box speaks English** — The corpus stays the English docs, but the invitation and the UI should be Portuguese. → §CXV.19
- 📋 **T1386** (deps: T1377, T1379) **Nothing stops a new page or string from shipping in English only** — A bilingual site drifts the first time a page ships in one language; the build must say so. → §CXV.20

## Block BV — The quality gate stops at the package boundary

## Block CJ — Releases ship from the repository the README names

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

## Block CL — SN sites for research and memory archives

## Block CF — What the SN site manifest cannot declare

## Block CT — The WordPress plugin (Viglet Turing Search)

- 📋 **T1260** (deps: T1253 ✅, T1255 ✅, WordPress.org: first review approved and SVN access granted) **A release tag publishes the zip on GitHub only, so the plugin cannot be found or updated from wp-admin** — Sites update from the directory; a manual SVN commit per release gets skipped (ex-Dumont D227). → §CLXXXII.6
- 📋 **T1374** (deps: —) **The search button says Busca in pt_BR, a noun, and the block has no attribute to set its text** — A button reads as an action, and the one translation shared with the column and panel makes it read as a heading. → §CLXXXII.31
- 📋 **T1375** (deps: —) **The search block has no AI mode: asking the site opens a modal, never turns the results page into a chat** — A conversation with sources beside the answer, follow-ups and its own URL is how modern search answers, and a modal cannot hold it. → §CLXXXII.32
- 📋 **T1387** (deps: —) **A search box that sends its query to the search page has no form action or scope field, so it fails without JavaScript** — A scoped section box falls back to its own page with no results and drops the scope when the script is off or still loading. → §CLXXXII.33
- 📋 **T1388** (deps: —) **A search the site starts itself, like a 404's, cannot say where it came from, so Insights mixes it with typed ones** — A 404 searching the broken URL is the list of missing redirects, and without an origin it cannot be read apart. → §CLXXXII.34

## Block CY — Filters from a sentence say what visitors say

## Done when — T1184

- **An evaluator sees the admin console without installing** A read-only guest console
  on the demo host (browse, no save, reset on a schedule) or a recorded walk-through on
  the site, linked from the README.

## Done when — T1370

- **The answer's metadata names what each retrieval pass returned** An /answer response
  carries the vector documents, lexical ids and lexical chunks counts, so a report names
  the failing pass; a unit test asserts them for a vector-thin question.
- **An IT finds a long page as the source of a question about one sentence** An IT
  indexes a 5,500-character page and asks about one sentence deep in it; the answer
  lists that page as a source.

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
