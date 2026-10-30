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

- 📋 **T1174** (deps: —) **A copilot kit product's reason lists raw indexed fields, site key and image URL included, as why it fits** — A shopper reads that reason under each product, so plumbing fields there make the kit look broken. → §CLVIII.11
- 📋 **T1175** (deps: —) **The copilot kit stages any document that is not a guide type, a site's pages included, as a product** — A staged page cannot be bought, so the client's add-to-cart fails for it and the kit looks broken. → §CLVIII.12
- 📋 **T1176** (deps: —) **A question typed in the SN search box finds nothing, because every word, question words included, must match** — Shoppers type questions, and zero results also means no answer panel, so the box fails the visitor who asks. → §CLVIII.13
- 📋 **T1177** (deps: —) **After the stemming release a guide titled with the exact query phrase no longer outranks products matching its synonym** — A Dumont demo's curated searches lost their guide-first order, and an exact title phrase is the strongest signal a search has. → §CLVIII.14
- 📋 **T1178** (deps: —) **The SN copilot and chat cannot be told which document a question is about, so a product page gets a catalog-wide answer** — A shopper on a product page asks about that product, and an answer that cites other items misses the question. → §CLVIII.15
- 📋 **T1179** (deps: —) **Search metrics list a visitor's search lower-cased and stripped of its accents, so pt_BR searches read misspelt** — Site owners read and act on these words, and a Portuguese term without its accents is a typo in their own language. → §CLVIII.16

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
