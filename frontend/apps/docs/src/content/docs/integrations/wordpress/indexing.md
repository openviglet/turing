---
title: "What the WordPress plugin indexes"
description: "Choose which WordPress fields become Viglet Turing facets, what stays out of search, how PDFs are found, and how several sites share one SN site."
---

This page is for the administrator deciding what the Viglet Turing Search plugin sends to
Viglet Turing: which fields become facets, what stays out of search, how PDF files are
found, and how several WordPress sites share one SN site. To install and connect the plugin
first, see [WordPress](/docs/integrations/wordpress/).

## The document each post sends

Every post sends a fixed document: `id`, `title`, `text` (the rendered body without
markup, shortcodes or hidden UI text), `abstract`, `url`, `type`, `author`,
`publication_date`, `modification_date`, `site`, `image`, `site_key`, `source_apps`, plus
`categories` / `section` and `tags` when those options are on. Approved comments and named
custom fields can be added on the Settings page.

## Fields and facets

![The Fields page](/docs/img/screenshots/wordpress-fields.png)

**Viglet Turing Search → Fields** maps, per post type, what else becomes its own Viglet Turing field:

- **Source**: a taxonomy, a meta key the posts actually carry, a post field (excerpt,
  slug, menu order, parent) or the featured image URL.
- **Type**: `STRING`, `TEXT`, `INT`, `LONG`, `FLOAT`, `DOUBLE`, `DATE` or `BOOL`. A value
  that does not convert is left out, never guessed. Dates are read as `Ymd` (ACF), a Unix
  timestamp, or any date `strtotime` reads.
- **Multiple**, **Facet** with its label, and a **Weight** (1 to 100) for ranking.
- **Ranges**, on a `DATE` or number field: a range facet as well, provisioned as a Viglet Turing
  custom facet named `<field>_range`. A date picks its **Periods**: **Past**, for
  publication dates (upcoming, last 7 days, last 30 days, this year, before this year);
  **Agenda**, for dates still to come (today, next 7 days, this month, upcoming, past); or
  **My own**, up to eight, each bound in Viglet Turing's date math (`NOW/DAY`, `NOW/DAY+7DAYS`,
  `2026-12-01`). Viglet Turing's date math has no weeks, so an agenda offers the next 7 days rather
  than the calendar week. Viglet Turing includes a range's end, and a day-only date is stored at
  midnight UTC, so each agenda period ends a second before the next one starts: an event
  today is never under **Past**, and one tomorrow is never under **Today**. A period of your
  own that should end where another starts needs the same `-1SECOND`. A period buckets the one field, so an event is placed by its start
  date even when it runs for weeks. A number lists its own ranges, each with a label
  and a **From** and **To**; an empty bound is an open end, and an empty label is written
  from the bounds ("20 – 50", "Up to 20", "50 or more").
- **Show on result cards** adds the field to each result in the **Viglet Turing search** block,
  as a "Label: value" line under the snippet, in the order of the fields. The label is the
  field's label, or its name when it has none. A `DATE` field is written in the site's date
  format, a multiple field's values are joined with commas, and a URL is never shown as
  text. An event, say, can show the day it happens beside the day it was posted.
- **Date shown on cards** and **Image on cards**, per post type: the card's date can be one
  of the type's `DATE` fields instead of the publication date, and its image one of its
  text fields (an image URL) instead of the featured image. An event's card then shows the
  day it happens. A date with no time of day is shown as that day in every time zone. The
  instant search shows the same date, and the first card field, under each result.
- **Leave this type out of search** switches a whole post type off.
- **Types from other sources** lists the types of documents another source sent under the
  site key, such as a catalog's `Article` or `Thesis`, which no post type has. Each one
  gets a **Label** (its badge on result cards), a **Date shown on cards** from the date
  fields its documents carry (`defense_date`, say) and an **Image on cards** from their
  text fields. This changes how they look, never what is indexed. A record with several
  types, `["Article", "Thesis"]`, is shown as its first. The section appears once Viglet Turing
  holds such a document. The screen asks Viglet Turing for the other sources' documents only,
  leaving the plugin's own posts out with a negative filter, so however many posts the site
  has, the first 2,000 records from other sources are what it reads. This needs a Viglet Turing that
  honours a negative filter (2026.3.9 or later); an earlier one also returns the posts,
  which the screen skips.

**In Viglet Turing** says where each field stands: *In Viglet Turing*, *Added on the next save*, or
*Conflict* when Viglet Turing already holds the field with another type. **Preview document**
shows the JSON the newest post of the type sends. **Save and send to Viglet Turing** stores the
mapping and provisions the fields through Viglet Turing's manifest API, so facets need no console
step. Run a reindex afterwards so existing posts carry the new fields.

With nothing configured, every public custom taxonomy is mapped as a facet.

**People, organisations and places.** Names live in a post's text, not in its fields, so no
mapping can reach them. Turn on **Settings → Search → Generative AI → Find people,
organisations and places in posts** and Viglet Turing's language model reads each post as it is
indexed and lists the people, organisations and places it names. Viglet Turing keeps a name only
when the text holds it, and writes it to one of three facet fields: `entity_person`,
`entity_organization` and `entity_place`. The section of that name on the **Fields** screen
gives each one its facet label (*People*, *Organisations*, *Places* by default) and an
**Offered** box. The **Viglet Turing search** block then offers the ones you keep as facets beside
the others, under those labels, and its filter chips use them too.

- It needs **Generative AI** on as well: the names are found by the language model of the
  site's AI agent in Viglet Turing (its Default AI Agent, unless one was chosen in Viglet Turing's console).
  With generative AI off, nothing is asked and the facets are not offered. The Fields section
  says which setting is missing.
- Each post indexed is a call to the language model, counted in the site's AI spend in Viglet Turing.
  Viglet Turing keeps what it found in memory by text, so a post re-sent unchanged is not read again
  while Viglet Turing still holds it.
- The setting goes to Viglet Turing with the fields: on the next indexing run, or when the Fields
  screen is saved. Posts indexed before get their names once they are reindexed. Turning it
  off here tells Viglet Turing to stop, as long as generative AI is still on.
- If the model fails, the post is indexed without names.

**Name variants** (same Settings card) folds the ways a name is written into one facet value.
Write one name per line, the name to show first and then its variants after a `|`, separated
by `;`:

```text
Marta Vilar | Vilar, Marta; M. Vilar
Fundação Aurora | Aurora
```

Saving sends them to Viglet Turing's Thesaurus, in a thesaurus of the plugin's own named after the site
key, selected for the SN site. A post that writes *Vilar, Marta* is then listed under *Marta
Vilar*. Settings shows how many names were saved, or why Viglet Turing refused them. An API key bound to
a partition of a shared SN site cannot write the Thesaurus, so on such a key add the names in
Viglet Turing's Thesaurus instead. Posts already indexed follow once reindexed.

**WooCommerce.** With WooCommerce active and `product` among the indexed types, a product
offers its shop fields as sources: **Product: Price**, **Regular price**, **Sale price**,
**Stock status**, **SKU** and **Average rating**. With nothing configured a product sends
its price, its stock as a **Stock** facet (in the shop's words, "In stock"), its product
categories and tags, and each global attribute (`pa_color`, ...) as a facet. Turn on
**Ranges** for the price to filter by "Up to 50", "50 – 100" and so on. A product whose
stock changes from an order is sent again, although no post was saved, and so is a variable
product when one of its variations changes.

In the **Viglet Turing search** block a product's result shows its price in the shop's currency
(the regular price struck through when it is on sale) and an **Add to cart** button. It
adds one through WooCommerce's Store API without leaving the page, and says whether it
worked, with WooCommerce's reason when it did not (out of stock, say). A variable product
(one sold in colours or sizes) is indexed once, with its variations' values as facet values,
and **Add to cart** here, in the catalog copilot and in a kit puts in its first variation the
shop can sell.

The same mapping is readable and writable as JSON at `viglet-turing/v1/mapping`. A theme
can map in code with the `viglet_turing_mapping` filter, and change a document just before
it is sent with `viglet_turing_document`. `viglet_turing_result_card` changes a result card
the [search block](/docs/integrations/wordpress/search/#the-viglet-turing-search-block) draws on the server, before its script runs (`$card`, `$document`). The
script redraws a card from the mapping alone, so use the filter only for what Fields can't
set, or the card changes once the page loads.

## Excluding content

A post is left out when its type is off, when its ID is in **Exclude Posts/Pages**
(Settings), or when **Exclude from search** is on in its editor panel
(`_viglet_turing_exclude` meta).

A post can also leave search on a date: in **Fields**, **Leaves search on** picks one of
the type's `DATE` fields (a job opening's last day to apply, say). Once that date has passed
the post is taken out of search, at that moment, with no one saving it. A date with no time
of day lasts to the end of that day. Move the date forward and save, and the post is back.
The editor panel says when a post leaves search, and **Out of search: the date has passed**
afterwards. `wp turing sweep` applies the same rule.

For a rule no field can express, the `viglet_turing_should_index` filter (`$index`,
`$post`) has the last word on whether a post is in search.

## PDF files

With **PDF files attached to them** on (Settings → Content), each PDF in the media library
that a post in search attaches or links is a document of its own, found by the words
inside it. A link counts when the post's content points at the PDF's address in this
site's uploads, so a form uploaded once and linked from several pages is found even though
no page attached it. The plugin sends the file with the document, and Viglet Turing reads its
text. A PDF shows as **PDF** in the type badge and facet, with its post as its section. It
is sent when uploaded or edited, when a post in search first attaches or links it, and on
**Index All**. It is removed when the attachment is deleted, and when no post in search
holds it any more: the last post that attaches or links it is unpublished, excluded,
deleted or past its **Leaves search on** date, or stops linking it. A PDF another post in
search still links stays. Only this site's own uploads are sent, up to 20 MB each (the
`viglet_turing_pdf_max_bytes` filter changes the cap); a link to a file on another host is
never followed.

A post can also carry a PDF's words in its own document. In **Fields**, the source **Text
of the PDF in meta: `<key>`** reads the PDF a meta names, by this site's upload address or
by attachment ID, into a `TEXT` field of the post. A search for a sentence in the paper
then finds the post itself. This works whether or not **PDF files attached to them** is on,
and a file on another host is never fetched.

---

## Several sites, one SN site

Document ids are `<site key>:post:<post ID>`, and every document carries a `site_key`
field, provisioned as a facet. Several WordPress sites (or the blogs of a multisite
network) can therefore publish into one SN site for a unified search, without replacing
each other's documents. The key defaults to the site's host and path
(`example.org-projetos`); set it in Setup or with `VIGLET_TURING_SITE_KEY`.

**Delete All** and `wp turing sweep` remove only this key's documents, and only the ones the
plugin sent. A Viglet Dumont connector given the same site key, such as a catalog of theses that
should come up in the site's search, keeps its documents through Index All, the sweep and
Delete All. **Search health** says how many documents under the key came from another source.
Their types get a label, a card date and a card image under **Fields → Types from other
sources**.

A post and an archive record use the same field for the author, the date and the snippet:
Viglet Turing's own `author`, `publication_date` and `abstract`, which the
[catalog connector](https://dumont.viglet.org/docs/connectors/catalog/) sends for OAI-PMH and DSpace records
too. So a thesis and a post by the same person share one author facet value, and both show
a date and a snippet on their cards. The plugin declares `author` with several values, for a
thesis's co-authors. A site provisioned by an older version, where `author` held one value,
is converted the next time the fields are sent to Viglet Turing (Viglet Turing recreates the field), so
run **Index All** afterwards.

### A key per site

A plain API key reaches every document in the SN site, so one site's key could read or
delete another's. Instead, give each site its own key bound to its site key. In the Viglet Turing
console, under **Administration → API Tokens**, set **Partition of a shared site** to the
field `site_key` and this site's key as the value. Then paste the token into this site's
Setup. Viglet Turing then keeps the key inside the site's own documents. Some things are shared by
every site of the SN site, so this key cannot change them:

| What | With a key bound to the site |
|---|---|
| Fields | Saving fields that would change the SN site's schema is refused, and the Fields screen and Health show Viglet Turing's reason. Add or change fields with a key that has no partition |
| Boosts | Off. The Search tuning screen says why |
| Synonyms | Listed, but adding or removing one is refused, because every site shares the synonyms |
| Promoted results | Each site sees, adds and removes only its own, and they show only in its own searches |

Health's AI features check works with either key. On a multisite network, don't set
`VIGLET_TURING_TURING_API_KEY`: a constant applies to every blog and overrides each blog's
own key.

### Searching other sites too

Each site searches only its own key by default. To have a site also find what other sites
publish into the same SN site, list them in **Settings → Connection → Also search these
sites**, one per line: the other site's key, then optionally a label and its address,
separated by `|`:

```text
second-site | Second site | https://second.example.org/
library | The library
```

`VIGLET_TURING_SEARCH_SITE_KEYS` sets the same list from outside WordPress. Keys are cleaned up
the way the **Site key** is. A blank key, a key listed twice, or this site's own key is left out.
A label defaults to the key, and an address other than `http` or `https` is dropped.

With sites listed, every search here reads this site's key and theirs: the **Viglet Turing search**
block, the theme's search page, the plugin's results page, the instant search, the editor's
preview and the check behind the popular-search chips. A result from another site:

- links to that site's own address, never to a page here;
- carries its site's label: as a badge on the block's cards and the results page, and in the
  instant search's line under the title. On the theme's search page it uses the theme's
  markup, so it shows as the theme shows any post, without a badge;
- adds a **Site** facet to the block and the results page, one value per site by its label,
  so a visitor can narrow to one site and back.

The generative features (**Ask the catalog**, **Build my kit**, the **Site assistant** and
the filters read from a sentence) send every listed key, so they cite those sites' documents
too. The assistant may open a page on a listed site only when its address is given.

The **Related content** block stays on this site's posts unless its **Include the sites this
one also searches** setting is on. With it on, the other sites' documents appear with their
title, their site's label and a link to their own site, asked of Viglet Turing inside the same
scope the search uses, so the nearest posts across those sites come first. This needs a
Viglet Turing whose similar-documents search reads the site keys as alternatives (2026.3.9 or
later). The automatic placement after content keeps the default.

The list is one-way: a site you list does not find this one's documents unless it lists this
site too. Writing never reaches another site's documents. Index All, the sweep, **Delete All**,
the counts on **Search health**, **Search tuning** and **Already covered?** in the editor all
work on this site's key alone.

A [key bound to the site](#a-key-per-site) reads only its own site's documents, so searches
across sites are sent without the key, the way a visitor's browser sends them. Search in Viglet Turing
is public, so this exposes nothing new.

:::note[Two values in one facet]
Viglet Turing combines two values of one field as alternatives only when the request asks for that,
and the request can ask it only for every field at once (`fqi.op=OR`). So while other sites are
listed, two values a visitor picks in the same facet also become alternatives (either one)
rather than both. Filters on different facets still all apply.
:::

### A site whose search lives on another site

A site that indexes into the shared SN site but has no results page of its own can send
its searches to another site's page. In **Settings → Search**, set **Results page on
another site** to that page's address (or set `VIGLET_TURING_SEARCH_URL`). Then Enter in
the instant search, a search block with no results part and the theme's search box all
open that page with the query, as `?q=`, or as `?s=` when the address you set ends in
`?s=`. This site's own search results page is left to WordPress, because it is not
where results are shown. The address must be `https`, and it can't be this site's own
address.

:::tip[Documents with `wp-<ID>` ids]
The plugin's first releases used `wp-<ID>` ids. On a site indexed by one of them, run
**Index All** (or `wp turing reindex`) once: it indexes every post under its
`<site key>:post:<ID>` id and removes the copy under the old one.
:::

## Related pages

| Page | What it covers |
|---|---|
| [WordPress](/docs/integrations/wordpress/) | Install and connect the plugin |
| [Search on your site](/docs/integrations/wordpress/search/) | How the mapped facets and card fields appear to visitors |
| [Plugin reference](/docs/integrations/wordpress/reference/) | `wp turing` commands, REST routes and filters |
| [Field manifest](/docs/manifest/) | The Viglet Turing API that provisions the mapped fields |
