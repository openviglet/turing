import { describe, expect, it, vi } from "vitest";
import { createResearchController } from "../controllers/research.js";
import type { TuringClient } from "../client.js";

/** T1220 — the research controller consumes the `/research` stream's named SSE frames. */
describe("createResearchController (T1220)", () => {
  function frame(event: string, payload: unknown): string {
    return `event:${event}\ndata:${JSON.stringify(payload)}\n\n`;
  }

  function sse(frames: string[]): Response {
    return new Response(frames.join(""), { status: 200, headers: { "Content-Type": "text/event-stream" } });
  }

  const stream = () =>
    sse([
      frame("plan", {
        summary: "public health at the school",
        steps: [
          { index: 1, question: "Research?", query: "research health" },
          { index: 2, question: "Courses?", query: "courses health" },
        ],
      }),
      frame("step", { index: 1, question: "Research?", hits: 2, sources: [1, 2] }),
      frame("step", { index: 2, question: "Courses?", hits: 0, sources: [] }),
      frame("section", { title: "Research", text: "It studied the SUS [1][2].", citations: [1, 2] }),
      frame("uncovered", { items: ["Courses?"] }),
      frame("timeline", {
        entries: [
          { date: "2019-03-10", title: "B", url: "ub", source: 2 },
          { date: "2024-05-01", title: "A", url: "ua", source: 1 },
        ],
      }),
      frame("sources", { groups: { research: [{ rank: 1, id: "a" }, { rank: 2, id: "b" }] } }),
      frame("done", { sections: 1, sources: 2 }),
    ]);

  it("streams the plan, steps, sections, uncovered and grouped sources into state", async () => {
    const fetchRaw = vi.fn().mockResolvedValueOnce(stream());
    const client = { fetchRaw } as unknown as TuringClient;
    const controller = createResearchController(client, { site: "acervo", locale: "pt_BR" });

    await controller.ask("What on public health?", { filters: { site_key: ["main"] }, maxSteps: 4 });
    const state = controller.getState();

    expect(state.status).toBe("success");
    expect(state.summary).toBe("public health at the school");
    expect(state.steps.map((s) => s.hits)).toEqual([2, 0]);
    expect(state.sections[0].citations).toEqual([1, 2]);
    expect(state.uncovered).toEqual(["Courses?"]);
    expect(state.timeline.map((e) => e.source)).toEqual([2, 1]);
    expect(Object.keys(state.sourceGroups)).toEqual(["research"]);

    const [path, init] = fetchRaw.mock.calls[0];
    expect(path).toBe("/sn/acervo/research");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body)).toEqual({
      question: "What on public health?",
      locale: "pt_BR",
      filters: { site_key: ["main"] },
      maxSteps: 4,
    });
  });

  it("lands a refusal in error without throwing", async () => {
    const fetchRaw = vi.fn().mockResolvedValueOnce(sse([frame("error", { message: "AI is not enabled" })]));
    const client = { fetchRaw } as unknown as TuringClient;
    const controller = createResearchController(client, { site: "acervo" });

    await controller.ask("x");

    expect(controller.getState()).toMatchObject({ status: "error", error: "AI is not enabled", sections: [] });
  });

  it("ignores a blank question", async () => {
    const fetchRaw = vi.fn();
    const controller = createResearchController({ fetchRaw } as unknown as TuringClient, { site: "s" });

    await controller.ask("  ");

    expect(fetchRaw).not.toHaveBeenCalled();
  });
});
