---
title: "Upgrading to 2026.3"
description: "Move an installation from an earlier Viglet Turing version to 2026.3: export from the old version, start 2026.3 on an empty database and import."
---
Viglet Turing 2026.3 starts its database from a new schema baseline instead of replaying every earlier schema change. An installation from an earlier version, a 2026.2 one or a 2026.3 build from before the baseline, cannot be upgraded in place. You move it instead: export from the old version, start 2026.3 on an empty database and import the export there.

If you start 2026.3 against an existing database by mistake, nothing is changed. Startup stops with a message that says the database was created before the 2026.3 schema baseline and points at this page.

---

## Before you start

- Keep the old installation and its database. You need it running to export, and it is your way back.
- Note every API key the old installation uses: LLM instances, embedding stores and search engines. Exports never carry API keys, so you enter them again after the import.
- Note the global settings you changed (the default LLM, the default AI agent, branding). They are not part of an export.

## Upgrade

1. **Export your SN sites** from the old version, with their content if you want the indexed documents to come back without reindexing:

   ```bash
   curl "http://old-host:2700/api/sn/export" \
     -H "Key: <YOUR_API_TOKEN>" \
     -o turing-sites-export.zip
   ```

   The archive also carries the LLM instances, embedding stores and search engines the sites use. To export one site with its content, call `GET /api/sn/{id}/export?includeContent=true` instead. [Import & Export](/docs/import-export/#export) lists what an export holds.

2. **Export each AI agent** you want to keep, from its page in the console or with `GET /api/ai-agent/{id}/export`. The ZIP holds the agent, its chat flows and its custom tools.

3. **Start 2026.3 on an empty database.** Point `spring.datasource.url` at a new database, or a new H2 file, and start the new version. It creates the schema from the baseline. See the [Installation guide](/docs/installation-guide/) for the database settings.

4. **Import the exports** into the new installation, sites first and agents after, since an agent can refer to a site:

   ```bash
   curl -X POST "http://new-host:2700/api/import?includeContent=true" \
     -H "Key: <YOUR_API_TOKEN>" \
     -F "file=@turing-sites-export.zip"
   ```

   Without `includeContent=true` the import skips the documents in the archive and brings back the configuration only. Repeat the call for each agent ZIP. Placing the archives in the `export/` directory before the first startup does the same without API calls; see [Seeding a fresh installation](/docs/import-export/#seeding-a-fresh-installation).

5. **Enter the API keys again** on each LLM instance, embedding store and search engine, then restore the global settings you noted.

6. **Check the new installation**: run a search on each SN site and open a chat with each agent. A site exported without its content needs a reindex from its connector.

## After the upgrade

Later 2026.3 releases upgrade in place again: each one applies only the schema changes added since the baseline.
