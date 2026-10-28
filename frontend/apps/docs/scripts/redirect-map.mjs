#!/usr/bin/env node
/**
 * T1163 — the map from every docs.viglet.org Turing URL to its page here, for the
 * redirect that will retire docs.viglet.org/turing. Search engines, posts, the
 * console and the other Viglet products link to deep Docusaurus URLs, and a
 * domain-level redirect to the docs home would throw all of them away.
 *
 *   node scripts/redirect-map.mjs [path-to-vigletdocs.github.io]
 *
 * Writes redirects.json: `[{ from, to }]` in the shape vigletdocs'
 * @docusaurus/plugin-client-redirects takes (the one its Dumont move already uses),
 * so the redirect is a paste. The old URL set is derived with Docusaurus's own
 * routing for the `turing` plugin (routeBasePath "turing", trailing slash):
 *   - current docs: /turing/<id>, the page's `slug` frontmatter winning
 *     (`/` for the index, `/category/<x>` for the four section landings);
 *   - archived versions: /turing/<version>/<id> for 0.3.5..0.3.9, each pointing at
 *     the current page with the same id, else at the docs home.
 * It fails when a ported page has no entry, so a page added here cannot be
 * unreachable from its old address. scripts/check-links.mjs checks every `to`
 * against the build.
 */
import fs from "node:fs";
import path from "node:path";
import url from "node:url";

const HERE = path.dirname(url.fileURLToPath(import.meta.url));
const AREA = path.resolve(HERE, "..");
const SOURCE_ROOT = path.resolve(process.argv[2] ?? "D:/Git/viglet/vigletdocs/vigletdocs.github.io");
const CONTENT = path.join(AREA, "src", "content", "docs");
const TARGET = "https://turing.viglet.org/docs";
const SKIP = new Set(["COVERAGE.md", "_documentation-update-plan.md"]);

function walk(dir, rel = "") {
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const relPath = rel ? `${rel}/${entry.name}` : entry.name;
    if (entry.isDirectory()) out.push(...walk(path.join(dir, entry.name), relPath));
    else if (/\.mdx?$/.test(entry.name) && !SKIP.has(relPath) && !entry.name.startsWith("_")) out.push(relPath);
  }
  return out;
}

/** The URL tail Docusaurus gives a page: its `slug` frontmatter, else its id. */
function routeOf(file, relPath) {
  const text = fs.readFileSync(file, "utf8");
  const fm = text.match(/^---\r?\n([\s\S]*?)\r?\n---/);
  const slug = fm && (fm[1].match(/^slug:\s*(.+)$/m) ?? [])[1];
  const id = relPath.replace(/\.mdx?$/, "");
  if (slug) return slug.trim().replace(/^["']|["']$/g, "").replace(/^\//, "").replace(/\/$/, "");
  return id === "index" ? "" : id;
}

function newHref(slug) {
  return slug ? `${TARGET}/${slug}/` : `${TARGET}/`;
}

/**
 * Archived pages with no namesake here, sent where their subject lives now
 * rather than to the docs home: the connectors moved to Dumont's docs (the
 * vigletdocs DUMONT_MOVED list), and release notes are viglet.org's.
 */
const LANDINGS = {
  connectors: "https://dumont.viglet.org/docs/connectors/overview/",
  "release-notes": "https://www.viglet.org/turing/release-notes/",
};

function targetOf(slug) {
  if (ported.has(slug)) return newHref(slug);
  return LANDINGS[slug] ?? newHref("");
}

/** The pages ported here, by their Starlight slug (the source id). */
const ported = new Set(walk(CONTENT).map((rel) => {
  const id = rel.replace(/\.mdx?$/, "");
  return id === "index" ? "" : id;
}));

const entries = [];
const covered = new Set();

// Current docs.
const current = path.join(SOURCE_ROOT, "docs-turing");
for (const rel of walk(current)) {
  const id = rel.replace(/\.mdx?$/, "");
  const slug = id === "index" ? "" : id;
  const route = routeOf(path.join(current, rel), rel);
  entries.push({ from: route ? `/turing/${route}` : "/turing", to: targetOf(slug) });
  covered.add(slug);
}

// Archived versions, each page to its current namesake or the home.
const versions = JSON.parse(fs.readFileSync(path.join(SOURCE_ROOT, "turing_versions.json"), "utf8"));
for (const version of versions) {
  const dir = path.join(SOURCE_ROOT, "turing_versioned_docs", `version-${version}`);
  for (const rel of walk(dir)) {
    const id = rel.replace(/\.mdx?$/, "");
    const slug = id === "index" ? "" : id;
    const route = routeOf(path.join(dir, rel), rel);
    entries.push({
      from: route ? `/turing/${version}/${route}` : `/turing/${version}`,
      to: targetOf(slug),
    });
  }
}

const missing = [...ported].filter((slug) => !covered.has(slug));
if (missing.length) {
  console.error(`[redirect-map] ported pages with no old URL: ${missing.join(", ")}`);
  process.exit(1);
}

entries.sort((a, b) => a.from.localeCompare(b.from));
fs.writeFileSync(path.join(AREA, "redirects.json"), `${JSON.stringify(entries, null, 2)}\n`);
console.log(`[redirect-map] ${entries.length} old URLs mapped (${versions.length} archived versions)`);
