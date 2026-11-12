---
title: "Personas"
description: "Reference for Viglet Turing personas: every field and value, prompt order, content-fit, Persona Match, dialogue, synthetic research and the REST API."
---

A **persona** is the voice profile an [AI agent](/docs/ai-agents/) speaks in, or the reader
profile you measure content against. This page is the reference for administrators and
developers configuring personas: every field and its values, the defaults, the actions
on a persona, and the endpoints. Personas live in **Administration → Personas**
(`/bento/persona`) and are reusable across agents.

:::tip
For why personas exist, what each setting does to an answer, and worked examples and
customer scenarios, read [The Persona Book](/docs/persona-book/).
:::

## Persona fields

The fields are grouped as the persona form groups them.

### Identity and voice

| Field | Values | Default | Description |
|---|---|---|---|
| **Persona Kind** (`personaKind`) | `SPEAKER`, `AUDIENCE`, `BOTH` | `SPEAKER` | A voice an agent speaks in, a reader you evaluate content against, or both. An `AUDIENCE` persona is never used as a voice |
| **Name** | Text | | Internal identifier, never shown to end users |
| **Description** | Text, up to 500 characters | | When to use the persona; shown in the agent's persona list |
| **Enabled** | On, off | On | Off retires the persona without deleting it; its agents answer without a persona |
| **System Instruction** (`systemInstruction`) | Text | | Placed before the agent's own system prompt: who the persona is and how it speaks |
| **Tone** (`tone`) | `FORMAL`, `CASUAL`, `TECHNICAL`, `EXECUTIVE` | | The register of the answer |
| **Verbosity** (`verbosity`) | 1 to 5 | | Lower is shorter |
| **Language Style** (`languageStyle`) | `NEUTRAL`, `DIRECT`, `NARRATIVE`, `PERSUASIVE`, `INSTRUCTIONAL` | | The shape of the answer |
| **Mandatory Terms** (`mandatoryTerms`) | Pipe-separated (`\|`) list | Empty | Words and phrases the model must use |
| **Forbidden Terms** (`forbiddenTerms`) | Pipe-separated (`\|`) list | Empty | Words and phrases that must never reach the user; see [tone validation](#tone-validation) |
| **Few-shot Store** (`fewShotStore`) | An [embedding store](/docs/embedding-stores/) | None | Model conversations retrieved as examples; see [the few-shot store](#the-few-shot-store) |
| **Brand Context MCP** (`brandContextMcpServer`) | An [MCP server](/docs/mcp-servers/) | None | Live brand facts added to the prompt; see [brand context](#brand-context) |
| **Calibrate model from style** (`calibrateModelParams`) | On, off | Off | Lets tone and verbosity set the sampling parameters; see [style calibration](#style-calibration) |

In the form you type one term per line in **Mandatory Terms** and **Forbidden Terms**;
Viglet Turing stores them joined with pipes, exactly as received, so a term can contain a
comma. An empty list adds no constraint.

### Personality and grounding

| Field | Values | Default | Description |
|---|---|---|---|
| **Openness**, **Conscientiousness**, **Extraversion**, **Agreeableness**, **Neuroticism** | 0 to 100 each | Not set | Big Five (OCEAN) traits. Only values outside 34 to 66 reach the prompt, as behavioural guidance; with style calibration on they also nudge the temperature |
| **Grounding source** (`groundingSource`) | `NONE`, `SN_SITE`, `NOTEBOOK` | `NONE` | Where answers come from: the model's own knowledge, an indexed [SN site](/docs/semantic-navigation/), or the persona's own notebook sources. If the search fails, the conversation continues without the passages |

### Audience profile

Shown for `AUDIENCE` and `BOTH` personas.

| Field | Values | Description |
|---|---|---|
| **Reading level** | `ELEMENTARY(5)`, `MIDDLE(8)`, `SECONDARY(12)`, `UNDERGRADUATE(15)`, `GRADUATE(18)` | The number is the reader's approximate years of study |
| **Domain expertise** | `NOVICE`, `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT` | How much the reader knows of the subject |
| **Vocabulary ceiling** | List of terms | Terms the reader knows; any other word can count as difficult |
| **Comprehension notes**, **Accessibility notes** | Text | Free-text notes on the reader |
| **Primary language** | ISO 639 code (`pt`, `en`, `es`) | The reader's language |

## How the prompt is assembled

A persona adds to the agent's system prompt; it does not replace it. On each turn the
prompt is built in this order:

1. The agent's system prompt.
2. The persona's system instruction.
3. The style block from tone, verbosity and language style, for example *"Use an
   EXECUTIVE tone, verbosity 2 (concise), PERSUASIVE language."*
4. *"Always use:"* and *"Never use:"* lists from the mandatory and forbidden terms.
5. The personality traits outside the neutral band.
6. The brand facts from the brand context MCP server.
7. The grounding passages for the current message.
8. The tool definitions.
9. The few-shot examples for the current message.
10. The conversation history.
11. The current user message.

Layers 1 to 6 are cached as a static block and rebuilt when the persona is edited or
deleted. After the model answers, [tone validation](#tone-validation) runs before the
answer reaches the user.

## Style calibration

With **Calibrate model from style** on, the style sets the sampling parameters on each
turn instead of the [LLM instance](/docs/llm-instances/):

| Style field | Sets | Mapping |
|---|---|---|
| **Verbosity** | Maximum tokens | 1 → 256, 2 → 512, 3 → 1024, 4 → 2048, 5 → 4096 |
| **Tone** | Temperature | `TECHNICAL` 0.2, `FORMAL` 0.3, `EXECUTIVE` 0.4, `CASUAL` 0.8 |

With no tone set, the temperature is left as the LLM instance sets it. With the toggle
off, the LLM instance's temperature and maximum tokens apply. Calibration applies to
agent chat, [direct chat](#open-chat) with the persona and
[Persona Dialogue](#persona-dialogue).

## Tone validation

Every assistant answer is checked against the persona's forbidden terms before the user
sees it. A match is masked as `[***]`; adjacent masks merge into one. The answer is not
blocked, and the match is logged. The check reads the answer and the terms in Portuguese,
English or Spanish, matches variations of a word (singular and plural, conjugations) and
whole phrases. It is always on when a persona is attached.

## The few-shot store

A persona's **Few-shot Store** is an [embedding store](/docs/embedding-stores/) holding
question-and-answer pairs written in the persona's voice. On each turn, Viglet Turing
embeds the current user message, retrieves the three most similar pairs with a similarity
of at least 0.5, and adds them to the prompt as examples before the user's message.

To set one up:

1. Create an embedding store collection for the persona, for example
   `sales-persona-fewshot`.
2. Index question-and-answer pairs in it, each a question your customers ask and the
   answer in the persona's voice. Sessions from [Chat Analytics](/docs/chat-analytics/)
   that reached their goal are a good source.
3. Select the collection in the persona's **Few-shot Store** field.

The store should hold only examples in the persona's voice. Content for the persona to
answer from belongs in its [grounding source](#personality-and-grounding).

## Brand context

The **Brand Context MCP** field points the persona at an MCP server you control. In each
conversation Viglet Turing calls one of its tools (for example `get_brand_context`) and
adds the returned JSON to the prompt as a *"Current brand facts:"* block. Keep the answer
small and current: filter it on the server.

## Choose a persona for an agent

Attach personas in the agent editor's **Persona** tab
(`/bento/ai-agent/instance/:id/persona`): select the personas the agent may use and star
the default. On each turn the persona that governs is, in order: the one a
[chat flow](/docs/chat-flow/) switched to (`__activePersonaId` in the flow state), the one
the request names (`requestPersonaId`), and the agent's default.

To choose:

1. Decide whether the agent faces customers. If it does, give it a persona, even a minimal
   one; an internal agent can do without.
2. Pick a persona built for the agent's funnel stage (discovery, evaluation, onboarding,
   retention, escalation) rather than one persona for every stage.
3. Check that the persona's few-shot store reflects the product as it is, and review it
   every quarter.

## Persona actions

A persona's detail page is a dashboard with a card per part of its configuration and a
**Launchpad** of actions.

### Open chat

**Open chat** starts a conversation with the persona itself, with no agent. It runs on any
enabled LLM instance you pick, keeps no history, accepts attachments and the native tools,
and has a shareable URL, `/bento/chat/persona/{id}`. It is offered for `SPEAKER` and
`BOTH` personas only.

### Validate content

For an `AUDIENCE` or `BOTH` persona, **Validate content** opens a one-persona
[Persona Match](#persona-match) project for it, creating it the first time. For a
`SPEAKER` persona the action is disabled.

### Persona Dialogue

**Persona Dialogue** (`/bento/persona/dialogue`, from the personas list) runs a
turn-by-turn conversation between personas, organized as saved projects. A project has:

| Setting | Values |
|---|---|
| **Topic** | Text |
| **Speakers** | Two or more `SPEAKER` or `BOTH` personas; the badge number is the speaking order |
| **LLM instance** | Any enabled instance |
| **Turns** | 2 to 300, default 10 |

The first speaker receives the topic; each reply goes to the next speaker in turn until
the budget runs out. Each speaker must end its reply with a question, and the last round
closes the conversation. The transcript streams as it is generated and is saved; a re-run
replaces it. If a turn fails, the partial transcript is kept with a notice.

### Suggest persona

**Suggest persona** (`/bento/persona/suggest`) scores a pasted piece of content against
every enabled audience persona, or a subset you choose, and lists them from best fit to
worst with the same detail as a [fit report](#fit-report).

## Audience personas and content-fit

### Evaluation notebook sources

An audience persona's **Evaluation notebook** holds the content to evaluate:

| Source type | Value | Text comes from |
|---|---|---|
| Indexed document | `SN_DOC` | A document in an SN site, looked up per locale |
| File | `ASSET` | An uploaded file (PDF, DOC and others), with the text extracted |
| Link | `URL` | A web page, fetched and extracted; see [content ingestion](#content-ingestion-fetching-js-rendered-pages) for JavaScript-rendered pages |

Each source is `PENDING`, `EXTRACTED` or `FAILED`, and a failed one can be extracted
again. Extracted text is cached.

### Content-fit scoring

| Signal | How it is computed |
|---|---|
| Readability | Without an LLM, in Portuguese, English and Spanish: Flesch reading ease and Flesch–Kincaid grade adapted to the language, sentence length, syllables per word, the share of difficult words (not counting the vocabulary ceiling) and passive voice, as a score from 0 to 100 against the reader's level |
| LLM evaluation | The model reads the source as the persona and returns a fit score, what fits, and what does not (`span`, `reason`, `suggestion`). A flagged passage that does not appear word for word in the source is dropped |
| Final score | The average of the two; readability alone when no default LLM is set or its answer cannot be read |

### Fit report

`POST /api/persona/{id}/content-fit` evaluates one source (`?sourceId=`) or the whole
notebook and returns the notebook's average and a verdict per source. The persona's
**Fit report** section colours each score green at 70 and above, amber at 40 and above,
and red below 40, and lists what fits and the flagged passages with their suggested
rewrites.

## Persona Match

**Persona Match** (`/bento/persona/match`, **Administration → Personas → Persona Match**)
evaluates many contents against many personas as a saved project.

### Projects

| Setting | Description |
|---|---|
| **Contents** | Added once per project: a link, an indexed SN site document or an uploaded file. The text is extracted and cached; a re-run skips contents whose text has not changed |
| **Personas** | Any set of personas |
| **Schedule** | `Manual`, `Daily` or `Weekly` |
| **LLM instance** | Optional; defaults to the global default LLM. It produces both the scores and the suggested adjustments. Changing it re-scores on the next run |
| **Fit threshold** | Optional; the project's good-fit bar. Without one, the deployment default applies: 75, set by `turing.persona-match.fit-threshold` |

A run scores every content and persona pair with the content-fit evaluation and shows the
result three ways: a grid coloured by fit that fills in while the run streams, with a
drawer per cell; **by content**, the personas ranked for each content; and **by
persona**, the contents ranked for each persona. Both lenses export to PDF.

A `Daily` or `Weekly` project runs on its own, once across the cluster: it extracts its
sources again and recomputes only the cells whose content changed. A `Manual` project runs
when you select **Run analysis**.

### The tuning loop: fit trend, drift & suggested adjustments

Each run keeps a history point per cell. Once a project has run, its **Tuning loop**
section shows:

- **Average fit per run**, as a line chart.
- **Dropped / Improved since last run**: the pairs that moved between the last two runs,
  sharpest first, with both scores and the change. A pair that fell below the fit
  threshold is flagged, and a pair whose text changed is marked **content changed**.
- A change marker on each cell, and a sparkline of the pair's history in its drawer.

Trend and drift are computed from the stored scores, with no LLM call. Each cell keeps its
latest 24 points (`turing.persona-match.history.max-points`), and a history is deleted
with its content, persona or project.

**Suggest adjustments** proposes edits to the contents that score low and to the
personas whose audience profile the contents keep missing. Suggestions come from the
project's LLM, grounded on the grid, the flagged passages and the drift; a suggestion that
names a content or persona outside the project is discarded. Without an LLM, or when its
answer cannot be read, the list is built from the rewrite suggestions already in the
cells. **Regenerate** asks for a new set. Content suggestions are advice only: the content
lives in your CMS.

#### Applying a persona adjustment

When a suggestion names a persona field and a value, its card offers **Apply to this
persona**, showing each change as current value → proposed value with the reason. You
tick the fields to change and select **Apply**; nothing is ticked in advance.

- Eligible fields: **Reading level**, **Domain expertise**, **Vocabulary ceiling** and
  **Comprehension notes**. **Accessibility notes** and **Primary language** are never
  changed.
- A field that cannot be applied, such as a value outside the allowed set or one already
  in place, is reported, and the other ticked fields are still applied.
- Applying needs the same permission as editing a persona: `ROLE_ADMIN` or
  `AI_AGENT_EDIT`.
- A suggestion built without an LLM names no value, so it cannot be applied.

Every applied field is recorded with the value it replaced, the reason, the project that
proposed it, whether an LLM produced it, and who confirmed it.

<a id="content-ingestion-fetching-js-rendered-pages"></a>

### Content ingestion: pages rendered by JavaScript

A link source is fetched with a plain HTTP request by default. A page rendered by
JavaScript returns an almost empty shell, so the source fails with *"No text could be
extracted"*. To render such pages in a headless browser, set **Administration → Settings
→ Platform → Content Ingestion**:

| Setting | Values | Description |
|---|---|---|
| **Fetch mode** | **Simple** (default), **Auto**, **Headless** | Simple fetches only. Auto renders a page in the browser when the plain fetch extracts too little text. Headless renders every page |
| **Browserless URL** | URL | A [`browserless/chromium`](https://www.browserless.io/) sidecar, for example `http://browserless:3000`. `docker-compose.yaml` includes a `turing-browserless` service. Keep it on an internal network |
| **Browserless token** | Text | The sidecar's token, if it has one |

**Check browserless** tests the sidecar before you save. The properties
`turing.url-fetch.mode` and `turing.url-fetch.browserless-url` take precedence over the
screen. This applies to Persona Match contents and to notebook link sources.

## Synthetic user research

**Synthetic research** (`/bento/persona/research`, **Administration → Personas →
Synthetic research**) interviews a cohort of personas against a protocol and synthesizes an
insights report.

:::caution
Synthetic research is a discovery aid, not a substitute for real users, and has no
statistical validity. Validate with real users before acting.
:::

### Study fields

| Field | Values | Description |
|---|---|---|
| **Goal**, **Hypothesis** | Text | What you want to learn; the hypothesis is optional |
| **Protocol** | `DYNAMIC_SCRIPT` (dynamic interview), `CUSTOM_SCRIPT` (scripted questions), `CONCEPT_TEST` (concept test) | A dynamic interview adapts its follow-ups up to a maximum number of questions; scripted questions are asked word for word in order; a concept test presents a concept and records the reaction |
| **Audience** | Ordered list of personas | Each participant is interviewed separately |
| **Interview target** | The default LLM, or a deployed agent | With an agent, the persona plays the user and your agent, with its prompt, tools and RAG, answers |
| **Interview model**, **Synthesis model** | LLM instances | Optional separate models for the two stages |
| **Schedule** | `Manual`, `Daily`, `Weekly` | Re-runs the study |

**Run interviews** streams each transcript as it completes. Re-running an unchanged study
reuses what was already done.

### Insights report

The report holds an executive summary, themes ranked by relevance with verbatim quotes
traced to their persona (a quote not found in the interviews is flagged), recommendations,
views **by theme** and **by persona**, **Regenerate** and PDF export.

- **Saturation**: whether each participant, in audience order, raised a new theme. When new
  themes stop, the report says *"sample adequate at N"*. Computed without an LLM.
- **Theme affinity**: which participants cluster on each theme.
- **Insight drift**: for a scheduled study, how themes move across runs.
- **Concept fit**: for a concept test, the concept scored for each participant with the
  content-fit evaluation.

### Cohort synthesis, research assistant and program view

- **Synthesize cohort** (`/bento/persona/cohort`) drafts a varied set of personas, with
  spread personality traits, from a one-paragraph brief. You keep each draft, which opens
  the persona form, or discard it; nothing is saved automatically.
- The **Research Assistant**, offered when a study is created, drafts the name, goal,
  hypothesis, protocol and questions, and a cohort brief from a plain description. You can
  skip it.
- **Program rollup** (`/bento/persona/research/program`) combines several studies: totals,
  each study's saturation, and themes raised in two or more studies, highlighted. It adds
  no inference of its own.

### Evaluation datasets and the CLI

A completed study can be promoted to an evaluation dataset in
[agent evaluation](/docs/agent-eval/): the interviewer's questions become replay turns and
the participants' answers become reference material. The
[`turing research`](/docs/cli/#turing-research-define-run--fetch-a-study) command defines
and runs a study and fetches its insights.

## Persona-from-audio authoring

**Derive from audio** drafts a `BOTH` persona from a recording: Viglet Turing transcribes
it, classifies the voice fields (tone, verbosity, language style, vocabulary) and the
audience fields (reading level, expertise, language), and writes a character portrait as
the system instruction. The draft opens in the new-persona form and is never saved
automatically. The background variant reports its state as `QUEUED`, `TRANSCRIBING`,
`ANALYZING`, `SUCCEEDED` or `FAILED`.

Transcription uses OpenAI or an OpenAI-compatible server, chosen in **Administration →
Settings → Global Settings → Transcription**, and splits large files. See
[Transcription](/docs/transcription/).

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/persona` | List all personas |
| `GET` | `/api/persona/{id}` | Get a single persona |
| `POST` | `/api/persona` | Create a persona |
| `PUT` | `/api/persona/{id}` | Update a persona |
| `DELETE` | `/api/persona/{id}` | Delete a persona |
| `POST` | `/api/persona/chat` | The **Help me write** assistant: drafts persona fields from a conversation |
| `POST` | `/api/persona/suggest` | Rank the active audience personas by fit to a piece of content |
| `GET`/`POST`/`DELETE` | `/api/persona/{id}/source` | Manage the evaluation notebook (`/upload` multipart, `/{id}/extract` to re-extract) |
| `POST` | `/api/persona/{id}/content-fit` | Run the audience-fit report (`?sourceId` for one source, else the whole notebook) |
| `GET` | `/api/sn/{site}/personas` | The audience personas a site's anonymous content-fit accepts (`id`, `name`, `description`, ordered by name), for a client's persona picker. Same site API-key rules as content-fit; an empty list when the site has no agent. SDK: `fetchSitePersonas` |
| `POST` | `/api/sn/{site}/persona/{id}/content-fit` | Anonymous content-fit as one of those personas (read-only; 12,000-character limit). SDK: `fetchPersonaContentFit` |
| `POST` | `/api/persona/derive-from-audio` | Draft a `BOTH` persona from an audio recording (multipart; never auto-saved). `POST .../jobs` runs it in the background, with status at `GET .../jobs/{jobId}` and progress over SSE at `.../jobs/{jobId}/stream` |
| `POST` | `/api/v2/persona/{id}/chat` | Talk directly to a persona (SSE; JSON or multipart) |
| `GET`/`POST`/`PUT`/`DELETE` | `/api/persona-match` | Persona Match **projects**: CRUD, `PUT .../{id}/personas`, contents (`POST .../{id}/sources`, `/sources/upload`, `/sources/{sourceId}/extract`), `GET .../{id}/matrix` and `.../report`, and the run: `POST .../{id}/run` or `.../run/stream` (SSE, live heatmap fill) |
| `GET` | `/api/persona-match/{id}/trend` | Per-cell fit history across runs and the run-average series |
| `GET` | `/api/persona-match/{id}/drift` | How fit moved between the last two runs (sharpest regression first) |
| `POST` | `/api/persona-match/{id}/adjustments` | Suggested content and persona adjustments (`?regenerate=true` for a fresh set) |
| `GET`/`POST`/`PUT`/`DELETE` | `/api/persona-dialogue` | Persona Dialogue **projects**: CRUD, ordered `PUT .../{id}/speakers`, `GET .../{id}/transcript`, and `POST .../{id}/run/stream` (SSE, saves the transcript) |
| `POST` | `/api/v2/persona-dialogue` | Run a dialogue between personas on a topic as a stream, without saving a project |

## Related pages

| Page | What it covers |
|---|---|
| [The Persona Book](/docs/persona-book/) | Why personas exist, what each setting does, worked examples and customer scenarios |
| [AI agents](/docs/ai-agents/) | Where personas are attached |
| [Chat](/docs/chat/) | The interface where personas speak |
| [Agent evaluation](/docs/agent-eval/) | Where a promoted research study runs as a dataset |
| [Transcription](/docs/transcription/) | The engine behind persona-from-audio |
