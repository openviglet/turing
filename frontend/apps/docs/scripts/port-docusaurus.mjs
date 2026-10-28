#!/usr/bin/env node
/**
 * T1161 — ports the Docusaurus `docs-turing` content (vigletdocs) into this Starlight
 * area. Re-runnable: it rewrites src/content/docs, public/img and
 * src/sidebar.generated.json from the source each time, so the port can be repeated
 * while vigletdocs keeps receiving edits.
 *
 *   node scripts/port-docusaurus.mjs [path-to-vigletdocs.github.io]
 *
 * What it converts, and nothing else (the source uses no Tabs and no other theme
 * components):
 *   - frontmatter: keeps title/description, `sidebar_label` -> `sidebar.label`,
 *     `hide_table_of_contents` -> `tableOfContents: false`; drops `slug` and
 *     `sidebar_position` (the order is the sidebar's).
 *   - the body H1 that repeats the title (Starlight renders the title).
 *   - admonitions: `:::tip Title` -> `:::tip[Title]`; info/important -> note,
 *     warning -> caution.
 *   - `## Heading {#id}` -> an `<a id>` anchor before the heading, so `#id` links hold.
 *   - relative `.md`/`.mdx` links -> `/docs/<slug>/`, written with the base on
 *     purpose: Astro does not prefix hand-written links.
 *   - `/img/...` -> `/docs/img/...`, copying each referenced image from `static/img`.
 *   - `<DocCardList />` on a category page -> a LinkCard per page of that category.
 *   - the landing page (React components with mouse handlers) -> a splash page with
 *     the same entries as Starlight cards.
 * `COVERAGE.md` (the docs gate, T1164) and `_documentation-update-plan.md` (retired)
 * are not pages and are skipped.
 */
import fs from "node:fs";
import path from "node:path";
import url from "node:url";

const HERE = path.dirname(url.fileURLToPath(import.meta.url));
const AREA = path.resolve(HERE, "..");
const SOURCE_ROOT = path.resolve(process.argv[2] ?? "D:/Git/viglet/vigletdocs/vigletdocs.github.io");
const SOURCE = path.join(SOURCE_ROOT, "docs-turing");
const CONTENT = path.join(AREA, "src", "content", "docs");
const PUBLIC_IMG = path.join(AREA, "public", "img");
const BASE = "/docs";
const SKIP = new Set(["COVERAGE.md", "_documentation-update-plan.md"]);
const ADMONITION = { tip: "tip", note: "note", info: "note", important: "note", warning: "caution",
  caution: "caution", danger: "danger" };

// ── read the source pages ────────────────────────────────────────────────────

function walk(dir, rel = "") {
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const relPath = rel ? `${rel}/${entry.name}` : entry.name;
    if (entry.isDirectory()) {
      out.push(...walk(path.join(dir, entry.name), relPath));
    } else if (/\.mdx?$/.test(entry.name) && !SKIP.has(relPath)) {
      out.push(relPath);
    }
  }
  return out;
}

/** The Starlight slug of a source page: its path without extension; `index` is the root. */
function slugOf(relPath) {
  const slug = relPath.replace(/\.mdx?$/, "");
  return slug === "index" ? "" : slug;
}

function hrefOf(slug) {
  return slug ? `${BASE}/${slug}/` : `${BASE}/`;
}

function splitFrontmatter(text) {
  const match = text.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?/);
  if (!match) return { data: {}, body: text };
  const data = {};
  for (const line of match[1].split(/\r?\n/)) {
    const kv = line.match(/^(\w+):\s*(.*)$/);
    if (kv) data[kv[1]] = kv[2].trim();
  }
  return { data, body: text.slice(match[0].length) };
}

function unquote(value) {
  if (value == null) return value;
  const v = value.trim();
  if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) {
    return v.slice(1, -1);
  }
  return v;
}

function yamlString(value) {
  return JSON.stringify(value ?? "");
}

const pages = walk(SOURCE).map((relPath) => {
  const text = fs.readFileSync(path.join(SOURCE, relPath), "utf8").replace(/^\uFEFF/, "");
  const { data, body } = splitFrontmatter(text);
  return { relPath, slug: slugOf(relPath), data, body };
});
const bySource = new Map(pages.map((p) => [p.relPath, p]));
const bySlug = new Map(pages.map((p) => [p.slug, p]));

// ── the sidebar ──────────────────────────────────────────────────────────────

async function readSidebar() {
  let ts = fs.readFileSync(path.join(SOURCE_ROOT, "sidebars-turing.ts"), "utf8");
  ts = ts.replace(/^import type .*$/m, "").replace(/:\s*SidebarsConfig/, "");
  const module = await import(`data:text/javascript;base64,${Buffer.from(ts).toString("base64")}`);
  return module.default.turingSidebar;
}

/** A Docusaurus doc id is the page path without extension. */
function slugOfId(id) {
  return id === "index" ? "" : id;
}

const categories = new Map(); // landing slug -> [child slugs]

function toStarlight(item) {
  if (typeof item === "string") {
    if (!bySlug.has(slugOfId(item))) throw new Error(`sidebar names a missing page: ${item}`);
    return { slug: slugOfId(item) };
  }
  if (item.type === "link") {
    return { label: item.label, link: item.href };
  }
  if (item.type === "category") {
    const items = item.items.map(toStarlight);
    if (item.link?.type === "doc") {
      const landing = slugOfId(item.link.id);
      categories.set(landing, item.items);
      items.unshift({ label: "Overview", slug: landing });
    }
    return { label: item.label, collapsed: false, items };
  }
  throw new Error(`unknown sidebar item: ${JSON.stringify(item)}`);
}

const sidebarSource = await readSidebar();
// The root category is "Viglet Turing" linking to index: its items are the top level.
const root = sidebarSource[0];
const sidebar = [{ label: "Introduction", link: "/" }, ...root.items.map(toStarlight)];

const listed = new Set();
(function collect(items) {
  for (const it of items) {
    if (it.slug !== undefined) listed.add(it.slug);
    if (it.items) collect(it.items);
  }
})(sidebar);
listed.add("");
const unlisted = pages.filter((p) => !listed.has(p.slug)).map((p) => p.slug);
if (unlisted.length) {
  sidebar.push({ label: "More", collapsed: true, items: unlisted.map((slug) => ({ slug })) });
}

// ── body conversion ──────────────────────────────────────────────────────────

const images = new Set();

function resolveLink(fromRel, target) {
  const [file, hash] = target.split("#");
  const resolved = path.posix.normalize(path.posix.join(path.posix.dirname(fromRel), file));
  const page = bySource.get(resolved);
  if (!page) return null;
  return hrefOf(page.slug) + (hash ? `#${hash}` : "");
}

function convertBody(page) {
  let body = page.body;
  const lines = body.split(/\r?\n/);
  const out = [];
  let inFence = false;
  let droppedH1 = false;
  for (let line of lines) {
    if (/^\s*(```|~~~)/.test(line)) {
      inFence = !inFence;
      out.push(line);
      continue;
    }
    if (inFence) {
      out.push(line);
      continue;
    }
    if (!droppedH1 && /^#\s+/.test(line)) {
      droppedH1 = true;
      continue;
    }
    const adm = line.match(/^(\s*):::(\w+)\s*(.*)$/);
    if (adm) {
      const type = ADMONITION[adm[2].toLowerCase()] ?? "note";
      const title = adm[3].trim();
      line = `${adm[1]}:::${type}${title ? `[${title}]` : ""}`;
    }
    const heading = line.match(/^(#{2,6}\s+.*?)\s*\{#([\w-]+)\}\s*$/);
    if (heading) {
      out.push(`<a id="${heading[2]}"></a>`, "");
      line = heading[1];
    }
    line = line.replace(/\]\(([^)\s]+?\.mdx?(?:#[^)\s]*)?)\)/g, (whole, target) => {
      if (/^https?:/.test(target)) return whole;
      const href = resolveLink(page.relPath, target);
      if (!href) throw new Error(`${page.relPath}: link to a missing page: ${target}`);
      return `](${href})`;
    });
    line = line.replace(/(\]\(|src=")\/img\/([^)"\s]+)/g, (_, lead, img) => {
      images.add(img);
      return `${lead}${BASE}/img/${img}`;
    });
    out.push(line);
  }
  return out.join("\n").replace(/^\s+/, "");
}

/** Docusaurus falls back to the body's H1 when the frontmatter has no title; so does this. */
function titleOf(page) {
  return unquote(page.data.title) ?? (page.body.match(/^#\s+(.+?)\s*$/m) ?? [])[1] ?? page.slug;
}

function frontmatter(page, extra = {}) {
  const fm = ["---", `title: ${yamlString(titleOf(page))}`];
  if (page.data.description) fm.push(`description: ${yamlString(unquote(page.data.description))}`);
  if (page.data.sidebar_label) fm.push("sidebar:", `  label: ${yamlString(unquote(page.data.sidebar_label))}`);
  if (page.data.hide_table_of_contents === "true") fm.push("tableOfContents: false");
  for (const [key, value] of Object.entries(extra)) fm.push(`${key}: ${value}`);
  fm.push("---", "");
  return fm.join("\n");
}

function cardFor(slug) {
  const page = bySlug.get(slugOfId(slug));
  const title = titleOf(page);
  const description = unquote(page.data.description) ?? "";
  return `  <LinkCard title=${JSON.stringify(title)} description=${JSON.stringify(description)} href="${hrefOf(page.slug)}" />`;
}

function cardsFor(items) {
  const slugs = [];
  (function collect(list) {
    for (const it of list) {
      if (typeof it === "string") slugs.push(it);
      else if (it.type === "category") {
        if (it.link?.type === "doc") slugs.push(it.link.id);
        collect(it.items);
      }
    }
  })(items);
  return ["<CardGrid>", ...slugs.map(cardFor), "</CardGrid>"].join("\n");
}

function convertCategory(page) {
  const body = convertBody(page)
    .replace(/^import DocCardList from .*$/m, "")
    .replace(/<DocCardList\s*\/>/, cardsFor(categories.get(page.slug) ?? []));
  return `${frontmatter(page)}import { CardGrid, LinkCard } from "@astrojs/starlight/components";\n\n${body.trim()}\n`;
}

/** The landing page's entries, read out of its JSX and laid out as Starlight cards. */
function convertLanding(page) {
  const body = page.body;
  const intro = (body.match(/^# Viglet Turing\s*\n+([^\n]+)/m) ?? [])[1] ?? "";
  const features = [...body.matchAll(/<FeatureCard icon="[^"]*" title="([^"]+)">\s*([\s\S]*?)\s*<\/FeatureCard>/g)]
    .map((m) => `  <Card title=${JSON.stringify(m[1])}>\n    ${m[2].trim()}\n  </Card>`);
  const steps = [...body.matchAll(/\{ step: "(\d+)", title: "([^"]+)", desc: "([^"]+)", href: "([^"]+)" \}/g)]
    .map((m) => `  <LinkCard title=${JSON.stringify(`${m[1]}. ${m[2]}`)} description=${JSON.stringify(m[3])} href="${legacyHref(m[4])}" />`);
  const sections = [];
  const sectionRe = /<SectionLabel>([^<]+)<\/SectionLabel>([\s\S]*?)(?=<SectionLabel>|$)/g;
  for (const m of body.matchAll(sectionRe)) {
    const cards = [...m[2].matchAll(/<NavCard icon="[^"]*" title="([^"]+)" description="([^"]+)" href="([^"]+)" \/>/g)]
      .map((c) => `  <LinkCard title=${JSON.stringify(c[1])} description=${JSON.stringify(c[2])} href="${legacyHref(c[3])}" />`);
    if (cards.length) sections.push(`## ${m[1].trim()}\n\n<CardGrid>\n${cards.join("\n")}\n</CardGrid>`);
  }
  return [
    frontmatter(page, { template: "splash" }).trimEnd(),
    "",
    'import { Card, CardGrid, LinkCard } from "@astrojs/starlight/components";',
    "",
    intro,
    "",
    "## Key capabilities",
    "",
    "<CardGrid>",
    ...features,
    "</CardGrid>",
    "",
    "## Quick start",
    "",
    "<CardGrid>",
    ...steps,
    "</CardGrid>",
    "",
    ...sections.flatMap((s) => [s, ""]),
  ].join("\n");
}

/** `/turing/<id>` (the Docusaurus route) -> this area's href. */
function legacyHref(href) {
  const id = href.replace(/^\/turing\/?/, "");
  if (!bySlug.has(slugOfId(id))) throw new Error(`landing page links to a missing page: ${href}`);
  return hrefOf(slugOfId(id));
}

// ── write ────────────────────────────────────────────────────────────────────

fs.rmSync(CONTENT, { recursive: true, force: true });
fs.rmSync(PUBLIC_IMG, { recursive: true, force: true });
for (const page of pages) {
  let text;
  let ext;
  if (page.slug === "") {
    text = convertLanding(page);
    ext = ".mdx";
  } else if (categories.has(page.slug) && /<DocCardList/.test(page.body)) {
    text = convertCategory(page);
    ext = ".mdx";
  } else {
    text = frontmatter(page) + convertBody(page) + "\n";
    ext = ".md";
  }
  const target = path.join(CONTENT, page.relPath.replace(/\.mdx?$/, ext));
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, text);
}
for (const img of images) {
  const from = path.join(SOURCE_ROOT, "static", "img", img);
  if (!fs.existsSync(from)) throw new Error(`missing image: static/img/${img}`);
  const to = path.join(PUBLIC_IMG, img);
  fs.mkdirSync(path.dirname(to), { recursive: true });
  fs.copyFileSync(from, to);
}
fs.writeFileSync(path.join(AREA, "src", "sidebar.generated.json"), `${JSON.stringify(sidebar, null, 2)}\n`);
console.log(`[port] ${pages.length} pages, ${images.size} images, ${unlisted.length} outside the sidebar`);
