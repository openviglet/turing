import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import type { TurSNSite } from "@/models/sn/sn-site.model";

/**
 * T1151 — a site configured for RAG whose store holds none of its content tells
 * the admin to reindex, and a site on the Default AI Agent gets the reindex
 * action it needs to fix that.
 */

const { reindexGenAi, capability } = vi.hoisted(() => ({
  reindexGenAi: vi.fn(),
  capability: { reason: "NOT_EMBEDDED" },
}));

vi.mock("@/api/queries/sn-site.queries", () => ({
  useUpdateSnSite: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));
vi.mock("@/api/queries/persona.queries", () => ({
  usePersonas: () => ({ data: [] }),
}));
vi.mock("@/api/queries/sn-ai-capabilities.queries", () => ({
  NOT_EMBEDDED: "NOT_EMBEDDED",
  useSnAiCapabilities: () => ({
    data: { similarByVector: { enabled: capability.reason === "NONE", reason: capability.reason } },
  }),
}));
vi.mock("@/services/agent/ai-agent.service", () => ({
  TurAIAgentService: class {
    query = () =>
      Promise.resolve([{ id: "default-agent", title: "Default", enabled: 1, ragEnabled: true }]);
  },
}));
vi.mock("@/services/se/se.service", () => ({
  TurSEInstanceService: class {
    query = () => Promise.resolve([]);
  },
}));
vi.mock("@/services/embedding/embedding-model.service", () => ({
  TurEmbeddingModelService: class {
    query = () => Promise.resolve([]);
  },
}));
vi.mock("@/services/system/global-settings.service", () => ({
  TurGlobalSettingsService: class {
    query = () => Promise.resolve({ defaultLlmId: "", defaultAiAgentId: "default-agent" });
  },
}));
vi.mock("@/services/sn/sn.service", () => ({
  TurSNSiteService: class {
    getRagCores = () => Promise.resolve([]);
    getActiveReindex = () => Promise.resolve(null);
    reindexGenAi = reindexGenAi;
    subscribeExportProgress = () => Promise.resolve();
  },
}));
vi.mock("@viglet/turing-react-sdk", () => ({
  postLlmChat: () => Promise.resolve(""),
}));

import { SNSiteGenAiForm } from "../sn.site.genai.form";

function renderForm() {
  // No agent of its own: the site answers through the Default AI Agent.
  const site = {
    id: "site-1",
    name: "catalog",
    turSNSiteGenAi: { id: "genai-1", turAIAgent: null },
  } as unknown as TurSNSite;
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <SNSiteGenAiForm snSite={site} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("SNSiteGenAiForm — content not embedded (T1151)", () => {
  it("asks a Default-agent site with no vectors to reindex, and starts the reindex", async () => {
    capability.reason = "NOT_EMBEDDED";
    reindexGenAi.mockReset();
    reindexGenAi.mockResolvedValue("task-1");
    renderForm();

    expect(await screen.findByTestId("sn-genai-not-embedded")).toHaveTextContent("forms.snGenai.notEmbedded");
    fireEvent.click(screen.getByText("forms.snGenai.reindexAll"));

    await waitFor(() => expect(reindexGenAi).toHaveBeenCalledWith("site-1", expect.any(String)));
  });

  it("says nothing once the store holds the site's content", async () => {
    capability.reason = "NONE";
    renderForm();

    expect(await screen.findByText("forms.snGenai.reindexAll")).toBeInTheDocument();
    expect(screen.queryByTestId("sn-genai-not-embedded")).not.toBeInTheDocument();
  });
});
