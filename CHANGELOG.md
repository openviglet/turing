## 2026.3.7

Releases of the 2026.3 line now ship from this repository, openviglet/turing. Earlier 2026.3 builds (up to 2026.3.6) are on openviglet/turing-ce, and the Docker image stays `ghcr.io/openviglet/turing-ce`.

#### SEARCH
* Text search is stemmed by the site's locale on Lucene and on new Elasticsearch cores, so a plural finds its singular.
* Facet filters and custom sorts now apply on Elasticsearch.
* Hybrid sites recall semantic hits the lexical page missed, keep the request's filters on them, and can tune the ranking per site.
* An instant search matches the last word typed as a prefix.
* Applied facets stay in place on drill-down sites and when their field resolves to the OR item type.
* Synonyms on Lucene sites survive a restart, and wildcard queries or half-typed words are no longer mined as synonyms.
* Similar documents honour the search's filters and locale.

#### SEMANTIC NAVIGATION COPILOT AND CHAT
* The copilot stages the catalog products a described plan needs, explains each choice, respects availability, and cites the site's guides beside the kit; both SDKs expose the kit call.
* A follow-up question in SN chat and the copilot is retrieved as a standalone question, and Live Preview shows the query it searched.
* Deleting a document also removes its chunks from the RAG collection (Milvus included), and orphaned chunks can be purged.
* The console flags a site that has no vectors, and Default-agent sites can reindex.
* A site's GenAI and facet behaviour can be turned on from the SN manifest, and a site's AI capabilities can be probed without calling a model.

#### SECURITY AND MULTI-TENANT SITES
* An API key can be bound to a partition of a shared SN site; search, import, autocomplete, spell-check, similar documents and deletes stay inside it.
* Requests authenticated by an API key no longer need a CSRF token, and their audit entries name the key's user.

#### ANALYTICS, COST AND SDK
* Failed searches, content gaps and click-through rates per term are available as REST metrics, per partition.
* The JS and React SDKs ask for consent before sending search analytics or result clicks, send GA4 search events, report search abandonment, and cancel a search a newer one supersedes.
* SN chat, NL filters and summaries are recorded in the cost ledger against their site; a call with no known price is shown as unpriced, not free.

#### FIXES
* Streamed answers keep the space before each token.
* Wrong-type query parameters return 400 naming the parameter.
* Relative dates in natural-language filters resolve against today.
* Solr: currency fields exist in every configset, cores are created through the v1 Core Admin API, and exclusive range filters work.

#### DOCUMENTATION
* The end-user documentation moved into this repository and is published at openviglet.github.io/turing/docs.

## 2026.3

#### NEW FEATURES
* Bento UI: redesigned admin console (frosted-glass layout, inline editing, scroll-linked save bar)
* AI Agent Skills: skill folders executed in a Docker sandbox, usable from chat and Semantic Navigation
* Agent workspace: per-conversation file store with signed URLs, SSE events and a Groovy helper
* Code Interpreter: sandboxed Python execution (native or Docker) with warm pool and structured results
* Governed LLM Gateway: OpenAI-compatible governed egress
* Unified model entity, visual model-type selection and multi-model LLM instances
* Public curated model catalog API with a multi-source regeneration pipeline
* Vectorless (structured-data) RAG plus batch/bulk structured-feed indexing
* Hybrid ranking for Semantic Navigation (BM25 + kNN fused with Reciprocal Rank Fusion)
* Pluggable RAG reranker (LLM, cross-encoder or Cohere)
* Thesaurus: microthesauri and index-time term expansion
* Native synonyms and query expansion
* Synthetic personas: persona match, persona dialogue and synthetic user research projects
* Evaluation platform with pluggable graders and datasets
* Field-manifest provisioning (schema-as-code), LLM-assisted derivation and field-coverage reporting
* Migration tooling from Algolia and Elasticsearch
* HuggingFace embedding provider and provider-aware model discovery
* Scalable, pluggable audio transcription with chunking
* Pluggable URL content fetcher with headless-browser fallback
* Multi-tenancy with tenant auto-provisioning
* Chat webhooks, multi-modal slots, slot-delta streaming and submission retention
* Memory compression and automatic offload of large tool results
* Turing CLI (`@viglet/turing-cli`) and the zero-dependency vanilla JS SDK (`@viglet/turing-sdk`)
* `@viglet/turing-react-ui` headless component package
* Live Preview: per-turn prompt diagnostics
* MCP server support with injected LLM instructions
* Client-side conversion analytics and Google Analytics bridge
* Reference showcase application ("Atlas Store")

#### IMPROVEMENTS
* Open-core source split: public snapshots published to `openviglet/turing-ce`
* Composable, cache-friendly prompt-assembly pipeline
* Bounded hexagonal domain layer with enforced conventions
* JPA caching strategy reworked: cache DTOs, never entities
* Security hardening following an external audit
* Documentation parity refresh across the docs site
* SDK parity between the vanilla and React clients
* GEO / LLM discoverability for published content

## 2026.2

#### NEW FEATURES
* Chat Flow Visual Builder (drag-and-drop conversational flows)
* Agent-centric chat with default agent
* AI Mode chat
* Query DSL API with Solr and Elasticsearch executors
* DSL aggregations, collapse, suggest, rescore and kNN
* ANN / vector search UI and API
* RAG reindexing with filters, parallel reindex and smoothed ETA
* Lucene vector store provider
* Lucene autocomplete, highlighter, sort and numeric doc values
* Marketplace (API + UI) with connector and sample apps
* Customizable dashboard with draggable widgets
* Git server (JGit HTTP backend) and Git repository browser
* Collection import/export (JSONL/ZIP)
* OAuth2 social login (Google, Microsoft, GitHub)
* User self-registration
* SN SPA templates served from storage (`searchTemplate`, SPA manifest, `includeTemplate` export)
* URL-synced search store with composable hooks and grouped search
* `TuringSearchField` and search history hook
* Site icons and result-card thumbnails
* Additional custom facet operators
* Redis logging support and configurable engine
* Property-based Solr mode (`turing.solr.enabled` / `turing.solr.endpoint`)
* React SDK Storybook and documentation
* Turing bootloader UI with animated molecule logo
* Koyeb deploy configuration and GitHub Pages workflow

#### IMPROVEMENTS
* Migrated frontend to pnpm + Turborepo monorepo
* Hazelcast-backed distributed cache (`hazelcast-spring`)
* Refactored LLM/Store provider forms and options
* Improved RAG GenAI flows, logging and AI UX
* Upgraded Viglet Design System
* Dependency upgrades (Vite, React Router, Jedis 7, ESLint)

#### FIXES
* CORS wildcard origin handling now uses `allowedOriginPatterns`
* OAuth2 `ClientRegistrationRepository` bean made optional
* Setup flow handles missing admin user on fresh deploy
* Upgraded Storybook and Vite to patch security vulnerabilities
* Fixed Koyeb build (multi-stage Dockerfile, `git-commit-id` plugin)

## 2026.1

#### BREAKING CHANGES
* GraphQL `siteSearch` now exposes `SearchDocument.fields` as `JSON` scalar, replacing the fixed `SearchDocumentFields` object.

#### IMPROVEMENTS
* GraphQL `siteSearch` dynamic fields are now normalized from persisted site fields (`TurSNSiteFieldExt`), including disabled fields with `null` when absent.

## 0.3.7

#### NEW FEATURES
* Maven
* Ranking Rules
* Unit Tests
* Integration Tests
* AEM Connector
* Keycloak Integration
* ApiKey
* SN: Facets using OR
* SN: fq.op parameter
* SN: Search using asterisk when return no results.
* Create solr core automatically
* Using Artemis

#### IMPROVEMENTS
* Spring Boot 3.2.0
* Angular 17
* Spotlight
* UI Flow
* UI: Order by

## 0.3.6

#### NEW FEATURES
* Store information about user accesses and searches performed.
* Reports - Generates access report, including targeting rules.
* Latest searches - Allows you to show the latest searches performed by the user

#### IMPROVEMENTS
* Java 17

## 0.3.5

#### NEW FEATURES
* Spotlight
* Multi language
* UI: Angular 13

#### IMPROVEMENTS
* Java 14
* Spring Boot 2.5.6

## 0.3.4

#### NEW FEATURES

## 0.3.3

#### NEW FEATURES
* Targeting Rules
* UUID for Primary Key

## 0.3.2

#### NEW FEATURES
* Unit Test
* SpaCy Plugin
* DockerFile
* SN Site: Import
* Export SNSite
* Default Fields into Search
* NLP and Thesaurus Activation
* Dynamic Fields: Text, Description and Date
* SN: MaxRows
* Deindexing by Type

#### IMPROVEMENTS
* Check Box fields on SN were fixed
* Spring Boot 2.1.2
* Using lib instead of modules
* Release Resources: HTTPClient and SolrServer
* SNSite using UUID
* Remove newline and trim to concatenated Text

## 0.3.1

#### NEW FEATURES
* Deindexing API

#### IMPROVEMENTS
* Spring Boot 2.0.5
* Gradle 4.10.1
* Using WebJars