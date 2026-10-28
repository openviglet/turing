=== Viglet Dumont for WordPress ===
Contributors: viglet
Tags: search, turing, indexing, semantic navigation
Requires at least: 6.6
Tested up to: 7.1
Requires PHP: 8.1
Stable tag: 2.0.0
License: MIT
License URI: https://opensource.org/licenses/MIT

Indexes your posts and pages into Viglet Turing and answers the site search from it.

== Description ==

Viglet Dumont for WordPress sends your published content to a Viglet Turing Semantic
Navigation site and replaces the WordPress search page with Turing's results.

* Publishing, updating, unpublishing, trashing or deleting a post updates the index. The save
  only queues the post; WP-Cron sends the queue in batches, retries with backoff when Turing is
  unreachable, and `wp dumont queue run` drains it on hosts that disable WP-Cron.
* A bulk action in the settings page indexes everything already published, batch by batch.
* Posts, pages and the custom post types you choose are indexed, with categories, tags,
  approved comments and the custom fields you name.
* Custom taxonomies and meta become typed fields and facets, provisioned in Turing for you.
* The search page shows Turing's results with facets, spelling suggestions and pagination.
* Blocks: Turing search, Related content, Ask the catalog, Compare products and Site assistant.
  The generative ones (answers, the catalog copilot, the Site assistant) stay hidden while the
  Turing server has no language model for them.

The plugin needs a Viglet Turing server, which you run yourself (it is open source) or use as
a hosted service. It does nothing until you give it that server's address.

== Installation ==

1. In wp-admin, go to Plugins > Add New, search for "Viglet Dumont", then Install Now and
   Activate.
2. Open Viglet Dumont > Setup and give it the Turing URL, the API key and the SN site.
3. Test the connection, then run the first index.

A zip from the GitHub releases (Plugins > Add New > Upload Plugin) and Composer are described
in the plugin's README.

== External services ==

This plugin connects to a Viglet Turing server: the one whose address you enter under
Viglet Dumont > Setup, which you run yourself or contract from a host. It is needed to index
the site and to answer its search. Nothing is sent before that address is set, and nothing is
sent to the plugin's authors.

* From the WordPress server, when a post is published, updated, unpublished or deleted, and
  when you run an index: the post's title, content, excerpt, URL, dates, author name, terms,
  the custom fields you map, and approved comments if you turn them on. Also the field
  definitions (on Fields > Save) and the API key, in a request header.
* From the visitor's browser, when they search or use a Turing block: the query, the filters,
  the page of results and the site's locale; with the Site assistant or Ask the catalog, the
  messages they type; with "Report result clicks to Turing" on, the result they opened and its
  rank. The Site assistant keeps a TUR_SESSION cookie so its conversation continues.
* With "Send search events to Google Analytics" on, search events go to the Google Analytics
  tag the site already loads, not to a new one.

The terms of use and privacy notice that apply are those of whoever runs your Turing server. For
a server you run yourself, that is you. Turing's source and documentation:
https://github.com/openviglet/turing and https://docs.viglet.org/turing

== Source code ==

The admin screens are shipped built. Their source, the build scripts and the tests are in the
public repository: https://github.com/openviglet/dumont/tree/2026.3/wordpress

== Changelog ==

= Unreleased =
* Saving a post no longer calls Turing: it queues one job per post, the last save winning.
* WP-Cron sends the queue in batches with retry and backoff; a job that fails 8 times waits
  for `wp dumont queue run --retry-failed`. `wp dumont queue status` lists what waits.
* Each post records when it was last indexed, its last error and its failed attempts.
* A per-post-type field mapping sends taxonomies, meta keys and core fields as typed Turing
  fields (STRING, TEXT, INT, LONG, FLOAT, DOUBLE, DATE, BOOL), single or multi-valued, as
  facets with a label and a search weight. With nothing configured, every public custom
  taxonomy is a facet. The mapping is read and stored as JSON at `viglet-dumont/v1/mapping`.
* The mapped fields are provisioned through Turing's manifest before the documents that
  carry them, so a facet needs no console step. `viglet-dumont/v1/provision` re-sends it.
* `viglet_dumont_mapping` and `viglet_dumont_document` filters let a theme map in code.
* A post type can be switched off in the mapping, and a post with `_viglet_dumont_exclude`
  set stays out of the index.
* Hidden UI text (screen-reader labels, `hidden` and `aria-hidden` elements, buttons, forms,
  inline SVG) is no longer indexed as body text.
* Document ids are `<site key>:post:<ID>` and every document carries a `site_key` field, so
  several WordPress sites can share one SN site. The key defaults to the site's host and path
  and can be set in the settings. Run Index All once after upgrading: it also removes the
  copy indexed under the old `wp-<ID>` id.
* Delete All removes only this site key's documents. It used to send `id: *`, which Turing
  reads as a document id and which removed nothing.
* WP-CLI: `wp dumont status` compares published and indexed documents per type,
  `wp dumont index [--type] [--ids] [--since]` indexes synchronously, `wp dumont reindex`
  provisions the fields, indexes everything and sweeps, and `wp dumont sweep [--dry-run]`
  removes what Turing holds for this site key that WordPress no longer publishes. The same
  operations answer at `viglet-dumont/v1/status`, `/index` and `/sweep`.
* A Search column in the post lists says where each post stands: indexed and when, queued,
  failed (with the error), excluded or not published. The block editor has a Search panel
  with the same state, "Exclude from search" and "Reindex now".
* A Health page (Viglet Dumont > Health) shows the connection, published versus indexed
  documents per type, the queue and its last run, with "Retry failed" and "Send queue now".
* A Setup page (Viglet Dumont > Setup) takes the Turing URL, key, SN site, locale and site
  key, then tests the connection step by step and names what failed: a malformed address,
  DNS, TLS, nothing listening, a server that is not Turing, a refused key or a missing SN
  site, which it offers to create. Then it runs the first index with live progress.
* A Fields page (Viglet Dumont > Fields) edits the field mapping per post type, from the
  taxonomies and custom fields the posts actually carry. It shows whether Turing has each
  field, is about to add it, or holds it with another type, previews the document a real
  post produces, and saving sends the fields to Turing.
* The Health page lists the last 50 events: posts sent or removed, fields sent, sweeps,
  each with its error when it failed.
* Ships in Brazilian Portuguese (pt_BR), admin screens included.
* The connection can come from wp-config.php constants or environment variables:
  VIGLET_DUMONT_TURING_URL, VIGLET_DUMONT_TURING_SITE, VIGLET_DUMONT_TURING_API_KEY,
  VIGLET_DUMONT_TURING_LOCALE, VIGLET_DUMONT_SITE_KEY and VIGLET_DUMONT_PUBLIC_URL. A value
  set that way is read-only in the admin and never stored in the database.
* The API key is no longer printed back into the settings form.
* Search is now answered inside the theme: Turing ranks WordPress's own search query and
  the theme's search template shows the results, so block themes keep their header,
  footer and styles. WordPress search answers when Turing does not. Replacing search is
  off on a fresh install; the plugin's own results page remains an option.
* viglet_dumont_public_config() and GET viglet-dumont/v1/public-config give a theme the
  browser-side search configuration (no key), for search built on the Turing JS SDK.
* The plugin registers the Turing JS SDK as script modules: @viglet/turing-sdk, and
  viglet-dumont/search, which is the SDK already configured for this site, so a theme's
  search blocks import it instead of bundling their own copy.
* A "Turing search" block: a search box with suggestions, "did you mean", facets, results
  and pages, built with the Interactivity API and styled by the theme. It reads ?q= so a
  page holding it works as the search page. The viglet_dumont_register_blocks filter turns
  it off for sites with their own search blocks.
* Requires WordPress 6.6: the admin screens are built with @wordpress/scripts, whose JSX
  runtime script ships with 6.6.
* Index All stops and shows Turing's error when an import is refused, instead of reporting
  the batch as indexed.

= 2.0.0 =
* Requires PHP 8.1 and WordPress 6.5.
* Released as a versioned zip on GitHub, installable with Composer.
* Follows the WordPress Coding Standards; checked by PHPCS, PHPStan and PHPUnit in CI.
* Search input from the query string is unslashed and sanitized before use.

= 1.0.0 =
* First release.
