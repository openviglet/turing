---
title: "WordPress plugin reference"
description: "The Viglet Turing Search plugin's WP-CLI commands, REST routes, constants and PHP filters, for developers and operators of a WordPress site."
---

This page lists the Viglet Turing Search plugin's interfaces for developers and operators:
the `wp turing` WP-CLI commands, the REST routes, the `wp-config.php` constants and the
PHP filters. For what the plugin does with them, start at [WordPress](/docs/integrations/wordpress/).

## WP-CLI commands

The commands, in the order you would usually reach for them.

| Command | What it does |
|---|---|
| `wp turing status` | Connection, SN site, site key, published versus indexed documents per type, the queue, and where each setting came from |
| `wp turing index [--type=<type>] [--ids=<ids>] [--since=<date>]` | Indexes now, synchronously, with a progress bar; exits non-zero when a batch fails. `--ids` also removes the listed posts that are not published |
| `wp turing reindex` | Provisions the fields, indexes every published post, then sweeps. Runs in the terminal, synchronously, unlike Index All in wp-admin |
| `wp turing sweep [--dry-run]` | Removes from Viglet Turing this site key's documents that WordPress no longer publishes, e.g. posts deleted while the plugin was off |
| `wp turing queue run [--retry-failed]` | Sends the queue now, ignoring backoff, and names each post that failed |
| `wp turing queue status` | Lists the waiting and failed jobs with their last error |
| `wp turing ai status [--format=json]` | Which of Viglet Turing's AI features the site can use, and why each one that is off is off; exits non-zero when any is off. While the site's AI budget is spent it warns *AI budget exhausted until* and the reset date |

## REST routes

The plugin's routes live under `/wp-json/viglet-turing/v1/`. The operations behind the
commands above answer there as `status`, `index`, `sweep`, `queue/run` and `queue/retry`.
`POST reindex` starts Index All in the background (`type` is `all` or one post type),
`GET reindex` reads its progress and `POST reindex/cancel` stops it. The field mapping is
readable and writable as JSON at `mapping`, and `GET public-config` returns the public,
key-free configuration the browser scripts read.

## Constants and environment variables

The connection settings can come from `wp-config.php` constants or environment variables;
the table is in [Connect from the environment](/docs/integrations/wordpress/#from-the-environment).
`VIGLET_TURING_DEMO=1` turns on [demo mode](/docs/integrations/wordpress/#demo-mode).

## Filters

Every filter the plugin applies, alphabetically.

| Filter | Arguments | Default | What it changes |
|---|---|---|---|
| `viglet_turing_document` | `$document`, `$post` | | A document just before it is sent |
| `viglet_turing_http_timeout` | `$timeout`, `$documents` | 2 seconds a document, at least 30 | How long an import request to Viglet Turing may take |
| `viglet_turing_mapping` | `$mapping`, `$settings` | The **Fields** mapping | The field mapping, set in code |
| `viglet_turing_pdf_max_bytes` | `$bytes` | 20 MB | The largest PDF the plugin sends |
| `viglet_turing_register_blocks` | `$register` | `true` | `false` turns the plugin's blocks off |
| `viglet_turing_reindex_loopback` | `$kick` | `true` | `false` stops a background reindex from requesting its own next step |
| `viglet_turing_reindex_step_sslverify` | `$verify` | `true` | Whether that request to the site's own address verifies TLS |
| `viglet_turing_reindex_step_url` | `$url` | The site's own `viglet-turing/v1/reindex/step` | Where that request goes, for a server that cannot reach its own public address |
| `viglet_turing_render_timeout` | `$seconds` | 3 | How long a page waits for Viglet Turing before rendering without it |
| `viglet_turing_result_card` | `$card`, `$document` | | A result card the search block draws on the server |
| `viglet_turing_should_index` | `$index`, `$post` | | Whether a post is in search; it has the last word |
| `viglet_turing_tracking_consent` | `$consent` | `true` | Whether search events are sent when no WP Consent API plugin is installed; with one, the visitor's choice decides |

## Related pages

| Page | What it covers |
|---|---|
| [WordPress](/docs/integrations/wordpress/) | Install, connect and the queue |
| [Manage the plugin in wp-admin](/docs/integrations/wordpress/admin/) | The screens behind the same operations |
| [CLI](/docs/cli/) | Viglet Turing's own `turing` command line |
