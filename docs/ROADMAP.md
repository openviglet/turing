# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

- 🛠 **T1133** (deps: —) **An API key reads and deletes every document of a shared SN site, as nothing binds a key to its own partition** — Duplicates, nl-filters and copilot still refuse the key, and PgVector, Milvus and Chroma have no partition-filter IT. → §CXLV

## Block BW — What the link-free response and its consumers still lack (§LXX)

- 📋 **T1132** (deps: T1133 🛠) **The similar-documents search takes no filter, so a shared SN site answers with every site's documents** — The duplicates search takes no filter: vector points carry no field to filter on. → §CXLIV

## Block BV — The quality gate stops at the package boundary

- 🛠 **T1142** (deps: —) **The npm @viglet/turing-sdk lags its source: 2026.3.9 has no postNlFilters, so consumers hand-write the request** — Dispatch publish-sdk.yml and publish-js-sdk.yml by gh once origin carries the changeset step (T1142b), so npm serves postNlFilters. → §CLIII.2

## Block CJ — Releases ship from the repository the README names

- 📋 **T1180** (deps: —) **The 2026.3 line is released only on turing-ce, so openviglet/turing shows the older v2026.2.9 as its Latest release** — No 2026.3 release exists there yet: publish run 41103220225 failed. → §CLXI.1
- 📋 **T1184** (deps: —) **An evaluator cannot see the admin console without installing, and the README never links the live playground** — The console is still unseen without installing; the owner chose a live guest console that browses but never saves. → §CLXI.5

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

- 📋 **T1194** (deps: —) **An accented word combined with another finds fewer documents than either spelling alone, in pt_BR** — jaqueta impermeável misses the Borrowdale jacket that jaqueta impermeavel and impermeável alone both find, so Brazilian shoppers lose results. → §CLXII.4
- 📋 **T1195** (deps: —) **An SN site's search metrics cannot be cleared, so probes and tests fill a demo's insights for good** — GET search, ac and answer must honour populateMetrics=false from an authenticated caller only, so probes stay out of insights. → §CLXII.5

## Block AN — Security hardening (post-audit, §XXXVII)

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

## Done when — T1180

- **A 2026.3 release is Latest on openviglet/turing** gh release list -R
  openviglet/turing shows a v2026.3.x tag marked Latest, with the jar and install assets
  attached.

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
