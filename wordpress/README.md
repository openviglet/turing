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

The tests run on [Brain Monkey](https://giuseppe-mazzapica.gitbook.io/brain-monkey/), with no
WordPress install. Without PHP locally, the same runs in a container:

```sh
docker run --rm -v "$PWD:/app" -w /app composer:2 sh -c "composer install && composer check"
```

`phpcbf` capitalises `wordpress` wherever it reads it as prose, and the `source_apps` value is
data Turing matches on. The lines that carry it hold a `phpcs:ignore` for that sniff; keep it.

## Release

1. Bump `Version:` and `VIGLET_DUMONT_VERSION` in `src/viglet-dumont.php`, `Stable tag:` in
   `src/readme.txt`, and add the changelog entry there.
2. Push a tag `wordpress-v<version>`. The workflow
   [`wordpress-plugin.yml`](../.github/workflows/wordpress-plugin.yml) runs the checks, builds
   the zip with `bin/build-zip.sh` and attaches it to that tag's release. The script refuses a
   tag that disagrees with the plugin header or with the readme's stable tag.
