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

const sidebar = JSON.parse(
  readFileSync(new URL("./src/sidebar.generated.json", import.meta.url), "utf-8"),
);

// The base. Astro rewrites the links it generates and NOT the ones typed by hand, so an
// absolute href written into a page ("/getting-started/") drops the prefix and 404s in
// production alone. Link relatively, or through the sidebar.
const BASE = "/docs";

// The output: a subdirectory of the marketing site's build, because the Pages workflow
// publishes frontend/apps/site/dist and nothing else. Vite empties dist/ when the site
// builds, which is why `build:site` runs this build *last*; reordered, the site build
// deletes it and the deploy goes green with the docs missing. Nothing here is committed —
// the site's .gitignore holds dist/.
const OUT_DIR = "../site/dist/docs";

export default defineConfig({
  site: "https://turing.viglet.org",
  base: BASE,
  outDir: OUT_DIR,
  trailingSlash: "always",

  integrations: [
    starlight({
      title: "Viglet Turing Docs",
      description:
        "How to install, configure and use Viglet Turing: Semantic Navigation, search engines, connectors and GenAI.",
      logo: { src: "./src/assets/turing-logo.png", alt: "Viglet Turing" },
      favicon: "/favicon.ico",
      // Pagefind, indexed at build time: search needs no service to be up.
      pagefind: true,
      social: [
        { icon: "github", label: "GitHub", href: "https://github.com/openviglet/turing-ce" },
      ],
      customCss: ["./src/styles/docs.css"],
      // T1161 — the order of the Docusaurus sidebars-turing.ts, written by
      // scripts/port-docusaurus.mjs next to the pages it ports.
      sidebar,
      editLink: {},
      lastUpdated: true,
    }),
  ],
});
