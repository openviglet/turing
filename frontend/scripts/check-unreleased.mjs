#!/usr/bin/env node
/**
 * Counts, per published package, the commits under its `src` that npm has not
 * shipped yet (T1142).
 *
 * Run from the frontend root:
 *   node scripts/check-unreleased.mjs                 # report
 *   node scripts/check-unreleased.mjs --published @viglet/turing-sdk=2026-08-24T18:35:15Z
 *
 * The SDKs ship by a manual dispatch (CI is manual-dispatch on purpose), so a
 * release only happens when someone remembers one. `@viglet/turing-sdk` 2026.3.9
 * sat on npm for two months while 16 commits — `postNlFilters` among them —
 * reached no consumer, and nothing in the repo said so. This prints that number
 * for every package before a release, and it should read 0 right after one.
 *
 * The reference point is the publish time npm records for the `latest` dist-tag:
 * the tarballs carry no `gitHead` and no release tag exists, so time is the one
 * anchor every published version has. A commit is counted when its committer date
 * is after that instant and it touches `packages/<dir>/src`.
 *
 * `--published <name>=<iso>` replaces the npm lookup for one package, so the
 * suite runs offline. A package npm does not know is reported as unpublished.
 * The script never fails the build: it is a count to read, not a gate.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
import { spawnSync } from "node:child_process";
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";

const root = process.cwd();
const PACKAGES = "packages";

/** `--published name=iso` pairs from the command line. */
function overrides(argv) {
  const map = new Map();
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] !== "--published") continue;
    const pair = argv[++i] ?? "";
    const eq = pair.lastIndexOf("=");
    if (eq <= 0) {
      console.error(`check-unreleased: --published wants <name>=<iso>, got '${pair}'`);
      process.exit(2);
    }
    map.set(pair.slice(0, eq), pair.slice(eq + 1));
  }
  return map;
}

/** The workspace packages that npm publishes: everything under packages/ not marked private. */
function publishable() {
  const dir = join(root, PACKAGES);
  if (!existsSync(dir)) {
    console.error(`check-unreleased: no ${PACKAGES}/ here. Run this from the frontend root.`);
    process.exit(2);
  }
  return readdirSync(dir, { withFileTypes: true })
    .filter((d) => d.isDirectory() && existsSync(join(dir, d.name, "package.json")))
    .map((d) => ({ dir: d.name, pkg: JSON.parse(readFileSync(join(dir, d.name, "package.json"), "utf8")) }))
    .filter(({ pkg }) => !pkg.private)
    .map(({ dir: name, pkg }) => ({ dir: name, name: pkg.name, local: pkg.version }));
}

/** The `latest` version npm holds and when it was published, or null when npm does not know the package. */
function npmLatest(name) {
  // One command string through the shell: npm is a .cmd on Windows, and Node
  // deprecates passing an args array alongside `shell`. `name` comes from our own
  // package.json files, never from input.
  const out = spawnSync(`npm view ${name} dist-tags.latest time --json`, { encoding: "utf8", shell: true });
  if (out.status !== 0) return null;
  try {
    const json = JSON.parse(out.stdout);
    const version = json["dist-tags.latest"];
    return version ? { version, at: json.time?.[version] } : null;
  } catch {
    return null;
  }
}

/** Commits after `since` that touch the package's sources. */
function commitsSince(dir, since) {
  const out = spawnSync("git", ["log", `--since=${since}`, "--format=%h", "--", `${PACKAGES}/${dir}/src`], {
    cwd: root,
    encoding: "utf8",
  });
  if (out.status !== 0) {
    console.error(`check-unreleased: git log failed: ${out.stderr.trim()}`);
    process.exit(2);
  }
  return out.stdout.split(/\r?\n/).filter(Boolean).length;
}

const given = overrides(process.argv.slice(2));
const rows = publishable().map((p) => {
  const at = given.get(p.name);
  const latest = at ? { version: "(given)", at } : npmLatest(p.name);
  if (!latest?.at) return { ...p, published: "unpublished", unreleased: null };
  return { ...p, published: `${latest.version} @ ${latest.at.slice(0, 10)}`, unreleased: commitsSince(p.dir, latest.at) };
});

const width = Math.max(...rows.map((r) => r.name.length));
console.log("check-unreleased: commits under src since the version npm serves as latest\n");
for (const r of rows) {
  const count = r.unreleased === null ? "-" : String(r.unreleased);
  console.log(`  ${r.name.padEnd(width)}  ${count.padStart(4)}  npm ${r.published}, local ${r.local}`);
}
const waiting = rows.filter((r) => r.unreleased > 0);
console.log(
  waiting.length === 0
    ? "\n  Nothing waits for a release."
    : `\n  ${waiting.length} package(s) have unreleased source: ${waiting.map((r) => r.name).join(", ")}`,
);
