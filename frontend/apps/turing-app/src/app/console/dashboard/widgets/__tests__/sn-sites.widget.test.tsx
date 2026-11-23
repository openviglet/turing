import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { SNSitesWidget } from "../sn-sites.widget";

// T1343 — the widget flags a site whose last day of indexing dropped or refused values.

const sites = vi.hoisted(() => ({
  value: [] as Array<{
    site: { id: string; name: string };
    status: {
      queue: number;
      documents: number;
      searchEngineAvailable: boolean;
      droppedValues?: number;
      refusedDocuments?: number;
    } | null;
  }>,
}));

vi.mock("@/api/queries/sn-site.queries", () => ({
  useSnSitesWithStatus: () => ({ data: sites.value, isLoading: false, refetch: vi.fn() }),
}));

vi.mock("../widget-wrapper", () => ({
  WidgetWrapper: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

function renderWidget() {
  return render(
    <MemoryRouter>
      <SNSitesWidget onHide={vi.fn()} />
    </MemoryRouter>,
  );
}

describe("SNSitesWidget indexing issues", () => {
  it("flags a site whose engine dropped or refused values in the last day", () => {
    sites.value = [
      {
        site: { id: "s1", name: "jogos" },
        status: { queue: 0, documents: 3669, searchEngineAvailable: true, droppedValues: 42, refusedDocuments: 3 },
      },
    ];

    renderWidget();

    const badge = screen.getByText("dashboard.widgets.snSites.indexingIssues");
    expect(badge).toHaveAttribute("title", "dashboard.widgets.snSites.indexingIssuesTitle");
  });

  it("shows no flag when nothing was dropped or refused, or the server predates the counts", () => {
    sites.value = [
      {
        site: { id: "s1", name: "clean" },
        status: { queue: 0, documents: 10, searchEngineAvailable: true, droppedValues: 0, refusedDocuments: 0 },
      },
      { site: { id: "s2", name: "older" }, status: { queue: 0, documents: 5, searchEngineAvailable: true } },
    ];

    renderWidget();

    expect(screen.queryByText("dashboard.widgets.snSites.indexingIssues")).toBeNull();
  });
});
