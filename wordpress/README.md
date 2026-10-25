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
`tests/e2e/artifacts/screens/`. The HTML report is in `tests/e2e/artifacts/report/`.
`E2E_SKIP_SEED=1` reruns without seeding. It is not in CI, which has no Turing.

To look at one page in detail, logged in or not:

```sh
npm run e2e:look -- /wp-admin/admin.php?page=viglet-dumont-health
npm run e2e:look -- "/?page_id=7&q=workshop" --anon --mobile
npm run e2e:look -- /wp-admin/edit.php --script ./my-steps.mjs   # export default async ( page ) => { ... }
```

It saves a full-page screenshot to `tests/e2e/artifacts/look/` and prints the console
errors, failed requests, headings, buttons, form fields with their labels, and the
visible text.

### Translations

The plugin ships pt_BR in [`src/languages/`](src/languages/): the `.po` source, the `.mo`
PHP reads and one `.json` per admin script. After changing a string, regenerate them with
WP-CLI (Docker works where it is not installed):

```sh
docker run --rm -v "$PWD:/plugin" -w /plugin wordpress:cli-2.12-php8.5 sh -c '
  wp i18n make-pot . src/languages/viglet-dumont.pot --slug=viglet-dumont --domain=viglet-dumont \
    --exclude=node_modules,vendor,src/admin/build,tests,build &&
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
`TURING_ADMIN_PASSWORD` or `TURING_IMAGE` before the first run to change the defaults.

`phpcbf` capitalises `wordpress` wherever it reads it as prose, and the `source_apps` value is
data Turing matches on. The lines that carry it hold a `phpcs:ignore` for that sniff; keep it.

## Release

1. Bump `Version:` and `VIGLET_DUMONT_VERSION` in `src/viglet-dumont.php`, `Stable tag:` in
   `src/readme.txt`, and add the changelog entry there.
2. Push a tag `wordpress-v<version>`. The workflow
   [`wordpress-plugin.yml`](../.github/workflows/wordpress-plugin.yml) runs the checks, builds
   the zip with `bin/build-zip.sh` and attaches it to that tag's release. The script refuses a
   tag that disagrees with the plugin header or with the readme's stable tag.
