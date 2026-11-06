=== Viglet Turing Search ===
Contributors: viglet
Tags: search, turing, indexing, semantic navigation
Requires at least: 6.6
Tested up to: 7.1
Requires PHP: 8.1
Stable tag: 1.0.0
License: MIT
License URI: https://opensource.org/licenses/MIT

Indexes your posts and pages into Viglet Turing and answers the site search from it.

== Description ==

Viglet Turing Search sends your published content to a Viglet Turing Semantic
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

1. In wp-admin, go to Plugins > Add New, search for "Viglet Turing Search", then Install Now and
   Activate.
2. Open Viglet Turing Search > Setup and give it the Turing URL, the API key and the SN site.
3. Test the connection, then run the first index.

A zip from the GitHub releases (Plugins > Add New > Upload Plugin) and Composer are described
in the plugin's README.

== External services ==

This plugin connects to a Viglet Turing server: the one whose address you enter under
Viglet Turing Search > Setup, which you run yourself or contract from a host. It is needed to index
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
https://github.com/openviglet/turing and https://turing.viglet.org/docs/

== Source code ==

The admin screens are shipped built. Their source, the build scripts and the tests are in the
public repository: https://github.com/openviglet/turing/tree/2026.3/wordpress

== Changelog ==

= 1.0.0 =
* First release.
