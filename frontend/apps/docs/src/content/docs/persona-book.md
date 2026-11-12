---
title: "The Persona Book"
description: "Why personas exist in Viglet Turing, how each field shapes an answer, and how teams use them for voice, audience fit and research, with worked examples."
---

This book explains personas in Viglet Turing for the people who decide how an AI agent
should sound and who it should serve: administrators, marketing, customer service,
sales, product and UX research teams, and the developers who integrate them. After
reading it you know what each persona setting does to an answer, which feature fits
which business question, and how teams run personas over time. Every concept comes with
a worked example.

You can read it from start to finish, or jump to one section. Managers who want the
business case first can start with [customer scenarios](#customer-scenarios). The
first three sections and the scenarios assume no technical knowledge.

:::tip
This page explains. For the settings, endpoints and the short decision guide, see the
[Personas](/docs/personas/) reference.
:::

## Why a generic AI voice is expensive

Ask any modern LLM to *"answer like a sales rep"* and you get something polite,
helpful and indistinguishable from the next ten companies that asked the same model the
same thing. The same model, on the same question, can also quote different prices, use
wrong product names and forget your discount window.

That costs a business in two ways:

1. **Conversion.** A generic voice does not sell. Customers convert when someone who
   knows what they came for treats them like adults. A senior sales rep does not read
   scripts: they sound like the company. A persona gives your AI assistant that
   consistency, at scale, around the clock.
2. **Risk.** The same generic voice can say something you would never authorize: a
   wrong product name, a discount that does not exist, a claim about a competitor. A
   persona sets guardrails, vocabulary you require and vocabulary you forbid, enforced
   in the prompt and again after the response, before it reaches the user.

Companies measure the cost of training a human agent, but rarely the cost of a thousand
answers a day from an assistant that sounds like every other one. Each off-brand answer
erodes the brand a little. A persona is the cheapest, most repeatable way to make every
AI agent speak with one voice.

## What a persona is

A **persona** is the voice profile of your AI: the difference between an assistant that
sounds like every other chatbot and one that sounds like the most senior, most
consistent representative your team could hire. You configure a persona once, in
**Administration → Personas** (`/bento/persona`), and every [AI agent](/docs/ai-agents/)
it is attached to:

- speaks in your tone, not the model's default;
- uses the words you want to be remembered by, and never the words that get you in
  trouble;
- answers with the vocabulary of customers who already converted, retrieved from a
  few-shot store;
- pulls brand facts live from a brand server, so promotions and product names stay
  current.

Personas are reusable across agents, and they are more than a voice. A persona can also:

- describe a **target reader**, against whom you measure whether a piece of content fits;
- **talk to you** directly, so you can hear a voice before attaching it;
- **hold a dialogue** with other personas on a topic;
- act as a **synthetic research participant**, interviewed at scale to anticipate how
  an audience would react to an idea.

A persona is one of three layers that make up an agent:

| Layer | What it provides |
|---|---|
| [LLM instance](/docs/llm-instances/) | The brain: the reasoning engine |
| Tools and [MCP servers](/docs/mcp-servers/) | The hands: what the agent does |
| Persona | The voice: how it speaks |

Without a persona, an agent still works, but its voice is the LLM's default. Swap the
LLM or the tools and the persona's voice stays the same.

## Who in the company uses personas

A persona is a meeting point between teams that rarely share a vocabulary:

| Team | What the persona gives them |
|---|---|
| **Marketing** | Every AI answer uses brand vocabulary and never a forbidden term. Facts such as prices and promotions change on a brand server, without waiting for engineering. |
| **Sales** | One persona per funnel stage (top, evaluation, closing) that turns discovery into a booked demo without sounding scripted. |
| **Support / CX** | One voice across channels, compliance rules built in, and a tone fitted to the context: formal for regulated industries, casual for self-service. |
| **Product / UX** | Synthetic research tests an idea before real participants' time is spent; content-fit checks whether documentation reaches its audience. |
| **Content / editorial** | "Who is this article for?" becomes a measured answer from Suggest persona and Persona Match. |
| **Compliance / legal** | Forbidden terms are enforced twice, and a slip after the response is masked. |
| **Engineering** | A persona attaches to any agent without rewriting prompts. The voice is independent of the model and the tools. |

## The anatomy of a persona

A persona is a small set of decisions. Each one moves the model in one direction, and
together they replace the model's default voice with yours. This section takes the
fields in the order the editor shows them.

### Speaker, audience or both

The first field, **Persona Kind** (`personaKind`), decides what the persona is for:

| Kind | Role | Used by |
|---|---|---|
| `SPEAKER` *(default)* | A voice the agent speaks in | Agent attachment and prompt composition |
| `AUDIENCE` | A reader you evaluate content against | The content-fit evaluator |
| `BOTH` | Either role | Both |

An `AUDIENCE` persona never becomes an agent's voice: Viglet Turing refuses it as a
speaker. Choosing `AUDIENCE` or `BOTH` reveals the **Audience** section of the form.

> **Example.** *"Non-technical executive buyer"* is an `AUDIENCE` persona: you never
> want the AI to speak as that buyer, but you want to know whether your technical
> material makes sense to them. *"Senior account executive"* is a `SPEAKER`.
> *"Mid-level developer"* could be `BOTH`: sometimes the AI speaks as an experienced
> developer, and sometimes you measure whether content fits developers.

### Identity: name, description and state

| Field | What it does | Why it matters |
|---|---|---|
| **Name** | Internal identifier, never shown to the end user | The team can say "use the sales persona on that agent" without ambiguity |
| **Description** | One line on when to use it, up to 500 characters | Non-technical admins pick the right persona from the agent's list |
| **Enabled** | On or off | Retires a persona without deleting it; agents fall back to answering without one |

Names that say the stage or function work best: `top-of-funnel-sales`,
`solutions-engineer`, `onboarding-coach`, `executive-buyer-audience`. Personas are
cheap, so make several, each narrowly scoped.

> **Worked example: the pharmacy chain.** "VitaPharm" has four personas:
> `pharmacist-advisor` (medication questions), `deals-promoter` (the week's
> promotion), `formal-support` (complaints) and `senior-audience` (the target reader
> for package inserts). When a manager says the homepage chat is too aggressive, the
> team knows which persona to disable (`deals-promoter`) without touching code. Each
> persona's **Description** (*"use in the clinical-question flow"*) lets a
> non-technical manager pick the right one in the agent's list.

### The system instruction

The **System Instruction** (`systemInstruction`) is free text placed before the agent's
prompt. It is the narrative core of the persona: who it is and how it behaves. For a
top-of-funnel sales persona:

> *"You are a senior account executive. Open with a one-sentence outcome the prospect
> can imagine. Ask exactly one qualifying question per turn. Never quote prices in the
> first 5 turns: instead, anchor on value."*

The editor's **Help me write** assistant drafts the instruction, and other fields, from
a description in plain language. Nothing is saved until you approve it.

:::tip
The system instruction says how the persona speaks. It must not repeat the agent's
purpose, which is what the agent does. When the two overlap, the model receives two
competing sets of instructions and its answers become inconsistent.
:::

> **Worked example: one shop, two personalities.** An artisan coffee shop writes the
> instruction for `passionate-barista`: *"You are a barista who loves specialty coffee.
> Speak with genuine enthusiasm, always suggest a bean by its origin, and never push the
> most expensive one: recommend what matches the person's taste."* For the airport
> counter, where customers are in a hurry, it creates `express-barista`: *"You are quick
> and kind. Answer in one sentence, offer the most popular order right away, and confirm
> whether the person wants it to go."* Same menu, same brand: the instruction draws two
> personalities, each right for its context.

### Style: tone, verbosity and language style

Three structured fields set the register of the answer.

**Tone** (`tone`):

| Value | When to use |
|---|---|
| `FORMAL` | Regulated industries, institutional communication |
| `CASUAL` | Self-service, younger audiences |
| `TECHNICAL` | Developer relations, technical pre-sales |
| `EXECUTIVE` | C-level discovery, emphasis on outcomes |

**Verbosity** (`verbosity`, 1 to 5): lower is shorter. Use 2 for chat, where answers are
read on a phone, and 4 for advisory contexts where the user expects depth.

**Language style** (`languageStyle`), the shape of the answer:

| Value | Typical use |
|---|---|
| `NEUTRAL` | Balanced default |
| `DIRECT` | Objective, no-nonsense answers |
| `NARRATIVE` | Long-form content, storytelling |
| `PERSUASIVE` | Sales |
| `INSTRUCTIONAL` | Onboarding, tutorials |

The three fields become a short paragraph in the prompt, such as *"Use an EXECUTIVE
tone, verbosity 2 (concise), PERSUASIVE language."*

> **Worked example: one question, three voices.** A customer asks *"is the annual plan
> worth it?"*
> - **`EXECUTIVE`, verbosity 2, `PERSUASIVE`**: *"It is: you lock in the 2025 price and
>   get 2 months free. Most teams that switch recover the cost within 90 days. Want me to
>   run the numbers for your volume?"*
> - **`TECHNICAL`, verbosity 4, `INSTRUCTIONAL`**: a paragraph with the month-by-month
>   savings calculation and each plan's limits as bullets.
> - **`CASUAL`, verbosity 2, `DIRECT`**: *"Way cheaper over the year 😊 it's 16% off.
>   Shall we?"*
>
> Same AI, same model, same question; only the persona differs. The corporate site's
> sales chat uses the first; the delivery app's WhatsApp channel uses the third.

### Mandatory and forbidden terms

Two fields make the brand's language explicit:

- **Mandatory Terms** (`mandatoryTerms`): words or phrases the LLM must use, which anchor
  the brand vocabulary. For example `Customer Success | ROI | outcome`.
- **Forbidden Terms** (`forbiddenTerms`): words or phrases that must never appear:
  competitor names, discontinued products, sensitive claims.

The separator is the pipe (`|`), not the comma, so a single term can contain a comma,
such as *"Tier 1, Tier 2 customers"*. In the form you type one term per line, and
Viglet Turing joins them with pipes when it saves.

Forbidden terms are enforced twice: in the prompt, so the model does not set out to say
them, and after the response, where any that appear anyway are masked. See
[tone validation after the response](#tone-validation-after-the-response).

> **Worked example: the health insurer.** "HealthPlus" cannot let the AI say "cure" or
> "guaranteed", or name the competitor "LifePlan", and it wants every answer to use
> "coverage", "provider network" and "waiting period":
> - Mandatory: `coverage | provider network | waiting period`
> - Forbidden: `cure | guaranteed | no waiting period | LifePlan`
>
> When the model, pressed by an insistent member, writes *"this treatment is guaranteed
> by LifePlan"*, the member reads *"this treatment is `[***]` by `[***]`"*, and the log
> records the slip for the compliance team to review. No forbidden claim reaches the
> member.

### Personality traits

Five optional controls, each from 0 to 100, give the persona a measurable personality
based on the Big Five (OCEAN) model: **O**penness, **C**onscientiousness,
**E**xtraversion, **A**greeableness and **N**euroticism.

Only traits outside the neutral band of 34 to 66 reach the prompt, as behavioural
guidance, which keeps the prompt short. With
[style calibration](#calibrating-the-model-from-style) on, the traits also nudge the
sampling temperature. The traits are off by default.

Their main use is in a [research cohort](#synthetic-user-research): spreading the traits
across participants is what stops every simulated user from sounding the same, the
difference between ten polite clones and a varied, realistic audience.

> **Worked example: testing a finance app with a varied cohort.** A fintech wants to
> know how different people react to its "invest now" screen. Instead of five identical
> personas, the team spreads the traits:
> - `anxious-anna`: Neuroticism **85**, Openness 30. Afraid of losing money, asks for
>   guarantees.
> - `adventurous-bruno`: Openness **90**, Neuroticism 15. Wants the riskiest product and
>   finds the screen too conservative.
> - `methodical-carla`: Conscientiousness **90**. Wants to read every term before
>   clicking.
>
> In synthetic research each one answers in character, and the report shows the screen
> needs two versions: one that reassures Anna and one that highlights the upside for
> Bruno. Without the traits, all three would have given the same lukewarm answer.

### Grounding source

By default a persona answers from the model's general knowledge. On subjects specific
to your business, the model sometimes makes up something that looks right. The
**grounding source** (`groundingSource`) ties answers to content you control:

| Source | What it does |
|---|---|
| `NONE` *(default)* | The persona answers from the model's general knowledge |
| `SN_SITE` | Answers are grounded on an indexed [SN site](/docs/semantic-navigation/): Viglet Turing searches real content before answering |
| `NOTEBOOK` | Answers are grounded on the persona's own material, the sources attached to it |

Before each answer, Viglet Turing searches the chosen content for the most relevant
passages and gives them to the model in a separate, labelled block, so the model answers
from what is written there. If the search fails, the conversation continues without the
passages.

> **Worked example: the advisor who only says what is in the manual.** A carmaker's
> `mechanic-specialist` persona serves dealerships. Without a grounding source it
> sometimes gives a bolt torque that looks right but is wrong. With `SN_SITE` pointing at
> the SN site of the official technical manuals, every answer is grounded in those
> manuals: if the 2024 model's manual says "45 N·m", that is the answer. The
> `institutional-support` persona uses `NOTEBOOK` with its approved FAQ material, so it
> never says anything legal has not validated.

### The few-shot store

The hardest part of a brand voice is the feel, not the rules. You can describe "be
formal", but you cannot fully describe the way your best salesperson answers a discount
request.

So the persona learns by example. Its **Few-shot Store** is a collection of model
conversations, each a typical customer question with the ideal answer written in the
persona's voice. The collection lives in an [embedding store](/docs/embedding-stores/),
the same kind that semantic search uses. When a customer writes, Viglet Turing retrieves
the three most similar examples and places them before the model as "here is how we
answer questions like this". The model then answers in the same style, adapted to the
case, without copying.

What goes into the store decides the result:

- Give each persona its own collection, for example `sales-examples`, holding only
  question-and-answer pairs in that persona's voice.
- Prefer reviewed, real exchanges. Twenty well-written examples beat two hundred
  mediocre ones.
- The best examples come from [Chat Analytics](/docs/chat-analytics/): sessions where
  the customer reached their goal, sentiment was positive or the deal closed. Feeding the
  store from conversations that worked keeps it current.

> **Worked example: the agency that learned to talk like an agent.** "RightHome" had
> set its voice to persuasive, but the answers still sounded robotic. The team gathered
> 25 real conversations where its best agent booked a viewing. One of the stored
> examples:
> - **Customer:** *"is the 2-bedroom still available?"*
> - **Ideal answer:** *"It is! And lucky you: it's the corner unit, with the balcony that
>   catches the morning sun. I can fit you in for a viewing tomorrow at 10am, or do you
>   prefer late afternoon?"*
>
> When a new customer asks about a 3-bedroom, the model does not copy that answer. It
> follows the rhythm: confirm availability, name one concrete differentiator, offer two
> time slots.

### Live brand context from an MCP server

Customers ask about prices, active promotions and whether a feature is in beta. Writing
those facts into the prompt makes the prompt long, and turns every price change into a
request to engineering.

The **Brand Context MCP** field (`brandContextMcpServer`) points the persona at an
[MCP server](/docs/mcp-servers/) you control. In each conversation, Viglet Turing calls
one of its tools (for example `get_brand_context`) and adds the returned JSON to the
prompt as a *"Current brand facts:"* block. That gives you:

- one source of truth for prices, names and promotions;
- facts that marketing updates without anyone editing the persona;
- shorter prompts, which models follow better;
- one server reused by several personas (sales, support, partners).

A common setup: marketing maintains a CMS, an MCP server reads from it, and the persona
reads from the MCP server. The CMS is the system of record.

> **Worked example: the Black Friday that needed no engineering.** An electronics
> retailer's `store-salesperson` persona reads from a `deals-mcp` server connected to
> marketing's campaign panel. At 11:59 pm on Thursday the gaming laptop is 10% off. At
> midnight marketing changes it to 25% in the panel, and the next message in the chat
> says *"the gaming laptop is 25% off today, only on Black Friday!"*. Nobody edited the
> persona or shipped code. The same server feeds the personas on the website, WhatsApp
> and the in-store kiosk.

### Calibrating the model from style

By default the style reaches the model only as text; the sampling parameters
(temperature and maximum tokens) come from the LLM instance. With **Calibrate model from
style** (`calibrateModelParams`) on, the style also sets those parameters on each turn:

| Style field | Sets | Mapping |
|---|---|---|
| **Verbosity** | Maximum tokens | 1 → 256, 2 → 512, 3 → 1024, 4 → 2048, 5 → 4096 |
| **Tone** | Temperature | `TECHNICAL` 0.2, `FORMAL` 0.3, `EXECUTIVE` 0.4, `CASUAL` 0.8 |

A `TECHNICAL`, verbosity-2 persona runs cooler and shorter; a `CASUAL`, verbosity-5
persona runs warmer and longer. The personality traits nudge the temperature further.
Calibration is off by default and applies to agent chat, direct chat with the persona
and Persona Dialogue.

> **Worked example: reining in the legal assistant.** A law firm's `paralegal` persona
> (`TECHNICAL` tone) runs on an LLM instance set to temperature 0.7, and sometimes cites
> case law that does not exist. With calibration on, the `TECHNICAL` tone brings the
> temperature down to 0.2, and verbosity 2 keeps the answers short. The firm's
> `blog-writer` persona (`CASUAL`, verbosity 5) gets long, loose answers from the same
> LLM instance.

## How a persona reaches the model

### The prompt, layer by layer

A persona does not replace the agent's system prompt; it is added to it. At chat time
the prompt is built in a fixed order:

```text
1. Agent system prompt          → "You help customers find products"
2. Persona system instruction   → the brand voice directive
3. Style block                  → tone · verbosity · language style
4. Mandatory/forbidden terms    → "Always use: …" / "Never use: …"
5. Personality traits           → traits outside the neutral band
6. Brand context                → fetched from the MCP server
7. Grounding passages           → real content, per message
8. Tool definitions             → JSON schemas of native and MCP tools
9. Few-shot examples            → the most similar model conversations
10. Conversation history
11. Current user message
        ↓
       LLM
        ↓
   Tone validation (masks forbidden terms)
        ↓
   Answer to the user
```

Layers 1 to 6 form a cached static block, so the model does not have them rebuilt on
every message. Layers 7 and 9 change on each turn, and layers 10 and 11 grow with the
conversation. Because the order never changes, you can audit what the model received.

> **Worked example: why the second message is cheaper.** In a telecom support chat, the
> first message builds the whole prompt, including the persona's static block and the
> brand facts. From the second message on, the static block comes from the cache, which
> cuts work and latency over a 15-message conversation. When you edit the persona, for
> example to add a forbidden term, the cache is cleared and the next chat uses the new
> block, with no restart.

### Tone validation after the response

Forbidden terms in the prompt are necessary but not enough: models drift, especially
under tool-calling pressure or in long sessions. So Viglet Turing checks every assistant
answer against the forbidden list a second time, before the user sees it.

Matches are masked with `[***]` instead of blocking the whole answer. The check works in
Portuguese, English and Spanish: it reads the answer and the terms in the answer's
language, recognizes variations of a word (singular and plural, conjugations) and
matches whole phrases.

| Layer | Stops | Cost |
|---|---|---|
| In the prompt | The model setting out to say the term | None: it is already in the prompt |
| After the response | A term the model said anyway | Microseconds |

Both layers are always on when a persona is attached.

> **Worked example: why word variations matter.** A brand forbids `promotion`. A literal
> comparison would let *"we're promoting this item"* and *"see our promotions"* through.
> Because the check recognizes variations, it masks all three forms. For a store serving
> customers in Portuguese, English and Spanish, the same persona catches the term in the
> language of each answer.

## Personas as a voice

### Attaching personas to an AI agent

A persona's voice is applied through an agent. In the agent editor, the **Persona** tab
(`/bento/ai-agent/instance/:id/persona`) holds the agent's catalog of personas, with one
marked as the default (the star). On each turn, the persona that governs is, in order of
precedence:

1. the persona a chat flow switched to;
2. the persona the request names (`requestPersonaId`);
3. the agent's default persona.

`AUDIENCE` personas are never used as a voice. For the questions to ask when choosing an
agent's persona, see [how to choose a persona for an agent](/docs/personas/#choose-a-persona-for-an-agent).

> **Worked example: one agent, two personas.** A university's "Student Center" agent
> holds two personas: `formal-registrar` (the default) and `laid-back-senior`. Normally
> the agent answers as the registrar. During the admissions campaign, the landing page
> sends `requestPersonaId = laid-back-senior` with each chat call, and the same agent,
> with the same tools and knowledge, answers applicants as an upbeat senior student.
> When the campaign ends, the page stops sending the parameter and the agent is the
> registrar again. The agent itself never changes.

### Talking directly to a persona

The **Open chat** action on the persona's dashboard starts a conversation with the
persona itself, with no agent involved. It is the fastest way to hear a voice before
attaching it: type a message, read the answer, refine the instruction, try again.

- It runs on any enabled LLM instance you pick.
- It keeps no history: nothing is saved.
- It has a shareable URL (`/bento/chat/persona/{id}`), so a colleague can test the same
  persona.
- It accepts attachments and uses the native tools.
- It is offered only for `SPEAKER` and `BOTH` personas.

> **Worked example: approving the voice before launch.** A digital bank's marketing
> director wants to approve the assistant's voice before launch. The team opens
> **Administration → Personas**, picks `genz-assistant`, chooses **Open chat** and the
> production LLM, and asks *"my bill is wrong, now what?"*. The director finds the answer
> too informal for billing; the team adjusts the instruction and tries again, in
> minutes, without touching any agent. The director then sends the link to legal, who
> test the same persona. Only then is it attached to the real agent.

### Switching persona inside a chat flow

In a [chat flow](/docs/chat-flow/), a node can switch the active persona by setting
`__activePersonaId` in the flow state. A conversation can start with a casual triage
persona and hand over to a formal specialist persona when it turns technical, in the
same session, without the user seeing a seam.

## Personas as an audience

So far the persona has been a voice. It can also describe a reader, the audience a piece
of content is written for, so you can check whether a document fits the people who
should read it. *"Will a non-technical buyer understand this spec sheet?"* becomes a
measured question.

### The reader profile and the evaluation notebook

An `AUDIENCE` or `BOTH` persona carries a reader profile:

| Field | What it describes |
|---|---|
| **Reading level** | `ELEMENTARY(5)`, `MIDDLE(8)`, `SECONDARY(12)`, `UNDERGRADUATE(15)`, `GRADUATE(18)`: the number is the reader's approximate years of study |
| **Domain expertise** | `NOVICE`, `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT` |
| **Vocabulary ceiling** | The terms the reader knows |
| **Comprehension and accessibility notes** | Free text |
| **Primary language** | An ISO 639 code, such as `pt`, `en` or `es` |

The content to evaluate goes into the persona's **evaluation notebook**, a set of
sources:

| Source | Where the text comes from |
|---|---|
| Indexed document | A page already indexed in an SN site |
| File | An uploaded file (PDF, DOC and others); Viglet Turing extracts the text |
| Link | A web page that Viglet Turing fetches and extracts the text from |

Each source shows its state, **pending**, **extracted** or **failed**, and a failed one
can be extracted again. The extracted text is stored for reuse. In the persona's
**Evaluation notebook** section you add sources, see their state, preview the text and
retry what failed.

> **Worked example: the profile that exposes hidden jargon.** An insurer creates the
> `AUDIENCE` persona `everyday-customer`: reading level `MIDDLE(8)`, expertise `NOVICE`,
> and a vocabulary ceiling with the few terms its customers know (`deductible`, `claim`,
> `policy`). It adds three sources: the URL of the general terms page, the policy PDF and
> the indexed FAQ. The URL blocks the fetch and shows **failed**; the other two are
> extracted and the evaluation runs on them while the team retries. Because any word
> outside the vocabulary ceiling counts as difficult, *"subrogation"* stands out in the
> readability score.

### How the content-fit score is calculated

**Content-fit** measures how well a piece of content matches a reader. It combines two
signals and works even without an LLM:

1. **Readability**, computed without AI, in Portuguese, English and Spanish. Standard
   readability formulas measure sentence length, syllables per word, the share of
   difficult words (not counting the terms in the reader's vocabulary ceiling) and use
   of the passive voice, and turn them into a score from 0 to 100 relative to the
   reader's reading level. The same text always gets the same score.
2. **LLM evaluation**, grounded in the text. The model takes the reader's point of view
   and, looking only at the source text, returns a fit score, what fits and what does
   not, each problem with its exact passage, the reason and a suggested rewrite. A
   problem whose passage does not appear word for word in the text is discarded.

The final score is the average of the two. Without an LLM, it is the readability score
alone. Colour bands: green at 70 and above, amber at 40 and above, red below 40.

> **Worked example: two signals that disagree.** An HR article on career paths is
> evaluated for the `intern` persona.
> - Readability: 82 (green), short sentences and few difficult words.
> - LLM evaluation: 45 (amber). From the intern's point of view, the article assumes the
>   reader knows what a "9-box review cycle" and a "PDP" are, and it flags each passage
>   with a suggested rewrite.
> - Final score: about 64 (amber).
>
> The article is easy to read but hard for this reader to understand. Without an LLM,
> the report would show only the 82, which misses the jargon.

### The fit report

You can evaluate one source or the whole notebook. The persona's **Fit report** section
shows:

- an overall bar and one per source, in red, amber or green;
- what fits;
- what does not fit, as highlighted passages, each with the reason and a suggested
  rewrite.

Without an LLM, it shows only the readability score.

> **Worked example: the brochure that missed the CFO.** Marketing writes a piece for
> finance directors. Run against the `non-technical-cfo` persona, it scores 48 (amber),
> with problems flagged on "microservices architecture" and "p99 latency", each with a
> rewrite in terms of financial outcomes. The piece is fixed before it goes live.

### Suggesting the best persona for a piece of content

Content-fit answers "does this text fit this persona?". **Suggest persona** answers the
reverse: "which audience is this text for?".

On the personas list (`/bento/persona/suggest`) you paste a piece of content, optionally
limit the candidates, and Viglet Turing scores it against every active audience persona,
from best fit to worst. Each row shows the coloured bar and the same fits and does-not-fit
detail, with the best fit marked at the top. Use it to decide which audience to publish
for, or to choose between similar personas.

> **Worked example: the right section for a dense article.** An editor finishes a dense
> piece on tax reform for small businesses and does not know which section it belongs in.
> Suggest persona, with no candidates excluded, returns:
> 1. `business-accountant`: 88 (green), the best fit
> 2. `solo-entrepreneur`: 61 (amber), follows the topic but stumbles on "assessment
>    regime"
> 3. `general-reader`: 34 (red), too dense
>
> The article goes to the accounting section, and a lighter version for solo
> entrepreneurs is commissioned.

## Persona Match: many contents against many personas

### What a project holds

Content-fit measures one persona against content. **Persona Match** makes it a reusable
project that evaluates many contents against many personas at once, and keeps the
results current as content changes. It is under **Administration → Personas → Persona
Match** (`/bento/persona/match`).

A project holds:

- **Contents**, added once per project rather than per persona: a link, a document
  already indexed in an SN site, or a file whose text is extracted. The text is read and
  stored when you add it.
- **Personas**: any set of your personas.
- A **schedule** (`Manual`, `Daily` or `Weekly`) and, optionally, an LLM other than the
  default.

Running the analysis evaluates each content and persona pair with the content-fit score
and stores the result in a cell of the grid, with what fits, what does not and the
suggested rewrites. A re-run skips pairs whose content has not changed.

Because a project can hold a single persona, the persona's **Validate content** action
opens a one-persona project.

### The grid, the lenses and scheduled re-analysis

The project shows its results three ways:

- **The grid**: contents as rows, personas as columns, each cell coloured by fit. It
  fills in cell by cell while the analysis runs. Selecting a cell opens that pair's
  score, what fits and what does not.
- **By content**: for each content, the personas ranked, showing which audience each
  piece serves best.
- **By persona**: for each persona, the contents ranked, showing which content serves
  each audience best.

Both lenses export to PDF.

A `Daily` or `Weekly` project re-runs on its own, once across the cluster. It re-reads
the sources, so a page whose content changed is picked up, and recomputes only the cells
whose content changed. A `Manual` project runs when you select **Run analysis**. Across
runs, the project tracks how each cell's fit moves and can suggest adjustments to the
content or the persona; see
[the tuning loop](/docs/personas/#the-tuning-loop-fit-trend-drift--suggested-adjustments).

> **Worked example: a help center audited every Monday.** A customer success team creates
> a "Help Center × Personas" project with the 40 indexed help articles and three
> quick-start PDFs, the personas `beginner-user`, `it-admin` and `developer`, and a
> **Weekly** schedule. Every Monday morning the 43 × 3 grid is filled in.
> - "Configuring SSO" is green for `it-admin` and `developer` and red for
>   `beginner-user`, as expected.
> - "Getting started" is amber for the beginner, which it should not be. The cell shows
>   why: the article assumes the reader has already created an API token, and a rewrite
>   is suggested.
>
> One article changed during the week, so only its three cells were recomputed. The
> by-persona lens exports a PDF that becomes the quarter's rewrite backlog.

## Persona Dialogue

**Persona Dialogue** runs a turn-by-turn conversation between two or more personas on a
topic you choose. No persona owns the conversation, so dialogues live on their own page,
reached from the personas list (`/bento/persona/dialogue`), as saved projects you can
reopen and re-run.

A dialogue project has:

- a **topic**;
- a **cast** of at least two speaker personas, where the number on each badge is the
  speaking order (only `SPEAKER` and `BOTH` personas can be chosen);
- an **LLM instance**;
- a **turn budget**, in rounds, 10 by default.

Viglet Turing gives the topic to the first persona, then passes each reply to the next
speaker in turn until the budget runs out. The transcript streams as it is generated,
colour-coded by speaker, and is saved, so reopening the project shows the last
conversation; re-running replaces it. Each persona is asked to end its reply with a
question to the others, which keeps the conversation moving, and the last round closes
it. If a turn fails, the partial transcript is kept with a notice.

Dialogue is the quickest way to hear several brand voices side by side. Ask
`aggressive-sales` and `cautious-advisor` to debate "is the Enterprise plan worth it?"
and you hear where the two voices diverge before deciding which agent gets which. Teams
also use it for training and for drafting scripts.

## Synthetic user research

**Synthetic user research** interviews a cohort of personas, playing participants,
against a research protocol, then synthesizes what they said into an insights report
organized by theme. It is under **Administration → Personas → Synthetic research**
(`/bento/persona/research`).

:::caution
Synthetic research is a discovery aid, not a replacement for real users. It sharpens
your questions, surfaces likely themes and tests an idea before you spend real
participants' time. It is not ground truth and has no statistical validity. Two
mechanisms keep the output honest: the saturation signal, which tells you when more
interviews stop adding anything new, and the persona's
[grounding source](#grounding-source). Validate with real users before acting.
:::

### Studies, protocols and the participant cohort

A **study** is the reusable unit. It holds:

- A **goal**, and optionally a **hypothesis**: what you want to learn.
- An **interview protocol**, in one of three formats:
  - **Dynamic interview**: the interviewer starts with the basics and adapts the next
    questions to the answers, up to a maximum number of questions. Use it when you do not
    yet know what to ask.
  - **Scripted questions**: a fixed list, asked word for word in the same order, so the
    answers can be compared side by side.
  - **Concept test**: the interviewer presents an idea or message and measures the
    reaction.
- An **audience**: the ordered list of participant personas, each interviewed
  separately.
- **Who answers the questions**: a plain LLM playing the assistant, or one of your
  deployed agents. Choosing an agent reverses the roles: the persona becomes the user and
  your agent, with its knowledge and tools, answers. The cohort then tests the assistant
  you have already shipped.
- Optionally, **two models**: one to interview and another to synthesize, so a single
  model's bias does not colour the whole study.
- A **schedule** (`Manual`, `Daily` or `Weekly`) for re-running the study.

When you select **Run interviews**, each conversation appears as soon as it finishes,
with its status, and expands to show the questions and answers. Re-running an unchanged
study reuses what was already done.

> **Worked example: three protocols, three questions.** A mobility startup is about to
> launch a scooter subscription.
> - **Dynamic interview**, goal *"understand what is blocking the subscription"*. When a
>   persona mentions a fear of not using it enough, the interviewer probes that fear.
> - **Scripted questions**: the same six questions for every persona, to compare
>   answers.
> - **Concept test**: the real offer (*"$49/month, 60 min/day included"*). Here
>   `anxious-anna` asks *"what if I travel for a month?"*, an objection nobody had
>   foreseen.
>
> Pointed at the deployed support agent instead of a plain model, the personas become
> users: `methodical-carla` asks eight questions in a row and shows that the agent loses
> track after the third.

### The insights report

Once a study has run, Viglet Turing synthesizes the interviews, and caches the result,
into an insights report:

- an executive summary;
- themes ranked by relevance, each with verbatim quotes traced to the persona who said
  them (a quote that does not appear in the interviews is flagged);
- recommendations;
- views **by theme** and **by persona**, a **Regenerate** button and PDF export.

The **saturation signal** answers "have I interviewed enough people?". As personas are
interviewed in list order, Viglet Turing measures whether each new participant raises a
theme nobody raised before. When new themes stop appearing, the report says *"sample
adequate at N participants"*. The signal is calculated directly, with no LLM, and acts
as a brake on overconfidence.

A **theme-affinity map** shows which participants cluster around each theme. For a
scheduled study, a **change-over-time** chart shows how themes shift from run to run. A
concept test also gets a **concept-fit** score: the idea is scored for each participant
with the same content-fit evaluation that Persona Match uses.

> **Worked example: when to stop interviewing.** The scooter study has 12 personas. The
> report has a summary, five themes ranked by relevance (*"fear of a fine for parking in
> the wrong place"* on top, with the quote) and recommendations. The saturation signal
> says no new theme appeared after the seventh participant: *"sample adequate at 7"*. The
> team skips the remaining five, and has a reasoned answer for anyone who wants twenty
> more. In the by-persona view, `anxious-anna` alone raises the parking fine, a niche
> concern rather than a general one. Because the study runs weekly, the change-over-time
> chart shows that fear shrinking after the app adds a parking-photo feature.

### Cohort synthesis, the research assistant and the program view

- **Synthesize a cohort** (`/bento/persona/cohort`): describe the audience in one
  paragraph, and Viglet Turing drafts a varied set of personas with well-spread
  personality traits. Nothing is saved automatically: in a grid you **keep** each draft,
  which opens the normal form to edit and save, or **discard** it.
- **Research assistant**: a new study opens with an assistant you can skip. Describe in
  plain language what you want to learn, and it proposes a complete study (name, goal,
  hypothesis, protocol with questions and a suggested audience) and fills in the form.
- **Program view** (`/bento/persona/research/program`): plan several studies with
  different audiences and see them rolled up: totals, each study's saturation and,
  most useful, the themes that recur across audiences (a theme raised in two or more
  studies is highlighted). The view sums existing reports and draws no new conclusions.

> **Worked example: a research program in one afternoon.** A product manager who has
> never run research needs to test three feature ideas.
> - The research assistant turns *"I want to know whether pet owners would pay for
>   veterinary telemedicine"* into a complete concept-test study with five questions and
>   a suggested audience.
> - There are no pet-owner personas yet, so the manager writes *"dog and cat owners in
>   major cities, middle income, varying levels of attachment"* and asks for eight. Of
>   the eight drafts, six are kept and two discarded.
> - After running the three studies (telemedicine, pet daycare, a vaccine plan), the
>   program view shows *"price distrust"* in all three. That theme, not any one feature,
>   becomes the quarter's priority.

### From a study to an agent evaluation

A completed study can be promoted to an evaluation dataset, which opens it in
[agent evaluation](/docs/agent-eval/): the interviewer's questions become replay turns
and the participants' answers become reference material. A one-off study becomes a
repeatable quality check for an agent.

> **Worked example: the study that became a regression test.** The study that tested the
> support agent produced 40 good questions with expected answers. Promoted to an
> evaluation dataset, those 40 questions run every time someone changes the agent's
> prompt or LLM, and the build fails if quality drops. `methodical-carla` now tests
> every version of the agent.

Developers can define and run a study and fetch its findings with the
`turing research` command of the [CLI](/docs/cli/), alongside `turing eval`.

## Drafting a persona from audio

You can draft a persona from a voice recording, for example five minutes of someone
describing how the brand's ideal salesperson talks. Viglet Turing:

1. transcribes the audio with the engine chosen in **Administration → Settings → Global
   Settings → Transcription** (OpenAI, or an OpenAI-compatible server in your own
   environment), splitting large files automatically;
2. classifies the structured fields (tone, verbosity, style, vocabulary and audience
   descriptors) and writes a character portrait that becomes the system instruction;
3. hands a `BOTH` draft to the new-persona form for you to review and save. Nothing is
   saved automatically.

The **Derive from audio** action runs this in the background and shows its progress.

## Customer scenarios

Each scenario shows a real problem, the persona features that address it and the
outcome. The details are illustrative.

### Fashion e-commerce: turn browsing into a purchase

**Problem.** The store's chat is polite but does not sell; it mentions products that
are gone and sometimes names a competitor.

**Solution.**
- `SPEAKER` persona `personal-stylist`: `CASUAL` tone, `PERSUASIVE` style, verbosity 2;
  mandatory `look | matches | your style`; forbidden `competitor | cheap | clearance-bin`.
- A few-shot store filled from Chat Analytics conversations that converted.
- A brand-context MCP server with live stock and promotions from the CMS.
- `SN_SITE` grounding on the indexed catalog, so discontinued products are never
  suggested.

**Outcome.** The AI talks like the store's best salesperson, with current stock and no
competitor names. Marketing changes the week's promotion without an engineering ticket.

### Banking and regulated industries: consistency and compliance

**Problem.** An investment assistant must be accurate and never suggest guaranteed
returns.

**Solution.**
- `SPEAKER` persona `formal-advisor`: `FORMAL` tone, `INSTRUCTIONAL` style; forbidden
  `guaranteed | risk-free | sure profit | guaranteed returns`.
- Tone validation masks any slip before the customer sees it.
- `AUDIENCE` persona `beginner-investor`, reading level `SECONDARY`, to check that the
  educational material is understandable.

**Outcome.** Every answer passes two compliance checks, and legal audits the forbidden
list instead of every answer.

### B2B SaaS: one funnel, several voices

**Problem.** One chatbot tries to handle discovery, technical evaluation and onboarding,
and does all three badly.

**Solution**, one persona per stage:
- `top-of-funnel-sales` (`EXECUTIVE`, `PERSUASIVE`) turns discovery into a demo.
- `solutions-engineer` (`TECHNICAL`, `INSTRUCTIONAL`) wins over the technical
  evaluators.
- `onboarding-coach` (`CASUAL`, `INSTRUCTIONAL`) turns activation into retention.

A chat flow switches the active persona as the conversation moves between stages.

**Outcome.** Each stage has the right voice, and no persona tries to do everything.

### Healthcare: language patients understand

**Problem.** Patient leaflets are written in medical jargon, and nobody knows whether
patients understand them.

**Solution.**
- `AUDIENCE` persona `layperson-patient`: `ELEMENTARY` reading level, `NOVICE`
  expertise, and a vocabulary ceiling with the terms patients know.
- A weekly Persona Match project over all leaflets and patient personas: the grid shows
  hard-to-understand leaflets in red, with suggested rewrites per passage.

**Outcome.** Health content is measured by readability and text-grounded evaluation, and
rewrites start with what is red.

### Public sector: serve every citizen

**Problem.** A services portal must speak to very different audiences (older people,
young people, business owners) and show that it is accessible.

**Solution.**
- Several `AUDIENCE` personas covering the target audiences, with accessibility notes.
- Suggest persona routes each new article to the right audience.
- Synthetic research with a varied cohort anticipates questions before publication.

**Outcome.** Public communication validated per audience, with a record of how.

### Media and editorial: who is this content for?

**Problem.** A newsroom produces a lot of content and does not know which audience each
piece serves best.

**Solution.** A Persona Match project over the archive and every reader persona. The
by-content lens shows which audience each article reaches; the by-persona lens shows
which content is missing for each audience.

**Outcome.** An editorial backlog driven by fit data rather than intuition.

## Running personas over time

### Who does what, and how often

| Routine | Who | How often |
|---|---|---|
| Review mandatory and forbidden terms | Marketing and compliance | Monthly |
| Refill the few-shot store with recent conversations that worked | CX and sales | Quarterly |
| Run Persona Match over the content archive | Editorial | Weekly, scheduled |
| Update facts on the brand-context server | Marketing | As they change |
| Run synthetic research before a product decision | Product and UX | Per initiative |
| Audit the forbidden list and the masking log | Compliance | Monthly |

### Common pitfalls

| Pitfall | Symptom | Fix |
|---|---|---|
| The persona contradicts the agent's instructions | Inconsistent answers | The agent's instructions say what it does; the persona says how it speaks. Keep them separate. |
| Forbidden terms overlap the agent's domain | Answers are masked often and users see `[***]` | Keep the forbidden list for brand and compliance terms. Put technical restrictions in the agent's instructions. |
| The few-shot store holds general content | The persona imitates whatever is in the collection | Keep only model conversations in the persona's voice there; use a separate knowledge store for retrieval. |
| The brand-context server returns too much | Slow, expensive answers | Return only the essential, current facts, filtered on the server. |
| One persona for the whole funnel | The onboarding coach tries to sell; the sales rep tries to teach | Make several personas, each narrowly scoped. |
| Synthetic research treated as fact | Decisions made without validation | Use the saturation signal and grounding, and validate with real users. |

### Good practice

1. **Start small.** A minimal persona (name, instruction and tone) already beats the
   model's default voice for any customer-facing agent.
2. **Scope tightly.** Personas per stage or function beat one persona for everything.
3. **Curate the few-shot store.** Twenty excellent examples beat two hundred mediocre
   ones.
4. **Let the brand server carry facts that change.** A short instruction works better.
5. **Listen before attaching.** Use **Open chat** and Persona Dialogue to hear the voice.
6. **Measure the audience as well as the voice.** Content-fit and Persona Match turn
   "I think it works" into a number.

### Checklist for a customer-facing persona

- [ ] The **kind** is right: voice, audience or both.
- [ ] The **system instruction** describes who speaks and how, without repeating the
      agent's purpose.
- [ ] **Tone, verbosity and style** suit the funnel stage.
- [ ] **Forbidden terms** cover brand and compliance, not everyday technical vocabulary.
- [ ] The **few-shot store** holds real conversations that worked.
- [ ] A **brand-context server** supplies the facts that change, such as prices and
      promotions.
- [ ] A **grounding source** is set if the persona should answer from real content.
- [ ] The voice was tested with **Open chat** before attaching.
- [ ] The persona is attached to the agent, and marked as default where it should be.
- [ ] For an audience persona: the reader profile and notebook are filled in and fit was
      measured.

## Related pages

| Page | What it covers |
|---|---|
| [Personas](/docs/personas/) | The reference: fields, REST API, choosing a persona for an agent |
| [AI agents](/docs/ai-agents/) | Where personas are attached |
| [Chat flow](/docs/chat-flow/) | Switching persona inside a conversation |
| [Agent evaluation](/docs/agent-eval/) | Where a promoted research study runs as a dataset |
| [Chat Analytics](/docs/chat-analytics/) | Where few-shot examples come from |
| [MCP servers](/docs/mcp-servers/) | Serving live brand context |
