/**
 * T1027: the outbound links, on their own.
 *
 * A record of URLs with a comment explaining each destination. Nothing here
 * renders, and the module it sat in exports components.
 */
import { withBase } from "@/lib/site-url";

export const LINKS = {
  docs: withBase("/docs/"),
  // Connectors ship in the Dumont project; the AEM connector docs live under
  // the /dumont route (docs-dumont/connectors/aem.md), not under /turing.
  aemConnector: "https://docs.viglet.org/dumont/connectors/aem",
  // T1254 — the WordPress plugin's docs, moved here from Dumont's site.
  wordpressDocs: withBase("/docs/integrations/wordpress/"),
  // The demo store in WordPress Playground, through this site's own short link.
  wordpressDemo: withBase("/wp-demo/"),
  // The connector platform for the sources WordPress is not (AEM, databases, files).
  dumont: "https://dumont.viglet.org/",
  // Roadmap/Changelog are rendered markdown (blob/HEAD follows the default branch).
  roadmap: "https://github.com/openviglet/turing/blob/HEAD/docs/ROADMAP.md",
  // User-facing release notes render the single-source changelog.json this site
  // owns and serves (public/changelog.json → turing.viglet.org/changelog.json).
  // The dev-facing ledger lives in docs/CHANGELOG.md (linked as Roadmap above).
  changelog: "https://www.viglet.org/turing/release-notes/",
  npm: "https://www.npmjs.com/package/@viglet/turing-react-sdk",
  cli: "https://www.npmjs.com/package/@viglet/turing-cli",
  storybook: withBase("/react-sdk/"),
  viglet: "https://www.viglet.org",
} as const;
