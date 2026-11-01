#!/usr/bin/env sh
# Build what the WordPress Playground demo (D239) downloads, into the folder named by the first
# argument (the site's Pages workflow passes _site/playground): demo/playground/blueprint.json
# and the three zips its URLs name.
#
#   viglet-turing-search.zip   the plugin, as bin/build-zip.sh packages it (run npm run build first)
#   tarnfell.zip        the demo theme, one tarnfell/ folder
#   tarnfell-demo.zip   demo/setup.php, the catalog seed and its photos, one tarnfell-demo/ folder
#
# Playground fetches them from the browser, so they are served from a host that sends CORS
# headers, which GitHub Pages does and a GitHub release download does not.
set -eu

out=${1:?usage: build-playground.sh <out-dir>}
cd "$(dirname "$0")/.."
mkdir -p "$out"
out=$(cd "$out" && pwd)

cp "$(sh bin/build-zip.sh)" "$out/viglet-turing-search.zip"
(cd demo/theme && zip -qr "$out/tarnfell.zip" tarnfell)

rm -rf build/tarnfell-demo
mkdir -p build/tarnfell-demo
cp -R demo/setup.php demo/seed demo/photos build/tarnfell-demo/
(cd build && zip -qr "$out/tarnfell-demo.zip" tarnfell-demo)
rm -rf build/tarnfell-demo

cp demo/playground/blueprint.json "$out/blueprint.json"
echo "$out"
