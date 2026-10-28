---
title: "The Persona Book"
description: "The complete, didactic, strategic guide to Personas in Viglet Turing, from why a generic AI voice costs you money to Persona Match, Dialogue, and Synthetic User Research, with worked examples for every feature."
---
### The Book: A Complete, Didactic, Strategic Guide

:::tip
This is the long-form companion guide to Personas. For a concise, technical overview, see [Personas](/docs/personas/) for the concise reference.
:::

> *Your customer doesn't want to talk to an LLM. They want to talk to **your company**.*

---

## How to read this book

This document is a **complete guide** to everything a Persona can do in Viglet
Turing. It was written to be read three ways:

- **From start to finish**, like a book, if you want to understand deeply why the
  Persona exists and how it changes a company's day-to-day work.
- **By isolated chapter**, as reference, if you already know what you're looking
  for (for example, "how Persona Match works" or "how to validate content").
- **By customer scenario** (Part IX), if you're a manager and want to grasp the
  business value quickly before diving into the technical detail.

Every feature is explained in three layers: **what it is**, **why it matters for
the business**, and **how you use it in practice**, always with examples.

> **Audience**: platform administrators, marketing, customer service, sales,
> product, and UX research teams, plus developers who integrate Turing. No
> technical knowledge is needed for Parts I, II, and IX.

---

# Contents

**Part I: Fundamentals**
1. The problem: why the generic voice is expensive
2. What a Persona is in Turing
3. The Persona in the company's day-to-day: who gets what

**Part II: Anatomy of a Persona**
4. The three roles: SPEAKER, AUDIENCE, and BOTH
5. Identity: name, description, state
6. The System Instruction (the narrative core)
7. Style: tone, verbosity, and language style
8. Vocabulary: mandatory and forbidden terms
9. Big Five (OCEAN) personality
10. Grounding source: where the persona gets its answers
11. Few-shot Store: teaching the persona by imitation
12. Live brand context (MCP)
13. Calibrating the model from style

**Part III: How the Persona reaches the model**
14. Assembling the prompt, layer by layer
15. Post-response tone validation (the second barrier)

**Part IV: Persona as Voice**
16. Attaching personas to an AI Agent
17. Talking directly to a Persona
18. Switching persona mid-conversation (flows)

**Part V: Persona as Audience**
19. The reader profile and the evaluation notebook
20. The content-fit score: does the text match the reader?
21. The fit report
22. Suggesting the best persona for a piece of content

**Part VI: Persona Match (N×N)**
23. Content-fit projects: many contents × many personas
24. Matrix, lenses, and scheduled re-analysis

**Part VII: Persona Dialogue**
25. Dialogues between personas

**Part VIII: Synthetic User Research**
26. Studies, protocols, and the participant cohort
27. The insights report: themes, the saturation signal, and change over time
28. Synthesize a cohort, assistant, and program
29. The bridge to agent evaluation

**Part IX: Customer scenarios**
30. E-commerce, banking, healthcare, SaaS, public sector, media

**Part X: Persona from audio**

**Part XI: Operational guide and best practices**

**Appendices**: API reference · Glossary of options · Checklist

---

# Part I: Fundamentals

## 1. The problem: why the generic voice is expensive

Take any modern LLM and ask it: *"answer like a sales rep"*. You'll get something
polite, helpful, and **indistinguishable** from the next ten companies that asked
the same model the same thing. Worse: the same model, on the same question, can
quote different prices, use wrong product names, and forget your discount window.

This is a problem for **two concrete business reasons**:

1. **Conversion.** A generic voice doesn't sell. Customers convert when they feel
   they're being treated like adults, by someone who knows what they came for. A
   senior sales rep doesn't read scripts: they sound like *the company*. A
   Persona gives your AI assistant that consistency, at scale, 24 hours a day.
2. **Risk.** That same generic voice can say something you'd never authorize: a
   wrong product name, a discount that doesn't exist, a claim about a competitor.
   A Persona installs guardrails: vocabulary you require, vocabulary you forbid,
   enforced **in the prompt** and **after the response**, before it reaches the
   user.

A Persona is the cheapest, most repeatable way to make every AI Agent speak with
**one single voice**.

> **The invisible cost.** Companies measure the cost of training a human agent,
> but they rarely measure the cost of *a thousand answers a day* from an
> assistant that sounds like every other one. Every off-brand response is a
> micro-erosion of the brand. The Persona turns that erosion into an asset.

## 2. What a Persona is in Turing

A **Persona** is the **voice profile** of your AI. It's the difference between an
assistant that sounds like every other chatbot on the internet and one that
sounds like the most senior, most on-brand, most consistent representative your
team could possibly hire.

You configure a Persona **once**, and from that moment on every agent it's
attached to:

- speaks in **your tone**, not the model's default;
- uses the words you want to be remembered by;
- never uses the words that get you in trouble;
- answers with the **vocabulary of customers who already converted**, retrieved
  live from a few-shot store;
- pulls **brand facts in real time** from a brand server, so promotions and
  product names are never out of date.

But the Persona in Turing is more than voice. It has evolved into something you
**operate directly**. Beyond giving an agent its voice, a Persona can:

- represent a **target reader** (an audience) against whom you measure whether a
  piece of content is a good fit;
- **talk to you** directly, to test the voice before attaching it;
- **hold a dialogue with other personas** on a topic, in a live "voice diff";
- serve as a **synthetic research participant**, interviewed at scale to
  anticipate how an audience would react to an idea.

Where to configure it: **Administration → Personas** (`/bento/persona`). Personas
are **reusable** across any AI Agent.

## 3. The Persona in the company's day-to-day: who gets what

The Persona isn't a feature for one team only. It's a meeting point between areas
that rarely speak the same language. Here's how it shows up in the daily routine:

| Area | What the Persona solves day-to-day |
|---|---|
| **Marketing** | Ensures every AI response uses brand vocabulary and never forbidden terms. Updates facts (prices, promotions) through a brand server **without depending on engineering to publish the change**. |
| **Sales** | Personas by funnel stage (top, evaluation, closing) that convert discovery into a booked demo, without sounding like a script. |
| **Support / CX** | A consistent voice across every channel; compliance rules built in; tone tuned to context (formal for regulated industries, casual for self-service). |
| **Product / UX** | Synthetic research anticipates how personas react to an idea before spending real-participant time; content-fit evaluation checks whether documentation reaches the right audience. |
| **Content / Editorial** | "Who is this article for?" becomes a measurable question (Suggest + Persona Match), not a guess. |
| **Compliance / Legal** | Forbidden terms enforced in two layers; post-response validation masks slips. |
| **Engineering** | A Persona is attached to any agent without rewriting prompts; the voice is decoupled from the model and the tools. Swap the LLM, the voice stays. |

> **The sentence that sums it all up:** *swap the brain (the LLM), swap the hands
> (the tools): the voice is still yours.*

---

# Part II: Anatomy of a Persona

A Persona is a small bundle of decisions. Each one nudges the model in a specific
direction, and together they replace the model's default voice with yours. In the
chapters that follow, each field is explained with **what it does**, **why it
matters**, and **how to fill it in well**.

## 4. The three roles: SPEAKER, AUDIENCE, and BOTH

The first field, **Persona Kind** (`personaKind`), decides what the persona is
for:

| Kind | Role | Used by |
|---|---|---|
| **`SPEAKER`** *(default)* | A **voice** the agent speaks in | Agent attachment + prompt composition |
| **`AUDIENCE`** | A **reader** you evaluate content against | The content-fit evaluator |
| **`BOTH`** | Serves either role | Both |

The default `SPEAKER` keeps every existing persona byte-for-byte unchanged. An
**`AUDIENCE`-only persona never resolves as an agent's voice**: the system has a
guard that prevents it (`isUsableAsSpeaker()`). When you pick `AUDIENCE` or
`BOTH`, the form reveals the **Audience** section.

> **Example.** *"Non-technical executive buyer"* is an `AUDIENCE` persona: you
> never want the AI to speak *as* that buyer, but you want to measure whether your
> technical material is understandable *for* them. *"Senior account executive"*,
> by contrast, is `SPEAKER`. And *"Mid-level developer"* could be `BOTH`: your AI
> sometimes speaks as an experienced dev, and sometimes you measure whether
> content fits developers.

## 5. Identity: name, description, state

| Field | What it does | Why it matters |
|---|---|---|
| **Name** | Internal identifier (not shown to the end user) | Lets the team say *"use the Sales Persona on that agent"* without ambiguity |
| **Description** | One-line summary of when to use it | Helps non-technical admins pick the right persona (max. 500 characters) |
| **Enabled** | On/off | Retire a persona without deleting it; agents fall back to no-persona behavior |

Naming best practice: use names that signal the stage or the function:
`top-of-funnel-sales`, `solutions-engineer`, `onboarding-coach`,
`executive-buyer-audience`. Personas are cheap; make several, each one nicely
specific.

> **💡 Worked example: the pharmacy chain.** "VitaPharm" has 4 personas in its
> catalog: `pharmacist-advisor` (answers medication questions), `deals-promoter`
> (pushes the week's promotion), `formal-support` (complaints), and
> `senior-audience` (target reader for validating package inserts). When a manager
> says *"the homepage chat is too aggressive"*, the team knows exactly which
> persona to disable (`deals-promoter`) and which to keep, without touching code.
> The **Description** field on each one (*"use in the clinical-question flow"*)
> lets the manager herself, non-technical, choose correctly in the agent's
> dropdown.

## 6. The System Instruction (the narrative core)

The **System Instruction** (`systemInstruction`) is free text that is prepended
to the agent's prompt. It's the narrative core of the persona, where you describe
*who it is* and *how it behaves*.

Example (top-of-funnel sales persona):

> *"You are a senior account executive. Open with a one-sentence outcome the
> prospect can imagine. Ask exactly one qualifying question per turn. Never quote
> prices in the first 5 turns: instead, anchor on value."*

The editor has a **"Help me write"** assistant (AI Authoring): describe in natural
language what you want and the AI drafts the instruction (and other fields) for
you to review. Nothing is saved automatically: you approve.

:::tip[Golden tip]
The system instruction says **what** and **how** the persona *speaks*; it must
not repeat the agent's **purpose** (what the agent *does*). Keep the two separate:
 otherwise the model "fights itself".
:::

> **💡 Worked example, the same shop, two personalities.** An artisan coffee shop
> writes the instruction for the `passionate-barista` persona: *"You are a barista
> who loves specialty coffee. Speak with genuine enthusiasm, always suggest a bean
> by its origin, and never push the most expensive one: recommend what matches
> the person's taste."* Months later, for the airport counter (where the customer
> is in a hurry), they create the `express-barista`: *"You are quick and kind.
> Answer in one sentence, offer the most popular order right away, and confirm
> whether the person wants it to go."* Same menu, same brand, but the
> **instruction** draws two complete personalities, each right for its context.

## 7. Style: tone, verbosity, and language style

Three structured fields shape the register of the response:

**Tone** (`tone`):

| Value | When to use |
|---|---|
| `FORMAL` | Regulated industries, institutional communication |
| `CASUAL` | Self-service, younger audiences |
| `TECHNICAL` | Developer relations, technical pre-sales |
| `EXECUTIVE` | C-level discovery, outcome emphasis |

**Verbosity** (`verbosity`, 1–5 scale): lower is shorter. Use **2** for chat
(mobile-friendly answers) and **4** for advisory contexts where the user expects
depth.

**Language style** (`languageStyle`), the shape of the response:

| Value | Typical use |
|---|---|
| `NEUTRAL` | Balanced default |
| `DIRECT` | Objective, no-nonsense answers |
| `NARRATIVE` | Long-form content, storytelling |
| `PERSUASIVE` | **Sales** |
| `INSTRUCTIONAL` | **Onboarding**, tutorials |

These three fields become a small structured paragraph in the prompt (e.g., *"Use
an EXECUTIVE tone, verbosity 2 (concise), PERSUASIVE language."*).

> **💡 Worked example, the same question, three voices.** A customer asks *"is the
> annual plan worth it?"*. See how each configuration changes the answer:
> - **`EXECUTIVE` · verbosity 2 · `PERSUASIVE`** → *"It is: you lock in the 2025
>   price and get 2 months free. Most teams that switch recover the cost within 90
>   days. Want me to run the numbers for your volume?"*
> - **`TECHNICAL` · verbosity 4 · `INSTRUCTIONAL`** → a paragraph detailing the
>   month-by-month savings calculation, with each plan's limits in bullets.
> - **`CASUAL` · verbosity 2 · `DIRECT`** → *"Way cheaper over the year 😊 it's 16%
>   off. Shall we?"*
>
> It's the **same AI, the same model, the same question**, only the persona
> changes. Pick `EXECUTIVE`/verbosity 2 for the sales chat on the corporate site
> and `CASUAL`/verbosity 2 for the delivery app's WhatsApp.

## 8. Vocabulary: mandatory and forbidden terms

Two fields enforce the brand's language explicitly:

- **Mandatory Terms** (`mandatoryTerms`): words/phrases the LLM **must** use. It
  anchors the brand vocabulary. E.g.: `Customer Success | ROI | outcome`.
- **Forbidden Terms** (`forbiddenTerms`): words/phrases that must **never**
  appear. Compliance and brand safety: competitor names, discontinued products,
  sensitive claims.

> **The separator is the pipe (`|`), not the comma.** This lets a single term
> contain a comma: important for phrases like *"Tier 1, Tier 2 customers"*. In
> the form you type **one term per line**; the system joins them with pipes on
> save.

Forbidden terms are enforced in **two layers** (see Chapter 15): in the prompt
(the model never *intends* to say them) and after the response (if it says them
anyway, they're masked).

> **💡 Worked example, the health insurer.** "HealthPlus" **cannot** let the AI
> say "cure", "guaranteed", or name the competitor "LifePlan". And it **wants**
> every response to use "coverage", "provider network", and "waiting period".
> Configuration:
> - Mandatory: `coverage | provider network | waiting period`
> - Forbidden: `cure | guaranteed | no waiting period | LifePlan`
>
> When the model, under pressure from an insistent question, writes *"this
> treatment is guaranteed by LifePlan"*, the user receives *"this treatment is
> `[***]` by `[***]`"*, and a log records the slip for the compliance team to
> review the prompt. No forbidden claim reaches the member.

## 9. Big Five (OCEAN) personality

Five optional controls from 0–100 give the persona a measurable psychological
personality, the **Big Five (OCEAN)** model:

- **O**penness
- **C**onscientiousness
- **E**xtraversion
- **A**greeableness
- **N**euroticism

When set, they become behavioral guidance in the prompt (only the traits outside
the neutral 34–66 band are injected, to avoid clutter) and (with style
calibration on) nudge the sampling temperature slightly.

**Why does this matter?** In a **research participant cohort** (Part VIII),
spreading OCEAN traits across participants is what stops every simulated user
sounding the same. It's the difference between "10 polite clones" and "a diverse,
realistic audience". They're optional and off by default, existing personas don't
change.

> **💡 Worked example, testing a finance app with a realistic cohort.** A fintech
> wants to know how different profiles react to the "invest now" screen. Instead
> of 5 identical personas, the team spreads the OCEAN traits:
> - `anxious-anna`: Neuroticism **85**, Openness 30 → reacts with fear of losing
>   money, asks for guarantees.
> - `adventurous-bruno`: Openness **90**, Neuroticism 15 → wants the riskiest
>   product, finds the screen "too conservative".
> - `methodical-carla`: Conscientiousness **90** → wants to read every term before
>   clicking.
>
> In synthetic research (Part VIII), each one responds **consistently with its
> personality**, and the report reveals that the screen needs two versions: one
> that reassures (for Anna) and one that highlights upside (for Bruno). Without
> OCEAN, all three would have given the same lukewarm answer.

## 10. Grounding source: where the persona gets its answers

By default, a persona answers "from memory", using the AI model's general
knowledge. The problem is that, when the subject is specific to your business, the
model sometimes **makes up** something that *looks* right. The **grounding source**
(`groundingSource` field) solves this: it **binds the answers to real content you
control**, instead of letting the AI improvise.

| Source | What it does |
|---|---|
| `NONE` *(default)* | The persona answers with the model's own general knowledge |
| `SN_SITE` | Answers become grounded on an **indexed search site** (a Semantic Navigation site), the AI consults real content before answering |
| `NOTEBOOK` | Answers are grounded on the **persona's own material** (the sources you attached to it) |

In practice: before answering, Turing searches the chosen content for the most
relevant passages and hands them to the model in a separate, labeled block. That
way, the AI answers from what's written there, not from guesses. It's a safe
process: if the search fails for any reason, the conversation **continues
normally**, without breaking.

> **💡 Worked example, the advisor who only says what's in the manual.** A
> carmaker has a `mechanic-specialist` persona that serves dealerships. Without a
> grounding source, it sometimes "invents" a bolt torque that looks right but is
> wrong. By turning on the `SN_SITE` source pointing to the indexed site of the
> **official technical manuals**, every answer becomes grounded in the real
> content: if the 2024 model's manual says "45 N·m", that's what comes out. The
> `institutional-support` persona, meanwhile, uses the `NOTEBOOK` source with its
> own already-approved FAQ material, so it never says anything beyond what legal
> validated.

## 11. Few-shot Store: teaching the persona by imitation

The hardest part of a brand voice isn't the rules: it's the *feel*. You can
describe "be formal", but you can't fully describe *"the way Maria, the best
salesperson, answers a discount request"*.

The solution is to teach **by example**, not just by description. The persona can
have a **Few-shot Store**, a collection of model conversations, each with a
typical customer question and the ideal answer, written in the persona's voice. At
every new conversation, Turing looks in the store for the examples most similar to
what the customer just asked and shows them to the AI as a reference, an instant
before it answers. It's like handing the new employee a few dialogues from the
top-performing employee and saying: *"answer roughly like this"*.

> In the form, this feature appears as the **Few-shot Store** field and, under the
> hood, uses a "searchable content bank" of the platform (the *Embedding Store*,
> the same one semantic search uses). You don't need to understand the mechanics:
> just point the persona at the right collection.

**How to build the store:**

1. Create a dedicated collection just for that persona's examples (e.g.,
   `sales-examples`).
2. Put pairs of **real or reviewed question and answer** in it: each item is a
   question your customers would ask + the answer in the persona's tone. **Quality
   beats quantity: 20 well-written examples beat 200 mediocre ones.**
3. Point that collection at the persona's **Few-shot Store** field.

**During the conversation:** when the customer writes, Turing retrieves the 3 most
similar conversations from the store (by default) and places them before the AI as
*"here's how we've answered questions like this"*. The AI then answers in the same
style, adapting to the current case, without copying.

> **Where do the best examples come from?** From **Chat Analytics**: export the
> sessions where the AI got it right (the customer reached their goal, sentiment
> was positive, the deal closed) and use them as a starting point. The store stays
> fresh because *it's the conversations that worked that feed it*.

> **💡 Worked example, the real-estate agency that learned to "talk like an
> agent".** "RightHome" had defined its voice as persuasive, but the answers
> sounded robotic. The team gathered 25 real conversations where the
> top-performing agent booked a viewing. One of the examples stored in the
> Few-shot Store:
> - **Customer's question:** *"is the 2-bedroom still available?"*
> - **Ideal answer (in the persona's voice):** *"It is! And lucky you: it's the
>   corner unit, with the balcony that catches the morning sun. I can fit you in
>   for a viewing tomorrow at 10am, or do you prefer late afternoon?"*
>
> When a new customer asks about a 3-bedroom, the AI **doesn't copy** that answer
>: it mimics the *rhythm*: confirms availability, highlights one concrete
> differentiator, and already offers two time slots. It's the house's "way of
> closing", learned by example, not memorized as a rule.

## 12. Live brand context (MCP)

Customers ask about prices, active promotions, whether a feature is in beta. If
you "bake" those facts into the prompt, two bad things happen: the prompt grows
too large, and every price change becomes a request for engineering to publish.

The **Brand Context MCP** field (`brandContextMcpServer`) solves this. It points
the persona at an MCP server you control. At every conversation, Turing calls an
MCP tool (e.g., `get_brand_context`) and injects the response JSON as a *"Current
brand facts:"* block.

You get:

- A **single source of truth** for prices, names, and promotions;
- **Marketing updates the facts** without anyone touching the persona;
- Short, readable prompts (a key driver of LLM performance);
- The same MCP reused across multiple personas (sales, support, partners).

A common architecture: marketing maintains a CMS, an MCP server reads from the
CMS, the persona pulls from the MCP. The CMS is the system of record; everything
else is downstream.

> **💡 Worked example, the Black Friday that didn't need engineering.** An
> electronics retailer has the `store-salesperson` persona wired to a `deals-mcp`
> that reads marketing's campaign panel. At 11:59pm on Thursday, the discount on
> the "gaming laptop" was 10%. At midnight, marketing changes it to 25% in the
> panel, and **on the very next message** the chat already answers *"the gaming
> laptop is 25% off today, only on Black Friday!"*. Nobody touched the persona,
> nobody shipped code, and the promotion was never out of date. The same
> `deals-mcp` feeds the personas for the website, WhatsApp, and the in-store kiosk
> at the same time.

## 13. Calibrating the model from style

By default, the style reaches the model **only as text**, the actual sampling
parameters (`temperature`, `max tokens`) come from the LLM Instance. Turn on
**Calibrate model from style** (`calibrateModelParams`) so the style also tunes
those parameters per turn:

| Style field | Tunes | Mapping |
|---|---|---|
| **Verbosity** | `max tokens` (length ceiling) | 1→256 · 2→512 · 3→1024 · 4→2048 · 5→4096 |
| **Tone** | `temperature` (determinism vs. creativity) | TECHNICAL 0.2 · FORMAL 0.3 · EXECUTIVE 0.4 · CASUAL 0.8 |

So a `TECHNICAL`, verbosity-2 persona runs cooler and shorter; a `CASUAL`,
verbosity-5 persona runs warmer and longer. The OCEAN traits give an additional
nudge to the temperature. With the toggle **off** (the default), nothing changes.
The calibration applies to agent chat, direct chat with the persona, and the
persona dialogue.

> **💡 Worked example, stopping the legal assistant's "flights of fancy".** A law
> firm had a `paralegal` persona (`TECHNICAL` tone) that, on the LLM Instance
> configured with temperature 0.7, sometimes over-embellished and cited
> nonexistent case law. By turning on **Calibrate model from style**, the
> `TECHNICAL` tone pulls the temperature down to **0.2** automatically, more
> predictable, sober answers, and verbosity 2 keeps the answers **short**,
> cutting the rambling. The same firm's `blog-writer` persona (`CASUAL` tone,
> verbosity 5), by contrast, gets long, loose answers. One LLM Instance, two
> behaviors, decided by the persona.

---

# Part III: How the Persona reaches the model

## 14. Assembling the prompt, layer by layer

A Persona doesn't **replace** the agent's system prompt: it's **stacked on top**.
Here's exactly what happens at chat time:

```
1. Agent system prompt          → "You help customers find products"
2. Persona system instruction   → the brand voice directive
3. Style block                  → tone · verbosity · language style
4. Mandatory/forbidden vocab    → "Always use: …" / "Never use: …"
5. Personality (OCEAN)          → traits outside the neutral band
6. Brand context (live)         → fetched from the MCP at session start
7. Grounding knowledge          → passages of real content, per message
8. Tool definitions             → JSON schemas of native + MCP tools
9. Few-shot examples            → the most similar model conversations
10. Conversation history
11. Current user message
        ↓
       🧠 LLM
        ↓
   🛡 Tone validator (masks forbidden terms)
        ↓
      💬 Response to the user
```

Layers 1–6 form a cached **static block** (`TurPersonaStaticPromptCache`): the
model doesn't reprocess them on every message, which saves time and cost. Layers 7
and 9 evolve per turn; 10 and 11 grow naturally with the conversation.

Internally, the one that performs this merge is the `TurPersonaPromptComposer`,
fed by prompt contributors (`TurPersonaPromptContributor`,
`TurPersonaGroundingPromptContributor`). You don't need to know this to use it,
but it's helpful to understand that **the order is always the same and can be
audited**.

> **💡 Worked example: why the second message is cheaper.** In a telecom support
> chat, the customer's first message assembles the whole prompt: layers 1–6 (the
> persona's static block) plus the brand facts from the MCP. From the second
> message on, that block comes **from the cache**: the model doesn't reprocess
> the system instruction, the vocabulary, or the personality on every turn. In a
> 15-message conversation, this reduces reprocessing and latency visibly. And when
> you **edit the persona** (change a forbidden term), a database listener
> invalidates the cache automatically: the next chat already uses the new block,
> without restarting anything.

## 15. Post-response tone validation (the second barrier)

The prompt's guardrails (forbidden vocabulary in the system message) are
necessary but not sufficient. LLMs sometimes drift, under tool-calling pressure
or in long sessions. That's why Turing enforces forbidden terms **a second time,
after the response**, before it reaches the user.

`TurPersonaToneValidator` scans every assistant response against the forbidden
list. Matches are **masked** (replaced with `[***]`) rather than blocking the
entire response. And it works in Portuguese, English, and Spanish: it analyzes the
response and the forbidden terms in the right language and recognizes variations
of the same word (singular/plural, conjugations), as well as whole phrases.

Why two layers?

| Layer | Stops what | Latency |
|---|---|---|
| **Prompt-side** | The model never *intends* to say it | Free (already in the prompt) |
| **Post-LLM** | The model said it anyway | Microseconds (regex/stem) |

You don't choose between the two: both stay on whenever a Persona is attached.

> **💡 Worked example: why recognizing word variations matters.** A brand forbids
> the term `promotion`. A literal comparison would let *"we're **promoting** this
> item"* and *"see our **promotions**"* slip through. Because the check understands
> **English** and recognizes variations of the same word, it catches all three
> forms (`promotion`, `promotions`, `promoting`) and masks them all. In a store
> serving customers in Portuguese, English, and Spanish, the same persona catches
> `oferta`/`offer`/`oferta` in the language of each response, without you
> maintaining three lists.

---

# Part IV: Persona as Voice

## 16. Attaching personas to an AI Agent

This is how a persona's voice actually gets applied. When creating/editing an AI
Agent, open the **Persona** tab (`/bento/ai-agent/instance/:id/persona`):

- An agent carries a **catalog** of personas (multi-select) and marks **one as
  default** (the star).
- On every turn, the `TurAgentPersonaResolver` decides which persona governs, in
  this order of precedence:
  1. Flow override (`__activePersonaId` in the flow state);
  2. Per-request persona (`requestPersonaId`);
  3. The agent's default persona.
- `AUDIENCE`-only personas are rejected as a voice.

**Decision tree for choosing an agent's persona:**

1. **Is the agent customer-facing?** If not, the persona is optional (internal
   agents often just need precision). If yes, **always pick a persona**, even a
   minimal one. Customer-facing without a persona means *the LLM's default voice
   is your company's voice*.
2. **Which part of the funnel?** Pick a persona built for that stage. Don't try to
   make one persona cover all five.
3. **Is the few-shot store fresh?** If it was built six months ago and the product
   has had two launches since, the answers will cite features that no longer
   exist. Review it quarterly.

> **💡 Worked example: one agent, two personas, one default.** A university has a
> "Student Center" agent. Its catalog holds two personas: `formal-registrar`
> (default ⭐) and `laid-back-senior`. In normal service the AI uses the default.
> But during the admissions campaign, the landing page passes `requestPersonaId =
> laid-back-senior` on the chat call, and the **same agent**, with the same tools
> and the same knowledge, serves applicants in the voice of an upbeat senior
> student. Once the campaign is over, just stop sending the override: it goes back
> to the registrar. Zero change to the agent.

## 17. Talking directly to a Persona

The **Open chat** action (on the persona's dashboard) starts a live conversation
*with the persona itself*, no agent required. It's the fastest way to **hear a
voice before attaching it**: type a message, hear the answer in the persona's
tone, refine the instruction, try again.

- Runs on any enabled LLM Instance you pick;
- It's **stateless** (nothing is persisted);
- Lives at a shareable URL (`/bento/chat/persona/{id}`), so you can send a
  colleague the exact persona under test;
- Supports multimodal attachments and all native tools;
- An `AUDIENCE`-only persona can't speak: the action appears only for
  `SPEAKER`/`BOTH`.

Under the hood, this uses the endpoint `POST /api/v2/persona/{personaId}/chat`
(for those integrating on their own).

> **💡 Worked example, approving the voice before going live.** The marketing
> director of a digital bank wants to approve the assistant's new voice before
> launch. The team opens `Administration → Personas → genz-assistant → Open chat`,
> picks the production LLM, and chats: *"my bill is wrong, now what?"*. The answer
> comes in the voice under test. She finds it "too informal for billing", the team
> adjusts the system instruction and tests again, all in minutes, without
> touching any agent. Satisfied, she **sends the link
> (`/bento/chat/persona/{id}`) to legal** to validate the same persona. Only then
> is the persona attached to the real agent.

## 18. Switching persona mid-conversation (flows)

In a **Chat Flow**, a node can switch the active persona mid-conversation by
setting `__activePersonaId` in the flow state. This lets you, for example, start
with a **casual triage** persona and escalate to a **formal specialist** persona
when the conversation gets technical, all in the same session, without the user
noticing the infrastructure transition.

---

# Part V: Persona as Audience

Up to here the persona has been a **voice**. Now comes the mirror: a persona can
describe a **reader** (the audience your content is made for) so you can
evaluate whether a document **actually fits** the people who should read it.
*"Will a non-technical buyer understand this spec sheet?"* becomes a measurable
question, not a guess.

## 19. The reader profile and the evaluation notebook

An `AUDIENCE`/`BOTH` persona carries a **reader profile**:

| Field | What it describes |
|---|---|
| **Reading level** | `ELEMENTARY(5)` · `MIDDLE(8)` · `SECONDARY(12)` · `UNDERGRADUATE(15)` · `GRADUATE(18)` (the number is the reader's approximate years of study) |
| **Domain expertise** | `NOVICE` · `BEGINNER` · `INTERMEDIATE` · `ADVANCED` · `EXPERT` |
| **Vocabulary ceiling** | An allow-list of terms the reader *knows* |
| **Comprehension / accessibility notes** | Free text |
| **Primary language** | ISO-639 (e.g., `pt`, `en`, `es`) |

To evaluate fit you need content. An audience persona gets an **evaluation
notebook**, a set of sources:

| Source type | Where the text comes from |
|---|---|
| **Indexed document** | A page already indexed in a search site (Semantic Navigation) |
| **File** | An uploaded file (PDF, DOC…), from which Turing extracts the text automatically |
| **Link (URL)** | A web page, which Turing fetches safely and extracts the text from |

Reading each source is fault-tolerant: each one shows its state (**pending**,
**extracted**, or **failed**, with the option to retry) and the text is stored for
reuse. You manage everything in the persona's **Evaluation notebook** section: add
a link, an indexed document, or a file; see the state in red/amber/green; preview
the text; and re-extract what failed.

> **💡 Worked example: the reader profile that exposes hidden jargon.** An insurer
> creates the `AUDIENCE` persona `everyday-customer`: reading level `MIDDLE(8)`,
> expertise `NOVICE`, and a **vocabulary ceiling** with the few terms the customer
> knows (`deductible`, `claim`, `policy`). In the notebook, it adds 3 sources: the
> URL of the "general terms" page, the policy PDF (a file, from which the text is
> extracted), and the indexed document of the FAQs. One of the sources fails
> extraction (the URL blocked the bot): it stays **red (`FAILED`)**, but the
> other two go green, and the evaluation runs on them while the team re-extracts
> the failed one. Since any word outside the vocabulary ceiling counts as
> "complex", terms like *"subrogation"* jump out in the readability score.

## 20. The content-fit score: does the text match the reader?

**Content-fit** measures how well a piece of content matches a given reader. It
combines two signals, and keeps working even if you don't have an AI configured:

1. **Readability (deterministic, no AI, in Portuguese, English, and Spanish).**
   Turing computes, with recognized readability formulas, the average sentence
   length, syllables per word, the proportion of difficult words (discounting the
   terms the reader already knows), and passive-voice usage. All of this becomes a
   0-to-100 score compared to the reader's reading level. Because it doesn't
   depend on AI, the result is always the same for the same text.
2. **AI-based evaluation (grounded in the text, no inventing).** The AI **puts
   itself in the reader's shoes** and, looking **only** at the source text,
   returns: a fit score, **what fits** and **what doesn't fit** (each problem with
   the exact span, the reason, and an improvement suggestion). If the AI flags a
   span that **doesn't exist** verbatim in the text, that flag is discarded, no
   invented problems.
3. **Final score** = the average of the two (half each). If there's no AI, it uses
   readability only.

Color bands: **green ≥ 70 · amber ≥ 40 · red < 40**.

> **💡 Worked example, the two signals disagreeing on purpose.** An HR piece about
> career paths is evaluated for the `intern` persona.
> - **Readability** gives 82 (green): short sentences, few difficult words.
> - **AI evaluation** gives 45 (amber): in the intern's shoes, it points out that
>   the text assumes you know what a "9-box review cycle" and a "PDP" are
(>   concepts an intern hasn't mastered yet) each with the **exact span** and a
>   rewrite suggestion.
> - **Final score** = the average → **~64 (amber)**.
>
> The lesson: the text is *easy to read*, but *hard for this reader to
> understand*. Without AI configured, the report would show only the 82
> readability score, still useful, but missing the jargon.

## 21. The fit report

You can evaluate **one source** or **the whole notebook** at once; the result is a
report with an overall average and each source's score. The persona's **Fit
report** section shows, in "fits / doesn't fit" style:

- an overall bar and one per source, in red/amber/green;
- the list of what **fits**;
- what **doesn't fit**, as highlighted spans, each with the reason and a **rewrite
  suggestion**.

Without an AI configured, it shows only the readability score.

> **💡 Worked example, the material that didn't speak the CFO's language.**
> Marketing writes a piece aimed at finance directors. You point the
> `non-technical-cfo` persona at the file and run the report: score 48 (amber),
> with problems in "microservices architecture" and "p99 latency", each with a
> rewrite suggestion in financial-outcome language. The material is fixed
> **before** it goes live.

## 22. Suggesting the best persona for a piece of content

Content-fit answers *"does this text fit **this** persona?"*. **Suggest persona**
answers the inverse: *"so **which** audience is this content actually for?"*.

On the personas list (`/bento/persona/suggest`), you paste a piece of content
(optionally limiting to a few personas) and Turing evaluates its fit against **all
active audience personas**, returning them **in order, from the best fit to the
worst**. Each row shows the red/amber/green bar and the same *fits* / *doesn't
fit* detail, with the best one flagged at the top.

It's for **deciding whom to publish each article for** and for **choosing between**
similar personas.

> **💡 Worked example, the news portal that discovered the right audience.** An
> editor finished a dense piece on "tax reform and small-business tax regimes" and
> didn't know which section to publish it in. They paste the text into **Suggest
> persona**, without limiting the options. Turing evaluates it against every
> audience persona and returns the order:
> 1. `business-accountant`: **88 (green)** ← best fit, flagged at the top
> 2. `solo-entrepreneur`: 61 (amber): understands the topic, but gets stuck on
>    "assessment regime"
> 3. `general-reader`: 34 (red): too dense
>
> Immediate editorial decision: publish in the Accounting section and, if they
> want to reach solo entrepreneurs, commission a lighter version. What was a guess
> became a number.

---

# Part VI: Persona Match (N×N)

## 23. Content-fit projects: many contents × many personas

Content-fit evaluation (Ch. 20) measures **one** persona against your content.
**Persona Match** turns that into a **reusable project** that evaluates **many
contents against many personas at once**, and keeps them tuned to each other over
time. It's at **Administration → Personas → Persona Match**
(`/bento/persona/match`), alongside Persona Dialogue.

A **project** brings together three things:

- **Contents**: added **once per project** (not per persona), in three ways: a
  **link (URL)**, an **already-indexed document** (picked from a search site), or
  a **file** (PDF, DOC…) from which the text is extracted. Each content's text is
  read and stored on add; when you re-run the analysis, Turing recognizes what
  hasn't changed and doesn't redo the work needlessly.
- **Personas**: any set of your personas.
- A **schedule** (`Manual` / `Daily` / `Weekly`) and, if you want, a specific AI
  model (by default, the global model).

Running the analysis evaluates each **(content × persona)** pair and stores the
result in a **grid cell**: the same content-fit score (readability + AI
evaluation), with what **fits**, what **doesn't fit**, and rewrite suggestions.

## 24. The matrix, the lenses, and scheduled re-analysis

The project screen shows the result three ways:

- **Colored grid**: a table (contents as rows, personas as columns) painted
  green/amber/red by fit. It **fills live**, cell by cell, as the analysis runs.
  Click any cell to open a panel with that pair's score, what fits, and what
  doesn't.
- **By content**: for each content, the personas ranked (which audience each
  piece serves best).
- **By persona**: for each persona, the contents ranked (which content serves
  each audience best).

Both lenses export to **PDF**.

**Scheduled re-analysis.** A `Daily`/`Weekly` project re-runs itself (once only,
even if you have several servers): the sources are re-read (so a link whose
content changed is captured) and only the cells whose content changed are
recomputed. `Manual` projects run only when you click **Run analysis**. This is
how personas and content stay tuned to each other as content changes.

Because a project can hold a **single** persona, it also **replaces the old
per-persona validate notebook**: the persona's **Validate content** action now
opens (or re-opens) a one-persona project and takes you to the project area.

> **💡 Worked example, a SaaS help center audited every Monday.** The Success team
> creates the "Help Center × Personas" project. Contents: the 40 indexed articles
> from the help center (SN_DOC) + 3 quick-start guide PDFs (upload). Personas:
> `beginner-user`, `it-admin`, `developer`. Schedule **Weekly**. Every Monday
> morning the colored 40×3 grid appears already filled in:
> - The row for "Configuring SSO" is **green** for `it-admin` and `developer`, but
>   **red** for `beginner-user`, expected.
> - But "Getting started" is also **amber** for the beginner: it shouldn't be!
>   Clicking the cell, the drawer shows the mismatch: the article already assumes
>   the user created an API key. A rewrite suggestion is attached.
>
> Because one article changed since last week, only its 3 cells were recomputed
> (Turing recognized the other 117 hadn't changed). The **by persona** lens
> exports a PDF that becomes the quarter's rewrite backlog.

---

# Part VII: Persona Dialogue

## 25. Dialogues between personas

**Persona Dialogue** runs an automatic, turn-by-turn conversation between **two or
more** personas on a topic you choose. Because a dialogue is about peer speakers
(none of them "owns" the conversation) it's a **global** surface, reached from the
personas list (`/bento/persona/dialogue`) and organized as **saved projects**
(each one can be re-opened and re-run).

When opening/creating a project, you configure:

- a **topic**;
- a **cast** of speaker personas (minimum 2, the number in the badge is the
  **speaking order**; only `SPEAKER`/`BOTH` are selectable);
- an **LLM Instance**;
- a **turn budget** (rounds; default **10**, with a generous practical limit).

Turing seeds the first persona with the topic, then goes **round-robin** (feeding
each persona the previous speaker's reply) until the budget runs out. The
transcript is **streamed live** (each turn appears the moment it's generated,
color-coded per speaker) **and persisted**, so re-opening a project shows the last
conversation; re-running replaces it.

To keep the conversation from stalling, **every persona is required to end each
reply with a question** to the others (`DIALOGUE_DIRECTIVE`); the last round uses
a closing directive. If a turn fails mid-way, the partial transcript is kept with
a notice.

> **What it's for.** It's the quickest way to hear several brand voices talking to
> each other, a live "voice diff". Put `aggressive-sales` and `cautious-advisor`
> to debate "is the Enterprise plan worth it?" and you **hear** where the two
> voices diverge before deciding which to attach to which agent. It's also great
> for team training and content scripting.

For those integrating on their own: the saved projects live at
`/api/persona-dialogue` (with endpoints to set the participants, read the history,
and run live) and there's an older `/api/v2/persona-dialogue`, kept for
compatibility.

---

# Part VIII: Synthetic User Research

**Synthetic User Research** interviews a **cohort of personas** (playing the role
of participants) over a research protocol and then **synthesizes everything they
said** into an insights report organized by theme. It's at **Administration →
Personas → Synthetic research** (`/bento/persona/research`), alongside Persona
Match and Persona Dialogue.

:::caution
**It's a discovery co-pilot, not a replacement for real users.** Synthetic
research **front-loads** the problem: it sharpens your questions, reveals the
likely themes, and stress-tests an idea before you spend the (expensive) time of a
real participant. It is **not** absolute truth and has no statistical validity.
Two mechanisms exist precisely to keep the output honest, the **saturation
signal** (warns you when interviewing more people no longer brings anything new)
and the **persona's grounding source** (Ch. 10). The report frames itself as a
co-pilot throughout. **Always validate with real users before acting.**
:::

## 26. Studies, protocols, and the participant cohort

A **study** is the reusable unit. It brings together:

- A **goal** and an optional **hypothesis**: what you want to learn.
- An **interview protocol**: how each participant is interviewed. There are three
  formats:
  - **Dynamic interview**: the interviewer starts with the basics and **adapts
    the next questions** based on the answers, until it has enough (bounded by a
    maximum number of questions). Ideal when you don't yet know what to ask.
  - **Scripted questions**: a fixed list, asked in the same order for everyone,
    word for word. Good for comparing answers side by side.
  - **Concept test**: the interviewer presents a proposed idea or message and
    measures the participant's reaction.
- An **audience**, the ordered list of participant personas, each interviewed
  separately.
- **Who answers the questions**: either a "bare" AI model (playing the
  assistant), or **one of your already-deployed agents**. Choosing one of your
  agents **inverts the roles**: the persona becomes the *user* and your real agent
   (with all its knowledge and tools) is the one that answers. That way the
  cohort **stress-tests the assistant you've already shipped**, finding flaws
  before the real customer does.
- **Two models, one for each stage** (an optional feature nicknamed "Big
  Shuffle"): you can use one AI model to *interview* and another to *synthesize*,
  so no single model's bias colors the whole study.
- A **schedule** (`Manual` / `Daily` / `Weekly`) to re-run the research
  automatically.

**Running, live.** When you click **Run interviews**, each conversation appears on
screen as soon as it finishes, with its status, and you can expand it to read the
questions and answers. Re-running an unchanged study is cheap: the system
recognizes what has already been done and doesn't redo work needlessly.

> **💡 Worked example, three protocols, three business questions.** A mobility
> startup wants to launch a scooter subscription plan:
> - **Dynamic interview**: goal *"understand what's blocking the subscription"*.
>   The interviewer asks the basics and **digs deeper**: if the persona mentions
>   "fear of not using it enough", it probes that fear. Good when you don't yet
>   know which questions to ask.
> - **Scripted questions**: the same 6 questions for all personas, in the same
>   order, to **compare the answers side by side**.
> - **Concept test**: presents the real offer text (*"$49/month, 60 min/day
>   included"*) and measures the reaction. This is where the `anxious-anna`
>   persona says *"what if I travel for a month?"*: an objection nobody had
>   foreseen.
>
> **Inverting the roles:** by pointing the interviews at the *already-deployed*
> support agent (instead of the bare model), the personas become *users* and test
> the real assistant: `methodical-carla` asks 8 questions in a row and reveals
> that the agent loses track after the third. It's putting the assistant to the
> test before the real customer complains. And, using **two models**, the study
> interviews with one and synthesizes with the other, so a single model's bias
> doesn't color everything.

## 27. The insights report: themes, the saturation signal, and change over time

Once a study has run, Turing **synthesizes** the interviews (and caches the
result) into an **insights report**:

- An **executive summary**.
- **Themes ranked by relevance**, each with **verbatim quotes** from the
  participants, traced back to the persona who said them (fabricated quotes are
  flagged, not accepted).
- **Recommendations**.
- Two ways to view (**by theme** and **by persona**) a **Regenerate** button,
  and **PDF export**.

**The saturation signal** answers the honest question *"have I interviewed enough
people?"*. As personas are interviewed in list order, Turing measures whether each
new participant still raises a **new** theme. When new themes stop appearing, it
reports: *"sample adequate at N participants"*. It's a direct calculation (no AI),
so it serves as a brake against overconfidence. A **theme-affinity map** shows
which participants cluster around each theme; and (for scheduled studies) a
**change-over-time** chart shows how the themes shift with each new run (one-shot
research becoming continuous validation).

A **concept-test** study also gets a **concept-fit** score: the proposed idea is
scored for each participant through the same content-fit evaluator as Persona
Match (Ch. 20).

> **💡 Worked example: when to stop interviewing.** In the scooter study, the team
> registered 12 personas in the cohort. The report brings the executive summary, 5
> themes ranked by relevance (*"fear of a fine for parking wrong"* at the top,
> with the verbatim quote of whoever said it) and recommendations. But the most
> valuable part is the **saturation signal**: from the 7th participant on, no
> **new** theme appeared, Turing reports *"sample adequate at 7"*. The team saves
> the remaining 5 and, better yet, gains an honest argument against the boss who
> wanted "just 20 more personas to be sure". In the **by persona** view,
> `anxious-anna` is alone on the "fear of a fine" theme, a sign that it's a niche
> concern, not a general one. Because the study runs **every week**, the
> **change-over-time** chart shows that, after the app added a "parking photo"
> feature, that fear shrank in the following rounds.

## 28. Synthesize a cohort, assistant, and program

- **Synthesize a cohort** (`/bento/persona/cohort`): don't have personas for the
  cohort yet? Write a **one-paragraph summary** describing the audience and Turing
  creates a **diverse** set of persona **drafts**, with well-spread OCEAN
  personalities. Nothing is saved automatically: you review each draft in a grid
  and **keep** it (opens the normal form to edit and save) or **discard** it.
- **Research Assistant**: every new study opens with an assistant (which you can
  skip): describe in free text what you want to learn and it builds a complete
  proposal (name, goal, hypothesis, protocol with the questions, and a suggested
  audience), already filling in the form for you.
- **Program view** (`/bento/persona/research/program`): plan **several studies**
  with different audiences and roll everything up into a single view: totals, each
  study's saturation, and (most importantly) the themes that **recur across
  different audiences** (a theme raised in 2 or more studies is highlighted). It's
  just a sum of the reports that already exist; it generates no new conclusion on
  its own.

> **💡 Worked example, from zero to a research program in one afternoon.** A
> product manager has never run research and needs to validate 3 feature ideas.
> - **Research Assistant:** she types *"I want to know whether pet owners would pay
>   for veterinary telemedicine"* → the assistant builds a complete study (goal,
>   hypothesis, concept-test protocol, 5 questions, and a suggested audience). She
>   just adjusts and saves.
> - **Synthesize a cohort:** she has no "pet owner" personas. She writes *"dog and
>   cat owners in major cities, middle income, varying levels of attachment"* and
>   asks for 8. Turing creates 8 **drafts** with varied personalities (from the
>   super-devoted owner to the practical one). She **keeps** 6 and **discards** 2
>, nothing was saved without review.
> - **Program view:** she runs the 3 studies (telemedicine, pet daycare, vaccine
>   plan) and opens the program view. The theme *"price distrust"* appeared in
>   **all three**, highlighted as recurring. That, and not any specific feature,
>   becomes the quarter's number-one priority.

## 29. The bridge to agent evaluation

A completed study can be **promoted to an evaluation dataset** (one click →
deep-link into the **Eval Studio**): the interviewer's questions become replay
turns and the participants' answers become reference material, turning a one-shot
study into a **repeatable agent-QA harness** that regression-tests against a
synthetic audience.

> **💡 Worked example, the research that became an automated test.** The study
> that stress-tested the support agent (Ch. 26) yielded 40 good synthetic-user
> questions and the expected answers. With one click, the team **promotes it to an
> evaluation dataset** and lands in the Eval Studio. From then on, every time
> someone tweaks the agent's prompt or swaps the LLM, CI runs those 40 questions
> and fails the build if quality drops. The interview that was "one-shot
> discovery" became a **permanent safety net**: `methodical-carla` now tests
> every new version, for free.

**Developer surface.** The `turing research` CLI verb defines/runs a study and
fetches its findings from code or from an automation (in the same spirit as the
`turing eval` command).

---

# Part IX: Customer scenarios

This part is for managers. Each scenario shows **the real problem**, **which
persona features solve it**, and **the business outcome**. The figures are
illustrative.

## 30.1 Fashion e-commerce: "turn browsing into a purchase"

**Problem.** The store's chat answers politely but doesn't sell; it cites products
that no longer exist and sometimes mentions "competitor X".

**Persona solution.**
- `SPEAKER` persona `personal-stylist`: `CASUAL` tone, `PERSUASIVE` style,
  verbosity 2; mandatory `look | matches | your style`; forbidden
  `competitor | cheap | clearance-bin`.
- Few-shot Store populated with Chat Analytics conversations that **converted**.
- Brand-context MCP pulling **live inventory and promotions** from the CMS.
- `SN_SITE` grounding on the indexed catalog, never suggests a discontinued
  product.

**Outcome.** The AI talks like the store's senior salesperson, with always-current
inventory, without naming a competitor. Marketing swaps the week's promotion
without filing a ticket with engineering.

## 30.2 Banking / regulated industry: "consistency and compliance"

**Problem.** An investment assistant must be accurate and **never** use language
that suggests a guarantee of returns.

**Solution.**
- `SPEAKER` persona `formal-advisor`: `FORMAL` tone, `INSTRUCTIONAL` style;
  forbidden `guaranteed | risk-free | sure profit | guaranteed returns`.
- Post-response tone validation masks any slip before it reaches the customer, a
  double compliance barrier.
- `AUDIENCE` persona `beginner-investor` to validate whether the educational
  materials are understandable (reading level `SECONDARY`).

**Outcome.** Every response passes through two compliance filters; the legal team
audits the forbidden-terms list, not every response.

## 30.3 B2B SaaS: "one funnel, several voices"

**Problem.** A single chatbot tries to do top-of-funnel, technical evaluation, and
onboarding, and does all three badly.

**Solution, one persona per stage:**
- `top-of-funnel-sales` (EXECUTIVE, PERSUASIVE) → converts discovery into a demo.
- `solutions-engineer` (TECHNICAL, INSTRUCTIONAL) → converts technical evaluation
  into buy-in from the technical side.
- `onboarding-coach` (CASUAL, INSTRUCTIONAL) → converts activation into retention.

A Chat Flow switches the active persona as the conversation moves from stage to
stage (`__activePersonaId`).

**Outcome.** Each funnel stage has the right voice; no persona tries to do
everything.

## 30.4 Healthcare: "genuinely accessible language"

**Problem.** Patient-guidance materials are written in medical jargon; nobody
knows whether the patient understands.

**Solution.**
- `AUDIENCE` persona `layperson-patient`: `ELEMENTARY` level, `NOVICE` expertise,
  a vocabulary ceiling with the terms the patient actually knows.
- **Persona Match** runs weekly on all the leaflets × patient personas; the
  colored grid shows incomprehensible documents in red, with per-span rewrite
  suggestions.

**Outcome.** Health content measured objectively by readability + AI evaluation
grounded in the text; rewrites prioritized by what's red.

## 30.5 Public sector: "serve every citizen"

**Problem.** A services portal must speak to very different audiences (seniors,
young people, business owners) and prove accessibility.

**Solution.**
- Several `AUDIENCE` personas covering the target audiences; accessibility notes
  filled in.
- **Suggest persona** routes each new article to the right audience.
- Synthetic research with a diverse cohort (spread OCEAN personalities) anticipates
  questions before publication.

**Outcome.** Public communication validated by audience, with an auditable trail.

## 30.6 Media / editorial: "who is this content for?"

**Problem.** The newsroom produces a lot of content and doesn't know which
audience each piece serves best.

**Solution.** N×N **Persona Match**: the entire archive × all reader personas. The
"by content" lens says which audience each article reaches; the "by persona" lens
says which content is missing for each audience (editorial gaps).

**Outcome.** An editorial backlog guided by content-fit data, not intuition.

---

# Part X: Persona from audio

You can **draft** a persona from a voice recording. The flow
(`POST /api/persona/derive-from-audio`, with a version that runs in the background
and shows progress live):

1. **Transcribes** the audio (the transcription engine is chosen in settings
   (cloud OpenAI, a compatible server installed in your own environment, or an
   engine embedded in Turing itself) and it splits large files into parts
   automatically).
2. **Two LLM calls**: (a) classifies the constrained fields as strict JSON (tone,
   verbosity, style, vocabulary, and audience descriptors); (b) writes a
   **character portrait** in text → becomes the system instruction.
3. Produces a `personaKind = BOTH` draft, **never saved automatically**. The
   "Derive from audio" action hands the draft to the new-persona form for human
   review and save.

> **Scenario.** A customer records 5 minutes describing "how our brand's ideal
> salesperson talks". In a minute, you have a persona draft with a tone,
> vocabulary, and a written system instruction: ready to edit and save. The same
> "derive, never auto-apply" discipline as the manifest wizard.

Choose the engine in **Administration → Settings → Global Settings →
Transcription**.

---

# Part XI: Operational guide and best practices

## Who does what, and how often

| Routine | Who | Frequency |
|---|---|---|
| Review mandatory/forbidden terms | Marketing + Compliance | Monthly |
| Re-populate the few-shot store with recent conversations that worked | CX / Sales | Quarterly |
| Run Persona Match over the content archive | Editorial / Content | Weekly (scheduled) |
| Update the facts on the brand-context server | Marketing | Continuous (no engineering involved) |
| Synthetic research before a product decision | Product / UX | Per initiative |
| Audit the forbidden list + masking logs | Compliance | Monthly |

## Common pitfalls

| Pitfall | Symptom | Fix |
|---|---|---|
| **The persona contradicts the agent's instructions** | Inconsistent answers; the AI "fights itself" | The agent's instructions say *what* it does; the persona says *how* it speaks. Keep the two separate. |
| **Forbidden terms overlap with the agent's domain** | Responses masked frequently; the user sees `[***]` | The forbidden list is for brand/compliance terms, not day-to-day technical vocabulary. Technical filters go in the agent's instructions. |
| **Few-shot store mixed with general knowledge** | The persona starts mimicking whatever's in the collection | The few-shot store should hold **only** model conversations in the persona's voice. To retrieve content, use a separate knowledge base. |
| **The brand-context server returns too much information** | Responses get slow and expensive | It should return only the essential and most recent facts (ideally very short). Filter on the server itself, not on the client. |
| **One persona for the entire funnel** | The onboarding coach tries to sell; the sales rep tries to teach programming | Personas are cheap. Make several, each tightly scoped. |
| **Treating synthetic research as truth** | A decision made without validating | It's a support tool. Use the saturation signal + the persona's grounding source and validate with real users. |

## Golden best practices

1. **Start small.** A minimal persona (name + instruction + tone) is already
   better than the model's default voice for any customer-facing agent.
2. **Tight scope.** Personas by stage/function beat a jack-of-all-trades persona.
3. **Quality in the few-shot store.** 20 excellent examples > 200 mediocre ones.
4. **Let the brand server carry the facts that change.** A short instruction =
   a better AI.
5. **Test by listening.** Use "Open chat" and "Persona Dialogue" to *hear* the
   voice before attaching it.
6. **Measure the audience, not just the voice.** Content-fit evaluation and
   Persona Match turn "I think it works" into a number.

---

# Appendix A: API reference

> This appendix is **for those integrating Turing by code**. If you only use the
> administration screen, you can safely ignore it, nothing here is needed to
> operate personas through the interface.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/persona` | List all personas |
| `GET` | `/api/persona/{id}` | Get a single persona |
| `POST` | `/api/persona` | Create a persona |
| `PUT` | `/api/persona/{id}` | Update a persona |
| `DELETE` | `/api/persona/{id}` | Delete a persona (evicts the static prompt cache) |
| `POST` | `/api/persona/chat` | "Help me write" assistant: drafts persona fields via conversation |
| `GET`/`POST`/`DELETE` | `/api/persona/{id}/source` | Evaluation notebook (`/upload` multipart, `/{id}/extract` re-extracts) |
| `POST` | `/api/persona/{id}/content-fit` | Fit report (`?sourceId` for one source, else the whole notebook) |
| `POST` | `/api/persona/suggest` | Ranks audience personas by fit to a piece of content |
| `POST` | `/api/persona/derive-from-audio` | Draft a `BOTH` persona from audio (multipart; never auto-saved); an async `/jobs` variant + SSE exists |
| `POST` | `/api/v2/persona/{id}/chat` | Talk directly to a persona (SSE; JSON or multipart) |
| `GET`/`POST`/`PUT`/`DELETE` | `/api/persona-match` | Persona Match projects: CRUD, `/{id}/personas`, `/{id}/sources` (+`/upload`, `/{sid}/extract`), `/{id}/matrix`, `/{id}/report`, `/{id}/run` (+`/stream` SSE) |
| `GET`/`POST`/`PUT`/`DELETE` | `/api/persona-dialogue` | Dialogue projects: CRUD, `PUT .../{id}/speakers`, `GET .../{id}/transcript`, `POST .../{id}/run/stream` (SSE, persists) |
| `POST` | `/api/v2/persona-dialogue` | Ephemeral dialogue stream (compatibility) |
| `GET` | `/api/sn/{site}/personas` | The audience personas the anonymous fit evaluation below accepts, for a client to offer |
| `POST` | `/api/sn/{site}/persona/{id}/content-fit` | Anonymous fit evaluation (public demo; read-only; 12,000-character limit) |

> On save, the mandatory and forbidden terms arrive from the form as a
> pipe-separated (`|`) list and are stored exactly as received. The check happens
> at conversation-assembly time (an empty list is fine, it simply adds no
> constraint).

# Appendix B: Glossary of options (each field's values)

> These are the internal values of each field; on the administration screen they
> appear with translated labels.

- **Persona Kind**: `SPEAKER` (voice) · `AUDIENCE` (audience) · `BOTH` (both)
- **Tone**: `FORMAL` · `CASUAL` · `TECHNICAL` · `EXECUTIVE`
- **Language style**: `NEUTRAL` · `DIRECT` · `NARRATIVE` · `PERSUASIVE` ·
  `INSTRUCTIONAL`
- **Grounding source**: `NONE` · `SN_SITE` (search site) · `NOTEBOOK` (persona's
  material)
- **Reading level**: `ELEMENTARY(5)` · `MIDDLE(8)` · `SECONDARY(12)` ·
  `UNDERGRADUATE(15)` · `GRADUATE(18)` (the number = approximate years of study)
- **Domain expertise**: `NOVICE` · `BEGINNER` · `INTERMEDIATE` · `ADVANCED` ·
  `EXPERT`
- **Source type**: `SN_DOC` (indexed document) · `ASSET` (file) · `URL` (link)
- **Source state**: `PENDING` · `EXTRACTED` · `FAILED`
- **Persona Match schedule**: `MANUAL` · `DAILY` · `WEEKLY`
- **Research protocol**: `DYNAMIC_SCRIPT` (dynamic interview) · `CUSTOM_SCRIPT`
  (scripted questions) · `CONCEPT_TEST` (concept test)
- **Audio processing state**: `QUEUED` · `TRANSCRIBING` · `ANALYZING` ·
  `SUCCEEDED` · `FAILED`

# Appendix C: Checklist for a new customer-facing persona

- [ ] Correct **kind** set (voice / audience / both)?
- [ ] Does the **system instruction** describe *who speaks* and *how*, without
      repeating the agent's purpose?
- [ ] **Tone + verbosity + style** chosen for the funnel stage?
- [ ] **Forbidden terms** for brand/compliance filled in (not day-to-day technical
      vocabulary)?
- [ ] **Few-shot Store** populated with real conversations that worked?
- [ ] **Brand-context server** connected for the facts that change (price,
      promotion)?
- [ ] **Grounding source** set, if the persona should answer from real content?
- [ ] Voice **tested** in "Open chat" before attaching to the agent?
- [ ] Persona **attached** to the agent and marked as default, if applicable?
- [ ] If it's an **audience** persona: reader profile and notebook filled in, and
      fit measured?

---

## Where the Persona fits in the bigger picture

A Persona is one of three layers that bring an AI Agent to life:

| Layer | What it provides |
|---|---|
| **LLM Instance** | The brain: the reasoning engine |
| **Tools + MCP Servers** | The hands: what the agent *does* |
| **Persona** | The voice: *how* it speaks |

Without a Persona, an Agent still works, but its voice is the LLM's default
voice. With a Persona, every agent that uses it sounds like the same coherent
representative of your brand. Swap the LLM, swap the tools: **the voice stays**.

---

*Viglet Turing: Enterprise Search Intelligence. This guide tracks the current platform release; the concise technical reference lives in [Personas](/docs/personas/).*

