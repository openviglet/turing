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

### Translations

The plugin ships pt_BR in [`src/languages/`](src/languages/): the `.po` source, the `.mo`
PHP reads and one `.json` per admin script. After changing a string, regenerate them with
WP-CLI (Docker works where it is not installed):

```sh
docker run --rm -v "$PWD:/plugin" -w /plugin wordpress:cli-php8.3 sh -c '
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

[`run-docker.cmd`](run-docker.cmd) starts WordPress 6.8 with MariaDB and a local Turing
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
