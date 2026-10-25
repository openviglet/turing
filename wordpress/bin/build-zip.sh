#!/usr/bin/env sh
# Build the installable plugin zip: build/viglet-dumont-<version>.zip, holding one
# viglet-dumont/ folder, which is what both WordPress and Composer's package repository expect.
#
# The version comes from the plugin header, and a version argument (the release tag without its
# wordpress-v prefix) must match it, so a tag can never publish a zip that says otherwise.
set -eu

cd "$(dirname "$0")/.."

version=$(sed -n 's/^ \* Version:[[:space:]]*//p' src/viglet-dumont.php)
stable=$(sed -n 's/^Stable tag:[[:space:]]*//p' src/readme.txt)
if [ -z "$version" ]; then
    echo "No Version: line in src/viglet-dumont.php" >&2
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

for entry in health mapping; do
    if [ ! -f "src/admin/build/$entry.asset.php" ]; then
        echo "src/admin/build/$entry is missing: run npm ci && npm run build first" >&2
        exit 1
    fi
done

rm -rf build
mkdir -p build/viglet-dumont
cp -R src/. build/viglet-dumont/
cp composer.json build/viglet-dumont/
(cd build && zip -qr "viglet-dumont-$version.zip" viglet-dumont)
rm -rf build/viglet-dumont

echo "build/viglet-dumont-$version.zip"
