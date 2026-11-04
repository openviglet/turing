#!/usr/bin/env node
/**
 * Moves the pending changesets of one package into its CHANGELOG.md, for the
 * publish workflows (T1142).
 *
 * Run from the frontend root, after bump-patch.mjs has set the version:
 *   node scripts/consume-changesets.mjs packages/sdk
 *   node scripts/consume-changesets.mjs packages/sdk --changesets .changeset
 *
 * The workflows do not run `changeset version`: it computes each package's next
 * version from the bump levels, where bump-patch.mjs picks the next patch npm has
 * not seen, and it versions every package a changeset names while a workflow
 * publishes one. So the release notes come from here instead:
 *
 * - Every changeset that names the package adds its summary, as one entry,
 *   under a `## <version>` heading at the top of the package's CHANGELOG.md.
 * - The package's line leaves the changeset's frontmatter, and a changeset no
 *   package is left in is deleted, so the next publish never repeats an entry.
 * - With no changeset for the package nothing is written, and the version shows
 *   up in the changelog only with what it changed.
 *
 * Prints the notes it wrote (empty when none), for the workflow summary.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
import { existsSync, readdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const FRONTMATTER = /^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$/;
const ENTRY = /^\s*["']?([^"':]+)["']?\s*:\s*(major|minor|patch)\s*$/;

function fail(message) {
  console.error(`consume-changesets: ${message}`);
  process.exit(2);
}

/** A changeset as its package lines and its summary; null when it is not one. */
function parse(text) {
  const m = FRONTMATTER.exec(text);
  if (!m) return null;
  const lines = m[1].split(/\r?\n/).filter((line) => line.trim());
  const packages = lines.map((line) => ENTRY.exec(line)?.[1]?.trim());
  if (packages.some((name) => !name)) return null;
  return { lines, packages, summary: m[2].trim() };
}

/** The summary as one list entry: its first line after the dash, the rest indented. */
function entry(summary) {
  const [first, ...rest] = summary.split(/\r?\n/);
  return [`- ${first}`, ...rest.map((line) => (line.trim() ? `  ${line}` : ""))].join("\n");
}

const args = process.argv.slice(2);
const dir = args[0];
if (!dir || dir.startsWith("--")) fail("usage: consume-changesets.mjs <package dir> [--changesets <dir>]");
const at = args.indexOf("--changesets");
const changesets = at >= 0 ? args[at + 1] : ".changeset";
if (!changesets || !existsSync(changesets)) fail(`no changeset folder at ${changesets}`);

const pkg = JSON.parse(readFileSync(join(dir, "package.json"), "utf8"));
const entries = [];
for (const name of readdirSync(changesets).filter((f) => f.endsWith(".md") && f !== "README.md").sort()) {
  const file = join(changesets, name);
  const changeset = parse(readFileSync(file, "utf8"));
  const index = changeset ? changeset.packages.indexOf(pkg.name) : -1;
  if (index < 0) continue;
  if (changeset.summary) entries.push(entry(changeset.summary));
  const lines = changeset.lines.filter((_, i) => i !== index);
  if (lines.length === 0) {
    rmSync(file);
  } else {
    writeFileSync(file, `---\n${lines.join("\n")}\n---\n\n${changeset.summary}\n`);
  }
}

if (entries.length > 0) {
  const notes = `## ${pkg.version}\n\n${entries.join("\n\n")}\n`;
  const changelog = join(dir, "CHANGELOG.md");
  const title = `# ${pkg.name}\n\n`;
  const previous = existsSync(changelog) ? readFileSync(changelog, "utf8") : title;
  const body = previous.startsWith(title) ? previous.slice(title.length) : previous;
  writeFileSync(changelog, `${title}${notes}${body ? `\n${body}` : ""}`);
  console.log(notes);
}
