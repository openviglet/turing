/**
 * T1005 — the two errors this SDK throws, as types a consumer can match on.
 *
 * Every non-2xx used to be `new Error(\`HTTP ${status}: ${statusText}\`)`, and a
 * `fetch` rejection was let through untouched. So the two facts a caller needs
 * in order to react — was this a transport failure or an answer, and what status
 * was it — existed only as substrings of a message. Telling "the tenant is
 * unreachable" from "the tenant said 404" from "the body was not readable" took
 * a regex over `error.message`, an `instanceof TypeError` check and an
 * `error.cause !== undefined` check, and every consumer wrote that guess again.
 *
 * A message is also the wrong place for it: it is the one part of an error that
 * is free to change, so a rewording silently breaks every consumer parsing it
 * and nothing in CI notices.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */

/**
 * The request completed and the server answered with a non-2xx status.
 *
 * `status` is the field to branch on. The message stays human-readable, and
 * nothing needs to parse it.
 *
 * ```ts
 * try {
 *   await fetchSearch(client, "site", params);
 * } catch (err) {
 *   if (err instanceof TuringHttpError && err.status === 404) renderEmpty();
 *   else if (err instanceof TuringTransportError) scheduleRetry();
 *   else throw err;
 * }
 * ```
 */
export class TuringHttpError extends Error {
  /** The HTTP status the server answered with. */
  readonly status: number;
  /** The reason phrase, when the server sent one — often empty over HTTP/2. */
  readonly statusText: string;
  /** The URL that was requested, for logs that have to say which call failed. */
  readonly url: string;
  /**
   * T1234 — the RFC 7807 problem details the server explained the error with,
   * when it answered JSON. A spent AI budget answers 429 with a `type` ending in
   * `quota-exhausted`, so `error.problem?.resetsAt` says when answers come back.
   */
  readonly problem?: TuringProblem;

  constructor(status: number, statusText: string, url: string, problem?: TuringProblem) {
    super(`HTTP ${status}${statusText ? `: ${statusText}` : ""}${problem?.detail ? ` - ${problem.detail}` : ""}`);
    // Set explicitly rather than inherited: a subclass of Error reports the base
    // name otherwise, so a log line or a Sentry group would read "Error".
    this.name = "TuringHttpError";
    this.status = status;
    this.statusText = statusText;
    this.url = url;
    this.problem = problem;
  }
}

/**
 * T1234 — an RFC 7807 problem-details body, as Turing answers an error. The
 * standard members are typed; any extension member the server adds is kept.
 */
export interface TuringProblem {
  /** A URI naming the kind of problem, e.g. one ending in `quota-exhausted`. */
  type?: string;
  /** A short summary of the kind of problem. */
  title?: string;
  /** The HTTP status, as the server repeated it in the body. */
  status?: number;
  /** What went wrong this time, readable by a person. */
  detail?: string;
  /** A URI naming this occurrence. */
  instance?: string;
  /** Quota errors: when the budget resets (ISO-8601). */
  resetsAt?: string;
  /** Quota errors: how much of the budget was used. */
  used?: number;
  /** Quota errors: the budget. */
  limit?: number;
  /** Quota errors: where the budget can be raised. */
  upgradeUrl?: string;
  /** Any other member the server sent. */
  [member: string]: unknown;
}

/**
 * The request never completed — DNS failure, refused connection, CORS, an
 * offline device, a body that could not be read.
 *
 * Distinct from {@link TuringHttpError} because the two call for different
 * handling: a transport failure is worth retrying, an answer usually is not.
 * The underlying failure is on `cause`.
 */
export class TuringTransportError extends Error {
  /** The URL that was requested. */
  readonly url: string;
  /**
   * The underlying failure — the `fetch` rejection, or `undefined` where the
   * response arrived but carried nothing to read.
   *
   * Declared here rather than inherited: this package's `lib` is ES2021, which
   * predates `Error.cause`, so assigning it without this compiles under the test
   * config and then fails `tsc --emitDeclarationOnly` — and a consumer's `.d.ts`
   * would not have carried the field at all.
   */
  readonly cause: unknown;

  constructor(url: string, cause?: unknown, message?: string) {
    super(message ?? `Request to ${url} did not complete: ${describe(cause)}`);
    this.name = "TuringTransportError";
    this.url = url;
    // `cause` via the standard Error option would be cleaner, but this package
    // targets ES2020, where the constructor does not accept it.
    this.cause = cause;
  }
}

/**
 * T1079 — the request did not finish within the client's `timeoutMs`, and was
 * aborted.
 *
 * A {@link TuringTransportError}, because to a caller it is the same decision as
 * a refused connection: Turing produced no answer, and an "unavailable" page or a
 * retry is the reaction. A separate class so a page can say "too slow" rather
 * than "unreachable", and log the limit that was hit.
 */
export class TuringTimeoutError extends TuringTransportError {
  /** The limit that elapsed, in milliseconds. */
  readonly timeoutMs: number;

  constructor(url: string, timeoutMs: number) {
    super(url, undefined, `Request to ${url} did not finish within ${timeoutMs} ms`);
    this.name = "TuringTimeoutError";
    this.timeoutMs = timeoutMs;
  }
}

/**
 * A response arrived but carried nothing to read — a streaming endpoint with a
 * null body, or a JSON endpoint that answered empty.
 *
 * Typed as a {@link TuringTransportError} rather than a third class: from a
 * caller's side it is the same decision as a refused connection — the request
 * produced no usable result and retrying is reasonable — and it is the third of
 * the three cases a consumer previously had to tell apart by reading messages.
 * The message is given explicitly, because "did not complete" would be false of
 * a response that did.
 */
export function emptyBody(url: string, what: string): TuringTransportError {
  return new TuringTransportError(url, undefined, `${what} has no readable body`);
}

function describe(cause: unknown): string {
  if (cause instanceof Error) return cause.message;
  return typeof cause === "string" ? cause : String(cause);
}

/**
 * Throw {@link TuringHttpError} when a response is not ok, and return otherwise.
 *
 * The single place the status becomes an error, so a new endpoint cannot go back
 * to throwing a plain `Error` by copying its neighbour.
 */
export async function assertOk(response: Response): Promise<void> {
  if (!response.ok) {
    throw new TuringHttpError(response.status, response.statusText, response.url, await readProblem(response));
  }
}

/**
 * T1234 — the problem-details body of an error response, when it is a JSON
 * object; `undefined` for any other body, so a page that answers HTML or nothing
 * still throws the plain status.
 */
export async function readProblem(response: Response): Promise<TuringProblem | undefined> {
  const contentType = response.headers?.get?.("content-type") ?? "";
  if (!contentType.includes("json")) return undefined;
  try {
    const body: unknown = JSON.parse(await response.text());
    return body !== null && typeof body === "object" && !Array.isArray(body) ? (body as TuringProblem) : undefined;
  } catch {
    return undefined;
  }
}

/**
 * T1240 — the problem details a failure carries, for a controller or hook to keep
 * beside its `error` text: `undefined` unless `err` is a {@link TuringHttpError}
 * whose server answered problem-details JSON. A 429 `quota-exhausted` keeps its
 * `resetsAt` and `upgradeUrl` this way, which the text alone loses.
 */
export function problemOf(err: unknown): TuringProblem | undefined {
  return err instanceof TuringHttpError ? err.problem : undefined;
}

/**
 * Whether this rejection is the caller's own `AbortSignal` firing.
 *
 * Cancellation is not a transport failure, and wrapping it would break every
 * consumer that catches `AbortError` to distinguish "I cancelled this" from
 * "this broke" — a search-as-you-type controller aborts on every keystroke.
 */
export function isAbort(cause: unknown): boolean {
  return cause instanceof DOMException
    ? cause.name === "AbortError"
    : cause instanceof Error && cause.name === "AbortError";
}
