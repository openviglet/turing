/**
 * Tests for `consume-changesets.mjs` (T1142).
 *
 * The script runs as a subprocess against a throwaway package and changeset
 * folder. What is pinned: a package's changesets land under its version at the
 * top of its CHANGELOG.md, the package leaves a changeset that names others too,
 * a changeset nobody is left in is deleted, another package's changeset is left
 * alone, and a publish with nothing pending writes nothing.
 *
 * `node --test scripts/__tests__` from the frontend root.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
import { deepStrictEqual, ok, strictEqual } from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { afterEach, beforeEach, describe, it } from "node:test";

const SCRIPT = join(dirname(dirname(fileURLToPath(import.meta.url))), "consume-changesets.mjs");
let root;
let pkg;
let changesets;

beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), "turing-changesets-"));
  pkg = join(root, "sdk");
  changesets = join(root, ".changeset");
  mkdirSync(pkg);
  mkdirSync(changesets);
  writeFileSync(join(pkg, "package.json"), '{ "name": "@x/sdk", "version": "2026.3.10" }\n');
  writeFileSync(join(changesets, "README.md"), "# Changesets\n");
});

afterEach(() => rmSync(root, { recursive: true, force: true }));

function consume() {
  const out = spawnSync(process.execPath, [SCRIPT, pkg, "--changesets", changesets], { encoding: "utf8" });
  strictEqual(out.status, 0, out.stderr);
  return out.stdout;
}

describe("consume-changesets", () => {
  it("writes the package's changesets under its version and removes them", () => {
    writeFileSync(join(changesets, "a-only.md"), '---\n"@x/sdk": minor\n---\n\nT1: read a document.\n\nWhole.\n');
    writeFileSync(join(changesets, "b-both.md"),
      '---\n"@x/sdk": patch\n"@x/react": patch\n---\n\nT2: a timeout.\n');
    writeFileSync(join(changesets, "c-other.md"), '---\n"@x/react": minor\n---\n\nT3: a hook.\n');

    const notes = consume();

    strictEqual(readFileSync(join(pkg, "CHANGELOG.md"), "utf8"),
      "# @x/sdk\n\n## 2026.3.10\n\n- T1: read a document.\n\n  Whole.\n\n- T2: a timeout.\n");
    ok(notes.startsWith("## 2026.3.10"));
    deepStrictEqual(readdirSync(changesets).sort(), ["README.md", "b-both.md", "c-other.md"]);
    strictEqual(readFileSync(join(changesets, "b-both.md"), "utf8"),
      '---\n"@x/react": patch\n---\n\nT2: a timeout.\n');
    ok(readFileSync(join(changesets, "c-other.md"), "utf8").includes('"@x/react": minor'));
  });

  it("puts a new release above the ones the changelog already has", () => {
    writeFileSync(join(pkg, "CHANGELOG.md"), "# @x/sdk\n\n## 2026.3.9\n\n- Older.\n");
    writeFileSync(join(changesets, "a.md"), '---\n"@x/sdk": patch\n---\n\nNewer.\n');

    consume();

    strictEqual(readFileSync(join(pkg, "CHANGELOG.md"), "utf8"),
      "# @x/sdk\n\n## 2026.3.10\n\n- Newer.\n\n## 2026.3.9\n\n- Older.\n");
  });

  it("writes nothing when no changeset names the package", () => {
    writeFileSync(join(changesets, "c-other.md"), '---\n"@x/react": minor\n---\n\nT3: a hook.\n');

    strictEqual(consume(), "");
    ok(!existsSync(join(pkg, "CHANGELOG.md")));
  });
});
