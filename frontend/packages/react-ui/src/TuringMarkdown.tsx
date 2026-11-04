import ReactMarkdown, { type Options } from "react-markdown";
import { resolveSandboxUrl } from "./sandboxUrl.js";

export { resolveSandboxUrl } from "./sandboxUrl.js";

/**
 * XSS-safe URL sanitizer — a local re-implementation of `react-markdown`'s
 * `defaultUrlTransform`. Inlined (instead of importing the named export) so this
 * module carries only the DEFAULT `react-markdown` import: a barrel consumer
 * that never renders markdown (so `react-markdown` — an OPTIONAL peer — isn't
 * installed) still builds. A static named import of `defaultUrlTransform` fails
 * the bundler's optional-peer stub with `MISSING_EXPORT`. Behaviour matches
 * upstream: only safe protocols (or protocol-relative / fragment / query / path
 * forms where no scheme precedes the first `/ ? #`) pass; anything else → "".
 */
const SAFE_URL_PROTOCOL = /^(https?|ircs?|mailto|xmpp)$/i;
function sanitizeUrl(value: string): string {
  const colon = value.indexOf(":");
  const questionMark = value.indexOf("?");
  const numberSign = value.indexOf("#");
  const slash = value.indexOf("/");
  if (
    colon < 0 ||
    (slash > -1 && colon > slash) ||
    (questionMark > -1 && colon > questionMark) ||
    (numberSign > -1 && colon > numberSign) ||
    SAFE_URL_PROTOCOL.test(value.slice(0, colon))
  ) {
    return value;
  }
  return "";
}

/**
 * Angle-bracket-wraps markdown link/image targets so a URL containing spaces
 * (e.g. an artifact filename like `chart 1.png`) isn't truncated by the markdown
 * parser at the first space. Spaces are also percent-encoded for good measure.
 * `[label](url with spaces)` → `[label](<url%20with%20spaces>)`.
 */
function encodeMarkdownLinkUrls(text: string): string {
  return text.replaceAll(
    /\[([^\]]*)\]\(([^)]+)\)/g,
    (_match, label: string, url: string) =>
      `[${label}](<${url.replaceAll(" ", "%20")}>)`,
  );
}

export interface TuringMarkdownProps {
  /** The markdown source to render. */
  children: string;
  /** remark plugins (e.g. `remarkGfm`). The host owns its plugin set. */
  remarkPlugins?: Options["remarkPlugins"];
  /** rehype plugins (e.g. `rehypeHighlight`). The host owns its plugin set. */
  rehypePlugins?: Options["rehypePlugins"];
  /** Per-tag component overrides forwarded to `react-markdown`. */
  components?: Options["components"];
  /**
   * Turing host base. When set, `sandbox:` / `/api` artifact URLs resolve to an
   * ABSOLUTE URL against this host (cross-origin embed / BFF). Default `""` =
   * same-origin (the `sandbox:` scheme is stripped to a relative path).
   */
  baseUrl?: string;
  /**
   * Angle-bracket-escape spaces in link/image URLs before parsing so artifact
   * paths with spaces aren't truncated. Default `true`.
   */
  encodeUrlSpaces?: boolean;
  /**
   * Override the URL transform entirely. Default resolves the `sandbox:` scheme
   * (honouring {@link baseUrl}) and then applies the same XSS-safe sanitization
   * as `react-markdown`'s `defaultUrlTransform` (see {@link sanitizeUrl}). Pass
   * the SDK's `resolveSandboxUrl` to keep a single source of truth.
   */
  urlTransform?: (url: string) => string;
  /** When set, wraps the rendered markdown in a `<div>` with this class. */
  className?: string;
}

/**
 * Headless `react-markdown` wrapper — ONE markdown contract for every Turing
 * surface. It adds the bits each app was re-implementing inline: a `sandbox:`
 * artifact `urlTransform` (so code-interpreter / workspace images render instead
 * of being dropped by the default sanitizer), space-escaping for URLs with
 * spaces, and injectable remark/rehype plugins + component overrides.
 *
 * <p>`react-markdown` is an OPTIONAL peer dependency (the host provides it —
 * both Turing apps already ship `react-markdown@^10`). T1215 — so this
 * component is imported from its subpath, `@viglet/turing-react-ui/TuringMarkdown`,
 * never from the package root, which must load without the peer. The component is
 * design-agnostic: it renders `react-markdown`'s output and optionally wraps it
 * in a `className`'d div — the host owns prose styling (Tailwind `prose`, etc.).</p>
 *
 * @author Alexandre Oliveira
 * @since 2026.3.1
 */
export function TuringMarkdown({
  children,
  remarkPlugins,
  rehypePlugins,
  components,
  baseUrl = "",
  encodeUrlSpaces = true,
  urlTransform,
  className,
}: Readonly<TuringMarkdownProps>) {
  const transform =
    urlTransform ?? ((url: string) => sanitizeUrl(resolveSandboxUrl(url, baseUrl)));
  const source = encodeUrlSpaces ? encodeMarkdownLinkUrls(children) : children;
  const rendered = (
    <ReactMarkdown
      remarkPlugins={remarkPlugins}
      rehypePlugins={rehypePlugins}
      components={components}
      urlTransform={transform}
    >
      {source}
    </ReactMarkdown>
  );
  return className ? <div className={className}>{rendered}</div> : rendered;
}
