---
name: turing-domain-layer
description: The Turing hexagonal domain layer (each module's <module>.domain package) — immutable annotation-free *Domain records + *RepositoryPort interfaces with adapters/MapStruct mappers under the module's persistence.adapter package. It is bounded by design and stays partial — do NOT complete it for symmetry. Explains when a read earns a *Domain record (hot/cached/cross-aggregate/lazy-escape paths), port vs mapper-backed reader (T546), that ports are read-only forever, and the TurDomainLayerConventionsTest guardrails. Use whenever adding/modifying a *Domain record, a *RepositoryPort, an adapter/mapper, or deciding whether a read should use the domain layer or the JPA repository directly.
---

# Domain layer (ports & adapters): bounded by design — do NOT go 100%

There is a partial hexagonal layer in each module that owns the data: immutable
annotation-free `*Domain` records + `*RepositoryPort` interfaces in
`com.viglet.turing.<module>.domain` (`sn.domain`, `agent.domain`, `llm.domain`,
`llm.mcp.domain`), adapters + MapStruct mappers in the same module's
`persistence.adapter`. Since ADR 0005 (section 3, T1313) there is no top-level `domain`
package; a module's ports are its named interface for reads, which other modules call.
**It is deliberately incomplete and stays that way.** Full rationale + the ADR:
[`docs/adr/0001-domain-layer-bounded-completion.md`](docs/adr/0001-domain-layer-bounded-completion.md).
The rule, in one screen:

- **Use a `*Domain` record** only when the read is on a **hot path**
  (search / chat / RAG / autocomplete), a **cached value**, a **cross-aggregate
  read** (would walk `@ManyToOne`/`@OneToOne` across aggregates — the EAGER
  200ms→5s hazard), or a **LazyInit-prone read** (returned/cached/passed outside
  its session). The record references neighbours **by ID**, never by object.
- **Port vs mapper-backed reader (T546):** wrap the record read in a
  `*RepositoryPort` interface only when it will be **mocked in a unit test** or
  gain a **second implementation**. Otherwise skip the interface and expose a
  concrete `*Reader` `@Component` that maps entity → record directly (see
  `TurCustomToolReader`). Today: **7 ports + 10 records + 1 reader**. Don't add a
  port "for symmetry" — a port with no mock is pure ceremony over its lone
  adapter.
- **Use the JPA repository directly** (with `@Transactional` + `LEFT JOIN FETCH`
  for the to-one graph, per T488 — not `@EntityGraph`, never a repo
  `@Cacheable`) for **admin/CRUD** reads and **all writes**.
- **Ports are read-only by design, permanently.** They will not grow `save` /
  `delete` — writes gain nothing from an immutable record and everything from a
  managed, dirty-checked entity. (Ignore any legacy "future slices will
  introduce save/delete" Javadoc; that promise was reversed by T544.)
- **The single criterion for a new aggregate:** is it read on a
  hot/cached/cross-aggregate path, or where a lazy proxy would escape its
  session? Yes → it earns a record. No → it stays on the JPA repo. Partial
  coverage of the entity space is the intended end state, **not** a debt — there
  is no "complete the set for consistency" obligation.
- The mapping cost is a non-issue: `entity → MapStruct → record` measures
  ≈48–53 ns/op (T540), immeasurable against a millisecond search request.
- `TurDomainLayerConventionsTest` (T545, in `modulith.domain`) enforces this: records
  carry no JPA/Hibernate/Jackson annotations, a `domain` package imports no JPA
  repository or adapter, the named hot-path consumers stay on their ports, and every
  surviving port has exactly one adapter + mapper. These are rules **inside** a module, so
  the module verifier (`TurModuleReportTest`, which checks only crossings between modules)
  does not replace them. (A collapsed
  reader has no port, so it's simply outside the port walk — T546.)
