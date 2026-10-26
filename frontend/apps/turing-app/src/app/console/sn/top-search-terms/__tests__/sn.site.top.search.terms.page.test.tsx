import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const useSnTopTerms = vi.fn();
const useSnClickThrough = vi.fn();

vi.mock("@/api/queries/sn-site-metrics.queries", () => ({
  useSnTopTerms: () => useSnTopTerms(),
  useSnClickThrough: () => useSnClickThrough(),
}));

vi.mock("@/hooks/use-sub-page-breadcrumb", () => ({
  useSubPageBreadcrumb: () => undefined,
}));

import SNSiteTopSearchTermsPage from "../sn.site.top.search.terms.page";

/** T1131 — the top-terms page shows each term's click-through rate. */
describe("SNSiteTopSearchTermsPage — click-through", () => {
  beforeEach(() => {
    useSnTopTerms.mockReturnValue({
      data: {
        topTerms: [
          { term: "workshop", total: 10, numFound: 8 },
          { term: "pricing", total: 4, numFound: 2 },
        ],
        totalTermsPeriod: 14,
        variationPeriod: 0,
      },
      isLoading: false,
      isError: false,
    });
  });

  function renderPage() {
    render(
      <MemoryRouter initialEntries={["/sn/site-1/top-terms/this-month"]}>
        <Routes>
          <Route path="/sn/:id/top-terms/:period?" element={<SNSiteTopSearchTermsPage baseRoute="/sn" />} />
        </Routes>
      </MemoryRouter>,
    );
  }

  it("adds a CTR column joined by term, and lists the terms never opened", () => {
    useSnClickThrough.mockReturnValue({
      data: {
        ctr: 0.2142,
        terms: [{ term: "workshop", searches: 10, clicks: 3, ctr: 0.3, avgPosition: 1.5, avgResults: 8 }],
        neverClicked: [{ term: "pricing", searches: 4, clicks: 0, ctr: 0, avgPosition: null, avgResults: 2 }],
      },
    });

    renderPage();

    expect(screen.getByText("sn.topSearchTerms.ctr")).toBeInTheDocument();
    expect(screen.getByText("30%")).toBeInTheDocument();
    expect(screen.getByText("—")).toBeInTheDocument();
    expect(screen.getByText("sn.topSearchTerms.neverClicked")).toBeInTheDocument();
  });

  it("shows no CTR column when the click report is unavailable", () => {
    useSnClickThrough.mockReturnValue({ data: undefined });

    renderPage();

    expect(screen.getByText("workshop")).toBeInTheDocument();
    expect(screen.queryByText("sn.topSearchTerms.ctr")).not.toBeInTheDocument();
  });
});
