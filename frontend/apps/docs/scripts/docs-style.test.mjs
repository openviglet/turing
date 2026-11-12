// The docs-style lint: a docs page judged as writing, not only as links.
//
// `scripts/check-links.mjs` proves that every link, anchor and image in the built area resolves,
// and COVERAGE.md proves that every shipped block has a page. Nothing read how a page is written,
// so pages grew one increment at a time. This lint holds the half of the shared `docs-writing`
// style guide that a machine can check (its reference/LINT-RULES.md); the judgement half lives in
// the `turing-docs-write` skill.
//
// The debt is a named list, BASELINE below, not a ceiling. The test fails when a page breaks a
// rule it is not listed for, and also when a listed page no longer breaks it: the list only
// shrinks, and the entry goes in the same change as the rewrite that paid it.
//
// Tuning is a list edit at the top of this file. A false positive is fixed by adding to
// PROPER_NOUNS or narrowing a phrase, never by adding a page to the baseline.
//
// Run: pnpm --filter turing-docs test

import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative, sep } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const CONTENT = join(dirname(fileURLToPath(import.meta.url)), "..", "src", "content", "docs");

/** Words that keep their capital inside a sentence-case heading. Acronyms and mixedCase pass on their own. */
const PROPER_NOUNS = new Set([
  "Viglet", "Turing", "Semantic", "Navigation", "Solr", "Elasticsearch", "Lucene", "Adobe",
  "Experience", "Manager", "Keycloak", "OpenAI", "Anthropic", "Claude", "Gemini", "Google", "Ollama",
  "Mistral", "Cohere", "Bedrock", "Amazon", "Azure", "Microsoft", "MinIO", "ChromaDB", "PgVector",
  "Milvus", "Docker", "Compose", "Kubernetes", "Helm", "WordPress", "Playground", "Algolia", "React",
  "JavaScript", "TypeScript", "Java", "Spring", "Boot", "Maven", "Groovy", "Python", "Node.js",
  "Prometheus", "Grafana", "Hazelcast", "Liquibase", "PostgreSQL", "MySQL", "MariaDB", "Oracle",
  "Hugging", "Face", "Content", "Model", "Context", "Protocol", "GitHub", "Git", "Linux", "Windows",
  "macOS", "Markdown", "Starlight", "Astro", "Vite", "Shio", "Dumont", "Edge", "Delivery", "Services",
  "Apache", "Tika", "Snowball", "Jlama", "ONNX", "Atlas", "Store", "Persona", "Book", "Thesaurus",
  "Groq", "DeepSeek", "Perplexity", "xAI", "Grok", "Voyage", "Jina", "Tavily", "Slack", "Teams",
  "Studio", "Code", "VS", "I", "Kotlin", "Swagger", "OpenAPI", "Redis", "Artemis", "Cloud", "SaaS",
  "Social", "Login", "Facebook", "Apple", "Matryoshka", "Match", "Dialogue",
]);

/**
 * Phrases that only make sense against a release the reader may never have seen. "no longer" is
 * not here: it describes a state change in the product as often as a release ("a token that no
 * longer matches"), and "as it is now" is the present, so "is now" skips it.
 */
const RELEASE_RELATIVE = [
  /(?<!as it )\bis now\b/i, /\bare now\b/i, /\bnow supports?\b/i, /\bas before\b/i,
  /\banymore\b/i, /\bhas been changed\b/i, /\bwas replaced\b/i,
];

/** Internal ids: the roadkeep task prefix (T<n>) and the roadmap's block labels. */
const INTERNAL_ID = /\bT\d{3,}\b|\bBlock [A-Z]{1,2}(\.\d+)?\b/;

/** How the code is tested or guarded is the engineer's business, not the reader's. */
const IMPLEMENTATION_CLAIMS = [/\basserted by a test\b/i, /\ba test (asserts|fails|proves|pins)\b/i];

/**
 * The glossary's "avoid" column: a second name for one thing makes the reader think there are two.
 * Read with **bold** blanked, since a UI label is quoted as the screen spells it.
 */
const GLOSSARY_AVOID = [
  [/\bTuring ES\b/, "Viglet Turing"],
  [/\bchatflows?\b/i, "chat flow"],
];

const PUBLIC_DOCS = "https://turing.viglet.org/docs";

/**
 * Today's offenders, page → rules. Shrinks only. Each rewrite removes its page's entry in the same
 * commit.
 */
const BASELINE = {
  "administration-guide.md": ["heading-sentence-case", "heading-unique"],
  "agent-eval.md": ["frontmatter-description", "heading-sentence-case", "release-relative"],
  "agent-workspace.md": ["heading-sentence-case"],
  "ai-agents.md": ["heading-sentence-case", "release-relative"],
  "answer.md": ["frontmatter-description", "heading-sentence-case"],
  "architecture-overview.md": ["heading-sentence-case"],
  "assets.md": ["heading-sentence-case"],
  "capabilities.md": ["frontmatter-description", "heading-sentence-case"],
  "chat-analytics.md": ["frontmatter-description", "heading-sentence-case"],
  "chat-flow.md": ["frontmatter-description", "heading-sentence-case", "heading-unique"],
  "chat-memory.md": ["heading-sentence-case"],
  "chat.md": ["frontmatter-description", "heading-sentence-case", "release-relative"],
  "cli.md": ["frontmatter-description"],
  "client-tools.md": ["heading-sentence-case"],
  "configuration-reference.md": ["heading-sentence-case", "internal-ids"],
  "conversion-analytics.md": ["frontmatter-description", "heading-sentence-case", "internal-ids"],
  "cost-governance.md": ["frontmatter-description"],
  "custom-tools.md": ["frontmatter-description", "heading-sentence-case", "release-relative"],
  "developer-guide.md": ["heading-sentence-case"],
  "dsl-compatibility.md": ["heading-levels", "heading-sentence-case"],
  "dsl-query.md": ["heading-sentence-case", "heading-unique"],
  "embedding-models.md": ["heading-sentence-case"],
  "embedding-stores.md": ["heading-sentence-case"],
  "experiments.md": ["frontmatter-description", "heading-sentence-case"],
  "flow-dsl.md": ["frontmatter-description", "internal-ids"],
  "genai-llm.md": ["heading-sentence-case"],
  "getting-started/core-concepts.md": ["heading-sentence-case"],
  "getting-started/intro.md": ["heading-sentence-case"],
  "graphql.md": ["heading-sentence-case"],
  "human-in-the-loop.md": ["frontmatter-description"],
  "import-export.md": ["heading-sentence-case"],
  "index.mdx": ["heading-sentence-case"],
  "installation-guide.md": ["frontmatter-description", "heading-sentence-case"],
  "integration-aem.md": ["heading-sentence-case"],
  "integration.md": ["heading-sentence-case"],
  "integrations/wordpress/features.md": ["frontmatter-description"],
  "intent.md": ["heading-sentence-case"],
  "javascript-sdk.md": ["frontmatter-description", "heading-levels", "heading-sentence-case"],
  "llm-gateway.md": ["frontmatter-description"],
  "llm-instances.md": ["frontmatter-description", "heading-sentence-case", "internal-ids", "release-relative"],
  "logging.md": ["heading-sentence-case"],
  "manifest.md": ["frontmatter-description", "heading-sentence-case", "internal-ids", "release-relative"],
  "mcp-servers.md": ["frontmatter-description", "heading-sentence-case"],
  "migration.md": ["frontmatter-description", "heading-sentence-case"],
  "multi-tenancy.md": ["frontmatter-description", "heading-sentence-case"],
  "observability.md": ["frontmatter-description", "heading-sentence-case"],
  "rag.md": ["heading-sentence-case"],
  "react-sdk.md": ["heading-sentence-case"],
  "reranking.md": ["frontmatter-description", "heading-sentence-case"],
  "routines.md": ["frontmatter-description"],
  "search-engine.md": ["frontmatter-description", "heading-sentence-case"],
  "security-authentication.md": ["heading-sentence-case", "release-relative"],
  "security-hardening.md": ["frontmatter-description", "release-relative"],
  "security-keycloak.md": ["heading-sentence-case"],
  "security-social-login.md": ["heading-sentence-case"],
  "showcase.md": ["frontmatter-description", "heading-sentence-case"],
  "skills.md": ["frontmatter-description", "heading-sentence-case"],
  "spa-pages.md": ["frontmatter-description", "heading-sentence-case"],
  "synonyms.md": ["frontmatter-description"],
  "thesaurus.md": ["frontmatter-description", "release-relative"],
  "token-usage.md": ["frontmatter-description", "heading-sentence-case"],
  "tool-calling.md": ["heading-sentence-case", "release-relative"],
  "transcription.md": ["frontmatter-description", "release-relative"],
  "vectorless-structured-rag.md": ["frontmatter-description"],
  "webhooks.md": ["frontmatter-description"],
};

// ---------------------------------------------------------------------------------------------

function walk(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? walk(path) : [path];
  });
}

/** github-slugger's rule, the one Astro uses for heading ids. */
function slugify(heading) {
  return heading
    .replace(/`([^`]*)`/g, "$1")
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/[*_]/g, "")
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s-]/gu, "")
    .replace(/\s/g, "-");
}

/**
 * Splits a page into its frontmatter and line-preserving views: `fences` lists every fenced block's
 * opening line and info string; `prose` is the body with frontmatter, fenced code, HTML comments
 * and inline code blanked, so a line number in it is the line in the file.
 */
function parse(text) {
  const lines = text.split(/\r?\n/);
  const front = {};
  let start = 0;
  if (lines[0] === "---") {
    const end = lines.indexOf("---", 1);
    for (const line of lines.slice(1, end)) {
      const m = line.match(/^(\w+):\s*(.*)$/);
      if (m) front[m[1]] = m[2].replace(/^["']|["']$/g, "").trim();
    }
    start = end + 1;
  }
  const prose = lines.map((line, i) => (i < start ? "" : line));
  const fences = [];
  let open = null;
  for (let i = start; i < lines.length; i++) {
    const m = lines[i].match(/^\s*(`{3,}|~{3,})\s*(\S*)/);
    if (open) {
      if (m && m[1][0] === open[0] && m[1].length >= open.length && !m[2]) open = null;
      prose[i] = "";
    } else if (m) {
      open = m[1];
      fences.push({ line: i + 1, info: m[2] });
      prose[i] = "";
    }
  }
  // A generated video transcript quotes what was said, so its words are not the page's prose.
  const body = prose
    .join("\n")
    .replace(/<!-- transcript:start[\s\S]*?<!-- transcript:end -->/g, (c) => c.replace(/[^\n]/g, " "))
    .replace(/<!--[\s\S]*?-->/g, (c) => c.replace(/[^\n]/g, " "));
  // `raw` keeps inline code: a heading made of code (`turing migrate`) still has an anchor.
  const raw = body.split("\n");
  return {
    front,
    raw,
    prose: raw.map((line) => line.replace(/`[^`]*`/g, (c) => " ".repeat(c.length))),
    fences,
  };
}

function isSentenceCase(heading) {
  const words = heading
    .replace(/`[^`]*`/g, "")
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/<[^>]+>/g, "")
    .replace(/\*\*[^*]+\*\*/g, "")
    .replace(/[*_"()]/g, "")
    .split(/\s+/)
    .filter(Boolean);
  let sentenceStart = true;
  for (const word of words) {
    const bare = word.replace(/[^\p{L}\p{N}.'-]/gu, "").replace(/[.:,;!?]+$/, "");
    const first = sentenceStart;
    sentenceStart = /[:.?!]$/.test(word);
    if (first || !bare || !/^\p{Lu}/u.test(bare)) continue;
    if (PROPER_NOUNS.has(bare) || /^\p{Lu}[\p{Lu}\p{N}]+s?$/u.test(bare) || /\p{Ll}\p{Lu}/u.test(bare)) continue;
    return false;
  }
  return true;
}

/** Every rule, run over one page: `{ rule: [ "line: detail", ... ] }`. */
function check(text) {
  const { front, raw, prose, fences } = parse(text);
  const out = {};
  const hit = (rule, line, detail) => (out[rule] ??= []).push(`${line}: ${detail}`);

  if (!front.title) hit("frontmatter-title", 1, "no title");
  const description = front.description ?? "";
  if (description.length < 70 || description.length > 160) {
    hit("frontmatter-description", 1, `description is ${description.length} characters; aim for 70 to 160`);
  }
  for (const { line, info } of fences) if (!info) hit("fence-language", line, "fence names no language");

  let previous = 1;
  const slugs = new Set();
  prose.forEach((line, i) => {
    const n = i + 1;
    const heading = raw[i].match(/^(#{2,6})\s+(.+?)\s*#*\s*$/);
    if (heading) {
      const level = heading[1].length;
      if (level > previous + 1) hit("heading-levels", n, `h${level} after h${previous}`);
      previous = level;
      const slug = slugify(heading[2]);
      if (slugs.has(slug)) hit("heading-unique", n, `"${heading[2]}" repeats an anchor on this page`);
      slugs.add(slug);
      if (!isSentenceCase(heading[2])) hit("heading-sentence-case", n, `"${heading[2]}"`);
    }
    for (const re of RELEASE_RELATIVE) if (re.test(line)) hit("release-relative", n, line.match(re)[0]);
    if (INTERNAL_ID.test(line)) hit("internal-ids", n, line.match(INTERNAL_ID)[0]);
    for (const re of IMPLEMENTATION_CLAIMS) if (re.test(line)) hit("implementation-claims", n, line.match(re)[0]);
    const unlabelled = line.replace(/\*\*[^*]+\*\*/g, "");
    for (const [re, use] of GLOSSARY_AVOID) {
      if (re.test(unlabelled)) hit("glossary", n, `"${unlabelled.match(re)[0]}": say "${use}"`);
    }
    if (/\[(click )?here\]\(/i.test(line)) hit("click-here", n, "link text says where it goes");
    if (line.includes(`](${PUBLIC_DOCS}`)) hit("absolute-self-link", n, "link the page as /docs/...");
  });
  return out;
}

const results = walk(CONTENT)
  .filter((path) => /\.mdx?$/.test(path))
  .map((file) => ({
    page: relative(CONTENT, file).split(sep).join("/"),
    found: check(readFileSync(file, "utf8")),
  }))
  .sort((a, b) => a.page.localeCompare(b.page));

test("docs-style: the lint reads every page and prints its debt", () => {
  assert.ok(results.length >= 70, `only ${results.length} pages under ${CONTENT}`);
  const perRule = {};
  for (const { found } of results) {
    for (const [rule, hits] of Object.entries(found)) {
      (perRule[rule] ??= { pages: 0, hits: 0 }).pages++;
      perRule[rule].hits += hits.length;
    }
  }
  const owed = Object.values(BASELINE).reduce((sum, rules) => sum + rules.length, 0);
  console.log(`docs-style: ${results.length} pages, baseline ${Object.keys(BASELINE).length} pages / ${owed} page-rules`);
  for (const [rule, { pages, hits }] of Object.entries(perRule).sort()) {
    console.log(`  ${rule.padEnd(24)} ${String(pages).padStart(3)} pages ${String(hits).padStart(5)} hits`);
  }
  if (process.env.DOCS_STYLE_DUMP) {
    const dump = Object.fromEntries(
      results.filter((r) => Object.keys(r.found).length).map((r) => [r.page, Object.keys(r.found).sort()]),
    );
    console.log(JSON.stringify(dump, null, 2));
  }
});

test("docs-style: no page breaks a style rule it is not listed for", () => {
  const regressions = [];
  for (const { page, found } of results) {
    const allowed = new Set(BASELINE[page] ?? []);
    for (const [rule, hits] of Object.entries(found)) {
      if (allowed.has(rule)) continue;
      for (const hit of hits.slice(0, 5)) regressions.push(`  ${page}:${hit}  [${rule}]`);
      if (hits.length > 5) regressions.push(`  ${page}: ... ${hits.length - 5} more [${rule}]`);
    }
  }
  assert.equal(
    regressions.length,
    0,
    "a docs page breaks the style guide (docs-writing reference/STYLE.md; the project skill is "
      + "turing-docs-write). Fix the page; a false positive is fixed in the lists at the top of "
      + `frontend/apps/docs/scripts/docs-style.test.mjs, never by adding the page to BASELINE.\n${regressions.join("\n")}`,
  );
});

test("docs-style: the baseline names no debt that has been paid", () => {
  const stale = [];
  const byPage = new Map(results.map((r) => [r.page, r.found]));
  for (const [page, rules] of Object.entries(BASELINE)) {
    const found = byPage.get(page);
    if (!found) {
      stale.push(`  ${page}: no such page`);
      continue;
    }
    for (const rule of rules) if (!found[rule]) stale.push(`  ${page}: ${rule}`);
  }
  assert.equal(
    stale.length,
    0,
    "these baseline entries are paid. Remove them from BASELINE in "
      + `frontend/apps/docs/scripts/docs-style.test.mjs, in this change, so the list only shrinks:\n${stale.join("\n")}`,
  );
});
