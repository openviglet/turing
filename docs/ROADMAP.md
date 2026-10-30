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

- 📋 **T1185** (deps: —) **On Solr and Elasticsearch a title holding the query as an exact phrase still ranks below synonym-only matches** — T1177 fixed the guide-first order on Lucene only, so a site on the other engines keeps the regression. → §CLVIII.17

## Block CJ — Releases ship from the repository the README names

- 📋 **T1181** (deps: —) **The Linux download is still named Turing-ES-x86_64.AppImage, with the acronym the product name dropped** — The download is the last place a visitor meets the old name, and it reads as another or older product. → §CLXI.2
- 📋 **T1182** (deps: —) **A patch release's notes are only the Dependabot pull request list, saying nothing a user would notice** — The release page is where an evaluator checks what changed, and the notes it shows say nothing about the product. → §CLXI.3
- 📋 **T1183** (deps: T1180 ✅) **turing-ce still reads as the live repository, with the old ES name and addresses and no notice pointing elsewhere** — Two repositories for one product split the stars and send visitors to the stale one. → §CLXI.4
- 📋 **T1184** (deps: —) **An evaluator cannot see the admin console without installing, and the README never links the live playground** — A demo that opens without installing is what every launch channel is missing, and the playground already exists. → §CLXI.5

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
