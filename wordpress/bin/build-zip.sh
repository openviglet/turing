#!/usr/bin/env sh
# Build the installable plugin zip: build/viglet-turing-search-<version>.zip, holding one
# viglet-turing-search/ folder (the plugin directory slug), which is what both WordPress and Composer's package repository expect.
#
# The version comes from the plugin header, and a version argument (the release tag without its
# wordpress-v prefix) must match it, so a tag can never publish a zip that says otherwise.
#
# --wporg builds the package uploaded to WordPress.org instead (build/viglet-turing-search-<version>-wporg.zip):
# without the compiled translations and their loader, since translate.wordpress.org serves the
# language packs there. The .pot stays.
set -eu

cd "$(dirname "$0")/.."

wporg=
if [ "${1:-}" = "--wporg" ]; then
    wporg=1
    shift
fi

version=$(sed -n 's/^ \* Version:[[:space:]]*//p' src/viglet-turing.php)
stable=$(sed -n 's/^Stable tag:[[:space:]]*//p' src/readme.txt)
if [ -z "$version" ]; then
    echo "No Version: line in src/viglet-turing.php" >&2
    exit 1
fi
if [ "$version" != "$stable" ]; then
    echo "Plugin header says $version but readme.txt's Stable tag says $stable" >&2
    exit 1
fi
if [ $# -gt 0 ] && [ "$1" != "$version" ]; then
    echo "Asked to release $1 but the plugin header says $version" >&2
    exit 1
fi

for entry in health mapping setup turing-sdk; do
    if [ ! -f "src/admin/build/$entry.asset.php" ]; then
        echo "src/admin/build/$entry is missing: run npm ci && npm run build first" >&2
        exit 1
    fi
done
# The build is flat: a folder in it is wp-scripts copying the plugin's own files into it (D211).
if [ -n "$(find src/admin/build -mindepth 1 -type d)" ]; then
    echo "src/admin/build holds folders: rm -rf src/admin/build && npm run build" >&2
    exit 1
fi

rm -rf build
mkdir -p build/viglet-turing-search
cp -R src/. build/viglet-turing-search/
cp composer.json build/viglet-turing-search/
zip_name="viglet-turing-search-$version.zip"
if [ -n "$wporg" ]; then
    rm -f build/viglet-turing-search/includes/bundled-translations.php
    find build/viglet-turing-search/languages -type f ! -name '*.pot' -delete
    zip_name="viglet-turing-search-$version-wporg.zip"
fi
(cd build && zip -qr "$zip_name" viglet-turing-search)
rm -rf build/viglet-turing-search

echo "build/$zip_name"
