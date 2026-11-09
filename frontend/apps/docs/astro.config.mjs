// The Viglet Turing end-user documentation, at https://turing.viglet.org/docs/.
//
// T1160 — the docs used to live in a separate Docusaurus repo (vigletdocs, published at
// docs.viglet.org), so every user-facing task needed a second commit in a second repo and
// the docs drifted from the code. They are built from this repo now, in the shape roadkeep's
// site/docs uses: their own Astro + Starlight project, a fixed base, and an outDir inside the
// marketing site's dist/ so the one GitHub Pages deploy carries both.
import { readFileSync } from "node:fs";

import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

import { DOCS_BASE, SITE_ORIGIN, SITE_ROOT } from "./site.mjs";

const sidebar = JSON.parse(
  readFileSync(new URL("./src/sidebar.generated.json", import.meta.url), "utf-8"),
);

// The base. Astro rewrites the links it generates and NOT the ones typed by hand, so an
// absolute href written into a page ("/getting-started/") drops the prefix and 404s in
// production alone. Link relatively, or through the sidebar.
const BASE = DOCS_BASE;

// The pages link to each other as "/docs/<page>/", the path from the site root. The
// site owns the root of turing.viglet.org, so SITE_ROOT is empty and this is a no-op;
// it stays so a move under a project path (SITE_ROOT) prefixes those hrefs and srcs
// at build time instead of rewriting every page. MDX component props (`<LinkCard href="/docs/...">`) are
// attributes of a JSX node rather than properties of an element, so both are covered.
function rebaseDocsLinks() {
  const rebase = (value) =>
    typeof value === "string" && (value === "/docs" || value.startsWith("/docs/"))
      ? SITE_ROOT + value
      : value;
  const visit = (node) => {
    if (node.type === "element" && node.properties) {
      for (const attr of ["href", "src"]) node.properties[attr] = rebase(node.properties[attr]);
    }
    for (const attr of node.attributes ?? []) {
      if (attr.type === "mdxJsxAttribute" && (attr.name === "href" || attr.name === "src")) {
        attr.value = rebase(attr.value);
      }
    }
    node.children?.forEach(visit);
  };
  return (tree) => visit(tree);
}

// The output: a subdirectory of the marketing site's build, because the Pages workflow
// publishes frontend/apps/site/dist and nothing else. Vite empties dist/ when the site
// builds, which is why `build:site` runs this build *last*; reordered, the site build
// deletes it and the deploy goes green with the docs missing. Nothing here is committed —
// the site's .gitignore holds dist/.
const OUT_DIR = "../site/dist/docs";

export default defineConfig({
  site: SITE_ORIGIN,
  base: BASE,
  outDir: OUT_DIR,
  trailingSlash: "always",
  // Only under a project path: at a domain root the rebase is a no-op, and since Astro 7
  // made Sätteri the default Markdown processor, any rehypePlugins entry requires the
  // separately installed @astrojs/markdown-remark (a clean CI install fails without it).
  ...(SITE_ROOT ? { markdown: { rehypePlugins: [rebaseDocsLinks] } } : {}),

  integrations: [
    starlight({
      title: "Viglet Turing",
      description:
        "How to install, configure and use Viglet Turing: Semantic Navigation, search engines, connectors and GenAI.",
      logo: { src: "./src/assets/turing-mark.svg", alt: "Viglet Turing" },
      favicon: "/favicon.ico",
      // Pagefind, indexed at build time: search needs no service to be up.
      pagefind: true,
      social: [
        { icon: "github", label: "GitHub", href: "https://github.com/openviglet/turing" },
      ],
      customCss: ["@viglet/viglet-design-system/tokens.css", "./src/styles/docs.css"],
      // T1162 — the marketing site's look: its fonts, and its brand, nav and footer.
      head: [
        { tag: "link", attrs: { rel: "preconnect", href: "https://fonts.googleapis.com" } },
        { tag: "link", attrs: { rel: "preconnect", href: "https://fonts.gstatic.com", crossorigin: true } },
        {
          tag: "link",
          attrs: {
            rel: "stylesheet",
            href: "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:ital,wght@0,400;0,500;0,600;0,700;0,800;1,400&family=JetBrains+Mono:wght@400;500;700&display=swap",
          },
        },
      ],
      components: {
        Head: "./src/components/Head.astro",
        SiteTitle: "./src/components/SiteTitle.astro",
        SocialIcons: "./src/components/SocialIcons.astro",
        Footer: "./src/components/Footer.astro",
      },
      // T1161 — the order of the Docusaurus sidebars-turing.ts, written by
      // scripts/port-docusaurus.mjs next to the pages it ports.
      sidebar,
      editLink: {},
      lastUpdated: true,
    }),
  ],
});
