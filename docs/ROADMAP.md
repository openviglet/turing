# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

## Block BW — What the link-free response and its consumers still lack (§LXX)

## Block BV — The quality gate stops at the package boundary

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

- ⏳ **T1158** (deps: —) **Deleting a document leaves its RAG chunks (by id, type or fields; on Milvus always), so site chat still cites it** — Delete by type or fields still leaves chunks, and orphans already stored need a purge. → §CLVIII.2
- 📋 **T1159** (deps: —) **The catalog copilot answers one need at a time; a described plan cannot come back as staged products by need** — A shopper planning a trip does not know every item to ask for, and each consumer would rebuild the gaps, the id check and the staging. → §CLVIII.3
- 📋 **T1166** (deps: —) **With chat memory on, a prompt capture counts memory turns in its index, so a follow-up's searched query lands off-turn** — The Live Preview is where an operator checks what a rewritten follow-up searched, and it must show that on the turn it belongs to. → §CLVIII.4

## Block U — Documentation refresh (`docs-turing`)

- 📋 **T1160** (deps: —) **End-user docs live in a separate Docusaurus repo (vigletdocs) instead of at turing.viglet.org/docs** — Scaffold frontend/apps/docs as Astro + Starlight (roadkeep's site/docs shape), base /docs, built into the site dist by build:site and the Pages deploy. → §CLIX.1
- 📋 **T1161** (deps: T1160) **The 70+ docs-turing pages, images and sidebar order exist only in Docusaurus form** — Port every page with sidebars-turing.ts order, converting admonitions, Tabs, MDX imports, static/img paths and internal links to Starlight, with no page dropped. → §CLIX.2
- 📋 **T1162** (deps: T1160) **Stock Starlight theme does not look like Viglet Turing or the turing.viglet.org site around it** — Theme the docs area from the Viglet Design System tokens: blue-indigo gradient, dark mode, typography, header and footer matching frontend/apps/site. → §CLIX.3
- 📋 **T1163** (deps: T1161) **No map from docs.viglet.org/turing/* URLs to /docs pages, so the planned redirect would 404 old links** — Generate a redirect map from every old Docusaurus URL (current and 0.3.x versions) to its /docs page and keep it in the repo, ready for the docs.viglet.org redirect. → §CLIX.4
- 📋 **T1164** (deps: T1161, T1162) **The ship workflow, COVERAGE.md gate and skills still send end-user doc edits to the vigletdocs repo** — Move COVERAGE.md beside the new docs, repoint turing-roadmap-docs and agents.md to frontend/apps/docs, and freeze docs-turing in vigletdocs as superseded. → §CLIX.5

## Done when — T1133

- **Every SN endpoint takes a partitioned key and keeps it in its partition**
  TurSdkContractIT's two-partition test covers every endpoint, and the guard's ENFORCED
  list matches every SN route.

## Done when — T1132

- **fq[] on /search/duplicates keeps clusters to the documents that match**
  TurSdkContractIT's two-value test also runs on the duplicates endpoint of a hybrid
  site.

## Done when — T1142

- **npm serves postNlFilters and TurNLFilters from @viglet/turing-sdk** After the sdk
  bump and a publish-sdk.yml dispatch, `pnpm check:unreleased` reads 0 for turing-sdk
  and turing-react-sdk.
- **One release record: changesets consumed by the publish or retired**
  frontend/.changeset either feeds the publish workflows' version and notes, or is
  deleted along with its README and scripts.

## Done when — T1158

- **A delete by type or by fields leaves no RAG chunk of the matching documents** A test
  per store provider indexes documents of two types, deletes one type and one field set
  through the queue, and finds no chunk of them left, with another site's chunks intact.
- **A purge removes stored chunks whose document the engine no longer holds** Run
  against a collection holding chunks of a deleted document and of a live one, the purge
  removes only the deleted document's chunks.

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
