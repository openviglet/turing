---
title: Installation
description: Build and run Viglet Turing locally.
sidebar:
  order: 1
---

Viglet Turing runs as a single Spring Boot application with an embedded H2 database
and the embedded Lucene search engine, so a first run needs only Java 21.

## Build

```bash
./mvnw clean install
```

## Run

```bash
java -jar turing-app/target/viglet-turing.jar
```

The administration console is at `http://localhost:2700/console`.

## Next steps

Create a Semantic Navigation site, index a few documents, and search them from the
site's preview. The pages that cover each step are being moved here from
docs.viglet.org.
