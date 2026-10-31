# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

- 📋 **T1192** (deps: —) **An import bundle whose site names its agent loses that link, and the site falls back to the default agent** — The demo's personas then 404 on content-fit whenever the global default is not the bundle's agent. → §CXV.8
- 📋 **T1193** (deps: —) **The site's playground pins three persona ids that the demo backend may not offer** — A demo whose catalog drifts from the pinned list breaks content-fit with a 404, while the site lists personas that do not exist. → §CXV.9

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

## Block BW — What the link-free response and its consumers still lack (§LXX)

## Block BV — The quality gate stops at the package boundary

## Block CJ — Releases ship from the repository the README names

- 🛠 **T1189** (deps: —) **/answer reads only locale, and a locale the site has no index for answers from the global collection** — A caller with the search spelling, or a locale like en on an en_US site, gets an apology and no sources from another corpus, with no error. → §CLXIII.2

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

- 📋 **T1190** (deps: —) **A kit stage leads with the dearest product and a winter bag for a July first-timer's plan** — A store ticks each stage's first product, so the kit costs 1,455 dollars and contradicts the season the plan named. → §CLXII.2
- 📋 **T1191** (deps: —) **A kit product's reason quotes Site: localhost, a bookkeeping field, to the shopper** — Every staged product shows a debug-looking line in a shopper-facing kit. → §CLXII.3
- 📋 **T1194** (deps: —) **An accented word combined with another finds fewer documents than either spelling alone, in pt_BR** — jaqueta impermeável misses the Borrowdale jacket that jaqueta impermeavel and impermeável alone both find, so Brazilian shoppers lose results. → §CLXII.4

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
