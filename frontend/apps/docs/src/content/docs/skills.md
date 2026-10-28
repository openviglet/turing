---
title: "Run Anthropic-Compatible Agent Skills in Viglet Turing"
description: "Import, author and run Anthropic-compatible Agent Skills (a SKILL.md folder with scripts and references) in a Docker sandbox. Skills exported from Claude run unchanged in chat and search."
sidebar:
  label: "Skills"
---
> *A Custom Tool teaches an agent one function. An MCP server connects it to one system. A **Skill** packages a whole capability (instructions, scripts, reference material, and the runtime to execute them) into one portable folder that runs the same way in Claude **and** in Viglet Turing.*

A **Skill** is an [Anthropic-compatible](https://www.anthropic.com/news/skills) folder: a `SKILL.md` file at the root (with YAML frontmatter), optionally accompanied by `scripts/`, `references/`, and `assets/`. You author it like a mini project, store it in object storage, and Turing runs it inside a **Docker sandbox that is compatible with Anthropic's skill container**.

The whole point is **portability**. The same folder you build in Turing runs unmodified in Claude, and any skill ZIP you export from Claude imports straight into Turing. Turing becomes an *entry and exit door* for the broader skills ecosystem, not a fork of it.

Three things distinguish a Skill from the other ways of extending an agent:

1. **It carries its own runtime.** Unlike a [Custom Tool](/docs/custom-tools/) (one Groovy function) or an [MCP server](/docs/mcp-servers/) (a remote tool surface), a Skill bundles *instructions + executable scripts + reference docs* and runs them in a real Linux container with `bash`, Python, and Node.
2. **It activates progressively.** The LLM doesn't pay for the skill's full content up front. It sees only the name and description, then loads the body on demand, then runs commands in the sandbox, three levels, each more expensive than the last, opened only when needed.
3. **It's portable across the ecosystem.** Import any Anthropic skill ZIP; export any Turing skill folder as an Anthropic-compatible ZIP. One ZIP can carry many skills, each an independent, separately activatable unit.

Manage skills in **Administration → Skills**.

:::note[Requirements]
Skills are a **gated** feature. They are available only when **all three** of the following are true:

- A [storage backend](/docs/configuration-reference/#storage) is configured (`turing.storage.type` ≠ `none`): this is where skill folders live.
- The [Code Interpreter](/docs/tool-calling/#code-interpreter-1-tool) execution mode is set to **`DOCKER`**, a `bash` session with no container is exactly the escape vector containerization closes, so there is **no native fallback**.
- A **Default LLM** is configured and enabled in Global Settings: every skill runs on it (see [Why a skill always runs on the Default LLM](#why-a-skill-always-runs-on-the-default-llm)).

When any of these is missing, the feature is a complete no-op: prompts and agents behave byte-for-byte as if Skills did not exist.
:::

---

## Anatomy of a Skill

A Skill is just a folder. The only required file is `SKILL.md` at the root; everything else is optional and addressed by the instructions inside it.

```
brand-content-studio/
├── SKILL.md              ← required: frontmatter + instructions
├── references/           ← reference docs the model reads on demand
│   └── tone-of-voice.md
├── scripts/              ← executable helpers (python, bash, node…)
│   └── render_banner.py
└── assets/               ← static files (templates, fonts, logos…)
    └── logo.svg
```

### `SKILL.md` frontmatter

The frontmatter is YAML between `---` fences. Turing parses it (leniently, a malformed file never crashes the catalog) to build the thin index row:

```markdown
---
name: Brand Content Studio
description: Generates on-brand marketing copy and banner images from a campaign brief. Use when the user asks for branded content, social posts, or campaign assets.
version: 1.0.0
author: Marketing Team
---

# Brand Content Studio

You are a brand content specialist. When the user provides a campaign brief:

1. Read `references/tone-of-voice.md` for the voice guidelines.
2. Draft the copy following those guidelines.
3. To render a banner, run `python scripts/render_banner.py --text "..."`.
   The output PNG lands in `/workspace`; return its URL to the user.
...
```

| Field | Source | Purpose |
|---|---|---|
| `name` | frontmatter `name` | Display name + the identity the LLM sees at level 1 |
| `description` | frontmatter `description` | **The single most important field**: it's what the LLM uses to decide whether to activate the skill. Write it like an LLM Description: concrete, with *"Use when…"* triggers. |
| `version` | top-level `version`, else `metadata.version` | Informational version string |
| `author` | top-level `author`, else first of `metadata.authors` | Informational author string |

Everything except the **enabled** toggle is derived from the folder: the database holds only a thin catalog index (`id, name, description, version, author, path, enabled`). The folder in storage is the source of truth; the index is rebuilt from it.

### The body is the instructions

Below the frontmatter, the Markdown body is the skill's actual operating instructions, the procedure the model follows once it activates the skill. It can reference other files in the folder (`references/…`, `scripts/…`) and tell the model to run them in the sandbox.

---

## How a Skill activates: progressive disclosure

A skill can be large: thousands of words of instructions, plus scripts and reference docs. Loading all of that into every prompt would be wasteful. Instead, Turing exposes a skill in **three levels**, each opened only when the model decides it's worth it.

| Level | What the model sees | Cost | Opened by |
|---|---|---|---|
| **1: Metadata** | Each offered skill's `name` + `description` only, plus the activation protocol | Cheap: always in the prompt | Automatic (system prompt block) |
| **2: Instructions** | The full `SKILL.md` body for one chosen skill | Medium: one tool call | `load_skill` tool |
| **3: Execution** | A live `bash` session in the skill's Docker sandbox | Expensive: container run per command | `skill_bash` tool |

The model reads the descriptions (level 1), decides a skill is relevant, calls `load_skill` to read its instructions (level 2), then drives `skill_bash` to run the skill's scripts and produce artifacts (level 3). This mirrors how Claude itself activates skills.

| Tool | What it does |
|---|---|
| `load_skill` | Returns the chosen skill's full `SKILL.md` body on demand |
| `skill_bash` | Runs a `bash` command inside the skill's sandbox; returns stdout/stderr |

Both tools are **constrained to the offered set** for that turn: the model can't load or execute a skill that wasn't put on the table.

---

## The sandbox: where a Skill runs

A skill executes in a Docker sandbox built to be compatible with Anthropic's skill container. Two paths are mounted into every session:

| Mount | Mode | Contents |
|---|---|---|
| `/skill` | **read-only** | The skill folder, materialized from storage: `SKILL.md`, `scripts/`, `references/`, `assets/` |
| `/workspace` | **read-write** | A persistent [workspace](/docs/agent-workspace/), the skill writes its outputs (files, charts, exports) here, and they survive across turns |

The session is keyed by `(agentId, conversationId, skillId)`, so a warm conversation keeps its `/workspace` between turns while a fresh conversation gets a clean one.

:::note[The container is ephemeral; the filesystem is not]
Each `skill_bash` call spawns a **fresh, hardened `docker run … bash -lc <cmd>`**, not a long-lived container. It runs with `--network none`, memory/CPU/PID limits, `--cap-drop ALL`, `--security-opt no-new-privileges`, a read-only root filesystem, and a tmpfs `/tmp`; `/workspace` is the only writable mount. The filesystem *persists across turns* because the bind-mounted **host** directory is stable, not because any container stays alive. This is the same hardening posture as the [Code Interpreter](/docs/tool-calling/#code-interpreter-1-tool) DOCKER path.
:::

Idle session directories are reaped by modification time, so an active conversation keeps its work while abandoned ones are cleaned up.

---

## Using Skills in Chat

Attach skills to an [AI Agent](/docs/ai-agents/) with the **Skills** toggle in the agent settings (`skillsEnabled`, shown only when the feature is available). Once enabled, every enabled skill in the catalog is offered to that agent's conversations via progressive disclosure.

### Why a skill always runs on the Default LLM

A skill is **portable**: it should behave identically no matter which agent or site invokes it. So Turing runs every skill on the **Global Settings Default LLM**, *never* on the calling agent's own model. A skill authored against one model's behavior keeps that behavior everywhere.

### The `run_skill` delegation

A tool can't switch the model that's driving it. So Turing uses a **delegated sub-loop**:

1. The **parent turn** (running on the agent's own model) sees only the cheap level-1 block (each skill's name + description) plus a single `run_skill(skill, task)` tool.
2. When the model calls `run_skill`, Turing spins up a **sub-conversation on the Default LLM**. That sub-loop gets the full activation harness (the `SKILL.md` system prompt, `load_skill`, and `skill_bash`) and runs to completion.
3. The sub-loop's result text is handed back to the parent turn as the tool result.

The parent model orchestrates; the Default LLM executes the skill. Token usage for the sub-loop is recorded separately.

### Skill mode: pin one skill for a conversation

By default the agent is offered **all** enabled skills (the model picks). You can also pin **one** skill for a conversation (a distinct "mode") using the **Skill mode** selector beside the flow selector in the chat console:

- **Auto / all skills** (default): legacy progressive disclosure across the whole catalog.
- **A specific skill**: only that skill is offered; the model can't silently widen to others.

Under the hood this is an optional `selectedSkillId` on the chat request. A blank value means *all enabled skills*; an id or a case-insensitive name pins exactly one; an unknown or disabled pin offers nothing (a pinned mode never falls back to the full catalog).

---

## Using Skills in Semantic Navigation

Skills work in [Semantic Navigation](/docs/semantic-navigation/) AI chat too, and you don't configure anything extra. SN AI chat shares the same execution path as agent chat, so the agent's `skillsEnabled` toggle activates skills in SN mode automatically, still on the Default LLM.

There's one addition unique to the skill sub-loop: it's also given **Turing's own search tools**. The Semantic Navigation DSL toolset, `dsl_list_indices`, `dsl_get_mappings`, `dsl_search`, `dsl_get_document`, `dsl_suggest`, `dsl_get_shards` (the same surface the [DSL Query](/docs/dsl-query/) chat exposes), is available to the skill alongside `load_skill` and `skill_bash`. These run in-process (no container network); the model bridges their results into the sandbox `/workspace` via `skill_bash`.

This is the first place a skill can leverage Turing's *own* indexed data: search the enterprise content with standard tools, then process the results with the skill's scripts.

---

## Authoring Skills: the mini editor

Skills are authored in a built-in **mini-VS-Code** editor at **Administration → Skills** (`/admin/skill`). The list page shows skills by name; click one to open the editor.

- **File tree** on the left, reconstructed from the skill's folder, rooted at the skill itself: you can't navigate above the skill root.
- **CodeMirror editor** on the right, with syntax highlighting per file extension (Python, JavaScript, shell, YAML, XML, CSS; Markdown and unknown types as plain text).
- Create files and folders, rename, delete, and save, all scoped safely to the skill root (paths are validated so the editor can never escape into other storage).

Saving or deleting a `SKILL.md` automatically **reindexes** the catalog so the name/description/version stay in sync with the folder.

### Import & export

Skills round-trip as standard ZIP archives:

- **Import ZIP**: drop in an archive and each top-level folder that contains a `SKILL.md` becomes its **own** indexed skill. A root-level `SKILL.md` is treated as one skill. (`__MACOSX` and hidden entries are skipped.) This is how one ZIP carries many independent skills.
- **Export → ZIP**: every file in a skill is wrapped under one top-level `{slug}/` folder, producing an Anthropic-compatible archive that round-trips back through import unchanged.

Because import splits each `SKILL.md`-bearing folder into a separate catalog row, a multi-skill bundle behaves as a set of **independent, separately activatable units**, not a monolith.

---

## The catalog & reindexing

The database holds a **thin index**, not the skill content. `reindex` walks the immediate child folders under `turing.storage.skillsPath` (default `skills/`), reads each `<folder>/SKILL.md`, and upserts a catalog row by folder path:

- The folder **path** is the unique identity.
- Reindexing **preserves** the `enabled` flag and `id`; everything else is re-derived from frontmatter.
- Rows whose folder lost its `SKILL.md` are pruned: storage is the truth, the index is reconciled to it.

There is no manual *delete* on the index; remove the folder (or its `SKILL.md`) and reindex.

---

## Configuration

| Setting | Where | Default | Purpose |
|---|---|---|---|
| `turing.storage.type` | `application.yaml` | `none` | Must be `minio` or `filesystem`: skill folders are stored here |
| `turing.storage.skillsPath` | `application.yaml` | `skills` | Storage prefix under which skill folders live |
| Code Interpreter mode | Global Settings | `NATIVE` | Must be **`DOCKER`** for skills to run |
| `GLOBAL_CODE_INTERPRETER_SKILL_IMAGE` | Global Settings | `python:3.12-slim` | The Docker image for skill sandboxes (needs `python` + `node` + `bash`). Shown only in DOCKER mode. |
| Default LLM | Global Settings | n/a | The model every skill runs on |
| Skills toggle | per AI Agent | off | `skillsEnabled`: activates skills for that agent/SN site |

The feature flag `skillsEnabled` (equal to *storage enabled*) drives the **Skills** sidebar item and the agent toggle visibility in the admin console.

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/skill` | List all skills (catalog index) |
| `GET` | `/api/skill/{id}` | Get a single skill |
| `GET` | `/api/skill/{id}/skill-md` | Get the raw `SKILL.md` (text/markdown) |
| `POST` | `/api/skill` | Create a new skill (writes a starter `SKILL.md`) |
| `POST` | `/api/skill/reindex` | Re-scan storage and reconcile the catalog |
| `PUT` | `/api/skill/{id}/enabled?value=` | Enable / disable a skill |
| `GET` | `/api/skill/{id}/files` | List the skill's files (paths relative to root) |
| `GET` | `/api/skill/{id}/file?path=` | Read a file |
| `PUT` | `/api/skill/{id}/file?path=` | Write a file (`text/plain`) |
| `DELETE` | `/api/skill/{id}/file?path=` | Delete a file or folder |
| `POST` | `/api/skill/{id}/folder?path=` | Create a folder |
| `POST` | `/api/skill/{id}/rename?from=&to=` | Rename a file/folder |
| `POST` | `/api/skill/import` | Import a ZIP (multipart): 1..N skills |
| `GET` | `/api/skill/{id}/export` | Export the skill as an Anthropic-compatible ZIP |

The sandbox itself is reachable for diagnostics via `GET`/`POST /api/skill/{id}/sandbox/status` and `/exec`.

---

## SDK support

Both SDKs accept a `selectedSkillId` to pin skill mode for a chat:

- **`@viglet/turing-sdk`** and **`@viglet/turing-react-sdk`**: `selectedSkillId` on `PostAgentChatOptions`.
- **`useTuringChat`**: `selectedSkillId` on `UseTuringChatOptions`.

Leaving it blank keeps the legacy behavior (all enabled skills offered). See the [React SDK](/docs/react-sdk/) reference.

---

## Skills vs. Custom Tools vs. MCP

All three extend an agent, but they sit at different scales:

| | [Custom Tool](/docs/custom-tools/) | [MCP Server](/docs/mcp-servers/) | **Skill** |
|---|---|---|---|
| **Shape** | One Groovy function | A remote tool surface | A folder: instructions + scripts + references |
| **Runtime** | Sandboxed JVM/Groovy | The MCP server's own process | A hardened Docker container (`bash`, Python, Node) |
| **Best for** | A small, deterministic computation or API call | Connecting to a live external system | A multi-step capability with its own procedure and helper scripts |
| **Portability** | Turing-specific | Protocol-portable | **Ecosystem-portable**: runs identically in Claude |
| **How the LLM uses it** | Picks the function by description | Picks an MCP tool by description | Activates progressively (metadata → instructions → execution) |

Reach for a Skill when the work is a *whole capability* ("generate on-brand campaign assets", "produce a financial model from a CSV") that benefits from packaged instructions, bundled scripts, and a real execution environment, and that you might want to share with or import from the wider skills ecosystem.

---

## Related Pages

| Page | Description |
|---|---|
| [Custom Tools](/docs/custom-tools/) | A single Groovy function: the smaller-scale way to extend an agent |
| [MCP Servers](/docs/mcp-servers/) | Connect agents to external systems over the Model Context Protocol |
| [Tool Calling](/docs/tool-calling/) | Native tools and the Code Interpreter sandbox skills build on |
| [AI Agents](/docs/ai-agents/) | Where the **Skills** toggle lives, and where skills are activated |
| [Agent Workspace](/docs/agent-workspace/) | The `/workspace` mount: per-conversation file store skills write to |
| [Semantic Navigation](/docs/semantic-navigation/) | Skills work in SN AI chat, with Turing's search tools added |
| [Configuration Reference](/docs/configuration-reference/#storage) | Storage and Code Interpreter settings skills depend on |

---

