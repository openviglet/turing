---
title: "Custom Tools"
description: "Build your own AI tools in Groovy and let your agents call them like first-class capabilities. Plus Vibe Coding: the AI helps you write the tool, describe it to the LLM, and shape its parameters."
---
> *The 27 native tools cover most of what an agent needs to do. The 28th (and the 29th, and the 50th) is whatever your business actually does. **Custom Tools** is how you teach an agent to do it.*

A **Custom Tool** is a function you write that any [AI Agent](/docs/ai-agents/) or [Chat Flow](/docs/chat-flow/) can call as if it were native. You declare what it does, what arguments it accepts, and how the LLM should think about it. The body is a **Groovy script**, a friendly Java dialect that runs in a sandboxed JVM context.

Three things make Custom Tools different from "just write a microservice":

1. **No deployment, no rebuild.** You write a Custom Tool in the admin console, click Save, and it's available to every agent immediately.
2. **First-class to the LLM.** The tool's name, description, and parameter schema are exposed to the LLM exactly like a native tool. The model picks it on its own when relevant.
3. **Vibe Coding.** Tell the AI what you want (*"a tool that takes a CEP code and returns the address from BrasilAPI"*) and it writes the description, the LLM-facing prompt, the parameter schema, **and** the Groovy script. You review, save, ship.

Configure tools in **Administration → Custom Tools**.

---

## When You Need a Custom Tool

Some questions don't have a native tool, an MCP server, or a stock answer:

| Use case | Why custom |
|---|---|
| Look up a Brazilian postal code (CEP) | Public API; one-line script; no need to spin up an MCP for it |
| Fetch a customer's account balance from your internal API | Specific to your stack; rotating per agent doesn't scale |
| Run a scoring formula your sales team uses | Proprietary logic; encoding it in a prompt is fragile |
| Translate a value through a lookup table you maintain | Hot data; pulling it in via Groovy is faster than an MCP |
| Compute working days between two dates respecting your country's holidays | Domain-specific; trivial in Groovy; would be expensive to externalize |

A Custom Tool sits between *"too small for an MCP"* and *"too big for a prompt instruction"*. That's a productive middle ground for most operational queries.

---

## Anatomy of a Custom Tool

Every tool is a small bundle of seven decisions. Get the first three right and the LLM uses your tool well; get the script right and the tool *works*.

| Field | Required | What it's for |
|---|---|---|
| **Title** | Yes | The display name in the admin console (e.g., *"CEP Lookup"*) |
| **Description** | | A short admin-facing description. Helps your team know *when to attach this tool to an agent* |
| **Icon** | | A Lucide icon for visual identity in the admin lists |
| **LLM Description** | Yes | The description the LLM sees. **This is the most important field.** It's how the model decides whether to call your tool. *"Returns the full street address (street, neighborhood, city, state) for a Brazilian postal code (CEP)."* |
| **Parameters JSON** | | JSON-serialized list of `{name, type}` pairs declaring the arguments the LLM is allowed to pass. Types: `string`, `integer`, `number`, `boolean`. |
| **Return Type** | Yes | The JSON-Schema primitive of the value the script returns: `string`, `integer`, `number`, `boolean`, `object` |
| **Groovy Script** | Yes | The body. Receives parameters as named bindings, returns the value the LLM will see. |
| **Enabled** | | Toggle to activate/deactivate without deleting |

Two quiet but important fields back the Vibe Coding experience:

- `descriptionMetaPrompt`, `llmDescriptionMetaPrompt`, `groovyMetaPrompt`: when you ask the AI to author a piece, the meta-prompt that produced it is stored alongside the result. You can revise *the prompt* later, not just the output.

---

## The Groovy Sandbox

Custom Tools run in a Groovy script context: full Java/Groovy semantics, with one strong constraint: **the script returns a single value**, declared in `Return Type`.

A trivial example: a tool that adds two numbers.

**Parameters JSON:**
```json
[
  { "name": "a", "type": "number" },
  { "name": "b", "type": "number" }
]
```

**Return Type:** `number`

**Groovy Script:**
```groovy
return a + b
```

The names declared in Parameters JSON become bindings in the script. The LLM passes `a` and `b`; Groovy receives them; the result is the value of the last expression (or an explicit `return`).

A more realistic example: a CEP lookup against the public BrasilAPI.

**Parameters JSON:**
```json
[
  { "name": "cep", "type": "string" }
]
```

**Return Type:** `object`

**Groovy Script:**
```groovy
import groovy.json.JsonSlurper

def url = "https://brasilapi.com.br/api/cep/v2/${cep.replaceAll(/\D/, '')}"
def conn = new URL(url).openConnection()
conn.requestMethod = 'GET'
conn.connect()

if (conn.responseCode != 200) {
    return [error: "CEP not found", code: conn.responseCode]
}

return new JsonSlurper().parseText(conn.inputStream.text)
```

When an LLM calls this tool, it gets back something like:

```json
{
  "cep": "01310-100",
  "state": "SP",
  "city": "São Paulo",
  "neighborhood": "Bela Vista",
  "street": "Avenida Paulista",
  "service": "open-cep"
}
```

…and uses that to answer the user. The whole loop (user asks for an address from a postal code, model picks the tool, Groovy runs, model formats the answer) happens inside a single chat turn, no extra services.

:::caution[Security caveats]
The sandbox is a Groovy context, not a hardened VM. **Don't paste anything you wouldn't paste into a backend script you control.** Groovy can hit the network, read environment variables, and call any Java class on the classpath. In production, restrict who can author Custom Tools (the existing `CUSTOM_TOOL_*` permissions).
:::

---

## Built-in script bindings

Besides the parameters you declare, every script receives a set of pre-wired helper objects (scoped to the active agent and conversation) so tools compose with the rest of the platform without you re-implementing plumbing.

| Binding | What it does |
|---|---|
| `workspace` | Per-conversation [file store](/docs/agent-workspace/), persist drafts, reports, and artifacts across turns and hand the user a signed download URL |
| `slots` | Read/write [chat-flow](/docs/chat-flow/) variables on the live conversation (drives dynamic UI cards) |
| `http` | Generic HTTP client (`getJson`, `postJson`, `getBytes`) for external APIs |
| `code` | Run Python in the [Code Interpreter](/docs/tool-calling/#code-interpreter-1-tool) sandbox and read back its output/files |
| `turingSearch` | High-level access to the Turing search surfaces (semantic navigation, vector store) |
| `agent` | Delegate to another [AI Agent](/docs/ai-agents/) and use its reply |

### `turingSearch`: what a hit gives you

Both surfaces return a list of hit maps, and every field of the document is lifted to
the top level so a script reads `hit.title`, `hit.price` directly. Three keys are
always the same on both:

| Key | Meaning |
|---|---|
| `hit.id` | Document id |
| `hit.score` | Relevance score (`null` for browse-all) |
| `hit.content` | The matched **passage**: the piece of the document that matched, not the whole thing |

`turingSearch.ann(...)` searches the vector store and always fills `content` with the
matched chunk. `turingSearch.sn(...)` searches the site index; there `content` appears
only when the search engine returns passages of its own, which today means an
[Adobe AEM Content AI](/docs/search-engine/#content-ai) index with
`turing.content-ai.include-chunks` enabled. On Solr, Lucene and Elasticsearch the key
is **absent**: Turing indexed that content itself, so the document's own `abstract`
and `text` fields are what you ground on:

```groovy
// Works on every engine: prefer a passage, fall back to the document body.
def grounding = hit.content ?: hit.abstract ?: hit.text
```

### `workspace`: keep and hand back files

A tool that builds something (a CSV, a PDF, a chart) shouldn't cram it into the reply. Write it to the workspace and return a link instead, the bytes never bloat the prompt, and the file survives to later turns:

```groovy
// Persist an artifact and return a signed download URL:
workspace.put("reports/lead-export-${slots.get('export_date')}.csv", csvBytes, "text/csv")
return "Your export is ready: " + workspace.url("reports/lead-export-2026-06-03.csv")

// Read it back on a later turn:
def previous = workspace.getText("reports/lead-export-2026-06-03.csv")
```

| Method | Returns | Purpose |
|---|---|---|
| `workspace.put(key, value)` / `put(key, bytes, contentType)` | n/a | Store a string or byte blob |
| `workspace.get(key)` / `getText(key)` | `byte[]` / `String` / `null` | Read bytes or UTF-8 text |
| `workspace.list(prefix)` | `List` | List artifacts under a prefix (no-arg lists all) |
| `workspace.delete(key)` | n/a | Remove a blob |
| `workspace.url(key)` | `String` / `null` | Signed, time-limited download URL |

The workspace requires a [storage backend](/docs/configuration-reference/#storage); without one these calls are safe no-ops. See [Agent Workspace](/docs/agent-workspace/) for isolation, the live artifact stream, and automatic offload of large tool results.

---

## Vibe Coding: Let the AI Author the Tool

Writing a tool from scratch (even a trivial one) usually means writing four things: the LLM description, the parameter list, the script, and a quick test. The Vibe Coding chat does all four for you, then hands you a draft to review.

Click **Vibe Coding** on the Custom Tool form and describe what you want:

> **You:** *"A tool that takes a Brazilian CEP and returns the full address: street, neighborhood, city, state. Use BrasilAPI."*
>
> **Assistant:** *"Done. Here's the draft:*
> *LLM Description: 'Returns the full Brazilian address (street, neighborhood, city, state, full postal code) for a given CEP. Use this when the user asks about an address by postal code.'*
> *Parameters: `cep` (string)*
> *Return Type: object*
> *Script: a Groovy block that calls `https://brasilapi.com.br/api/cep/v2/{cep}`, parses the JSON, returns the result; on 404 it returns `{error: 'CEP not found'}`.*
> *Want me to also strip non-digits from the input?"*

You read the draft, accept it, save. Two minutes from idea to working tool.

The Vibe Coding chat is **incremental**, you can revise individual parts:

> **You:** *"Make the description more specific to addresses, and add a note that the script handles malformed CEP."*
>
> **Assistant:** *"Updated. The LLM Description now says: 'Returns the full Brazilian address... Robust against formatted CEP inputs (handles "01310-100" and "01310100" identically). Returns an error object when the CEP isn't found.'"*

The meta-prompts (`descriptionMetaPrompt`, `llmDescriptionMetaPrompt`, `groovyMetaPrompt`) are stored alongside each piece. The next time you Vibe-Code-revise, the assistant has full context of *what you asked for* and *what it generated*, so revisions stay coherent.

:::tip[The LLM Description deserves the most care]
The LLM Description is the single field that determines whether agents actually use your tool. A vague description (*"BrasilAPI lookup"*) means the model won't pick it when a user asks about an address. A specific description (*"Returns the full Brazilian address (street, neighborhood, city, state) for a postal code. Use when the user asks about an address by CEP, or wants to verify a CEP is valid."*) means the model picks it confidently. **Vibe Coding usually nails this on the first try because the assistant knows the LLM perspective from inside.** Read it back to yourself before saving.
:::

---

## Integrating Custom Tools

A Custom Tool is just an entry in the catalog until you wire it somewhere. Three places it can run:

### In an AI Agent

Open an [AI Agent](/docs/ai-agents/), navigate to the Tools tab. Custom Tools appear in their own group at the bottom (after the eight native categories). Check the ones this agent should have access to.

When the agent runs, those Custom Tools are merged with the native ones into a single tool catalog the LLM sees. The LLM picks among them by name + description, exactly like native tools.

The same agent can mix:

- Native tools (e.g., `search_knowledge_base`, `get_current_time`),
- MCP server tools (e.g., a CRM lookup),
- Custom Tools (e.g., your CEP lookup, your scoring formula).

The LLM doesn't distinguish: they all look like callable functions in the same catalog.

### In a Chat Flow

A [Chat Flow](/docs/chat-flow/) Tool node accepts `native`, `mcp`, or `custom` as its tool source. With `custom`, you point the node at one of your Custom Tools and pass arguments either statically (hard-coded values) or from collected flow variables.

Use this pattern when:

- A flow needs to look up data deterministically *between questions* (not when the LLM thinks it's a good time).
- You want auditability: the submission record will show that the tool ran with these inputs and got that result.

### In the Direct LLM Chat

The direct [Chat](/docs/chat/) mode (no agent) doesn't expose Custom Tools by default, that mode is for *general-purpose assistance with optional toolboxes*. If you need a Custom Tool in direct chat, attach it to a thin "default agent" instead. This keeps the catalog manageable.

---

## Three Real Custom Tools

### 1. Lead-Scoring Tool

**Use case:** A sales agent collects company size + role + timeline and needs to score the lead.

**LLM Description:** *"Computes a lead score from 0 to 100 based on company size, role title, and decision timeline. Returns the score and a tier label (HOT / WARM / COLD)."*

**Parameters:**
```json
[
  { "name": "company_size", "type": "integer" },
  { "name": "role", "type": "string" },
  { "name": "timeline", "type": "string" }
]
```

**Return Type:** `object`

**Groovy:**
```groovy
def sizeScore = company_size > 1000 ? 40 : company_size > 100 ? 25 : 10
def roleScore = role.toLowerCase() ==~ /.*(ceo|cto|vp|director).*/ ? 30 : 15
def timelineScore = timeline == 'short_term' ? 30 : timeline == 'long_term' ? 15 : 5

def total = sizeScore + roleScore + timelineScore
def tier = total >= 70 ? 'HOT' : total >= 40 ? 'WARM' : 'COLD'

return [score: total, tier: tier]
```

Wire this into the demo-qualification [Chat Flow](/docs/chat-flow/) right before End. The submission record now has the score; downstream automation routes accordingly.

### 2. Holiday-Aware Working Days Calculator

**Use case:** A support agent estimates SLA timing.

**LLM Description:** *"Calculates the number of working days between two dates, excluding Brazilian national holidays. Useful for SLA estimates."*

**Parameters:**
```json
[
  { "name": "start_date", "type": "string" },
  { "name": "end_date",   "type": "string" }
]
```

**Return Type:** `integer`

**Groovy:**
```groovy
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek

def start = LocalDate.parse(start_date)
def end   = LocalDate.parse(end_date)

def holidays = [
  '2026-01-01', '2026-04-21', '2026-05-01',
  '2026-09-07', '2026-10-12', '2026-11-02',
  '2026-11-15', '2026-12-25'
].collect { LocalDate.parse(it) } as Set

def days = 0
def cursor = start
while (cursor.isBefore(end) || cursor.isEqual(end)) {
    if (cursor.dayOfWeek != DayOfWeek.SATURDAY
        && cursor.dayOfWeek != DayOfWeek.SUNDAY
        && !holidays.contains(cursor)) {
        days++
    }
    cursor = cursor.plusDays(1)
}
return days
```

The agent doesn't have to know about your country's calendar; the tool handles it.

### 3. Internal API Wrapper

**Use case:** Your support agent needs to look up a customer's tier from your internal API.

**LLM Description:** *"Looks up a customer's subscription tier (FREE / STARTER / PRO / ENTERPRISE) from their email. Use this before quoting features or pricing limits."*

**Parameters:**
```json
[
  { "name": "email", "type": "string" }
]
```

**Return Type:** `object`

**Groovy:**
```groovy
import groovy.json.JsonSlurper

def url = "https://internal-api.example.com/customers/by-email?email=${URLEncoder.encode(email, 'UTF-8')}"
def conn = new URL(url).openConnection()
conn.setRequestProperty('Authorization', "Bearer ${System.getenv('INTERNAL_API_TOKEN')}")
conn.requestMethod = 'GET'

if (conn.responseCode == 404) return [tier: 'NONE', found: false]
if (conn.responseCode != 200)  return [error: "API error ${conn.responseCode}"]

def data = new JsonSlurper().parseText(conn.inputStream.text)
return [tier: data.tier, since: data.created_at, found: true]
```

The agent is now grounded: it never quotes ENTERPRISE features to a STARTER customer.

:::tip[Secrets in Groovy]
Use `System.getenv('YOUR_TOKEN')` for secrets, not literal strings. The Groovy text is logged when the tool runs (for debugging), and the script may be exported with the agent definition. Pull tokens from the JVM environment so they don't show up in logs or exports.
:::

---

## How the LLM Sees Your Tool

When an agent runs, the system builds a tool catalog the LLM uses to pick. Each Custom Tool produces a JSON-Schema function definition like this:

```json
{
  "type": "function",
  "function": {
    "name": "cep_lookup",
    "description": "Returns the full Brazilian address (street, neighborhood, city, state) for a postal code. Use when the user asks about an address by CEP, or wants to verify a CEP is valid.",
    "parameters": {
      "type": "object",
      "properties": {
        "cep": { "type": "string" }
      },
      "required": ["cep"]
    }
  }
}
```

The model decides on its own when to call it. When it does, Viglet Turing:

1. Validates the arguments against the parameter declaration.
2. Executes the Groovy script with the bindings.
3. Captures the return value.
4. Feeds it back to the LLM in the next reasoning step.

If the script throws, the error message is fed back to the LLM as the tool result, the model usually recovers gracefully (*"I couldn't look up that CEP, could you double-check the digits?"*).

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/custom-tool` | List all custom tools |
| `GET` | `/api/custom-tool/{id}` | Get a single tool |
| `POST` | `/api/custom-tool` | Create a tool |
| `PUT` | `/api/custom-tool/{id}` | Update a tool |
| `DELETE` | `/api/custom-tool/{id}` | Delete a tool |
| `POST` | `/api/custom-tool/validate` | Validate a script: returns syntax errors with line/column |
| `POST` | `/api/custom-tool/ai-chat` | Vibe Coding endpoint: describe a tool, get a draft back |

The validate endpoint runs a fast Groovy parse without executing the script, a smoke test before save.

---

## Permissions

| Permission | Allows |
|---|---|
| `CUSTOM_TOOL_VIEW` | View custom tools |
| `CUSTOM_TOOL_CREATE` | Author new tools |
| `CUSTOM_TOOL_EDIT` | Edit existing tools |
| `CUSTOM_TOOL_DELETE` | Delete tools |

All require `ROLE_ADMIN`. In production, restrict authoring to a small group: Groovy is full Java, and bad scripts can do real damage.

---

## Testing & Iteration

The fastest dev loop:

1. Open the Custom Tool form, write or Vibe-Code a draft.
2. Click **Validate** to catch syntax errors instantly.
3. Save.
4. Attach the tool to a test [AI Agent](/docs/ai-agents/) (or use a "sandbox" agent you keep around).
5. Open [Chat](/docs/chat/), pick that agent, ask a question that should trigger the tool.
6. Watch the response. If the LLM didn't pick the tool, your **LLM Description** is too vague. If it picked it but with wrong arguments, your **Parameters JSON** doesn't match the script's bindings. If it picked it but the script errored, the script needs work.
7. Edit the tool. The agent picks up the change immediately, no restart.

For deeper diagnostics, [Observability](/docs/observability/) shows tool latency through the `turing.llm.calls` timer, and [Chat Analytics](/docs/chat-analytics/) shows whether the agent's goal-rate moves when you change the tool.

---

## Common Patterns

### One tool, multiple agents

A `cep_lookup` tool is useful to a sales agent (verifying a prospect's address), a support agent (validating a shipping address), and an onboarding coach (verifying signup data). Build it once; check it on three agents. No duplication.

### Tools that wrap MCP servers

If you already have an MCP server, you usually don't need a Custom Tool that calls the same thing. Custom Tools shine when the work is *small enough that running an MCP is overkill*. The cutoff in practice: if the logic is more than 50 lines of Groovy or needs durable state, build an MCP. Otherwise, Custom Tool wins on operational simplicity.

### Self-documenting business logic

A Groovy script reads top-to-bottom. When your finance team disputes how the lead score is computed, they can read the script. A custom tool can become *the* source of truth for a piece of business logic: readable, versionable, and callable.

### Composability with Chat Flow

A complex business process is often: collect inputs → compute → call external system → format response. **Chat Flow** does the collection and the orchestration; **Custom Tools** do the compute and the external calls; **Native tools or MCP** do the formatting (e.g., open a ticket). The three together replace what would otherwise be a custom microservice.

---

## Diagnostics

| Symptom | Likely cause | Fix |
|---|---|---|
| LLM doesn't call the tool when it obviously should | LLM Description is vague; or the agent has too many tools and yours got drowned out | Sharpen the LLM Description with concrete trigger phrases (*"Use when..."*); or reduce the agent's tool count |
| LLM calls the tool with wrong argument names | Parameters JSON doesn't match script bindings | Names in Parameters JSON must exactly equal the bindings used in Groovy |
| Script throws at runtime | Network issue, parse error, NPE in the script | Use the Validate endpoint; add `try/catch`; return an `[error: ...]` object instead of throwing |
| Tool works locally but fails in chat | Auth header missing in the script (e.g., environment variable not set on this server) | Verify env vars; or move the auth into the global config and reference it |
| Latency spike | The script makes a slow API call | Add a timeout: `conn.connectTimeout = 3000; conn.readTimeout = 3000` |

---

## Related Pages

| Page | Description |
|---|---|
| [Tool Calling](/docs/tool-calling/) | The 27 native tools: when to use a native tool vs. a custom one |
| [MCP Servers](/docs/mcp-servers/) | When to build an MCP instead of a Custom Tool |
| [AI Agents](/docs/ai-agents/) | Agents are where Custom Tools get attached and used |
| [Agent Workspace](/docs/agent-workspace/) | The `workspace` binding: persist artifacts across turns |
| [Chat Flow](/docs/chat-flow/) | Use Custom Tools as deterministic Tool nodes in a flow |
| [Observability](/docs/observability/) | Watch tool latency and error rates in real time |

---

