#!/usr/bin/env node
/**
 * T1161 — the gate on the built area: every internal href and src under /docs/ in the
 * built pages must name a file the build wrote, and every in-page `#anchor` on a link
 * to another page must exist on that page. Run after `astro build`; exits non-zero
 * with the list, so a broken link or a missing image fails the build instead of the
 * reader.
 */
import fs from "node:fs";
import path from "node:path";
import url from "node:url";

import { DOCS_BASE, SITE_ORIGIN } from "../site.mjs";

const HERE = path.dirname(url.fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, "..", "..", "site", "dist", "docs");
const BASE = DOCS_BASE;

function htmlFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const full = path.join(dir, e.name);
    return e.isDirectory() ? htmlFiles(full) : e.name.endsWith(".html") ? [full] : [];
  });
}

function targetFile(pathname) {
  const rel = decodeURIComponent(pathname.slice(BASE.length)).replace(/^\//, "");
  const direct = path.join(OUT, rel);
  if (rel === "" || rel.endsWith("/")) return path.join(direct, "index.html");
  return direct;
}

const ids = new Map();
function idsOf(file) {
  if (!ids.has(file)) {
    const html = fs.readFileSync(file, "utf8");
    ids.set(file, new Set([...html.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1])));
  }
  return ids.get(file);
}

const broken = [];
for (const file of htmlFiles(OUT)) {
  const html = fs.readFileSync(file, "utf8");
  for (const m of html.matchAll(/\s(?:href|src)="([^"]+)"/g)) {
    const ref = m[1];
    // A link written as "/docs/<page>/" that astro.config.mjs did not rebase points at
    // the origin's root, outside the site, and 404s in production alone.
    if (ref.startsWith("/docs/") || ref === "/docs") {
      broken.push(`${path.relative(OUT, file)}: ${ref} (missing the ${BASE} base)`);
      continue;
    }
    if (!ref.startsWith(`${BASE}/`) && ref !== BASE) continue;
    const [pathname, hash] = ref.split("#");
    const target = targetFile(pathname.split("?")[0]);
    if (!fs.existsSync(target)) {
      broken.push(`${path.relative(OUT, file)}: ${ref} (no ${path.relative(OUT, target)})`);
    } else if (hash && target.endsWith(".html") && !idsOf(target).has(decodeURIComponent(hash))) {
      broken.push(`${path.relative(OUT, file)}: ${ref} (no #${hash})`);
    }
  }
}

// T1163 — every redirect-map target on this area must be a page the build wrote.
const SITE = SITE_ORIGIN;
const redirects = JSON.parse(fs.readFileSync(path.resolve(HERE, "..", "redirects.json"), "utf8"));
for (const { from, to } of redirects) {
  if (!to.startsWith(`${SITE}${BASE}/`)) continue;
  const target = targetFile(to.slice(SITE.length));
  if (!fs.existsSync(target)) broken.push(`redirects.json: ${from} -> ${to} (no ${path.relative(OUT, target)})`);
}

if (broken.length) {
  console.error(`[check-links] ${broken.length} broken internal link(s):\n  ${broken.join("\n  ")}`);
  process.exit(1);
}
console.log("[check-links] every internal link and image resolves");
