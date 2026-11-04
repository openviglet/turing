# Set aside

## Block CD — turing.viglet.org relevance pass (2026-10)

- ⏸ **T1093** (deps: —) **The AEM page ranks 36th to 44th for the AEM queries, and almost nothing links to it** — set aside (Waits for weeks of Search Console data post-recrawl.): Measure the four AEM queries for /aem-search/ in Search Console against the 2026-10 baseline once Google has recrawled the page. → §CXV.1
- ⏸ **T1095** (deps: —) **Compare, migrate, persona and the four features pages had no impression in 90 days** — set aside (Needs a month of Search Console data.): Re-read Search Console for the seven pages a month after the deploy and merge any feature page still without impressions. → §CXV.3

## Block BV — The quality gate stops at the package boundary

- ⏸ **T1142** (deps: —) **The npm @viglet/turing-sdk lags its source: 2026.3.9 has no postNlFilters, so consumers hand-write the request** — set aside (Each npm trusted publisher must name publish-npm.yml.): Dispatch publish-npm.yml with react-sdk by gh. → §CLIII.2

## Block CJ — Releases ship from the repository the README names

- ⏸ **T1184** (deps: —) **An evaluator cannot see the admin console without installing, and the README never links the live playground** — set aside (Needs access to turing-demo.viglet.org.): Turn turing.guest-console on at the demo host. → §CLXI.5

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

- ⏸ **T1194** (deps: —) **An accented word combined with another finds fewer documents than either spelling alone, in pt_BR** — set aside (No pt_BR site on the demo server reproduces it.): jaqueta impermeável missed the Borrowdale jacket that each spelling alone found, on a pt_BR demo site. → §CLXII.4

## Block AN — Security hardening (post-audit, §XXXVII)

