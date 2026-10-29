// Where the docs are served from, shared by astro.config.mjs, scripts/check-links.mjs
// and the components that link back to the marketing site.
//
// GitHub Pages serves the marketing site under a project path, not at a domain root,
// and the docs one level below it: https://openviglet.github.io/turing/docs/.

/** The public origin. */
export const SITE_ORIGIN = "https://openviglet.github.io";

/** The marketing site's root path; its own pages ("/compare/") hang off it. */
export const SITE_ROOT = "/turing";

/** The docs' base path. */
export const DOCS_BASE = `${SITE_ROOT}/docs`;
