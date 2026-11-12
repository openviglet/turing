---
name: turing-docs-sync
description: Validate that Viglet Turing's end-user documentation (frontend/apps/docs, served at turing.viglet.org/docs) is in sync with the code, and report or file every drift. Runs the docs gates (docs-style lint, astro build + check-links), a reverse code-to-docs report (turing.* properties, @ConfigurationProperties prefixes, turing CLI commands, public /api/sn and /api/v2 endpoints no page names), sidebar membership, source-level link/anchor resolution and COVERAGE.md link resolution, then a judgment pass over what no instrument reads (defaults, limits, behaviour described in prose, pages that claim to be complete, features shipped since a page was last touched). Use when asked to check, audit, validate or sync the docs against the code, before a release, after a block completes, or when a docs page is suspected stale. Trigger words: docs sync, documentação, frontend/apps/docs, stale docs, COVERAGE.md, configuration reference, docs drift.
---

# Is frontend/apps/docs in sync with the code?

The pages live in `frontend/apps/docs/src/content/docs/`, with `COVERAGE.md` one level up
(outside the collection). Deciding where a shipped feature is documented and recording its
COVERAGE verdict is `turing-roadmap-docs`'s job; writing the page is `turing-docs-write`'s.
**This skill only measures**, then fixes or files what it measured.

Run from the repo root. Three passes, in this order, because each is cheaper and more certain
than the next.

## Pass 1: the gates (docs → docs, mechanical)

```bash
cd frontend
pnpm --filter turing-docs test    > "$TEMP/turing-docs-test.log"  2>&1   # docs-style lint
pnpm --filter turing-docs build   > "$TEMP/turing-docs-build.log" 2>&1   # astro build + check-links
```

Logs go outside the tree (`*.log` is not git-ignored here). Read them; do not pipe a gate into `grep` and trust `grep`'s exit code. A red here is a
confirmed defect with the page and line printed: start with it. `check-links.mjs` reads the
**built** HTML, so it is the authority on links; Pass 2's link check is a fast pre-build
approximation of it.

## Pass 2: the reverse report (code → docs, mechanical)

```bash
node .claude/skills/turing-docs-sync/docs-sync-report.mjs > "$TEMP/turing-docs-sync.log"   # add --json or --strict
```

The gates are blind in this direction: something the product **has** and no page mentions
passes them all. The report reads the code directly and prints:

- **declared, named on no page**, per population:
  - `turing.*` keys read through `@Value("${turing...}")` in `turing-app` (relaxed binding:
    `foo-bar`, `fooBar`, `foo_bar` count as one);
  - `@ConfigurationProperties` prefixes (a prefix named once is enough; its fields are
    judged in Pass 3);
  - `turing <command>` subcommands from `frontend/packages/cli/src/cli.ts`;
  - public endpoints under `/api/sn` and `/api/v2` (**advisory**: many are console-only or
    documented by family, so a miss is a question, not a finding);
- **pages missing from** `src/sidebar.generated.json`, and sidebar slugs with no page;
- **hand-typed `/docs/...` links** whose page, heading or `<a id>` anchor, or `public/` file
  does not exist;
- **`COVERAGE.md` links** (`./src/content/docs/<page>.md#anchor`) that no longer resolve:
  `check-links.mjs` never reads COVERAGE, so a renamed heading silently breaks its row.

`--strict` exits 1 on any non-advisory gap; the default is 0, because some gaps are
deliberate. Many `turing.*` misses are internal tuning knobs (cron expressions, cleanup
delays, build metadata): those are 🔒, not drift.

Blocks: `roadkeep block list` against `COVERAGE.md`, as `turing-roadmap-docs` § "Seeing the
current state" describes. A label the matrix does not spell is a question, not a verdict.

## Pass 3: what only a reader can judge

No instrument reads meaning. Spend the reading where drift is likely, not across every page:

1. **What shipped since the page was last touched.** `git log -1 --format=%cs -- <page>` gives
   the page's date; `git log --since=<date> --oneline -- <the code the page documents>` lists
   the commits it may not know about. A `feat:` there with no COVERAGE row, or a row pointing
   at an anchor that does not describe it, is drift.
2. **Pages that claim completeness.** "Every property below…", "all endpoints", "the N
   tools": a completeness claim is falsified by a single gap from Pass 2.
   `configuration-reference.md` and `rest-api.md` are the standing examples.
3. **Defaults and limits in tables.** For each `| key | default |` row, compare with the
   `@Value("${key:default}")` / `@ConfigurationProperties` field or
   `turing-app/src/main/resources/application.yaml`. A name check passes a wrong default.
4. **Behaviour in prose.** Status codes, response shapes, CLI flags, console steps. Check the
   controller, the command's `turing help`, or the React page, not memory.

## Classify, then act

For every gap, decide one of three, and say which:

| Verdict | Action |
|---|---|
| **Drift**: the page is wrong or silent about something a user would do differently | Fix it if the user asked for fixes; otherwise **file a task** |
| **Deliberately internal** (an internal knob, an admin-only endpoint, test-only property) | A 🔒 row in `COVERAGE.md` if the cluster has none; nothing else |
| **False positive** of the report | Note it; if it recurs, fix the report rather than re-judging it each run |

**Filing:** prove the gap with a command first (a grep with zero hits, a `turing help`, a
controller line), never from reading alone. Search the backlog before adding
(`roadkeep list` / `section find`), then `roadkeep add` under the docs-parity block
(Block **AE** / **U**, per `turing-roadmap-docs`), one task per page or per coherent cluster,
not one per missing key. Name the evidence in the line.

**Fixing:** write the page with `turing-docs-write`, fold into the closest page, update the
COVERAGE row in the same change. Then re-run Pass 1 and Pass 2; a fix that leaves its gap in
the report is not done.

## Report back

One table per pass: what was checked (counts, printed by each tool), what failed, the verdict
per gap, and what was fixed or filed (task ids). State plainly what was **not** checked (for
example, Pass 3 sampled four pages, or the build was skipped because `node_modules` was
locked).
