// Where the docs are served from, shared by astro.config.mjs, scripts/check-links.mjs
// and the components that link back to the marketing site.
//
// The marketing site owns the root of its custom domain, and the docs sit one level
// below it: https://turing.viglet.org/docs/. SITE_ROOT stays a seam (empty today) in
// case the site is ever served under a project path again.

/** The public origin. */
export const SITE_ORIGIN = "https://turing.viglet.org";

/** The marketing site's root path; its own pages ("/compare/") hang off it. */
export const SITE_ROOT = "";

/** The docs' base path. */
export const DOCS_BASE = `${SITE_ROOT}/docs`;
