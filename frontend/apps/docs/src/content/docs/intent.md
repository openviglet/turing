---
title: "Intent"
description: "Configure intent categories and action prompts for the Viglet Turing chat interface."
---
> *The hardest part of any AI product isn't the AI: it's getting the user to ask the first question. **Intents** are how you put a hundred good first questions, in your customer's voice, on the chat home screen. And **Vibe Coding** is how you write all of them in five minutes.*

The **Intent** feature (`/admin/intent/instance`) manages categorized prompt suggestions that appear in the Chat interface. Intents provide users with pre-built conversation starters organized by topic, making it easier to discover and use the AI capabilities of Viglet Turing.

Each intent represents a **category** (e.g., *"Boost my work productivity"*) containing one or more **actions**: each action is a labeled prompt that is sent directly to the chat when clicked.

Think of intents as the *hook* on the home screen of every conversation. The customer didn't type a question yet: they're staring at the prompt box wondering what to ask. Intents say *"here's what people like you ask"*, in their language. Conversion starts here.

---

## Intent Listing

The listing page shows all configured intents as a grid of cards. Each card displays:

- **Title**: the intent category name
- **Description**: a brief explanation of the category
- **Icon**: a Lucide icon representing the category

A **New Intent** button at the top opens the creation form. Clicking an existing card navigates to its detail page.

---

## Creating or Editing an Intent

The intent form is divided into three sections.

### General Information

| Field | Max Length | Description |
|---|---|---|
| Title | 150 chars | Category name displayed in the chat interface (required) |
| Description | 500 chars | Brief explanation of what prompts this intent provides |
| Icon | n/a | Lucide icon picker: select an icon to represent this category |

### Actions

Actions are the individual prompts within an intent category. Each action has:

| Field | Description |
|---|---|
| Label | Short action label shown to the user (e.g., "Create daily schedule"), required |
| Prompt | The full prompt text sent to the chat when the user clicks this action, required |

Actions support **drag-and-drop reordering**: drag the handle on each action card to change the display order. Use the **Add Action** button to create new actions, and the trash icon to remove existing ones.

:::tip[Write actions in the customer's voice]
The label is what the user clicks. The prompt is what the LLM receives. They don't have to be the same. The label should sound like *the user asking themselves a question* (*"Help me prepare for my next 1-on-1"*); the prompt can be a more elaborate brief for the LLM (*"Help me prepare for a 1-on-1 meeting with my manager. Suggest topics, questions to ask, and a way to bring up career growth."*). The pairing is what makes intents feel like a friendly assistant rather than a search box.
:::

### Status

| Field | Description |
|---|---|
| Enabled | Toggle to show or hide this intent in the chat interface. Disabled intents are not visible to users. |

---

## Vibe Coding: Author Intents With AI

Writing 4 categories × 6 actions × 2 languages = 48 prompts is exactly the kind of work the AI is best at. The Intent form has a **Vibe Coding** mode: you describe what you want; the assistant drafts the entire intent.

> **You:** *"Make me an intent for new product owners. Six actions covering the first 30 days on the job: backlog grooming, stakeholder mapping, OKRs, the first sprint demo, a retro plan, and a simple metric to track."*
>
> **Assistant:** *"Done. Title: 'New Product Owner: First 30 Days'. Icon: graduation-cap. Six actions ready:*
> *`Map your stakeholders` → 'Help me build a stakeholder map for my first month as PO. Roles, influence, what they need from me, and one question I should ask each of them.'*
> *`Groom the backlog` → 'Walk me through grooming a backlog I just inherited. What's the right order to clean it?'*
> *`Set OKRs for the first quarter` → ...*
> *Want me to also write a Portuguese version?"*

The same pattern as [Chat Flow](/docs/chat-flow/) and [Custom Tools](/docs/custom-tools/). Two interaction modes:

- **First-turn creation**: describe the intent, get the full structure (title, description, icon, all actions).
- **Revision turns**: *"add an action about pricing"*, *"make the third one shorter"*, *"translate everything to Portuguese"*.

Vibe Coding is especially powerful here because intents are usually authored *en masse*: you're populating the home screen, not writing a one-off. Three minutes of prompting beats two hours of typing.

---

## Pre-loaded Intents

Turing ships with four default intents:

| Intent | Icon | Actions |
|---|---|---|
| **Boost my work productivity** | Briefcase | Create daily schedule, Draft a difficult email, Prepare meeting agenda |
| **Learn something new** | Graduation Cap | Explain complex topics, 30-day study plan, Test my knowledge |
| **Create or improve content** | Pen Tool | Social Media Ideas, Proofread & Enhance, Video Scripting |
| **Health & Mindset** | Heart Pulse | Quick Meditation Guide, Rapid Meal Ideas, At-home Workout |

These can be edited, disabled, or deleted. New intents can be added at any time.

---

## REST API

All endpoints require authentication via the `Key` header.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/intent` | List all intents (sorted by title) |
| `GET` | `/api/intent/enabled` | List enabled intents only (sorted by display order) |
| `GET` | `/api/intent/{id}` | Get a specific intent with its actions |
| `POST` | `/api/intent` | Create a new intent |
| `PUT` | `/api/intent/{id}` | Update an intent |
| `DELETE` | `/api/intent/{id}` | Delete an intent and all its actions |

### Request / Response Body

```json
{
  "id": "intent-productivity",
  "title": "Boost my work productivity",
  "description": "Prompts to help you be more productive at work",
  "icon": "lucide:briefcase",
  "enabled": 1,
  "sortOrder": 0,
  "actions": [
    {
      "id": "action-1",
      "label": "Create daily schedule",
      "prompt": "Help me create an efficient daily schedule...",
      "sortOrder": 0
    },
    {
      "id": "action-2",
      "label": "Draft a difficult email",
      "prompt": "Help me draft a professional email...",
      "sortOrder": 1
    }
  ]
}
```

**Example: list enabled intents:**

```bash
curl "http://localhost:2700/api/intent/enabled" \
  -H "Key: <YOUR_API_TOKEN>"
```

**Example: create an intent:**

```bash
curl -X POST "http://localhost:2700/api/intent" \
  -H "Key: <YOUR_API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Data Analysis",
    "description": "Prompts for analyzing and visualizing data",
    "icon": "lucide:bar-chart-3",
    "enabled": 1,
    "sortOrder": 4,
    "actions": [
      {
        "label": "Summarize a dataset",
        "prompt": "Analyze the following dataset and provide key statistics, trends, and insights.",
        "sortOrder": 0
      }
    ]
  }'
```

---

## Permissions

Intent management uses role-based access control:

| Permission | Description |
|---|---|
| `INTENT_VIEW` | View intent categories |
| `INTENT_CREATE` | Create new intents |
| `INTENT_EDIT` | Edit existing intents |
| `INTENT_DELETE` | Delete intents |

All permissions also require the `ROLE_ADMIN` role.

---

## Related Pages

| Page | Description |
|---|---|
| [Chat](/docs/chat/) | The chat interface where intents are displayed to users |
| [AI Agents](/docs/ai-agents/) | Configure AI Agents that respond to chat prompts |
| [Administration Guide](/docs/administration-guide/) | User, group, and role management |

---

