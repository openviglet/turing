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

- 🛠 **T1245** (deps: —) **An API key created under Keycloak authenticates with no user and no authorities, so admin endpoints refuse it** — The key's creator has no local user row under Keycloak, and a Key-only request carries no realm roles (GitHub #81). → §XXXVII.21

## Block CL — SN sites for research and memory archives

## Block CF — What the SN site manifest cannot declare

## Block CN — Autocomplete and suggestions

- 📋 **T1246** (deps: —) **Autocomplete never suggests the site's most searched terms, though the search log already counts them** — A popular query a visitor starts typing is not offered unless the documents spell it (GitHub #66). → §CLXXI.2

## Block BH — SN result & facet rendering robustness for feed/crawl-indexed sites (§LVI)

- 📋 **T1249** (deps: —) **Clearing the facets also clears the secondary facets, so the visitor loses the tab they are on** — The clean-up link and clearAllFilters drop every facet filter, secondary ones included (GitHub #120). → §CLXXX.2
- 📋 **T1250** (deps: —) **A search request cannot leave out a facet the site enables; a page hides it client-side after it is computed** — One page of a site cannot drop a facet the rest of the site shows without code in the client (GitHub #103). → §CLXXX.3

## Block CG — Search-engine plugin parity (Solr, Lucene, Elasticsearch)

- 📋 **T1247** (deps: —) **A Solr instance is one HTTP endpoint: no ZooKeeper, no node list with failover, no separate query and update nodes** — Customers running SolrCloud or several nodes cannot point Turing at their topology, and one node down stops search (GitHub #95). → §CLXXV.5

## Block AG — Bento UI: complete the interface migration (§XXXI)

- 📋 **T1248** (deps: —) **The faceted-field form shows the date Range select for every field type, not only DATE** — A range set on a text or string field does nothing useful and leaves a stale setting behind (GitHub #72). → §XXXI.15

## Block AB — Public site & top-of-funnel conversion (`turing.viglet.org`, §XXVII)

- 📋 **T1251** (deps: —) **turing.viglet.org/wordpress, the address the plugin video ends on, only redirects to Dumont** — The plugin talks to Turing directly, so its page, video and demo belong on Turing's site, with a link to Dumont for other sources. → §CLXXXI.1

## Block CT — The WordPress plugin (Viglet Turing Search)

- 📋 **T1252** (deps: —) **The WordPress plugin's code, tests, demos and video pipeline live in the Dumont repository** — Every call it makes goes to Turing, so a Turing change can break it while CI runs against a published image in another repo. → §CLXXXII.1
- 📋 **T1253** (deps: T1252) **The plugin's PHP classes, constants and stored option keys are still named Viglet_Dumont_*** — Before the first wordpress.org release a rename is free; after it, every installed site needs an option migration. → §CLXXXII.2
- 📋 **T1254** (deps: T1252) **The plugin's end-user docs are pages on Dumont's docs site, not on turing.viglet.org/docs** — The install link on the WordPress page should open the docs of the product that is being installed. → §CLXXXII.3
- 📋 **T1255** (deps: T1252) **No workflow in this repository checks, tests or releases the WordPress plugin** — Dumont's wordpress-plugin.yml fires on push and tag; here every workflow is manual-dispatch on purpose (T928, T929). → §CLXXXII.4
- 📋 **T1256** (deps: T1252) **The Playground demo store loads its blueprint and zips from dumont.viglet.org/playground** — The try-it button on Turing's WordPress page should not depend on another product's site being built and deployed. → §CLXXXII.5
- 📋 **T1257** (deps: T1251, T1254, T1255) **The plugin video is not on the plugin docs, the README or the release, and no page carries its transcript** — Assistants and search read text, not video, and a release asset downloads instead of streaming (rest of Dumont D282). → §CLXXXII.7
- 📋 **T1258** (deps: T1252) **The plugin's search answer and Site assistant do not share context, so a follow-up starts over** — A visitor who asks one more thing after the answer loses the question, the answer and its sources (moved from Dumont D312). → §CLXXXII.8
- 📋 **T1259** (deps: T1252) **The plugin's Site assistant renders only text and citations, so chat flow choices, forms and slots cannot run in it** — Guided conversations built in Turing's console never reach a WordPress site (moved from Dumont D313). → §CLXXXII.9
- 📋 **T1260** (deps: T1253, T1255, WordPress.org: first review approved and SVN access granted) **A release tag publishes the zip on GitHub only, so the plugin cannot be found or updated from wp-admin** — Sites update from the directory, and a manual SVN commit per release gets skipped (ex-Dumont D227). → §CLXXXII.6
- 📋 **T1261** (deps: T1252) **The plugin's search answer lists every retrieved document as a source when it cites none** — The block falls back from citations to retrieval, so the published video shows a running shirt under a tent answer while Ana says each source can be checked. → §CLXXXII.10

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
