---
title: "Quick start"
description: "Run Viglet Turing on your own machine in a few minutes: what you need, the three commands, and how to tell it worked."
---

This page takes you from nothing to a running Viglet Turing on your own machine. The stack it starts is the minimal one: Viglet Turing with its built-in H2 database and the embedded Lucene search engine, from a single published container image. Nothing else has to be installed or configured first.

For a production installation (an external database, Apache Solr, Keycloak, a Linux service), follow the [Installation Guide](/docs/installation-guide/) instead.

## What you need

- **Docker** with Compose: Docker Desktop on Windows and macOS, or Docker Engine with the Compose plugin on Linux. Check it with `docker compose version`.
- **Node.js 26 or later**, to run the `turing` CLI. Check it with `node --version`.
- **Port 2700** free on your machine. Viglet Turing serves the admin console and the API there.

## Start it

```bash
npx @viglet/turing-cli init my-search   # 1. scaffold a project folder
cd my-search
npx @viglet/turing-cli dev              # 2. start Viglet Turing with Docker Compose
```

The first run downloads the `ghcr.io/openviglet/turing` image, so it takes longer than the ones after it. `dev` stays attached to the container logs; leave it running and use a second terminal for the next step.

To use the `turing` command without `npx`, install it once with `npm i -g @viglet/turing-cli`. The [CLI page](/docs/cli/) covers every command.

## Check that it worked

1. Open **http://localhost:2700** in a browser. On the first start the console asks you to choose the password for the `admin` user (at least 6 characters). [Login](/docs/administration-guide/#login) explains the alternative, the `TURING_ADMIN_PASSWORD` environment variable.
2. Sign in as `admin` with that password. The admin console opening is the first success.
3. From a terminal, the API answers with the list of search sites, as JSON:

   ```bash
   curl -u admin:your_password http://localhost:2700/api/sn
   ```

If the browser cannot reach the page, look at the `dev` terminal: the container logs say whether it is still starting or why it stopped. Another program using port 2700 is the most common cause.

## Next steps

- [Your first search and your first RAG answer](/docs/getting-started/intro/#your-learning-path), with the concepts behind them in [Core Concepts](/docs/getting-started/core-concepts/).
- [Deploy agents, flows and tools from the project](/docs/cli/#your-first-project-5-minutes) you just scaffolded.
- [Download a release](https://www.viglet.org/turing/download/) to run the JAR without Docker, and read [what changed in each release](https://www.viglet.org/turing/release-notes/).
