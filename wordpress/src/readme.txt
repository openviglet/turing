=== Viglet Dumont for WordPress ===
Contributors: viglet
Tags: search, turing, indexing, semantic navigation
Requires at least: 6.5
Tested up to: 6.8
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

== Installation ==

1. Download `viglet-dumont-<version>.zip` from the GitHub release and upload it under
   Plugins > Add New > Upload Plugin, or install it with Composer (see the plugin README).
2. Activate the plugin.
3. Open Viglet Dumont in the admin menu and set the Turing URL, the SN site name and the API key.
4. Use Test Connection, then Index All.

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
* Index All stops and shows Turing's error when an import is refused, instead of reporting
  the batch as indexed.

= 2.0.0 =
* Requires PHP 8.1 and WordPress 6.5.
* Released as a versioned zip on GitHub, installable with Composer.
* Follows the WordPress Coding Standards; checked by PHPCS, PHPStan and PHPUnit in CI.
* Search input from the query string is unslashed and sanitized before use.

= 1.0.0 =
* First release.
