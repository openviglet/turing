# Viglet Dumont for WordPress

The WordPress plugin that indexes a site's content into Viglet Turing and answers its search
from it. The plugin itself is [`src/`](src/); [`src/readme.txt`](src/readme.txt) is its
WordPress readme and changelog.

## Install

Each release is a zip on the Dumont GitHub releases page, tagged `wordpress-v<version>`, holding
one `viglet-dumont/` folder.

**From wp-admin**: download `viglet-dumont-<version>.zip` and upload it under
Plugins > Add New > Upload Plugin.

**With Composer** (a site built in CI, or one running with `DISALLOW_FILE_MODS`): Composer
cannot install from a subfolder of this repository, so declare the release zip as a package and
let [`composer/installers`](https://github.com/composer/installers) place it in the plugins
directory:

```json
{
    "repositories": [
        {
            "type": "package",
            "package": {
                "name": "viglet/dumont-wordpress",
                "version": "2.0.0",
                "type": "wordpress-plugin",
                "dist": {
                    "type": "zip",
                    "url": "https://github.com/openviglet/dumont/releases/download/wordpress-v2.0.0/viglet-dumont-2.0.0.zip"
                },
                "require": { "composer/installers": "^2.0" }
            }
        }
    ],
    "require": {
        "viglet/dumont-wordpress": "2.0.0"
    }
}
```

**With WP-CLI**: `wp plugin install <zip URL> --activate`.

## Configure from the environment

A container deploy can set the connection without a wp-admin step. Each of these is read
from a `wp-config.php` constant or, failing that, an environment variable of the same name;
a value set either way wins over the settings page, shows there read-only, and is never
written to `wp_options`, so the key stays out of database backups.

| Name | Setting |
|---|---|
| `VIGLET_DUMONT_TURING_URL` | Turing's URL, as this server reaches it |
| `VIGLET_DUMONT_TURING_SITE` | The SN site |
| `VIGLET_DUMONT_TURING_API_KEY` | The API key |
| `VIGLET_DUMONT_TURING_LOCALE` | The locale content is indexed in |
| `VIGLET_DUMONT_SITE_KEY` | The site key that starts every document id |
| `VIGLET_DUMONT_PUBLIC_URL` | Turing's URL as visitors' browsers reach it, when different |

`wp dumont status` says where each value came from. The Docker stack below sets the first
three this way.

## Search

With "Replace WordPress search with Turing" on (off on a fresh install), the default results
page is the **theme's own**: Turing answers WordPress's main search query and the theme's
search template renders it, so a block theme keeps its header, footer and styles. Results
are this site's documents only (its site key), and if Turing does not answer, WordPress
searches its database as before. The older plugin results page, with facets, stays
available as "The plugin's results page" for classic themes.

The **Turing search** block (`viglet-dumont/search`) is a complete search UI for any page: a
box with suggestions, "did you mean", facets from the mapping, results and pages, built with
the Interactivity API on the SDK below and styled by the theme. It reads `?q=` (or `?s=`),
so a page holding it can be the site's search page. A site with its own search blocks
turns it off: `add_filter( 'viglet_dumont_register_blocks', '__return_false' );`.

Its parts can be blocks of their own: Layout → **Arrange its parts** turns the box, the
filters, the results and the pages into inner blocks (`viglet-dumont/search-box`,
`-filters`, `-results`, `-pages`). A theme can then reorder them, put the pages above the
results, or set its own blocks between them. They stay one search with one store, and the
filters stay beside the results. A block whose parts were never arranged draws them in the
default order.

The **Related content** block (`viglet-dumont/related`) lists the posts Turing's
similar-documents search finds most like the one being read. It never lists the post itself,
only this site's published posts, and renders nothing when there are none. It is drawn on the
server, so the links are in the HTML, and each post's neighbours are cached for an hour.
Settings → Search → Related Content puts it after the content of chosen post types. The
block chooses how many to show, shared words or meaning (`MLT` / `VECTOR`; meaning falls back
to shared words on a site without embeddings), same post type only, and grid or list.

The **Site assistant** block (`viglet-dumont/assistant`) is a button that opens a chat with
Turing over the site's posts (`POST /api/sn/{site}/chat/conversation`, through the SDK's chat
controller). Answers stream in, labelled as generated, with the posts they used as links and
a "Was this helpful?" that goes to Turing's answer signals. The conversation is the visitor's
own (Turing's `TUR_SESSION` cookie) and lasts across the pages of the tab, so a cited post can
be opened without losing it; "New conversation" starts again. The only action the agent may
take on the page is opening one of this site's pages. The block suggests first questions,
its own or else the week's popular searches, and stays hidden while Turing reports chat off.
Settings → Search → Generative AI → "Site assistant on every page" puts it in a corner of
every page instead.

A theme that builds its own search on `@viglet/turing-sdk` does not vendor it: the plugin
registers two script modules, `@viglet/turing-sdk` (the SDK as published) and
`viglet-dumont/search` (the SDK configured for this site, filtered to its site key):

```js
// In a theme's module (registered with 'viglet-dumont/search' as a dependency).
import { createSiteSearch, createSiteAutoComplete, config } from 'viglet-dumont/search';

const search = createSiteSearch();          // search-only; { controller: { chat: true } } for answers
search.subscribe( ( state ) => render( state ) );
await search.searchQuery( 'workshop' );
```

The configuration it reads is the same one `viglet_dumont_public_config()` and
`GET /wp-json/viglet-dumont/v1/public-config` return:
Turing's public URL, the SN site, the locale, the site key (filter on `site_key`) and the
mapped facets. It holds no key. For the browser to call Turing, the SN site must allow the
WordPress origin in Turing's CORS settings (`TURING_ALLOWEDORIGINS` in a container).

### Search analytics

Two opt-in settings under Search, both off on a fresh install:

- **Report result clicks to Turing**: the block and the instant search send each opened
  result (query, document id, rank) to `POST /api/sn/{site}/click`, with `keepalive` so the
  navigation never waits for it. Turing keeps clicks only with search metrics on
  (`TURING_SEARCH_METRICS_ENABLED=true`).
- **Send search events to Google Analytics**: searches the visitor settled on (not each
  as-you-type one, nor a filter, sort or page on one), no-result searches, changed queries
  and result clicks, as the SDK's `turing_search*` events through the Google tag or Tag
  Manager the site already loads, with GA4's own `search` and `select_content` beside them so
  its built-in site-search reports fill. With clicks reported too, a search left without
  opening a result sends `turing_search_abandoned`. The measurement id is optional.

Events carry no visitor data, and the plugin sets no cookie. With a consent plugin that
speaks the WP Consent API, nothing is sent until the visitor allows `statistics`. Without
one, the site decides: `add_filter( 'viglet_dumont_tracking_consent', '__return_false' );`
sends nothing at all. A theme's own blocks report the same way: through
`createSiteSearch( { track: true } )` and `trackResultClick( click, controller )` from
`viglet-dumont/search`, or, searching without a tracked controller, `trackSearch()`.

## Develop

PHP 8.1+ and Composer:

```sh
composer install
composer check      # PHPCS (WordPress Coding Standards), PHPStan and PHPUnit
composer lint       # PHPCS alone; vendor/bin/phpcbf fixes what it can
```

The admin screens (the Health page) are React with `@wordpress/components`, written in
[`assets/admin/`](assets/admin/) and built into `src/admin/build/`, which is not committed:

```sh
npm ci
npm run build       # or npm start to rebuild on change
npm run lint:js
```

The release zip and `run-docker.cmd` build them; a checkout without a build shows a notice on
the Health page instead of a blank one.

### Browser tests (Playwright)

[`tests/e2e/`](tests/e2e/) drives the admin screens and the site in a real browser against
the `run-docker.cmd` stack. It uses the installed Microsoft Edge, so no browser download is
needed (`E2E_CHANNEL=chromium` after `npx playwright install chromium` to switch).

```sh
run-docker.cmd                 # the stack, once
npm run test:e2e               # seed, log in, run every spec
```

The global setup runs [`fixtures/seed.php`](tests/e2e/fixtures/seed.php) through WP-CLI:
twelve posts with a `seats` facet, a "Busca" page holding the search block, Turing search
on. It then reindexes and logs in once. Every test fails on a console error, an uncaught
page error, a failed request or a 4xx/5xx response, and saves its screens to
`tests/e2e/artifacts/screens/<project>/`. The HTML report is in `tests/e2e/artifacts/report/`.
`E2E_SKIP_SEED=1` reruns without seeding. It is not in CI, which has no Turing.

Beyond behaviour, the suite also checks:

- **Accessibility**: axe (WCAG 2.1 A and AA) on every screen it visits, failing on a serious
  or critical violation in the plugin's own region.
- **Viewports**: the site specs run at desktop (1280), tablet (820) and phone (390), as the
  projects `site`, `site-tablet` and `site-phone`.
- **Visual regression**: the search block and the admin screens against the baselines in
  `tests/e2e/snapshots/<platform>/<project>/` (50 pixels of tolerance, for antialiasing only; counts and events are masked).
  After a change to the look, refresh them on purpose with
  `npm run test:e2e -- --update-snapshots` and commit the new PNGs.
- **Speed**: the first results within 1.5 s of the navigation and a suggestion within
  500 ms of the typing, as the browser measures them (`site/budgets.spec.mjs`).
- **Keyboard**: every admin action and the search block reached with Tab alone, with a
  visible focus ring, and used with Enter or Space.
- **Themes and language**: after the default projects (Twenty Twenty-Five, English), the
  site specs run again under Twenty Twenty-One (`site-classic`, a classic theme) and Ollie
  1.6.5 (`site-ollie`, a third-party block theme), then `pt-br` switches the site to
  Portuguese and checks every admin screen and the block for Portuguese, failing on any
  English string the `.po` translates. One WordPress serves them all, so each variant is a
  `switch:` project chained by dependencies; the global setup puts back Twenty Twenty-Five
  in English at the start of every run. `--project=site` and the like skip the variants.
- **Multisite**: `run-docker.cmd multisite` adds a two-site network on
  `localhost:8081` (`/` and `/second/`), sharing the local Turing under its own SN site
  ("Network"). `E2E_MULTISITE=1 npm run test:e2e` seeds both sites and adds the `network`
  project, which checks that each site's search block finds only its own documents.
- **Demo storefront**: the `demo` project checks the curated searches, the comparison page
  and the tour on the demo's own WordPress (`run-docker.cmd demo`, `localhost:38082`), after
  `demo-setup` seeds its catalog if missing. It needs no switch, so `npm run test:e2e` runs it
  beside the rest and `npm run test:e2e:demo` runs it alone.

**The live-LLM suite** (`npm run test:e2e:ai`, `playwright.ai.config.mjs`) asks Turing's
generative features real questions, on a stack started with `OPENAI_API_KEY`. It is never
part of `npm run test:e2e`, which stays free and deterministic. Its seed
([`fixtures/ai-seed.php`](tests/e2e/fixtures/ai-seed.php)) adds posts holding known facts,
in English and Portuguese, and one with a planted instruction; the specs check that answers
come from those posts, ignore the instruction, invent no figure a post does not hold, and
stay within a p95 time budget (`E2E_AI_P95_MS`, 20 s). Each spec skips, with the reason
`wp dumont ai status` gives, a feature that is off. The answers land in
`tests/e2e/artifacts/ai/answers.json`, and the teardown deletes the seeded posts again.

**The recorded tour** (`npm run e2e:tour`, `playwright.tour.config.mjs`) walks the demo's
showcase with "How it works" on, after a search so the filters show, and opens each numbered
feature in turn. It leaves one screenshot per feature and `tour.webm` in
`tests/e2e/artifacts/tour/`, the source of the docs' per-feature screenshots. It asks the stack
as it is, so a feature with no language model behind it is skipped, not failed. Recording needs
Playwright's ffmpeg once: `npx playwright install ffmpeg`.

To look at one page in detail, logged in or not:

```sh
npm run e2e:look -- /wp-admin/admin.php?page=viglet-dumont-health
npm run e2e:look -- "/?page_id=7&q=workshop" --anon --mobile
npm run e2e:look -- /wp-admin/edit.php --script ./my-steps.mjs   # export default async ( page ) => { ... }
npm run e2e:look -- /wp-admin/admin.php?page=viglet-dumont --a11y
```

It saves a full-page screenshot to `tests/e2e/artifacts/look/` and prints the console
errors, failed requests, headings, buttons, form fields with their labels, and the
visible text. `--a11y` adds every axe violation on the whole page, marking with `!` the
serious and critical ones a spec would fail on.

### Translations

The plugin ships pt_BR in [`src/languages/`](src/languages/): the `.po` source, the `.mo`
PHP reads and one `.json` per admin script. After changing a string, regenerate them with
WP-CLI (Docker works where it is not installed):

```sh
docker run --rm -v "$PWD:/plugin" -w /plugin wordpress:cli-2.12-php8.5 sh -c '
  wp i18n make-pot . src/languages/viglet-dumont.pot --slug=viglet-dumont --domain=viglet-dumont \
    --exclude=node_modules,vendor,src/admin/build,tests,build,demo &&
  wp i18n update-po src/languages/viglet-dumont.pot src/languages &&
  wp i18n make-mo src/languages && wp i18n make-json src/languages --no-purge'
```

then translate the new entries in `viglet-dumont-pt_BR.po` and run the last two commands
again. The JSON files are named after the source script (`assets/admin/health.js`), and
`viglet_dumont_script_translation_path` maps the built script to it.

The tests run on [Brain Monkey](https://giuseppe-mazzapica.gitbook.io/brain-monkey/), with no
WordPress install. Without PHP locally, the same runs in a container:

```sh
docker run --rm -v "$PWD:/app" -w /app composer:2 sh -c "composer install && composer check"
```

### Try it in a real WordPress

[`run-docker.cmd`](run-docker.cmd) starts WordPress 7.1.3 on PHP 8.5 with MariaDB and a local Turing
([`docker-compose.yml`](docker-compose.yml)), mounts `src/` as the plugin and, on the first run,
installs WordPress, creates the SN site in Turing and gives the plugin an API key for it
([`bin/turing-setup.ps1`](bin/turing-setup.ps1)). Edits under `src/` show on the next page load.

```bat
run-docker.cmd                         :: WordPress http://localhost:8080/wp-admin (admin / admin)
                                       :: Turing    http://localhost:2700 (admin / dumont-dev)
run-docker.cmd wp dumont queue run     :: any WP-CLI command
run-docker.cmd wp dumont ai status     :: which of Turing's AI features the site can use (exits 1 if any is off)
run-docker.cmd demo                    :: also the Tarnfell demo storefront on http://localhost:38082
run-docker.cmd demo reset              :: drop the demo's WordPress and database only
run-docker.cmd multisite               :: also a two-site network on http://localhost:8081
run-docker.cmd logs turing             :: follow a log: wordpress, turing or db
run-docker.cmd stop                    :: keep the data
run-docker.cmd reset                   :: drop WordPress, its database and Turing's store
```

WP-Cron's loopback cannot reach the site URL inside the container, so `DISABLE_WP_CRON` is on
and a `cron` service runs due events every 15 seconds, as a host with a system cron does: a
saved post reaches Turing without `wp dumont queue run`.

Start the stack through `run-docker.cmd`, not a bare `docker compose up`: the script is what
picks `turing-ce:local`, and a bare `up` recreates Turing on the published image.

Turing runs as trabalharei runs it for development: embedded H2, Lucene and Artemis, plain HTTP,
local login. When an image `turing-ce:local` exists, it is used instead of the published one,
which lags the Turing source; build it from a Turing checkout after `mvnw package` with
`docker build -f Dockerfile.local -t turing-ce:local .` (the published 2026.3.4 image answers
every API-key import with HTTP 500). Set `WP_PORT`, `TURING_PORT`, `SN_SITE`, `TURING_LOCALE`,
`TURING_ADMIN_PASSWORD`, `TURING_IMAGE`, `WP_DEMO_PORT`, `SN_DEMO_SITE` or `DEMO_LOCALE` before the first run
to change the defaults.
To move an installed stack to another port (another project holds 8080, say), set `WP_PORT`,
run `run-docker.cmd`, then
`run-docker.cmd wp search-replace localhost:8080 localhost:<port> --all-tables --skip-columns=guid`;
the browser tests then need `WP_URL=http://localhost:<port>`. Their setup compares each URL
with the site URL WP-CLI reports (`WP_URL`, `WP_MS_URL`, `WP_DEMO_URL`) and stops before
seeding when they differ, so another WordPress on that port is never tested by mistake.

The demo storefront (`demo/`, outside the plugin zip) is Tarnfell, a fictional outdoor gear
shop: its own theme, WooCommerce with its catalog (`demo/seed`), a home whose hero is the
search block's box alone, a results page, product pages with the copilot and Related, a
journal, and a showcase (`/showcase/`) holding the search, the copilot and the Site assistant
on one page; "How it works" (`?tour=1`) numbers each Turing feature a page shows. The setup
turns the site's generative AI on (sent with the fields on the next index) and puts the Site
assistant in a corner of every page; both stay hidden while Turing has no language model. Lighthouse in headless Edge (October 2026): home 93 performance and 96
accessibility, results 90 and 96. axe finds nothing on home, search and a post; on a product,
only WooCommerce's own mini-cart drawer. The name was checked with a web search only (no
brand or store called Tarnfell): run a trademark registry search before any public use.

The demo is a WordPress of its own (`wordpress-demo`, `db-demo` and `cron-demo`, the compose
`demo` profile) on `WP_DEMO_PORT` (38082), sharing the one Turing under its own SN site,
`SN_DEMO_SITE` (`Demo`), with its own API key. The site on `WP_PORT` stays the browser tests'
and is never switched to Tarnfell, so the demo can stay up while the tests run and neither
site's searches return the other's documents. WP-CLI on the demo is
`docker compose run --rm --no-deps cli-demo wp <command>`. `DEMO_LOCALE=pt_BR` before
`run-docker.cmd demo` turns the demo into Portuguese: the site's language, the plugin's locale
and the SN site's, and a catalog and journal seeded from `demo/seed/pt_BR.php` with
Portuguese synonyms; run it again with `en_US` (the default) to go back. The `demo` Playwright project
runs against it (`WP_DEMO_URL`, default `http://localhost:38082`) and seeds its catalog only
when it is missing; `npm run test:e2e:demo` runs those specs alone, without touching the test
site. A test site that ran the demo before this split keeps its `TF-` products and journal:
`run-docker.cmd reset` clears them.

`phpcbf` capitalises `wordpress` wherever it reads it as prose, and the `source_apps` value is
data Turing matches on. The lines that carry it hold a `phpcs:ignore` for that sniff; keep it.

## Release

1. Bump `Version:` and `VIGLET_DUMONT_VERSION` in `src/viglet-dumont.php`, `Stable tag:` in
   `src/readme.txt`, and add the changelog entry there.
2. Push a tag `wordpress-v<version>`. The workflow
   [`wordpress-plugin.yml`](../.github/workflows/wordpress-plugin.yml) runs the checks, builds
   the zip with `bin/build-zip.sh` and attaches it to that tag's release. The script refuses a
   tag that disagrees with the plugin header or with the readme's stable tag.
