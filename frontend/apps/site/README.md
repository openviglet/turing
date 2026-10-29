# turing-site

Public marketing site for **Viglet Turing**, published to
[openviglet.github.io/turing](https://openviglet.github.io/turing/).

Stack: **Vite 8 + React 19 + TypeScript + Tailwind CSS v4 + shadcn/ui**
(same conventions as the other apps in this monorepo). It is a standalone,
dependency-light landing page: it does **not** depend on the Turing SDK or
backend.

## Develop

```bash
# from the repo frontend/ root
pnpm dev:site            # vite dev server

# or directly
pnpm --filter turing-site dev
```

## Build

```bash
pnpm build:site          # tsc -b && vite build  →  dist/
```

The build output in `dist/` is deployed to the root of `openviglet.github.io/turing` by
[.github/workflows/publish-github-pages.yml](../../../.github/workflows/publish-github-pages.yml),
alongside the React SDK Storybook (`/react-sdk/`) and the marketplace examples
(`/marketplace/`).

## Content

All page copy lives in [`src/lib/site-content.ts`](src/lib/site-content.ts)
(features, agent-loop steps, providers, stats, marketplace + ecosystem cards).
Edit that file to update the page; the section components in
[`src/components/sections/`](src/components/sections/) render it.

Static assets served from the site root (`robots.txt`, `sitemap.xml`,
`llms.txt`, favicons) live in [`public/`](public/).
