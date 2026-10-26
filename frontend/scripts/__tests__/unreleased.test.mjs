/**
 * Tests for `check-unreleased.mjs` (T1142).
 *
 * The script is run as a subprocess inside a throwaway git repository shaped like
 * the frontend root, with `--published` standing in for npm, so the suite needs no
 * network. What is pinned: a commit under `src` after the publish instant counts, one
 * before it or outside `src` does not, and a private package is never listed.
 *
 * `node --test scripts/__tests__` from the frontend root.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
import { match, ok, strictEqual } from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { after, describe, it } from "node:test";

const SCRIPT = join(dirname(dirname(fileURLToPath(import.meta.url))), "check-unreleased.mjs");
const root = mkdtempSync(join(tmpdir(), "turing-unreleased-"));

function write(relative, contents) {
  const full = join(root, relative);
  mkdirSync(dirname(full), { recursive: true });
  writeFileSync(full, contents);
}

function git(args, date) {
  const env = { ...process.env, GIT_AUTHOR_DATE: date, GIT_COMMITTER_DATE: date };
  const out = spawnSync("git", args, { cwd: root, encoding: "utf8", env });
  strictEqual(out.status, 0, out.stderr);
}

function commit(relative, date) {
  write(relative, date);
  git(["add", "-A"], date);
  git(["-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", relative], date);
}

after(() => rmSync(root, { recursive: true, force: true }));

describe("check-unreleased", () => {
  git(["init", "-q"], "2026-01-01T00:00:00Z");
  write("packages/sdk/package.json", JSON.stringify({ name: "@x/sdk", version: "1.0.0" }));
  write("packages/types/package.json", JSON.stringify({ name: "@x/types", version: "1.0.0", private: true }));
  commit("packages/sdk/src/before.ts", "2026-01-02T00:00:00Z");
  commit("packages/sdk/src/after-1.ts", "2026-03-02T00:00:00Z");
  commit("packages/sdk/src/after-2.ts", "2026-03-03T00:00:00Z");
  commit("packages/sdk/README.md", "2026-03-04T00:00:00Z");
  commit("packages/types/src/index.ts", "2026-03-05T00:00:00Z");

  function run() {
    return spawnSync(process.execPath, [SCRIPT, "--published", "@x/sdk=2026-02-01T00:00:00Z"], {
      cwd: root,
      encoding: "utf8",
    });
  }

  it("counts only commits under src after the publish instant", () => {
    const out = run();
    strictEqual(out.status, 0, out.stderr);
    match(out.stdout, /@x\/sdk\s+2\s/);
    match(out.stdout, /unreleased source: @x\/sdk/);
  });

  it("never lists a private package", () => {
    ok(!run().stdout.includes("@x/types"));
  });

  it("reads 0 when the publish is newer than every commit", () => {
    const out = spawnSync(process.execPath, [SCRIPT, "--published", "@x/sdk=2026-04-01T00:00:00Z"], {
      cwd: root,
      encoding: "utf8",
    });
    match(out.stdout, /@x\/sdk\s+0\s/);
    match(out.stdout, /Nothing waits for a release/);
  });
});
