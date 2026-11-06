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

## Block AN — Security hardening (post-audit, §XXXVII)

- 📋 **T1245** (deps: —) **An API key created under Keycloak authenticates with no user and no authorities, so admin endpoints refuse it** — The key's creator has no local user row under Keycloak, and a Key-only request carries no realm roles (GitHub #81). → §XXXVII.21

## Block CL — SN sites for research and memory archives

## Block CF — What the SN site manifest cannot declare

## Block CN — Autocomplete and suggestions

- 📋 **T1246** (deps: —) **Autocomplete never suggests the site's most searched terms, though the search log already counts them** — A popular query a visitor starts typing is not offered unless the documents spell it (GitHub #66). → §CLXXI.2

## Block D.6 — Analytics & observability gaps

- 📋 **T1244** (deps: —) **A search longer than 255 characters fails to save its metric row, and the search answers 500** — Recording a search can fail the search itself, so a pasted paragraph or a long question gets an error (GitHub #112). → §CLXXX.1

## Block BH — SN result & facet rendering robustness for feed/crawl-indexed sites (§LVI)

- 📋 **T1249** (deps: —) **Clearing the facets also clears the secondary facets, so the visitor loses the tab they are on** — The clean-up link and clearAllFilters drop every facet filter, secondary ones included (GitHub #120). → §CLXXX.2
- 📋 **T1250** (deps: —) **A search request cannot leave out a facet the site enables; a page hides it client-side after it is computed** — One page of a site cannot drop a facet the rest of the site shows without code in the client (GitHub #103). → §CLXXX.3

## Block CG — Search-engine plugin parity (Solr, Lucene, Elasticsearch)

- 📋 **T1247** (deps: —) **A Solr instance is one HTTP endpoint: no ZooKeeper, no node list with failover, no separate query and update nodes** — Customers running SolrCloud or several nodes cannot point Turing at their topology, and one node down stops search (GitHub #95). → §CLXXV.5

## Block AG — Bento UI: complete the interface migration (§XXXI)

- 📋 **T1248** (deps: —) **The faceted-field form shows the date Range select for every field type, not only DATE** — A range set on a text or string field does nothing useful and leaves a stale setting behind (GitHub #72). → §XXXI.15

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
