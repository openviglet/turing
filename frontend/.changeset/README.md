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

2. **Publish the SDKs.** `@viglet/turing-sdk` and `@viglet/turing-react-sdk`
   are released by dispatching `publish-sdk.yml` and `publish-js-sdk.yml`
   (T1142). Each sets the next patch npm has not seen
   (`scripts/bump-patch.mjs`), then `scripts/consume-changesets.mjs` moves that
   package's pending changesets into its `CHANGELOG.md` under the new version and
   removes them, and the bump, the changelog and the consumed changesets are
   committed back together. A changeset that names both SDKs keeps the other
   package's line until that package is published. The bump level in the
   changeset is not read: these packages follow the platform's
   `year.release.patch` version.

3. **Publish react-ui.** `pnpm version-packages` consumes the pending changesets,
   bumps each package's `version` by its bump level and appends to its
   `CHANGELOG.md`; `pnpm release` publishes. In CI this is the **Publish
   react-ui** workflow (`.github/workflows/publish-react-ui.yml`). Run
   `version-packages` only for react-ui's changesets: it consumes the SDKs'
   changesets too, which their own workflows would otherwise write.
