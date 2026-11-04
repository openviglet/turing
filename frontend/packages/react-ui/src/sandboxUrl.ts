/**
 * One-or-more leading `sandbox:` prefixes plus any following slashes.
 * Local copy of the SDK's resolver (see {@link resolveSandboxUrl}).
 */
const SANDBOX_PREFIX = /^(?:\s*sandbox:\/*)+/i;

function originOf(baseUrl: string): string {
  try {
    return new URL(baseUrl).origin;
  } catch {
    return baseUrl.replace(/\/+$/, "");
  }
}

/**
 * Resolves a (possibly `sandbox:`-prefixed) artifact URL to one a browser can
 * load. Mirrors `resolveSandboxUrl` from `@viglet/turing-sdk` — re-declared here
 * so `@viglet/turing-react-ui` keeps ZERO runtime dependencies (it cannot import
 * the SDK until that package is npm-published; see the package README / roadmap
 * T303). Pass {@link TuringMarkdownProps.urlTransform} to reuse the SDK's
 * canonical implementation instead.
 *
 * T1215 — its own module, so the package root exports it without loading
 * `TuringMarkdown` and with it the optional `react-markdown` peer.
 *
 * - `sandbox:/api/...` → `/api/...` (scheme stripped; collapses doubled/no-slash
 *   improvisation by the model);
 * - with a {@code baseUrl}, an absolute `/api/...` path becomes an absolute URL
 *   against that host (cross-origin embed);
 * - non-sandbox absolute URLs / `data:` / `mailto:` are left untouched.
 */
export function resolveSandboxUrl(url: string, baseUrl = ""): string {
  if (!url) return url;
  let path = url.trim();
  if (SANDBOX_PREFIX.test(path)) {
    path = path.replace(SANDBOX_PREFIX, "/");
  } else if (!path.startsWith("/")) {
    return path;
  }
  if (baseUrl && path.startsWith("/")) {
    return originOf(baseUrl) + path;
  }
  return path;
}
