/**
 * T1270: every destination a visitor's journey can reach, by the job it does.
 *
 * One map, read by the site's navigation, footer and CTAs (through LINKS in
 * components/brand-links.ts) and by the docs app's header and footer
 * (frontend/apps/docs/src/components). Pure data with no imports, so the Astro
 * docs build can import it as it is. A `path` is on this site and is prefixed
 * with the base by whoever renders it; a `url` is absolute and leaves the site.
 *
 * The journey, as the viglet.org hub spells it: explore it, install it, read the
 * documentation, see what each release changed, read the source, and get back
 * to the Viglet portfolio. Installation and release history are the hub's own
 * pages (www.viglet.org/turing/download/ and /release-notes/): this site keeps
 * no second release ledger. The development roadmap is GitHub's and is labelled
 * as such, apart from the user-facing release notes.
 */
export type Destination = Readonly<{ label: string } & ({ path: string } | { url: string })>;

export const DESTINATIONS = {
  /** Try it without installing: the live playground on the home page. */
  explore: { label: "Try it live", path: "/#playground" },
  /** The first actionable setup: prerequisites, three commands, a success check. */
  getStarted: { label: "Get started", path: "/docs/getting-started/quick-start/" },
  /** The documentation overview. */
  docs: { label: "Documentation", path: "/docs/" },
  /** Release downloads, on the hub. */
  download: { label: "Download", url: "https://www.viglet.org/turing/download/" },
  /** What changed in each release, for users, on the hub. */
  releaseNotes: { label: "Release notes", url: "https://www.viglet.org/turing/release-notes/" },
  /** The source repository. */
  source: { label: "Source on GitHub", url: "https://github.com/openviglet/turing" },
  /** The development roadmap: open engineering work, not release notes. */
  devRoadmap: {
    label: "Development roadmap",
    url: "https://github.com/openviglet/turing/blob/HEAD/docs/ROADMAP.md",
  },
  /** Back to the Viglet portfolio. */
  portfolio: { label: "viglet.org", url: "https://www.viglet.org/" },
  /** Viglet Dumont, the connector platform for the sources WordPress is not. */
  dumont: { label: "Viglet Dumont", url: "https://dumont.viglet.org/" },
  /** The AEM connector ships in Viglet Dumont; its docs are Dumont's own. */
  aemConnector: { label: "AEM connector docs", url: "https://dumont.viglet.org/docs/connectors/aem/" },
  /** The WordPress plugin's docs (T1254, moved here from Dumont's site). */
  wordpressDocs: { label: "WordPress plugin docs", path: "/docs/integrations/wordpress/" },
  /** The demo store in WordPress Playground, through this site's short link. */
  wordpressDemo: { label: "Try the demo store", path: "/wp-demo/" },
  /** The React SDK's Storybook, served by this site. */
  storybook: { label: "React SDK", path: "/react-sdk/" },
  npm: { label: "npm", url: "https://www.npmjs.com/package/@viglet/turing-react-sdk" },
  cli: { label: "CLI", url: "https://www.npmjs.com/package/@viglet/turing-cli" },
} as const satisfies Record<string, Destination>;

export type DestinationKey = keyof typeof DESTINATIONS;

/** The href of a destination, with `prefix` (the base path) before a site path. */
export function destinationHref(key: DestinationKey, prefix: (path: string) => string): string {
  const d: Destination = DESTINATIONS[key];
  return "url" in d ? d.url : prefix(d.path);
}
