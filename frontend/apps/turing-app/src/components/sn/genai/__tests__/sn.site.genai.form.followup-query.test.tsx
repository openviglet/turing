import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { TurSNSite } from "@/models/sn/sn-site.model";

/**
 * T1157: the follow-up question mode. A site that never picked one submits
 * LATEST_ONLY (retrieval as before), and a picked mode submits its enum value.
 */

const mutateAsync = vi.fn();

vi.mock("@/api/queries/sn-site.queries", () => ({
  useUpdateSnSite: () => ({ mutateAsync, isPending: false }),
}));

vi.mock("@/api/queries/persona.queries", () => ({
  usePersonas: () => ({ data: [] }),
}));

// The form eagerly loads reference data on mount; none of it is relevant to the
// planning card, so every service resolves empty.
vi.mock("@/services/agent/ai-agent.service", () => ({
  TurAIAgentService: class {
    query = () => Promise.resolve([]);
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
    query = () => Promise.resolve({ defaultLlmId: "", defaultAiAgentId: "" });
  },
}));
vi.mock("@/services/sn/sn.service", () => ({
  TurSNSiteService: class {
    getRagCores = () => Promise.resolve([]);
    getActiveReindex = () => Promise.resolve(null);
    subscribeToReindex = () => () => {};
  },
}));
vi.mock("@viglet/turing-react-sdk", () => ({
  postLlmChat: () => Promise.resolve(""),
}));

import { SNSiteGenAiForm } from "../sn.site.genai.form";

function renderForm(genAi: Record<string, unknown> = {}) {
  const site = {
    id: "site-1",
    name: "catalog",
    description: "A catalog",
    turSNSiteGenAi: { id: "genai-1", ...genAi },
  } as unknown as TurSNSite;

  return render(
    <QueryClientProvider client={new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })}>
      <MemoryRouter>
        <SNSiteGenAiForm snSite={site} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

/** The submitted `turSNSiteGenAi` payload from the single save call. */
async function submitAndReadPayload() {
  fireEvent.click(screen.getAllByText("forms.formActions.saveChanges")[0]);
  await waitFor(() => expect(mutateAsync).toHaveBeenCalled());
  return mutateAsync.mock.calls[0][0].turSNSiteGenAi;
}

describe("SNSiteGenAiForm — follow-up questions (T1157)", () => {
  beforeEach(() => {
    mutateAsync.mockReset();
    mutateAsync.mockResolvedValue({ id: "site-1" });
  });

  it("submits LATEST_ONLY for a site that never picked a mode", async () => {
    renderForm();

    expect(screen.getByText("forms.snGenai.followupQueryLatestHint")).toBeInTheDocument();
    const payload = await submitAndReadPayload();

    expect(payload.followupQueryMode).toBe("LATEST_ONLY");
  });

  it("pins the picked mode on save", async () => {
    renderForm();

    fireEvent.click(screen.getByText("forms.snGenai.followupQueryRewrite"));
    expect(screen.getByText("forms.snGenai.followupQueryRewriteHint")).toBeInTheDocument();
    const payload = await submitAndReadPayload();

    expect(payload.followupQueryMode).toBe("REWRITE");
  });

  it("shows the persisted mode's hint", () => {
    renderForm({ followupQueryMode: "JOINED" });

    expect(screen.getByText("forms.snGenai.followupQueryJoinedHint")).toBeInTheDocument();
  });
});