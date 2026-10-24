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
* Index All stops and shows Turing's error when an import is refused, instead of reporting
  the batch as indexed.

= 2.0.0 =
* Requires PHP 8.1 and WordPress 6.5.
* Released as a versioned zip on GitHub, installable with Composer.
* Follows the WordPress Coding Standards; checked by PHPCS, PHPStan and PHPUnit in CI.
* Search input from the query string is unslashed and sanitized before use.

= 1.0.0 =
* First release.
