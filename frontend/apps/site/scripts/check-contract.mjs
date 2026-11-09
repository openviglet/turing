#!/usr/bin/env node
/**
 * T1273: the deterministic half of the viglet.org hub contract, checked on the
 * BUILT output (dist/ with dist/docs/ inside it), so it sees what a visitor and
 * a crawler see rather than source literals. No network, no browser: the
 * browser half is e2e/ (Playwright), and external links are its own
 * retry-bounded, inconclusive-on-network-error project.
 *
 *   1. Full product names (T1268): no bare Turing / Dumont / Shio in visible
 *      text, outside the named technical exceptions below.
 *   2. Page identity (T1268, T1272): every marketing route has a title, a
 *      canonical on the public origin, og:title/og:url that agree with them,
 *      and JSON-LD that parses.
 *   3. Journeys (T1270, T1271): every internal link on a marketing page and in
 *      llms.txt resolves to a built page, and every #anchor exists on it.
 *
 * Usage: node scripts/check-contract.mjs [distDir]   (default: ./dist)
 */
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

const ORIGIN = "https://turing.viglet.org";
const dist = process.argv[2] ?? fileURLToPath(new URL("../dist", import.meta.url));
// Built by other jobs into the same Pages artifact; not this site's pages.
const FOREIGN = ["react-sdk", "marketplace", "playground"];

/**
 * Bare names that are deliberate, each with the reason. Matched against the
 * text around the hit, so an exception never hides a different sentence.
 */
const NAME_EXCEPTIONS = [
  // The WordPress plugin's own UI labels, quoted verbatim by its docs (T1274 renames them).
  [/Turing search block|Turing search\b|Turing URL/, "WordPress plugin UI label (T1274)"],
  // Code identifiers and log tags rendered as prose by a component.
  [/\[Turing\]/, "SDK log tag"],
];

const failures = [];
const fail = (where, what) => failures.push(`${where}: ${what}`);

function htmlFiles(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) {
      if (dir === dist && FOREIGN.includes(name)) continue;
      htmlFiles(p, out);
    } else if (name.endsWith(".html")) out.push(p);
  }
  return out;
}

/** The URL path a built file is served at ("/docs/rag/"). */
function routeOf(file) {
  const rel = relative(dist, file).split(sep).join("/");
  return "/" + rel.replace(/(^|\/)index\.html$/, "$1").replace(/\.html$/, "");
}

function visibleText(html) {
  return html
    .replace(/<script[\s\S]*?<\/script>/g, " ")
    .replace(/<style[\s\S]*?<\/style>/g, " ")
    .replace(/<pre\b[\s\S]*?<\/pre>/g, " ")
    .replace(/<code\b[\s\S]*?<\/code>/g, " ")
    // Quote-aware: a copy button's data-code attribute holds raw source, ">" included.
    .replace(/<(?:[^>"']|"[^"]*"|'[^']*')*>/g, " ")
    .replace(/&#x27;|&#39;|&rsquo;|&apos;/g, "'")
    .replace(/&amp;/g, "&")
    .replace(/\s+/g, " ");
}

const ids = new Map();
function idsOf(file) {
  if (!ids.has(file)) {
    const html = readFileSync(file, "utf-8");
    ids.set(file, new Set([...html.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1])));
  }
  return ids.get(file);
}

/** The built file behind a site path, or null. */
function fileFor(path) {
  const clean = decodeURIComponent(path.split("#")[0].split("?")[0]);
  for (const candidate of [join(dist, clean, "index.html"), join(dist, clean), join(dist, `${clean}.html`)])
    if (existsSync(candidate) && statSync(candidate).isFile()) return candidate;
  return null;
}

function checkLink(where, href) {
  let url;
  try {
    // Relative to the page it sits on: "#demo" on /aem-search/ is /aem-search/#demo.
    url = new URL(href, `${ORIGIN}${where.startsWith("/") ? where : "/"}`);
  } catch {
    return fail(where, `unparseable link ${href}`);
  }
  if (url.origin !== ORIGIN) return;
  const top = url.pathname.split("/")[1];
  if (FOREIGN.includes(top) || url.pathname === "/wp-demo/") return; // other jobs' output, or a redirect
  const file = fileFor(url.pathname);
  if (!file) return fail(where, `link to ${url.pathname} has no built page`);
  const anchor = decodeURIComponent(url.hash.slice(1));
  if (anchor && file.endsWith(".html") && !idsOf(file).has(anchor))
    fail(where, `link to ${url.pathname}#${anchor}: no element has that id`);
}

if (!existsSync(join(dist, "index.html"))) {
  console.error(`[check-contract] no built site at ${dist}; run the build first`);
  process.exit(2);
}

const pages = htmlFiles(dist);
let marketing = 0;
for (const file of pages) {
  const route = routeOf(file);
  const html = readFileSync(file, "utf-8");
  const isDocs = route.startsWith("/docs/");
  if (/^\/(404|docs\/404)\b/.test(route)) continue;
  // A redirect stub (/aem/, /wp-demo/) is not a page: its target is checked instead.
  if (/<meta[^>]+http-equiv="refresh"/i.test(html)) continue;

  // 1. Full product names.
  const text = visibleText(html);
  for (const m of text.matchAll(/(?<!Viglet )\b(Turing|Dumont|Shio)\b/g)) {
    const around = text.slice(Math.max(0, m.index - 40), m.index + 40);
    if (!NAME_EXCEPTIONS.some(([re]) => re.test(around))) fail(route, `bare product name: "...${around.trim()}..."`);
  }

  if (isDocs) continue; // docs identity and links: Starlight + docs/scripts/check-links.mjs
  marketing++;

  // 2. Page identity.
  const title = html.match(/<title>([^<]*)<\/title>/)?.[1]?.trim();
  const meta = (attr, key) =>
    html.match(new RegExp(`<meta[^>]+${attr}="${key}"[^>]+content="([^"]*)"`))?.[1] ??
    html.match(new RegExp(`<meta[^>]+content="([^"]*)"[^>]+${attr}="${key}"`))?.[1];
  const canonical = html.match(/<link[^>]+rel="canonical"[^>]+href="([^"]+)"/)?.[1];
  if (!title) fail(route, "no <title>");
  if (canonical !== `${ORIGIN}${route}`) fail(route, `canonical is ${canonical}, expected ${ORIGIN}${route}`);
  if (meta("property", "og:title") !== title) fail(route, `og:title differs from <title>`);
  if (meta("property", "og:url") !== canonical) fail(route, `og:url differs from the canonical`);
  if (!meta("name", "description")) fail(route, "no meta description");
  for (const m of html.matchAll(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/g)) {
    try {
      JSON.parse(m[1]);
    } catch (e) {
      fail(route, `JSON-LD does not parse: ${e.message}`);
    }
  }

  // 3. Journeys: every internal link and anchor.
  for (const m of html.matchAll(/<a\s[^>]*href="([^"]+)"/g)) checkLink(route, m[1].replace(/&amp;/g, "&"));
}

for (const m of readFileSync(join(dist, "llms.txt"), "utf-8").matchAll(/\]\((https?:[^)\s]+)\)/g))
  checkLink("llms.txt", m[1]);

const summary = `${pages.length} pages (${marketing} marketing), ${failures.length} problem(s)`;
if (failures.length) {
  console.error(`[check-contract] ${summary}:`);
  for (const f of failures) console.error(`  ${f}`);
  process.exit(1);
}
console.log(`[check-contract] ${summary}: names, identity, links and anchors hold`);
