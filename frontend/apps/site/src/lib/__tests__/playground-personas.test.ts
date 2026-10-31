import { describe, expect, it, vi } from "vitest";
import type { TuringClient } from "@viglet/turing-sdk";

import { loadSitePersonas } from "@/lib/playground-personas";

/** A client whose GET answers `reply` (or rejects with it, when it is an Error). */
function clientAnswering(reply: unknown): TuringClient {
  const get = vi.fn(() => (reply instanceof Error ? Promise.reject(reply) : Promise.resolve(reply)));
  return { get } as unknown as TuringClient;
}

/** T1193 — the playground offers the personas the demo site lists, never a pinned guess. */
describe("loadSitePersonas", () => {
  it("takes the options from the site's persona endpoint", async () => {
    const client = clientAnswering([
      { id: "persona-enterprise-buyer", name: "Enterprise Buyer", description: "Scans for value." },
      { id: "persona-new", name: "New Reader", description: null },
    ]);

    expect(await loadSitePersonas(client, "turing-docs")).toEqual([
      { id: "persona-enterprise-buyer", name: "Enterprise Buyer", description: "Scans for value." },
      { id: "persona-new", name: "New Reader", description: undefined },
    ]);
    expect(client.get).toHaveBeenCalledWith("/sn/turing-docs/personas", expect.anything());
  });

  it("offers nothing when the site lists none, so the picker hides", async () => {
    expect(await loadSitePersonas(clientAnswering([]), "turing-docs")).toEqual([]);
  });

  it("offers nothing when the call fails, rather than ids the backend may refuse", async () => {
    expect(await loadSitePersonas(clientAnswering(new Error("401")), "turing-docs")).toEqual([]);
  });
});
