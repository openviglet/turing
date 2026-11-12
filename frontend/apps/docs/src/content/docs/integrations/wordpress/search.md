---
title: "Search on a WordPress site"
description: "Serve a WordPress site's search from Viglet Turing: the theme's search page, the instant search, the search and Ask this site blocks, and the SDK."
---

This page is for the administrator or theme developer deciding how visitors search a
WordPress site through the Viglet Turing Search plugin: the theme's own search page, the
instant search, the plugin's blocks, and the SDK for a search of your own. Which features
need a language model is listed in [Viglet Turing on WordPress](/docs/integrations/wordpress/features/).

## The theme's search page

**Replace WordPress search with Viglet Turing** (Settings → Search) is off on a fresh install.
With it on, the results page is the **theme's own**: Viglet Turing answers WordPress's main search
query and the theme's search template renders the results, so a block theme keeps its
header, footer and styles. Only this site key's documents come back. If Viglet Turing does not
answer, WordPress searches its own database. The plugin's older results page (with
facets, for classic themes) remains available as **Results page → The plugin's results
page**.

### When Viglet Turing is down

A page a visitor is waiting on (the theme's search, the search
block's first page, Related, popular searches, the plugin's results page) waits at most
three seconds for Viglet Turing. When it gets no answer at all, it renders without it and marks
Viglet Turing down for a minute, so the pages after it do not wait: the theme's search falls back
to WordPress's own at once, and Related shows nothing. An HTTP error from Viglet Turing does not
set the mark. Admin screens, Index All and the queue keep their longer waits. The
`viglet_turing_render_timeout` filter changes the three seconds.

## The instant search

With **Instant search from anywhere** on, Ctrl+K (⌘K on a Mac) and the theme's own search
box open a search that answers as the visitor types, and Enter takes the query to the full
search page. **Instant search layout** picks how it looks:

- **A list of titles**: the first six results, each with its date and address.
- **Immersive**: up to twelve results grouped by type ("Posts", "Pages", "PDF files"), each
  with its image, date and first card field, and a **See all** per type that opens the
  results page filtered to that type. Before anything is typed it lists this browser's
  recent searches and the site's popular ones. On a desktop the highlighted result shows
  beside the list, with its image, snippet and card fields. Ctrl+Enter (⌘+Enter) opens the
  full results page whatever is highlighted. On a phone it fills the screen.

  **Explore links** adds a row of the site's own links to that empty state, below the
  recent and popular searches, and Tab reaches them. Write one `Label | URL` per line, at
  most eight, for example `Events | /events/`. The URL must be a full `http://` or
  `https://` address or a path on this site that starts with `/`. A line with any other URL,
  or with no label, is dropped when the settings are saved. The row hides once the visitor
  types.

  With **Answer questions on top of the immersive layout** on, a question that rests for a
  moment gets Viglet Turing's answer above the results. Each answer is an LLM call, so it is off
  by default, and it needs the site's answers working in Viglet Turing (Health shows whether they
  are).

A query of three words or more, or one that reads as a question, often finds nothing as
typed, because Viglet Turing requires every word. The instant search then searches again for any
of its words and puts the best matches first, in both layouts, as the search block does.

## The Viglet Turing search block

![The Viglet Turing search block](/docs/img/screenshots/wordpress-search-block.png)

The **Viglet Turing search** block is a complete search UI for any page. It has a box with
suggestions, "did you mean", the mapped facets, results and pages. It is built with the
Interactivity API on Viglet Turing's JS SDK and takes its colours and type from the theme. It
reads `?q=` (or `?s=`), so a page holding it can serve as the site's search page.

**Always filter by** scopes a block to one section of the site: a box on the events page
set to `type:event` searches only events, and one set to a mapped facet's value
(`publisher:sociology`) only what carries it. Each filter is `field:value`. Every search
the block makes stays inside them, the first page drawn on the server included, and so do
the terms it suggests as the visitor types: a box set to `type:event` never offers a word
only posts hold. Scoped suggestions need a Viglet Turing that answers a filtered autocomplete with
terms (2026.3.9 or later); an earlier one answers it with document titles. Visitors
never see them as a filter they can remove. Under the box a line names the scope ("In
Events") with a **Search the whole site** link to the site's search page, keeping the query.
A scoped block with only a search box takes its scope to the search page, where the same
line shows. A scoped block shows no popular searches, since they may find nothing inside it.

With **Suggest the filters a sentence names** on, a search that reads as a sentence ("posts
about php from last year") also asks Viglet Turing's natural-language filters which filters it
names. The ones Viglet Turing checked against the site, on a mapped facet or on the post type or
publication date, show as chips under the box; a click applies one and takes the words it
was read from out of the query. Nothing is applied without a click. Each sentence is an LLM
call, and with no language model in Viglet Turing nothing is offered.

With **Apply them instead of suggesting** on as well, the filters are applied straight away
and a line under the box says what was read: "Understood: Color: Red, Price: ≤ 200", with
**Undo** to put the search back as it was typed. On a WooCommerce shop a price the sentence
names ("under $400", "até R$ 400", "entre 100 e 300 reais", "over $50") is read by the block
itself and filters the shop's price field, so it works even with no language model in
Viglet Turing. Only an amount marked as money counts: "under 2 kg" is left alone. A size works
the same way ("size M", "tamanho 42") on a facet whose field or name says size, and only
when that value is one the site's products have: "size XXXL" sets nothing.

A period is read the same way, on a date field that has a period facet (an agenda's first):
"em novembro", "in March 2027", "next week", "semana que vem", "amanhã", "this month". A
month with no year is the next one to come on an agenda and the last one gone on past dates.
"May" and "March" need "in", "em" or a year beside them, since both are also verbs. So
"debates em novembro" on an agenda filters the event dates to November with no language
model, and searches "debates".

A site with its own search blocks turns the block off:

```php
add_filter( 'viglet_turing_register_blocks', '__return_false' );
```

## The **Ask this site** block

The **Ask this site** block is a conversation with Viglet Turing's copilot. It speaks as the site
does: on a WooCommerce shop it is headed **Ask the catalog** and offers a kit of products;
anywhere else it is headed **Ask this site**, names no catalog, and offers a plan only when
switched on. A visitor
asks in plain words, and the answer comes from this site's content and cites the items it
used. On a WooCommerce shop each cited product shows the shop's price and an **Add to cart**
button; the copilot never puts anything in the cart by itself. The block appears only when
Viglet Turing has a language model for the copilot.

On a product's page (or any indexed post's), the box reads **Ask about** and the product's
name, and the question goes to Viglet Turing with that product's document: Viglet Turing answers about it
first and always cites it, with other products only as alternatives. The Site assistant does
the same: a question asked in it on a product's page is about that product, and one asked
after moving to another page is about that page. This needs Viglet Turing 2026.3.8 or later, which takes the document; an earlier one
answers from the whole catalog.

**From the search answer to the assistant and back.** When the page also has the Site
assistant (its block, or its launcher on every page), the Viglet Turing search block's answer ends
with **Ask a follow-up in the assistant**. It opens the assistant with the search and its
answer, sources included, as the conversation's first exchange, so "and on Saturdays?" is
read as a follow-up instead of starting over. The other way, an assistant answer that cites
more than three posts ends with **See every result for this question**, which opens the
site's results page for that question, filtered to the posts' type when they all share one
(all products, say). The type comes from Viglet Turing with each source (2026.3.9 or later, for a site that
indexes a `type` field); without it the page opens unfiltered.

**Guided conversations.** A [chat flow](/docs/chat-flow/) built for the SN site's chat runs
in the Site assistant as it does in Viglet Turing: the choices a step offers show as buttons (typing
an answer still works), and a form step shows its fields as a labelled form, sent only when
the visitor submits it; the browser checks required fields and email, phone, number and date
fields first. **New conversation** also clears the flow's place on Viglet Turing, so the next
question starts it from the beginning.

**Build my kit.** With **Offer Build my kit** on (the default, in the block's settings), the
block has a second mode, **Plan a kit**. The visitor describes a plan ("two days crossing the
mountains in July, first time") and Viglet Turing answers with the needs the plan implies, each
with the products it found for it and a one-line reason drawn from the product's own
attributes:

- Prices come from WooCommerce, and so does stock: a product the shop cannot sell is marked
  **Out of stock** and left out of the cart, whatever the index says.
- A need the catalog has nothing for is named ("Nothing in the catalog yet for: shelter")
  rather than filled with something unrelated.
- Journal posts that cover the plan are listed as **Guides for this plan**.
- A need's products are alternatives: the first one the shop can sell starts ticked, and the
  visitor can tick another or untick it. **Add N products to the cart** puts the ticked ones
  in the cart at once, and the confirmation shows the cart's total as WooCommerce computes it.

The kit needs a Viglet Turing that has the kit call (`POST /api/sn/{site}/copilot/kit`), which
2026.3.6 does not, and the **Stock** field mapped, which a WooCommerce site has by default.

**A plan on a site that is not a shop.** Turn on **Offer a plan** in the block's settings
and say, under **What the plan uses**, what it works with:

| Setting | What it takes | Blank means |
|---|---|---|
| Types it proposes | Indexed types, comma-separated, e.g. `course, event` | Products on a shop; any type elsewhere |
| Types it cites as guides | Indexed types, e.g. `page` | Posts on a shop; none elsewhere |
| Availability field | An indexed field saying whether an item can be taken, e.g. `seats_left` | `stock_status` on a shop; none elsewhere |

The mode is then **Make a plan** and its button **Build my plan**. A school that stages
`event` gets the events a plan needs, grouped by need, as links; there is no cart, and an
item Viglet Turing marks unavailable reads **Unavailable**.

## Building your own search on the SDK

The plugin registers [`@viglet/turing-sdk`](https://www.npmjs.com/package/@viglet/turing-sdk)
as WordPress script modules, so a theme imports it instead of bundling a copy:

- `@viglet/turing-sdk`: the SDK as published;
- `viglet-turing/search`: the SDK already configured for this site, with searches filtered
  to its site key.

```js
// A theme module registered with 'viglet-turing/search' as a dependency.
import { createSiteSearch, createSiteAutoComplete, config } from 'viglet-turing/search';

const search = createSiteSearch();      // search-only; { controller: { chat: true } } adds answers
search.subscribe( ( state ) => render( state ) );
await search.searchQuery( 'workshop' );
```

The configuration it reads is public and holds no key: Viglet Turing's public URL, the SN site,
the locale, the site key and the mapped facets. PHP reads it with
`viglet_turing_public_config()`, and anything else with
`GET /wp-json/viglet-turing/v1/public-config`.

:::caution[CORS]
Browser search calls Viglet Turing directly, so Viglet Turing must allow the WordPress site's origin:
add it to Viglet Turing's allowed origins (`TURING_ALLOWEDORIGINS` in a container). When browsers
reach Viglet Turing at another address than the server does, set `VIGLET_TURING_PUBLIC_URL`.
:::

## Related pages

| Page | What it covers |
|---|---|
| [What the plugin indexes](/docs/integrations/wordpress/indexing/) | The facets and card fields these blocks show |
| [Viglet Turing on WordPress](/docs/integrations/wordpress/features/) | Each feature and the block that shows it |
| [JavaScript SDK](/docs/javascript-sdk/) | The SDK the blocks are built on |
| [Chat flow](/docs/chat-flow/) | Guided conversations in the Site assistant |
