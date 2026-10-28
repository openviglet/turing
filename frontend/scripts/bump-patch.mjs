#!/usr/bin/env node
/**
 * Sets a package's version to the next patch npm has not seen, for the publish
 * workflows.
 *
 * Run from the frontend root:
 *   node scripts/bump-patch.mjs packages/sdk
 *   node scripts/bump-patch.mjs packages/sdk --published 2026.3.9,2026.3.10
 *
 * `npm version patch` bumps from package.json alone, so a version that npm
 * already holds but the repo never recorded (a publish from another checkout, a
 * bump that was not committed) makes the publish fail with "You cannot publish
 * over the previously published versions". This reads the versions npm holds
 * instead:
 *
 * - Only versions on the package's own `major.minor` line count, so a release
 *   branch never jumps to a newer line published from another branch.
 * - A local version above every published one is kept as is: someone bumped it
 *   on purpose and it has not shipped yet.
 * - Otherwise the version becomes the highest published patch plus one.
 *
 * `--published <csv>` replaces the npm lookup, so the suite runs offline. A
 * package npm does not know publishes its local version. Prints the version it
 * settled on and writes package.json only when that differs.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
import { spawnSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const STABLE = /^(\d+)\.(\d+)\.(\d+)$/;

function fail(message) {
  console.error(`bump-patch: ${message}`);
  process.exit(2);
}

function parse(version) {
  const m = STABLE.exec(version);
  return m ? m.slice(1).map(Number) : null;
}

/** The versions npm holds for `name`; empty when npm does not know the package. */
function npmVersions(name) {
  // One command string through the shell: npm is a .cmd on Windows. `name` comes
  // from our own package.json, never from input.
  const out = spawnSync(`npm view ${name} versions --json`, { encoding: "utf8", shell: true });
  if (out.status !== 0) {
    if (/E404/.test(out.stdout + out.stderr)) return [];
    fail(`npm view ${name} failed: ${out.stderr.trim()}`);
  }
  const json = JSON.parse(out.stdout);
  return Array.isArray(json) ? json : [json];
}

const args = process.argv.slice(2);
const dir = args[0];
if (!dir || dir.startsWith("--")) fail("usage: bump-patch.mjs <package dir> [--published <csv>]");
const at = args.indexOf("--published");
const given = at >= 0 ? (args[at + 1] ?? "").split(",").filter(Boolean) : null;

const file = join(dir, "package.json");
const text = readFileSync(file, "utf8");
const pkg = JSON.parse(text);
const local = parse(pkg.version);
if (!local) fail(`${pkg.name} has a version this cannot bump: ${pkg.version}`);

const [major, minor] = local;
const patches = (given ?? npmVersions(pkg.name))
  .map(parse)
  .filter((v) => v && v[0] === major && v[1] === minor)
  .map((v) => v[2]);

const highest = patches.length ? Math.max(...patches) : null;
const patch = highest === null || local[2] > highest ? local[2] : highest + 1;
const next = `${major}.${minor}.${patch}`;

if (next !== pkg.version) {
  // Replace the version field in place, so the file keeps its own formatting.
  writeFileSync(file, text.replace(`"version": "${pkg.version}"`, `"version": "${next}"`));
}
console.log(next);
