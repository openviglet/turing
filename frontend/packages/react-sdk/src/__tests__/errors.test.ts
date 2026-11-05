import { describe, expect, it, vi } from "vitest";

import { TuringHttpError, TuringTransportError } from "../index.js";
import {
  TuringHttpError as CoreHttpError,
  TuringTransportError as CoreTransportError,
  assertOk,
  emptyBody,
  fetchOrThrow,
} from "../core/errors.js";

/**
 * T1012 — the React SDK throws the vanilla SDK's error types, not its own.
 *
 * The contract worth pinning is class *identity*. `instanceof` compares
 * constructors, so a second `TuringHttpError` declared in this package would not
 * match one thrown by `@viglet/turing-sdk`, and a consumer using both would be
 * back to regexing `error.message` — the exact defect T1005 removed from one SDK
 * and left standing in the other.
 */
const URL_ = "https://turing.example/api/sn/site/search";

describe("the classes are the vanilla SDK's", () => {
  it("re-exports the same constructor the core throws", async () => {
    const { TuringHttpError: VanillaHttpError, TuringTransportError: VanillaTransportError } =
      await import("@viglet/turing-sdk");

    // Same object, three ways in: this package's root, this package's core, and
    // the vanilla SDK itself. If any of these ever diverges, `instanceof` across
    // the two SDKs stops working and nothing else would notice.
    expect(TuringHttpError).toBe(VanillaHttpError);
    expect(CoreHttpError).toBe(VanillaHttpError);
    expect(TuringTransportError).toBe(VanillaTransportError);
    expect(CoreTransportError).toBe(VanillaTransportError);
  });
});

describe("assertOk", () => {
  it("throws TuringHttpError carrying the status", async () => {
    const response = new Response("", { status: 404, statusText: "Not Found" });
    const err = await assertOk(response).then(() => undefined, (e: unknown) => e);

    expect(err).toBeInstanceOf(TuringHttpError);
    expect((err as TuringHttpError).status).toBe(404);
  });

  it("passes a 2xx through", async () => {
    await expect(assertOk(new Response("{}", { status: 200 }))).resolves.toBeUndefined();
  });

  it("T1234 - carries the problem details of a JSON error body", async () => {
    const response = new Response(JSON.stringify({ type: "urn:quota-exhausted", resetsAt: "2026-12-01T00:00:00Z" }), {
      status: 429,
      headers: { "Content-Type": "application/problem+json" },
    });
    const err = (await assertOk(response).then(() => undefined, (e: unknown) => e)) as TuringHttpError;

    expect(err.problem?.resetsAt).toBe("2026-12-01T00:00:00Z");
  });
});

describe("emptyBody", () => {
  it("is a transport error, because a caller decides the same thing either way", () => {
    const err = emptyBody(URL_, "Chat response");
    expect(err).toBeInstanceOf(TuringTransportError);
    expect(err).not.toBeInstanceOf(TuringHttpError);
    expect(err.message).toContain("no readable body");
  });
});

describe("fetchOrThrow", () => {
  it("wraps a failed request as TuringTransportError, keeping the cause", async () => {
    const refused = new TypeError("Failed to fetch");
    vi.stubGlobal("fetch", vi.fn(async () => { throw refused; }));

    const err = await fetchOrThrow(URL_).catch((e: unknown) => e);

    expect(err).toBeInstanceOf(TuringTransportError);
    expect((err as TuringTransportError).cause).toBe(refused);
  });

  it("turns a non-2xx into TuringHttpError", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response("", { status: 503 })));

    const err = await fetchOrThrow(URL_).catch((e: unknown) => e);

    expect(err).toBeInstanceOf(TuringHttpError);
    expect((err as TuringHttpError).status).toBe(503);
  });

  it("returns the raw Response, so a streaming body is still readable", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response("data: hi\n\n", { status: 200 })));

    const response = await fetchOrThrow(URL_);

    expect(response.body).not.toBeNull();
  });

  it("does NOT swallow the caller's own abort", async () => {
    // A search-as-you-type hook aborts on every keystroke and catches AbortError
    // to tell "I cancelled this" from "this broke".
    const abort = new DOMException("The operation was aborted.", "AbortError");
    vi.stubGlobal("fetch", vi.fn(async () => { throw abort; }));

    const err = await fetchOrThrow(URL_).catch((e: unknown) => e);

    expect(err).toBe(abort);
    expect(err).not.toBeInstanceOf(TuringTransportError);
  });
});
