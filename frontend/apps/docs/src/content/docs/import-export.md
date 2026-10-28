---
title: "Import & Export"
description: "Export and import Semantic Navigation site configurations and content via ZIP archives."
---
Viglet Turing supports exporting and importing **Semantic Navigation site configurations** as ZIP archives. This allows you to migrate sites between environments, create backups, or share configurations across teams.

---

## Export

Export creates a ZIP archive containing a complete snapshot of one or all SN Sites, including field definitions, facet settings, spotlights, ranking expressions, merge providers, locale configuration, and GenAI settings.

### Export a Single Site

```
GET /api/sn/{id}/export
```

### Export All Sites

```
GET /api/sn/export
```

Both endpoints require authentication via the `Key` header. The response is a downloadable ZIP file containing an `export.json` with the full site configuration.

**Example:**

```bash
curl "http://localhost:2700/api/sn/export" \
  -H "Key: <YOUR_API_TOKEN>" \
  -o turing-sites-export.zip
```

### Export Contents

The `export.json` inside the ZIP archive contains:

| Section | Description |
|---|---|
| `snSites` | Complete SN Site configurations: fields, facets, spotlights, ranking expressions, merge providers, locales, and GenAI settings |
| `llm` | LLM instance references used by sites with GenAI enabled |
| `store` | Embedding store instance references |
| `se` | Search engine instance references |

Instance references never carry an owning tenant. When [multi-tenancy](/docs/multi-tenancy/) is enabled, a tenant id only identifies the installation that created it, so it is left out of the archive: an imported instance is claimed by whoever imports it, and one imported with no tenant bound stays shared (GLOBAL).

---

## Seeding a fresh installation

On the **first** startup against an empty database, Turing imports every `.zip` it finds in the `export/` directory of its working directory (alphabetical order). Use this to open a new environment with your sites, LLM instances, search engines and embedding stores already configured: no admin clicks, no API calls.

```
/app/export/           # in the Docker image
  defaults.zip
  my-sites.zip
```

A bundle may contain **only** instance configuration, with no site at all. That is the normal shape of a defaults bundle:

```json
{
  "se": [ { "id": "…", "title": "Default Search Engine", "enabled": 1,
            "endpointUrl": "./store/lucene-se",
            "turSEVendor": { "id": "LUCENE" } } ]
}
```

### What the Docker image already seeds

The official image ships `/app/export/defaults.zip` with two shared instances, both embedded and requiring no external service or credential:

| Instance | Vendor | Path |
|---|---|---|
| Default Search Engine | `LUCENE` | `./store/lucene-se` |
| Default Vector Store | `LUCENE` | `./store/lucene-vector` |

Both live under `/app/store`, which the image declares as a volume, and both are shared: with multi-tenancy enabled, every tenant can use them without configuring anything of their own.

No LLM instance is seeded: an exported LLM carries its API key encrypted with the key of the installation that exported it. Set `OPENAI_API_KEY` or `GEMINI_API_KEY` instead and Turing provisions a default LLM at startup (see [LLM instances](/docs/llm-instances/)).

### Seeding your own defaults

Mount your bundle into `export/` before the first startup:

```bash
docker run -v ./my-defaults.zip:/app/export/my-defaults.zip \
  -e TURING_TENANCY_ENABLED=true \
  viglet/turing
```

Export from a configured environment to produce the bundle: the archive an admin export gives you is exactly the format this reads.

:::caution[One shot per database]
Once every ZIP in `export/` has imported, Turing records an `EXPORT_AUTO_IMPORT` marker and never scans the directory again, so changing a bundle later has no effect on an existing database. Adding a ZIP still works if the directory was **absent or empty** at first startup, because nothing was recorded then.
:::

### When a seed ZIP fails

A ZIP that fails to import (a truncated file, a missing `export.json`, a malformed archive) is **retried on the next startup**. The "done" marker is written only when every ZIP in the directory succeeded, so a bad seed does not quietly disable seeding for that database.

Archives that did import are recorded individually and are **not** imported a second time on that retry. This matters: re-importing a bundle that contains a site *replaces* that site, discarding changes made to it since, so only the failures are re-run.

The startup log says which is which:

```
Successfully auto-imported 'defaults.zip'.
Failed to auto-import 'my-sites.zip': Missing required export.json in ZIP
Auto-import from 'export' incomplete: 1 of 2 ZIP file(s) failed. The successful
ones are recorded and will not be re-imported; the failures will be retried on
the next start.
```

Fix the archive and restart. To force a completed directory to be imported again, delete the `EXPORT_AUTO_IMPORT*` rows from the `config_var` table, but read the replacement warning above first.

If your deployment runs more than one container, give them all the same encryption key. Otherwise the API keys inside a seeded bundle cannot be decrypted. See [Security & authentication](/docs/security-authentication/).

---

## Import

The import page (`/admin/exchange/import`) accepts a ZIP file previously exported from Turing and recreates the site configurations in the target environment.

### Using the Admin Console

1. Navigate to **Import** in the sidebar
2. **Drag and drop** a ZIP file onto the upload area, or click to browse
3. The upload progress is displayed in real time (percentage bar)
4. On success, you are automatically redirected to the SN Sites listing after 2 seconds

**Supported formats:** `.zip` files exported from Turing.

**UI states:**

| State | Description |
|---|---|
| Idle | Dashed border with upload icon: ready for file drop or click |
| Uploading | Blue background with spinning loader and progress percentage |
| Success | Green border with checkmark: "Import Completed" message and auto-redirect |
| Error | Red border with error message and "Try Again" button |

### Using the REST API

```
POST /api/import
```

Accepts a `multipart/form-data` request with a `file` parameter containing the ZIP archive. Authentication required.

**Example:**

```bash
curl -X POST "http://localhost:2700/api/import" \
  -H "Key: <YOUR_API_TOKEN>" \
  -F "file=@turing-sites-export.zip"
```

---

## Content Import (SN Job Items)

In addition to site configuration import, Turing accepts **document-level indexing jobs** via the SN Import API. This is used by connectors and custom integrations to push content into the search index.

### JSON Import

```
POST /api/sn/import
```

Accepts a JSON body whose `turingDocuments` array lists the job items: documents to index, deindex, or commit. `snJobItems` is accepted as an alias. A body with no items answers `400` and names the expected property. That includes items sent under any other key, which used to be accepted silently with nothing indexed.

**Example:**

```bash
curl -X POST "http://localhost:2700/api/sn/import" \
  -H "Key: <YOUR_API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "turingDocuments": [
      {
        "locale": "en_US",
        "turSNJobAction": "CREATE",
        "siteNames": ["Sample"],
        "attributes": {
          "id": "doc-001",
          "title": "Getting Started Guide",
          "text": "This guide explains how to get started...",
          "url": "https://example.com/getting-started"
        }
      }
    ]
  }'
```

### ZIP Import (with file attachments)

```
POST /api/sn/import/zip
```

Accepts a `multipart/form-data` ZIP file containing an `export.json` with job items. Document attributes can reference embedded files using the `file://` protocol, Turing extracts text from these files using Apache Tika before indexing.

### Job Actions

| Action | Description |
|---|---|
| `CREATE` | Index or update a document in the search engine |
| `DELETE` | Remove documents from the index: by `id`, by `type`, or by the fields they share |
| `COMMIT` | Commit pending changes to the search engine |

<a id="deleting-documents"></a>

### Deleting documents

A `DELETE` item says what to remove through its `attributes`:

| Attributes | Removes |
|---|---|
| `{"id": "doc-001"}` | That document |
| `{"type": "news"}` | Every document of that type |
| `{"site_key": "a.example.org"}` | Every document whose fields hold all the values given. Name one field or several. |

A delete by field is how a connector that shares an SN site with others removes only its own documents, without listing their ids first:

```json
{
  "turingDocuments": [
    { "turSNJobAction": "DELETE", "siteNames": ["Sample"], "locale": "en_US",
      "attributes": { "site_key": "a.example.org" } }
  ]
}
```

It works on Solr, Lucene and Elasticsearch sites. Field names are letters, digits, `_`, `.` and `-`, with one value each. A `DELETE` that names nothing, and one with `"id": "*"` (which used to answer `200` and delete nothing), are refused with `400`. With a [partitioned key](/docs/rest-api/#partitioned-keys), a delete by field stays inside the key's partition: naming only the partition field deletes all of the key's documents. From the Java SDK, `TurSNJobUtils.deleteItemsByFields(server, Map.of("site_key", "a.example.org"))`.

### Job Item Structure

| Field | Type | Description |
|---|---|---|
| `locale` | `string` | Document locale (e.g., `en_US`) |
| `turSNJobAction` | `string` | `CREATE`, `DELETE`, or `COMMIT` |
| `siteNames` | `string[]` | Target SN Site names |
| `attributes` | `map` | Document fields: must include `id` |
| `checksum` | `string` | Optional content checksum for change detection |
| `environment` | `string` | Optional: `AUTHOR` or `PUBLISHING` |

---

## Related Pages

| Page | Description |
|---|---|
| [Semantic Navigation](/docs/semantic-navigation/) | SN Site configuration |
| [Integration](/docs/integration/) | Content connectors that automate document import |
| [REST API Reference](/docs/rest-api/) | Full API endpoint reference |
| [Developer Guide](/docs/developer-guide/) | Java SDK and JavaScript SDK for programmatic import |

---

