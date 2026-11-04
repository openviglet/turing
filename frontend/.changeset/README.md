# Changesets

This folder is managed by [changesets](https://github.com/changesets/changesets).
It drives **semver** for the publishable workspace packages — currently
`@viglet/turing-react-ui`, `@viglet/turing-react-sdk`, and `@viglet/turing-sdk`.
The apps and marketplace examples are `ignore`d in `config.json` (they are not
published).

## Workflow

1. **Describe a change.** After making a change that should ship, run from
   `frontend/`:

   ```bash
   pnpm changeset
   ```

   Pick the affected package(s), pick the bump (`patch` / `minor` / `major`),
   and write a one-line summary. This writes a markdown file into `.changeset/`
   — commit it with your PR.

2. **Publish.** Dispatch `.github/workflows/publish-npm.yml` (T1142), with one
   package or `all`, which publishes every package whose source changed since the
   version npm serves, in dependency order (`sdk` and `react-ui` before
   `react-sdk`). For each package it sets the next patch npm has not seen
   (`scripts/bump-patch.mjs`), then `scripts/consume-changesets.mjs` moves that
   package's pending changesets into its `CHANGELOG.md` under the new version and
   removes them, and the bump, the changelog and the consumed changesets are
   committed back together. A changeset that names several packages keeps the
   others' lines until they are published. The bump level in the changeset is not
   read: these packages follow the platform's `year.release.patch` version.

   Every package's npm trusted publisher names `publish-npm.yml`.
