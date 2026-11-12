---
title: "Manage the WordPress plugin in wp-admin"
description: "Use the Viglet Turing Search plugin's wp-admin screens: Health, the post list and editor panel, search insights and an exhausted AI budget."
---

This page is for the administrator running the Viglet Turing Search plugin day to day: the
screens it adds to wp-admin, what each post's search state means, what visitors searched
for, and what happens when the site's AI budget runs out.

## The plugin's screens

| Screen | What it is for |
|---|---|
| **Setup** | The connection and its step-by-step test, the SN site, the first index |
| **Fields** | The per-type field mapping, its state in Viglet Turing, a document preview |
| **Health** | Published versus indexed per type, the running reindex and the last one, the queue, its last run, the last 50 events |
| **Settings** | Post types, categories, tags, comments, custom fields, exclusions, batch size, search |

## Search health

![The Health page](/docs/img/screenshots/wordpress-health.png)

**Health** has **Retry failed** and **Send queue now**, and lists every send, removal,
provisioning, sweep and reindex with its error when it failed. While a reindex runs, its
card shows how many posts are done of how many, how many failed, a batch waiting to be
tried again and **Cancel reindex**, and it updates every few seconds. Cancelling keeps what
was already sent.

## When the AI budget runs out

Viglet Turing can cap what a site spends on AI. Once the cap is
reached, Viglet Turing refuses every AI call until the budget resets. While that lasts:

- The **Ask this site** block says *Answers are paused until* and the reset date, *search
  still works*, rather than that the site could not answer. It reads the date from Viglet Turing's
  refusal (with `@viglet/turing-sdk` 2026.3.17 or later).
- The **Viglet Turing search** block, the instant search's answer and the **Site assistant** show
  the same line where the answer would be. They name the date once this site has seen the
  budget run out (the first refusal is reported to WordPress, which asks Viglet Turing itself), and
  say *for now* before that. Search and its results are unaffected.
- A sentence whose filters cannot be read is searched as typed.
- **Health**'s **AI features** card shows *AI budget exhausted until* and the reset date
  (or *for now* when Viglet Turing names none), with Viglet Turing's own explanation, and **Check AI
  features** marks the features that are on as **Paused**. The moment the budget ran out and
  the moment it came back are listed among the events.

A visitor's browser sees only that Viglet Turing refused, never when the budget resets. So it tells
WordPress, and WordPress asks Viglet Turing once to read the reset date. It does that at most once
every ten minutes whatever the visitors send, so a forged report cannot make Health say the
budget is spent. The line goes away when the reset date passes, or as soon as an AI call
WordPress makes goes through again. A **Check AI features** asks Viglet Turing again first, so a
budget raised in Viglet Turing shows at once.

## The post list and the editor

The post lists gain a **Search** column: *Indexed 2 mins ago*, *Queued*, *Failed* (the
error shows on hover), *Excluded* or *Not published*. The block editor has a **Search**
panel with the same state, **Exclude from search** and **Reindex now**. A reindex Viglet Turing
refuses shows Viglet Turing's own reason under the state, so a refused document reads differently
from Viglet Turing being down.

With **Find people, organisations and places in posts** on, the panel's **Names in this post**
lists, under each facet's label, the names Viglet Turing holds for the indexed post. It only shows
them: Viglet Turing has no way to keep one name off one post, so to change them, change the text and
reindex.

The same panel's **Assist** section helps while writing, and changes nothing on its own:

- **Find related posts** lists what may already cover the post (*Already covered?*) and
  posts to link to (*Link ideas*), from Viglet Turing's similar-documents and duplicate searches.
  A draft not yet indexed is compared by its title. **Add a link** puts a paragraph with
  the link after the selected block.
- **Draft an excerpt** asks Viglet Turing's language model, from WordPress's server with the
  stored API key, for a one- or two-sentence excerpt of the saved post, in the site's
  language. **Use this excerpt** fills the excerpt field; the post changes only when you
  save it. **Draft another** asks again. Viglet Turing needs a default language model for it.

![The Search column in the post list](/docs/img/screenshots/wordpress-posts.png)

## Search insights

**Viglet Turing Search → Insights** shows what visitors searched for in the last 24 hours, 7 days,
30 days or all time, read from Viglet Turing's search metrics with the stored API key: the top
searches with their clicks and click-through, the searches that found nothing (with when
they were last made), and the ones that found results nobody opened. Each of those last
two offers **Write about this**, a new draft titled with the search, and a search that
found nothing also offers **Add a synonym**, which opens **Tuning** with it typed in.
Searches the block makes while the visitor is still typing are left out. The dashboard's
**Site search this week** widget shows the week's five top searches and five that found
nothing.

The admin ships in English and Brazilian Portuguese.

## Related pages

| Page | What it covers |
|---|---|
| [WordPress](/docs/integrations/wordpress/) | How the queue and the background reindex work |
| [Plugin reference](/docs/integrations/wordpress/reference/) | `wp turing` commands for the same operations |
| [Cost governance](/docs/cost-governance/) | Where a site's AI budget is set in Viglet Turing |
