#!/usr/bin/env node
/*
 * The half of "are the docs in sync with the code" that the docs gates do not ask.
 *
 * `pnpm --filter turing-docs build` (check-links.mjs) proves the links in the built area resolve,
 * and the docs-style lint reads how pages are written. Neither looks from the code towards the
 * docs: a property, a CLI command or a public endpoint the product HAS and no page mentions
 * passes both. This report walks that direction, plus structural checks that need no build: a
 * page missing from the sidebar, a hand-typed `/docs/...` link or anchor that no longer resolves,
 * and a COVERAGE.md row pointing at a page or anchor that does not exist.
 *
 * It is a REPORT, not a gate: the exit code is 0 unless --strict is given. Some gaps are
 * deliberate (an internal tuning knob, an admin-only endpoint), and deciding which is the
 * judgment the turing-docs-sync skill asks for. Read-only; needs no build and no instance.
 *
 * Usage (from the repo root):
 *   node .claude/skills/turing-docs-sync/docs-sync-report.mjs            # human-readable
 *   node .claude/skills/turing-docs-sync/docs-sync-report.mjs --json     # machine-readable
 *   node .claude/skills/turing-docs-sync/docs-sync-report.mjs --strict   # exit 1 on any gap
 */
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "..");
const DOCS_APP = join(ROOT, "frontend", "apps", "docs");
const DOCS_DIR = join(DOCS_APP, "src", "content", "docs");
const SIDEBAR = join(DOCS_APP, "src", "sidebar.generated.json");
const COVERAGE = join(DOCS_APP, "COVERAGE.md");
const JAVA = join(ROOT, "turing-app", "src", "main", "java");
const CLI = join(ROOT, "frontend", "packages", "cli", "src", "cli.ts");
const args = new Set(process.argv.slice(2));

if (!existsSync(DOCS_DIR)) {
  console.error(`docs not found at ${DOCS_DIR}`);
  process.exit(2);
}

function walk(dir, keep) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir).flatMap((name) => {
    const full = join(dir, name);
    return statSync(full).isDirectory() ? walk(full, keep) : keep(name) ? [full] : [];
  });
}

/** Every page, keyed by its Starlight slug (`getting-started/intro`, `index`). */
const pages = new Map(
  walk(DOCS_DIR, (n) => /\.mdx?$/.test(n)).map((full) => {
    let slug = relative(DOCS_DIR, full).split(sep).join("/").replace(/\.mdx?$/, "").toLowerCase();
    if (slug.endsWith("/index")) slug = slug.slice(0, -"/index".length);
    return [slug, readFileSync(full, "utf8")];
  }),
);

/** Starlight (github-slugger) heading ids, close enough for a report. */
const slugify = (heading) => heading.trim().toLowerCase()
  // A tag is stripped only outside inline code: `<id|name>` in a code span is text.
  .split("`").map((part, i) => (i % 2 ? part : part.replace(/<[^>]+>/g, ""))).join("")
  .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
  .replace(/[^\p{L}\p{N}\s_-]/gu, "").replace(/\s/g, "-");

function headings(body) {
  const ids = new Set();
  let fenced = false;
  for (const line of body.split(/\r?\n/)) {
    if (/^\s*(```|~~~)/.test(line)) fenced = !fenced;
    if (fenced) continue;
    const h = /^#{1,6}\s+(.*?)\s*(\{#([\w-]+)\})?\s*$/.exec(line);
    if (h) ids.add(h[3] ?? slugify(h[1]));
    // Explicit anchors the pages declare to keep an old link alive: <a id="storage"></a>.
    for (const [, id] of line.matchAll(/<a\s+(?:id|name)="([^"]+)"/g)) ids.add(id.toLowerCase());
  }
  return ids;
}
const ids = new Map([...pages].map(([slug, body]) => [slug, headings(body)]));

const prose = [...pages.values()].join("\n");
/** Spring relaxed binding: `foo-bar`, `fooBar` and `foo_bar` are one key. */
const canon = (key) => key.toLowerCase().replace(/[-_]/g, "");
const proseCanon = canon(prose);

// 1. Code → docs: what the product declares that no page names.
const javaFiles = walk(JAVA, (n) => n.endsWith(".java")).map((f) => readFileSync(f, "utf8"));

const properties = new Set();
for (const src of javaFiles) {
  for (const [, key] of src.matchAll(/\$\{(turing\.[\w.-]+)/g)) properties.add(key);
}
const prefixes = new Set();
for (const src of javaFiles) {
  for (const [, key] of src.matchAll(/@ConfigurationProperties\(\s*(?:prefix\s*=\s*|value\s*=\s*)?"(turing[\w.-]*)"/g)) {
    prefixes.add(key);
  }
}

/** Public endpoints: class-level + method-level mappings under /api/sn and /api/v2. */
const endpoints = new Set();
for (const src of javaFiles) {
  if (!/@RestController/.test(src)) continue;
  const base = /@RequestMapping\(\s*(?:value\s*=\s*|path\s*=\s*)?\{?\s*"([^"]*)"/.exec(src)?.[1] ?? "";
  for (const [, verb, path] of src.matchAll(/@(Get|Post|Put|Patch|Delete)Mapping\(\s*(?:value\s*=\s*|path\s*=\s*)?\{?\s*"([^"]*)"/g)) {
    // Spring joins "/api/x" and "y" as "/api/x/y": a method path without a leading slash still nests.
    const full = (path && !path.startsWith("/") ? `${base}/${path}` : base + path).replace(/\/+/g, "/");
    if (/^\/api\/(sn|v2)\//.test(full)) endpoints.add(`${verb.toUpperCase()} ${full}`);
  }
  if (/@(Get|Post)Mapping\s*$/m.test(src) && /^\/api\/(sn|v2)\//.test(base)) endpoints.add(`* ${base}`);
}
/** An endpoint is named when its path, with every {var} as a wildcard, appears on a page. */
const endpointNamed = (e) => {
  const path = e.split(" ")[1];
  const re = new RegExp(path.split(/\{[^}]+\}/).map((s) => s.replace(/[.*+?^$()|[\]\\]/g, "\\$&")).join("[^\\s/`)]+"));
  return re.test(prose);
};

const cliCommands = new Set();
if (existsSync(CLI)) {
  const cli = readFileSync(CLI, "utf8");
  const dispatch = cli.slice(cli.indexOf("switch (command)"));
  for (const [, c] of dispatch.slice(0, dispatch.indexOf("default:")).matchAll(/case "([a-z][\w-]*)":/g)) cliCommands.add(c);
}

const POPULATIONS = [
  { key: "properties", label: "turing.* properties (@Value)", advisory: false,
    subjects: [...properties], named: (s) => proseCanon.includes(canon(s)) },
  { key: "prefixes", label: "@ConfigurationProperties prefixes", advisory: false,
    subjects: [...prefixes].filter((p) => p !== "turing"), named: (s) => proseCanon.includes(canon(s)) },
  { key: "cliCommands", label: "CLI commands (turing <command>)", advisory: false,
    subjects: [...cliCommands], named: (s) => new RegExp(`\\bturing ${s}\\b`).test(prose) },
  { key: "endpoints", label: "public endpoints (/api/sn, /api/v2)", advisory: true,
    subjects: [...endpoints], named: endpointNamed },
];
const reverse = POPULATIONS.map(({ key, label, advisory, subjects, named }) => ({
  key, label, advisory, total: subjects.length, missing: subjects.filter((s) => !named(s)).sort(),
}));

// 2. Sidebar: a page absent from it builds as an unlisted route nobody reaches.
const sidebarSlugs = new Set();
if (existsSync(SIDEBAR)) {
  const visit = (node) => {
    if (Array.isArray(node)) return node.forEach(visit);
    if (node && typeof node === "object") {
      if (node.slug) sidebarSlugs.add(String(node.slug).toLowerCase());
      if (node.link && !/^https?:/.test(node.link)) sidebarSlugs.add(String(node.link).replace(/^\/|\/$/g, "").toLowerCase() || "index");
      if (node.items) visit(node.items);
    }
  };
  visit(JSON.parse(readFileSync(SIDEBAR, "utf8")));
}
const unlisted = [...pages.keys()].filter((s) => s !== "index" && !sidebarSlugs.has(s)).sort();
const sidebarOrphans = [...sidebarSlugs].filter((s) => s !== "index" && !pages.has(s)).sort();

// 3. Hand-typed internal links: `/docs/<slug>/` and `/docs/<slug>/#anchor`, in markdown and href="".
const PUBLIC = join(DOCS_APP, "public");
const brokenLinks = [];
let anchorsChecked = 0;
const resolve = (from, target, anchor, link) => {
  if (/\.[a-z0-9]+$/i.test(target)) {
    if (!existsSync(join(PUBLIC, ...target.split("/")))) brokenLinks.push({ from, link, reason: "no such file in public/" });
    return;
  }
  const page = (target.replace(/\/$/, "") || "index").toLowerCase();
  if (anchor) anchorsChecked++;
  if (!ids.has(page)) brokenLinks.push({ from, link, reason: "no such page" });
  else if (anchor && !ids.get(page).has(decodeURIComponent(anchor).toLowerCase())) {
    brokenLinks.push({ from, link, reason: "no such heading" });
  }
};
for (const [slug, body] of pages) {
  for (const [link, target, anchor] of body.matchAll(/(?<=\]\(|href=")\/docs\/([^)"#\s]*?)\/?(?:#([^)"\s]+))?(?=[)"])/g)) {
    resolve(slug, target, anchor, link);
  }
}

// 4. COVERAGE.md rows: every `./src/content/docs/<page>#anchor` must resolve.
const coverageBroken = [];
if (existsSync(COVERAGE)) {
  const coverage = readFileSync(COVERAGE, "utf8");
  for (const [link, file, anchor] of coverage.matchAll(/\]\(\.\/src\/content\/docs\/([^)#\s]+?)\.mdx?(?:#([^)\s]+))?\)/g)) {
    let page = file.toLowerCase();
    if (page.endsWith("/index")) page = page.slice(0, -"/index".length);
    if (!ids.has(page)) coverageBroken.push({ link, reason: "no such page" });
    else if (anchor && !ids.get(page).has(decodeURIComponent(anchor).toLowerCase())) {
      coverageBroken.push({ link, reason: "no such heading" });
    }
  }
}

const report = {
  docsDir: relative(ROOT, DOCS_DIR), pages: pages.size, reverse, unlisted, sidebarOrphans,
  brokenLinks, anchorsChecked, coverageBroken,
};

if (args.has("--json")) {
  console.log(JSON.stringify(report, null, 2));
} else {
  console.log(`docs: ${report.docsDir} (${pages.size} pages)\n`);
  console.log("== Code -> docs: declared by the product, named on no page ==");
  for (const r of reverse) {
    console.log(`${r.label}: ${r.total - r.missing.length}/${r.total} named${r.advisory ? " [advisory]" : ""}`);
    for (const m of r.missing) console.log(`  - ${m}`);
  }
  console.log(`\n== Pages missing from the sidebar (sidebar.generated.json): ${unlisted.length} ==`);
  for (const u of unlisted) console.log(`  - ${u}`);
  console.log(`\n== Sidebar slugs with no page: ${sidebarOrphans.length} ==`);
  for (const u of sidebarOrphans) console.log(`  - ${u}`);
  console.log(`\n== Internal links that do not resolve: ${brokenLinks.length} (${anchorsChecked} anchored links checked) ==`);
  for (const b of brokenLinks) console.log(`  - ${b.from}: ${b.link} (${b.reason})`);
  console.log(`\n== COVERAGE.md links that do not resolve: ${coverageBroken.length} ==`);
  for (const b of coverageBroken) console.log(`  - ${b.link} (${b.reason})`);
}

const gaps = reverse.filter((r) => !r.advisory).reduce((n, r) => n + r.missing.length, 0)
  + unlisted.length + sidebarOrphans.length + brokenLinks.length + coverageBroken.length;
if (args.has("--strict") && gaps > 0) process.exit(1);
