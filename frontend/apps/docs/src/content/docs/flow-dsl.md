---
title: "Flow DSL"
description: "@viglet/turing-flow-dsl: author Viglet Turing chat flows as typed TypeScript source, then transpile to the editor's JSON wire format. TypeScript catches enum typos (tone, guardrail, trigger) at compile time. Zero-dependency."
---
**`@viglet/turing-flow-dsl`** lets you author Viglet Turing [chat flows](/docs/chat-flow/) as **typed TypeScript source** instead of drawing them in the visual editor. You write a flow as a plain object, and the DSL **transpiles** it to the exact JSON wire format the editor imports: `{ graph: { nodes, edges }, name, slots, personas, … }`. Because it's typed, the TypeScript compiler catches enum typos (a misspelled `tone`, `guardrailMethod`, `triggerMode` or an unknown node `type`) at **build time**, before the flow ever reaches a running instance. Zero runtime dependencies.

**When you'd reach for it.** Use the DSL when you want **flows-as-code**: reviewable diffs, refactoring across many flows, sub-flow reuse, and CI validation: the things a visual editor can't give you. It's the natural authoring layer for a **[`@viglet/turing-cli`](/docs/cli/)** project (`flows/*.chat-flow.json`), but it's independent: you can also paste the compiled JSON into the editor's **Import JSON** button.

> **The workflow.** `flow.ts` (you write) → **transpile** → `flow.chat-flow.json` (wire format) → **import/deploy** to an agent. The DSL owns the first arrow; the CLI (or the editor, or `turing-flow-dsl deploy`) owns the last.

---

## Install

```bash
npm add -D @viglet/turing-flow-dsl
```

This gives you the library (import in `.flow.ts` files) and a `turing-flow-dsl` binary (build/deploy).

---

## Your first flow

Author a flow with `defineFlow`, an identity helper that preserves the literal-typed shape so `tsc` can check every enum:

```ts
// flows/lead-capture.flow.ts
import { defineFlow } from "@viglet/turing-flow-dsl";

export default defineFlow({
  name: "Lead capture",
  description: "Collect name, email and area of interest.",
  triggerMode: "ONCE",              // ✅ "ONCE" | "ALWAYS": "ONCEE" is a compile error
  triggerLanguage: "PT",
  guardrailMethod: "LLM_JUDGE",     // ✅ "HEURISTIC" | "LLM_JUDGE" | "STRUCTURED_OUTPUT"
  slots: [
    { name: "name",  description: "Visitor first name", type: "STRING" },
    { name: "email", description: "Visitor primary email", type: "STRING" },
    { name: "area",  description: "Area of interest", type: "STRING" },
  ],
  nodes: [
    { id: "start", type: "start" },
    {
      id: "ask-name",
      type: "aiQuestion",
      aiInstruction: "Greet the visitor and ask their name. One warm sentence.",
      outputVariable: "name",
    },
    {
      id: "ask-email",
      type: "aiQuestion",
      aiInstruction: "Use {{name}}. Ask for the best contact email.",
      outputVariable: "email",
      validationRule: "email",        // ✅ email|phone|url|number|date|cpf|cnpj|cep|none
    },
    {
      id: "ask-area",
      type: "aiQuestion",
      aiInstruction: "Ask {{name}} which area interests them.",
      outputVariable: "area",
      inlineOptions: ["Leadership", "Technology", "Sales", "Other"],
    },
    { id: "end", type: "end" },
  ],
  edges: [
    { source: "start", target: "ask-name" },
    { source: "ask-name", target: "ask-email" },
    { source: "ask-email", target: "ask-area" },
    { source: "ask-area", target: "end" },
  ],
});
```

Notice what you **don't** write: node positions, edge ids, and default labels. The transpiler fills them in (see below).

---

## Transpiling

Two ways: CLI for a build pipeline, programmatic for scripts/tests.

### CLI

```bash
# 1. compile your .flow.ts files to .flow.js (any tsc/tsup/esbuild setup works)
tsc

# 2. transpile to the .chat-flow.json wire format
turing-flow-dsl build build/flows --out dist
```

`turing-flow-dsl build <dir> [--out <dir>]` scans for `*.flow.js`/`*.flow.mjs` (compiled modules, default or `flow`/`flows` export) and `*.flow.json` (raw editor exports, normalized and passed through), emitting `<name>.chat-flow.json` (default output `./dist`). Exit code 1 on any validation error.

### Programmatic

```ts
import { transpileFlow, transpileBundle } from "@viglet/turing-flow-dsl";
import flow from "./flows/lead-capture.flow.js";

const wire = transpileFlow(flow); // → { name, graph: { nodes, edges }, slots, … }
```

### What the transpiler does for you

- Fills each node's `data.label` default per type (`START`, `AI QUESTION`, …).
- Lays nodes out vertically (`x: 0`, `y: index * 120`) for a readable initial canvas.
- Synthesizes edge ids (`e1`, `e2`, …) when you omit them.
- Applies defaults: `guardrailMethod = "LLM_JUDGE"`, `triggerMode = "ONCE"`, `triggerLanguage = "AUTO"`.
- Coerces blank optional fields to `null`.

### What it validates (throws `FlowSpecError`)

- Exactly **one** `start` node; no duplicate node ids.
- Every edge's `source`/`target` matches a declared node.
- `condition` nodes have exactly two outgoing edges, `sourceHandle: "yes"` and `"no"`.
- `switch` edges' `sourceHandle` matches a `switchOptions[].id` (or is blank = the single wildcard fallback).
- In a bundle: every flow has a stable, unique `id`, and every `subFlowId` reference resolves.

---

## Node types

Build a node by adding an object to `nodes[]`, discriminated on `type`. The full catalog:

| `type` | Purpose | Key fields |
|---|---|---|
| `start` / `end` | Flow entry / exit | just `id` |
| `aiQuestion` | Ask the visitor something; capture the answer | `aiInstruction`, `outputVariable`, `validationRule?`, `inlineOptions?`, `onJudgeReject?`, `toolsEnabled?` |
| `formCapture` | Native multi-field form (T107) | `formFields[]` (name/label/type/required/options), or `aiInstruction` for the conversational fallback |
| `condition` | Branch yes/no on an expression | `conditionExpression` (e.g. `area=='Sales'`); needs `yes`/`no` edges |
| `switch` | Branch on a slot value | `switchVariable`, `switchOptions[]` |
| `subFlow` | Descend into another flow in the bundle | `subFlowId` |
| `subFlowSwitch` | Pick a sub-flow by slot value (T47) | `switchVariable`, `switchOptions[]` (each with `subFlowId`) |
| `functionCall` | Call a NATIVE `@Tool` or an MCP tool (T49) | `toolSource`, `functionName` \| `mcpServerId`, `outputVariable?`, `continueOnFailure?` |
| `scheduleAgent` | Dispatch an async [routine](/docs/routines/) (T48) | `routineId`, `outputVariable?`, `routineTimeoutMs?`, `continueOnFailure?` |
| `persona` | Switch [persona](/docs/personas/) for downstream turns | `personaId` |
| `slot` | Set or delete a slot | `slotName`, `slotOperation` (`SET`/`DELETE`), `slotValue` |
| `writeSlot` | Write a slot (supports `{{slot}}` interpolation) | `slotName`, `slotValue` |
| `humanApproval` | Park for a human decision (T119) | `humanApproval: { channel, target?, timeoutSeconds?, timeoutBehavior? }` |
| `suspend` | Park the cursor until `POST …/chat/resume` (T121) | just `id`/`label` |

`aiInstruction` and `slotValue` support `{{slotName}}` interpolation against captured slots. `continueOnFailure: true` routes failures to a `sourceHandle: "failure"` edge instead of aborting the turn: see [chat flow error handling](/docs/chat-flow/).

### Declaring slots and personas

Declare `slots` and `personas` at the flow level so the import auto-creates them on the agent:

```ts
slots: [{ name: "email", description: "Contact email", type: "STRING" }],
personas: [{
  id: "advisor",
  name: "Course Advisor",
  systemInstruction: "You are a warm, concise academic advisor.",  // note: systemInstruction, not systemPrompt
  tone: "CASUAL",             // FORMAL | CASUAL | TECHNICAL | EXECUTIVE
  languageStyle: "PERSUASIVE",
  verbosity: 3,              // 1–5 register dial
}],
```

Then reference a persona from a `persona` node via `personaId: "advisor"`. Slot `type` is one of `STRING | INTEGER | BOOLEAN | FLOAT | TEXT`: note that `email`/`phone`/etc. are **validation rules** on the capturing node, not slot types.

---

## Bundles (sub-flows)

A real agent is usually a **main flow plus sub-flows**. Wrap them with `defineBundle` and transpile with `transpileBundle`, which additionally validates cross-flow references and fills each `subFlowName` from the referenced flow:

```ts
import { defineFlow, defineBundle } from "@viglet/turing-flow-dsl";

export const flows = defineBundle([
  defineFlow({
    id: "main", name: "Main",
    nodes: [
      { id: "start", type: "start" },
      { id: "route", type: "subFlowSwitch", switchVariable: "intent",
        switchOptions: [
          { id: "sales", label: "Sales", subFlowId: "sales-flow" },
          { id: "support", label: "Support", subFlowId: "support-flow" },
        ] },
      { id: "end", type: "end" },
    ],
    edges: [
      { source: "start", target: "route" },
      { source: "route", target: "end", sourceHandle: "sales" },
      { source: "route", target: "end", sourceHandle: "support" },
    ],
  }),
  defineFlow({ id: "sales-flow", name: "Sales", nodes: [/* … */], edges: [/* … */] }),
  defineFlow({ id: "support-flow", name: "Support", nodes: [/* … */], edges: [/* … */] }),
]);
```

`transpileBundle(flows)` returns the array you feed to the atomic **import-bundle** endpoint (which the [CLI](/docs/cli/)'s `deploy` uses).

---

## Deploying

Three paths, easiest first:

1. **CLI (recommended)**: put the compiled `*.chat-flow.json` in a project's `flows/` folder and run `turing deploy`. See **[Turing CLI](/docs/cli/)**.
2. **`turing-flow-dsl deploy <dir>`**: the DSL's own deploy command uploads every `*.chat-flow.json` to a running instance (idempotent by name; `--mode bundle|single`, `--create-only`). Config resolves from a `turing` block in `package.json` → env vars (`TURING_URL`, `TURING_AGENT_ID`, `TURING_USERNAME`, `TURING_PASSWORD`) → flags → interactive password prompt.
3. **Editor**: paste the JSON into the chat-flow editor's **Import JSON** button.

```json
// package.json
{
  "scripts": {
    "build:flows": "tsc && turing-flow-dsl build build/flows --out dist",
    "deploy": "pnpm build:flows && turing-flow-dsl deploy dist"
  },
  "turing": { "url": "https://turing.example.com", "agentId": "abc-123", "username": "you", "mode": "bundle" }
}
```

> **CI note.** Both `build` and `deploy` fail with a non-zero exit code on a validation error, and `deploy` needs `TURING_PASSWORD` (or `TURING_TOKEN` via the CLI) in the environment since there's no TTY to prompt.

---

## Related pages

- **[Chat Flow](/docs/chat-flow/)**: the visual editor and the full node/slot/trigger semantics the DSL emits.
- **[Turing CLI](/docs/cli/)**: the project workflow that deploys compiled flows.
- **[Routines](/docs/routines/)**: the async tasks a `scheduleAgent` node dispatches.
- **[Human-in-the-loop](/docs/human-in-the-loop/)**: the `humanApproval` node.
- **[Custom Tools](/docs/custom-tools/)**: the NATIVE/MCP tools a `functionCall` node invokes.
- **[Personas](/docs/personas/)**: persona declarations and the `persona` node.

