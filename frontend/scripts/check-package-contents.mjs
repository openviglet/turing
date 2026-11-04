#!/usr/bin/env node
/**
 * Asserts a publishable package ships what it declares and nothing it shouldn't
 * (T919, closing the class T915 opened).
 *
 * Run from a package directory, after its build:
 *   node ../../scripts/check-package-contents.mjs
 *
 * Two questions, both answered against the real tarball listing rather than
 * against the `files` field, because `files` is what was *meant* and the listing
 * is what would actually be published:
 *
 *   1. Does it carry anything that should not ship? T915 shipped 168 Storybook
 *      entries from react-sdk; T919 shipped 40 compiled tests from the CLI and 16
 *      from the flow DSL; T920 shipped 1.8 MB of source map across all five. Three
 *      different causes — a missing `tsconfig` exclude, a `files` glob one
 *      directory too wide, and an emit flag — so the check is on the output, where
 *      all three show up the same way.
 *
 *   2. Is every declared entry point actually in it? This is the failure mode the
 *      fix for (1) risks: narrowing `files` until it excludes `main`, `types` or a
 *      `bin`, which npm publishes without complaint and which breaks on install.
 *
 * `sizeBudget.contents` in package.json, all optional:
 *   {
 *     "forbid":  ["dist/test/"],   // extra posix substrings to reject
 *     "allow":   ["dist/src/testing/"],  // exempt paths a forbid pattern catches
 *     "rootOptionalPeers": { "<peer>": "<task id>: why" }  // optional peers the
 *                                      // root may still load, each with its task
 *   }
 *
 * T1214 / T1215 add two load checks on the built output: every JS entry point
 * imports in plain Node, and the root entry imports with the optional peers
 * (`peerDependenciesMeta`) made unresolvable.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
import { execFileSync, execSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { pathToFileURL } from "node:url";

const pkgDir = process.cwd();

function fail(msg) {
  console.error(`\n  package contents: ${msg}\n`);
  process.exit(1);
}

let pkg;
try {
  pkg = JSON.parse(readFileSync(join(pkgDir, "package.json"), "utf8"));
} catch {
  fail(`no readable package.json in ${pkgDir}`);
}

if (pkg.private) {
  console.log(`\n  ${pkg.name} is private — nothing is published, nothing to check.\n`);
  process.exit(0);
}

const cfg = pkg.sizeBudget?.contents ?? {};
const allow = cfg.allow ?? [];

/** Paths that must never reach a consumer, as posix substrings or suffixes. */
const FORBIDDEN = [
  "/__tests__/",
  "/__mocks__/",
  "/stories/",
  "dist/test/",
  ...(cfg.forbid ?? []),
];
// T920 — `.map` included: every map named `../src/*.ts` and carried no
// `sourcesContent`, while `files` published no `src`, so 1.8 MB of map across the
// five packages pointed at files the tarball did not contain. Making them resolve
// would mean publishing `src`, which reopens the tests-and-stories surface T915
// and T919 closed; so the coherent position is to ship compiled output only. The
// maps are still emitted into `dist` for anyone debugging the workspace.
const FORBIDDEN_SUFFIX = [
  ".test.js",
  ".test.d.ts",
  ".spec.js",
  ".stories.js",
  ".stories.d.ts",
  ".map",
];

// `npm pack --dry-run` writes nothing and reports exactly what publish would send.
let listing;
try {
  // A literal command string, not an args array: `npm` is `npm.cmd` on Windows,
  // which Node 26 refuses to spawn without a shell (EINVAL), and passing an args
  // array *with* a shell is the combination DEP0190 deprecates. Nothing here is
  // interpolated, so there is no argument to escape.
  const out = execSync("npm pack --dry-run --json", {
    cwd: pkgDir,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "ignore"],
  });
  listing = JSON.parse(out)[0];
} catch (error) {
  fail(`\`npm pack --dry-run\` failed — did the build run? (${error.message})`);
}

const files = (listing.files ?? []).map((f) => f.path.replaceAll("\\", "/"));
if (files.length === 0) fail("the tarball would be empty — did the build run?");

const violations = [];

// ── 1. Nothing that should not ship ──
const exempt = (p) => allow.some((a) => p.includes(a));
const shipped = files.filter(
  (p) =>
    !exempt(p) &&
    (FORBIDDEN.some((bad) => p.includes(bad)) || FORBIDDEN_SUFFIX.some((s) => p.endsWith(s))),
);
if (shipped.length > 0) {
  const shown = shipped.slice(0, 5).join(", ");
  violations.push(
    `${shipped.length} file(s) that must not be published would be — ${shown}` +
      (shipped.length > 5 ? ", …" : "") +
      " — narrow \"files\", or stop emitting them",
  );
}

// ── 2. Everything that must ship ──
const present = new Set(files);
const declared = [
  ["main", pkg.main],
  ["types", pkg.types],
  ...Object.entries(pkg.bin ?? {}).map(([k, v]) => [`bin.${k}`, v]),
].filter(([, value]) => typeof value === "string");

for (const [field, value] of declared) {
  const rel = value.replace(/^\.\//, "");
  if (!present.has(rel)) {
    violations.push(`"${field}" points at ${rel}, which "files" does not publish`);
  }
}

// ── 3. Every JS entry point loads in plain Node (T1214) ──
// A bundler resolves `from "./core/provider"`; Node's ESM loader does not, so a
// package built with `moduleResolution: "bundler"` installed and passed (1) and
// (2) while a Node script, a plain test runner or unbundled SSR could not import
// it. Each entry is imported here exactly as Node would, from the built output
// the tarball ships. `bin` targets are skipped: importing one runs the CLI.
const bins = new Set(Object.values(pkg.bin ?? {}).map((v) => v.replace(/^\.\//, "")));
const entries = new Set();
const addEntry = (target) => {
  if (typeof target !== "string") return;
  const rel = target.replace(/^\.\//, "");
  if (/\.(m?js|cjs)$/.test(rel) && !bins.has(rel)) entries.add(rel);
};
addEntry(pkg.main);
addEntry(pkg.module);
for (const value of Object.values(pkg.exports ?? {})) {
  if (typeof value === "string") addEntry(value);
  else if (value && typeof value === "object") {
    addEntry(value.import);
    addEntry(value.default);
  }
}
const loaded = [];
for (const rel of entries) {
  try {
    await import(pathToFileURL(join(pkgDir, rel)).href);
    loaded.push(rel);
  } catch (error) {
    violations.push(
      `${rel} does not load in plain Node (${error.code ?? error.name}: ${error.message.split("\n")[0]})` +
        " — emit relative specifiers with their .js extension (moduleResolution NodeNext)",
    );
  }
}

// ── 4. The root entry loads without the optional peers (T1215) ──
// `peerDependenciesMeta.<name>.optional` promises a consumer who never uses the
// feature can leave the peer out. The root barrel broke that promise when it
// re-exported a component importing the peer statically: every import of the
// package root then needed it. A child Node imports the root with every optional
// peer made unresolvable, so a static import of one fails here, while a dynamic
// import (loaded only on use, as the D2 renderer does) passes. Subpath entries are
// not checked: a subpath for the feature may need its peer.
//
// `sizeBudget.contents.rootOptionalPeers` names the peers a package's root is
// still allowed to load, each an acknowledged debt with its task id.
const optionalPeers = Object.entries(pkg.peerDependenciesMeta ?? {})
  .filter(([, meta]) => meta?.optional)
  .map(([name]) => name);
const rootAllowed = new Set(Object.keys(cfg.rootOptionalPeers ?? {}));
const hidden = optionalPeers.filter((name) => !rootAllowed.has(name));
const rootExport = pkg.exports?.["."];
const rootEntry = [
  typeof rootExport === "string" ? rootExport : (rootExport?.import ?? rootExport?.default),
  pkg.module,
  pkg.main,
].find((target) => typeof target === "string" && /\.(m?js|cjs)$/.test(target));
let peerFree = "—";
if (hidden.length > 0 && rootEntry) {
  const probe = `
    import { registerHooks } from "node:module";
    const hidden = new Set(JSON.parse(process.env.HIDDEN_PEERS));
    registerHooks({
      resolve(specifier, context, nextResolve) {
        const parts = specifier.split("/");
        const name = specifier.startsWith("@") ? parts.slice(0, 2).join("/") : parts[0];
        if (hidden.has(name)) {
          const error = new Error("Cannot find package '" + specifier + "' (optional peer, left out)");
          error.code = "ERR_MODULE_NOT_FOUND";
          throw error;
        }
        return nextResolve(specifier, context);
      },
    });
    await import(process.env.ROOT_ENTRY);
  `;
  try {
    execFileSync(process.execPath, ["--input-type=module", "-e", probe], {
      cwd: pkgDir,
      env: {
        ...process.env,
        HIDDEN_PEERS: JSON.stringify(hidden),
        ROOT_ENTRY: pathToFileURL(join(pkgDir, rootEntry.replace(/^\.\//, ""))).href,
      },
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    });
    peerFree = `yes, without ${hidden.join(", ")}`;
  } catch (error) {
    const reason =
      // `[^'"]` skips Node's echo of the probe's own source line, which spells the
      // same words around a `" + specifier + "` concatenation.
      /Cannot find package '[^'"]+' \(optional peer, left out\)/.exec(error.stderr ?? "")?.[0] ??
      (error.stderr || error.message).split("\n").find((line) => line.trim()) ??
      "unknown";
    violations.push(
      `the root entry ${rootEntry} needs an optional peer (${reason.trim()})` +
        " — import it lazily, or export the component from its subpath only",
    );
    peerFree = "no";
  }
}

// ── Report ──
const kb = (n) => `${(n / 1024).toFixed(2)} kB`;
console.log(
  `\n  ${pkg.name} — publish contents\n\n` +
    `    entries        ${files.length}\n` +
    `    unpacked       ${kb(listing.unpackedSize ?? 0)}\n` +
    `    entry points   ${declared.map(([f]) => f).join(", ") || "—"}\n` +
    `    node loads     ${loaded.length} of ${entries.size}\n` +
    `    root w/o opt.  ${peerFree}\n`,
);

if (violations.length > 0) {
  console.error("  package contents rejected:");
  for (const v of violations) console.error(`    - ${v}`);
  console.error("");
  process.exit(1);
}

console.log("  ✓ ships every entry point, loads each in plain Node, and no test, story or map file\n");
