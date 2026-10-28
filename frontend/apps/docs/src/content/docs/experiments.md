---
title: "Experiments (A/B Testing)"
description: "A/B test your chat flows natively. Split traffic across variants by weight, measure the winner with a real significance test, let a multi-armed bandit shift traffic automatically, and auto-promote the champion, all declared on the flow itself."
---
> *Is the friendlier welcome message actually converting better, or does it just feel better? "Ask for the email first" vs. "ask for it last", which one finishes more conversations? **Experiments** turn those hunches into measured decisions, built right into the chat flow.*

Viglet Turing has **native A/B testing** for [chat flows](/docs/chat-flow/). You don't bolt on a third-party experimentation tool: you mark two (or more) flows as variants of the same experiment, and the engine handles traffic splitting, sticky assignment, significance testing, and even automatic winner promotion.

This page covers the experiment subsystem end to end. The conversation-quality metrics it measures against live in [Chat Analytics](/docs/chat-analytics/).

---

## How an experiment is defined

An experiment is just a set of flows that share an **experiment key**. Each flow becomes one **variant**. The A/B fields live on the flow itself:

| Field | Meaning |
|---|---|
| `experimentKey` | Flows sharing this key are variants of the same experiment |
| `variantLabel` | This flow's arm name (e.g. `control`, `friendly`), required when `experimentKey` is set |
| `trafficWeight` | This arm's share of traffic (relative weight) |
| `experimentStartsAt` / `experimentEndsAt` | The scheduling window: outside it, the flow is excluded from fresh assignment |
| `experimentSuccessMetric` | The metric the winner is judged on (default `GOAL_ACHIEVED`) |
| `banditEnabled` | Opt into Thompson-sampling traffic allocation (see [bandit](#multi-armed-bandit-thompson-sampling)) |
| `autoPromote` | Opt into automatic champion promotion by the daily job |

### Sticky, deterministic assignment

When a new conversation starts, the engine picks a variant by hashing `conversationId + experimentKey`, weighted by each in-window arm's `trafficWeight`. Because the assignment is a deterministic hash of the conversation id, it's **sticky**: the same visitor always lands in the same arm for the life of that conversation, with no server-side session state. Flows outside their `[startsAt, endsAt]` window are simply not candidates.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '13px', 'primaryColor': '#fff', 'primaryBorderColor': '#c0c0c0', 'lineColor': '#888', 'textColor': '#333'}}}%%
graph LR
    CONV["💬 new conversation"] -->|hash(convId + key)| PICK{"⚖ weighted pick<br/>experimentKey = welcome"}
    PICK -->|weight 50| A["🅰 control"]
    PICK -->|weight 50| B["🅱 friendly"]
    A --> M["📊 scorecard<br/>by variant"]
    B --> M
```

---

## Measuring the winner: significance

Eyeballing two conversion rates isn't enough, a 52% vs. 48% split might be noise. Turing runs a real **two-proportion z-test** with a **Wald confidence interval** so you know whether a difference is statistically meaningful.

```
GET /api/system/chat-analytics/experiment/{experimentKey}/significance
      ?successMetric=GOAL_ACHIEVED   # the metric to compare arms on
      &alpha=0.05                    # significance threshold (default 0.05)
      &from=YYYY-MM-DD&to=YYYY-MM-DD # window (default last 30 days)
```

It returns each arm's sample size and success rate, the z-statistic, the p-value, the confidence interval, and a recommendation. Until the experiment has enough data, the recommendation is "underpowered: keep running"; that honesty is the point.

---

## Multi-armed bandit (Thompson sampling)

A fixed 50/50 split is "wasteful" once one arm is clearly winning: you keep sending half your traffic to the loser. Set **`banditEnabled`** on an arm and, when *any* arm of that experiment opts in, the engine stops using static `trafficWeight` and instead allocates traffic by **Thompson sampling**: it samples each arm's success probability from its observed results and routes the conversation to the sampled best. Winning arms naturally earn more traffic over time while losing arms still get the occasional probe.

The toggle is per-flow (not per-experiment-key) so a single edit on any arm flips the whole experiment into bandit mode: the symmetry keeps the data model simple.

---

## Champion-challenger auto-promotion

Significance tells you *whether* a variant won; promotion *acts* on it. The champion-challenger flow pins the winner to `trafficWeight = 100` and archives the losers (`trafficWeight = 0` + closes their assignment window).

```
POST /api/system/chat-analytics/experiment/{experimentKey}/promote
      ?successMetric=GOAL_ACHIEVED&alpha=0.05&dryRun=false
```

- It re-runs the same z-test; if there's no significant winner yet, it's a **no-op** returning `applied=false` with the reason.
- `dryRun=true` returns the promotion **plan** (which arm would win, which would be archived) without persisting anything: perfect for a "preview what would happen" button.
- An explicit operator call is itself the opt-in, so it ignores the per-flow `autoPromote` flag. That flag only governs the **daily** `TurChampionChallengerPromotionJob`, which auto-promotes hands-free when every arm has opted in.

---

## Per-node A/B

You don't always want to clone an entire flow to test one sentence. A single **node** can carry its own A/B variants: the engine picks a node variant with the same weighting machinery as flow-level assignment, so you can test one question's wording without duplicating the graph. See [Chat Flow](/docs/chat-flow/) for authoring node variants.

---

## Forcing a variant (QA & demos)

During QA or a sales demo you need to *see* a specific arm regardless of the hash. Append `?_ab_variant=<label>` to the chat URL and the engine hard-pins that arm, bypassing the deterministic hash, Thompson sampling, **and** the scheduled experiment window. A blank value or a label that doesn't match any arm falls back to normal assignment.

```
https://your-site/sn/{site}/chat?_ab_variant=friendly
```

---

## Slicing results: cohort filters

A variant can win overall but lose on mobile. The [Chat Analytics scorecard](/docs/chat-analytics/#rest-api) accepts cohort filters so you can ask *"does `friendly` win **on mobile / in pt-BR / in America/Sao_Paulo**?"*:

```
GET /api/system/chat-analytics/scorecard
      ?dimension=variantLabel&deviceType=mobile&locale=pt-BR
```

The scorecard supports four dimensions overall (**agent**, **persona**, **experiment**, and **variant**) so you can compare arms head-to-head and confirm the lift is real where it matters.

---

## Debugging an experiment

When a flow has a non-blank `experimentKey`, the engine emits one structured **variant trace** line per turn:

```
[A/B Trace] experimentKey:variantLabel:nodeId:userMessage
```

Plain (non-experiment) flows stay silent, so the trace is a clean, grep-able record of exactly which arm and node handled each turn, the first place to look when an experiment "isn't splitting" or a visitor reports the wrong wording.

---

## Related pages

- [Chat Flow](/docs/chat-flow/): the flows (and nodes) that become experiment variants
- [Chat Analytics](/docs/chat-analytics/): the scorecard, cohort filters, and the metrics arms are judged on
- [AI Agents](/docs/ai-agents/): where flows and their experiments are attached
- [Personas](/docs/personas/): another dimension you can A/B and slice the scorecard by

