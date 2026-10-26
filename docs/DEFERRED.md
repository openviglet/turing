# Set aside

## Block CD — turing.viglet.org relevance pass (2026-10)

- ⏸ **T1093** (deps: —) **The AEM page ranks 36th to 44th for the AEM queries, and almost nothing links to it** — set aside (Waits for weeks of Search Console data post-recrawl.): Measure the four AEM queries for /aem-search/ in Search Console against the 2026-10 baseline once Google has recrawled the page. → §CXV.1
- ⏸ **T1095** (deps: —) **Compare, migrate, persona and the four features pages had no impression in 90 days** — set aside (Needs a month of Search Console data.): Re-read Search Console for the seven pages a month after the deploy and merge any feature page still without impressions. → §CXV.3

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

- ⏸ **T1133** (deps: —) **An API key reads and deletes every document of a shared SN site, as nothing binds a key to its own partition** — set aside (A re-embedding decision, and PgVector, Milvus and Chroma to test on.): Duplicates, chat, answer, nl-filters and copilot need the partition in the vector store. → §CXLV

## Block BW — What the link-free response and its consumers still lack (§LXX)

- ⏸ **T1132** (deps: T1133 ✅) **The similar-documents search takes no filter, so a shared SN site answers with every site's documents** — set aside (Needs filterable fields on vector points, as T1133 does.): The duplicates search takes no filter: vector points carry no field to filter on. → §CXLIV

## Block CF — What the SN site manifest cannot declare

