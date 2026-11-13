# Set aside

## Block CD — turing.viglet.org relevance pass (2026-10)

- ⏸ **T1093** (deps: —) **The AEM page ranks 36th to 44th for the AEM queries, and almost nothing links to it** — set aside (Waits for weeks of Search Console data post-recrawl.): Measure the four AEM queries for /aem-search/ in Search Console against the 2026-10 baseline once Google has recrawled the page. → §CXV.1
- ⏸ **T1095** (deps: —) **Compare, migrate, persona and the four features pages had no impression in 90 days** — set aside (Needs a month of Search Console data.): Re-read Search Console for the seven pages a month after the deploy and merge any feature page still without impressions. → §CXV.3

## Block BV — The quality gate stops at the package boundary

## Block CJ — Releases ship from the repository the README names

- ⏸ **T1184** (deps: —) **An evaluator cannot see the admin console without installing, and the README never links the live playground** — set aside (Needs access to turing-demo.viglet.org.): Turn turing.guest-console on at the demo host. → §CLXI.5

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

## Block CL — SN sites for research and memory archives

- ⏸ **T1221** (deps: —) **Turing was never read end to end as an education and memory site uses it: pt_BR, OCR, images, audio, flows** — set aside (Needs the client's lab and cloud tenant.): A school client uses nearly every visitor feature at once; one analysis finds its gaps cheaper than production. → §CLXVII.2

## Block CF — What the SN site manifest cannot declare

- ⏸ **T1223** (deps: —) **The manifest cannot declare a field's filter values as alternatives: two fq values of site_key intersect to nothing** — set aside (Needs a viglet-core release.): A manifest field cannot yet declare facetItemType: VigletFieldSpec in viglet-core needs the field and a release first. → §CLXIX.1

## Block CT — The WordPress plugin (Viglet Turing Search)

- ⏸ **T1255** (deps: T1252 ✅) **No workflow in this repository checks, tests or releases the WordPress plugin** — set aside (Needs a push and one dispatch on GitHub.): Dispatch wordpress-plugin.yml once on GitHub with every job on, and fix what a real runner finds. → §CLXXXII.4
- ⏸ **T1256** (deps: T1252 ✅) **The Playground demo store loads its blueprint and zips from dumont.viglet.org/playground** — set aside (Needs a push and a Pages deploy.): Deploy Pages once, check /wp-demo boots the store and searches, then remove Dumont's copy. → §CLXXXII.5
- ⏸ **T1257** (deps: T1251 ✅, T1254 ✅, T1255 ✅) **The plugin video is not on the plugin docs, the README or the release, and no page carries its transcript** — set aside (Needs GitHub's web editor.): Left: the README player, via GitHub's editor, and the videos and captions on the release. → §CLXXXII.7
- ⏸ **T1259** (deps: T1252 ✅) **The plugin's Site assistant renders only text and citations, so chat flow choices, forms and slots cannot run in it** — set aside (Needs a chat flow in the demo's Turing.): A Playwright flow case against the demo store, once its Turing has a chat flow. → §CLXXXII.9
- ⏸ **T1261** (deps: T1252 ✅) **The plugin's search answer lists every retrieved document as a source when it cites none** — set aside (Needs the e2e stack and the video pipeline.): An e2e case for an answer with no [n] marker, then record the video's chapter 2 again. → §CLXXXII.10
- ⏸ **T1265** (deps: —) **An answer with no [n] marker still sends every retrieved source as its citations** — set aside (Needs real answers from the demo's model.): Find why the demo's tent question is answered with no [n] marker, and make the prompt ask for one. → §CLXXXII.12

## Block CU — Copilot answers over a structured feed: a correct plan must find what the index holds

## Block CW — Chat retrieval recalls what the site's search finds

