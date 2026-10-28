/**
 * Tests for `bump-patch.mjs`.
 *
 * The script is run as a subprocess against a throwaway package, with
 * `--published` standing in for npm, so the suite needs no network. What is
 * pinned: the version moves past the highest published patch, an unpublished
 * local bump is kept, another `major.minor` line is ignored, and the rest of
 * package.json is left byte for byte.
 *
 * `node --test scripts/__tests__` from the frontend root.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
import { strictEqual } from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { after, describe, it } from "node:test";

const SCRIPT = join(dirname(dirname(fileURLToPath(import.meta.url))), "bump-patch.mjs");
const dir = mkdtempSync(join(tmpdir(), "turing-bump-"));
const file = join(dir, "package.json");

after(() => rmSync(dir, { recursive: true, force: true }));

function bump(version, published) {
  writeFileSync(file, `{\n  "name": "@x/sdk",\n  "version": "${version}",\n  "type": "module"\n}\n`);
  const out = spawnSync(process.execPath, [SCRIPT, dir, "--published", published], { encoding: "utf8" });
  strictEqual(out.status, 0, out.stderr);
  return out.stdout.trim();
}

describe("bump-patch", () => {
  it("moves past the highest published patch, even when package.json is behind npm", () => {
    strictEqual(bump("2026.3.9", "2026.3.8,2026.3.10,2026.3.9"), "2026.3.11");
    strictEqual(JSON.parse(readFileSync(file, "utf8")).version, "2026.3.11");
  });

  it("bumps the version npm serves when package.json matches it", () => {
    strictEqual(bump("2026.3.10", "2026.3.10"), "2026.3.11");
  });

  it("keeps a local bump that has not shipped yet", () => {
    strictEqual(bump("2026.3.12", "2026.3.10"), "2026.3.12");
  });

  it("ignores another major.minor line and prereleases", () => {
    strictEqual(bump("2026.3.4", "2026.3.4,2026.4.0,2026.3.9-beta.1"), "2026.3.5");
  });

  it("publishes the local version of a package npm does not know", () => {
    strictEqual(bump("2026.3.1", ""), "2026.3.1");
  });

  it("leaves the rest of package.json untouched", () => {
    bump("2026.3.10", "2026.3.10");
    strictEqual(
      readFileSync(file, "utf8"),
      `{\n  "name": "@x/sdk",\n  "version": "2026.3.11",\n  "type": "module"\n}\n`,
    );
  });
});
