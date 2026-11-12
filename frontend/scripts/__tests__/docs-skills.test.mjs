/**
 * The docs page is written with a skill an agent actually reaches.
 *
 * The defect this holds is a missing route, not a missing rule: turing-roadmap-docs told an
 * agent *where* a shipped feature's page goes, and nothing told it *how* the page is written, so
 * each ship appended a paragraph to the closest page. A skill nobody is pointed at is the same
 * defect with a file added. What is pinned: the docs step names the skill, the agents.md index
 * lists both docs skills, the write skill applies the shared method rather than restating it and
 * names the lint and every gate, and the sync skill's report still exists.
 *
 * `node --test scripts/__tests__` from the frontend root.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "..");
const read = (path) => readFileSync(join(ROOT, path), "utf8");
const WRITE = ".claude/skills/turing-docs-write/SKILL.md";
const SYNC = ".claude/skills/turing-docs-sync/SKILL.md";

test("the turing-docs-write skill exists and applies the shared docs-writing method", () => {
  assert.ok(existsSync(join(ROOT, WRITE)), `${WRITE} is missing; regenerate it with docs-setup`);
  const skill = read(WRITE);
  assert.match(skill, /^name: turing-docs-write$/m);
  assert.match(skill, /Apply `docs-writing` first/, "the skill must point at the shared method, not restate it");
  assert.ok(skill.split("\n").length <= 120, "the generated skill stays under 120 lines (docs-setup's budget)");
});

test("the write skill names the docs-style lint and every docs gate", () => {
  const skill = read(WRITE);
  for (const gate of [
    "scripts/docs-style.test.mjs",
    "pnpm --filter turing-docs test",
    "pnpm --filter turing-docs build",
    "pnpm build:site",
  ]) {
    assert.ok(skill.includes(gate), `${WRITE} does not name ${gate}`);
  }
  assert.ok(existsSync(join(ROOT, "frontend/apps/docs/scripts/docs-style.test.mjs")), "the lint the skill names is gone");
  assert.match(read("frontend/apps/docs/package.json"), /"test":\s*"node --test/, "turing-docs lost its test script");
});

test("the step that writes a shipped feature's page names the skill", () => {
  const step = read(".claude/skills/turing-roadmap-docs/SKILL.md").match(
    /3\. \*\*Write for the user, not the commit\.\*\*[\s\S]*?(?=\n\d\. \*\*)/,
  );
  assert.ok(step, "turing-roadmap-docs lost its step 3 (Write for the user, not the commit)");
  assert.match(step[0], /`turing-docs-write`/, "step 3 of turing-roadmap-docs must name the turing-docs-write skill");
});

test("the sync skill exists and its reverse report is where it says", () => {
  assert.ok(existsSync(join(ROOT, SYNC)), `${SYNC} is missing`);
  const skill = read(SYNC);
  assert.match(skill, /^name: turing-docs-sync$/m);
  assert.ok(existsSync(join(ROOT, ".claude/skills/turing-docs-sync/docs-sync-report.mjs")), "the report the skill runs is gone");
  assert.ok(skill.includes("turing-docs-write"), "the sync skill must route fixes through turing-docs-write");
});

test("the agents.md index lists both docs skills", () => {
  const index = read("agents.md");
  assert.match(index, /\| `turing-docs-write` \|/);
  assert.match(index, /\| `turing-docs-sync` \|/);
});
