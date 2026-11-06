import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import type { TurSNSiteField } from "@/models/sn/sn-site-field.model";

/**
 * T1248 — the Range select is a date-field setting: the faceted-field form shows
 * it for a DATE field and hides it for any other type.
 */

vi.mock("@/api/queries/locale.queries", () => ({
  useGlobalLocales: () => ({ data: [] }),
}));
vi.mock("@/services/sn/sn.field.type.service", () => ({
  TurSNFieldTypeService: class {
    query = () => Promise.resolve([]);
  },
}));
vi.mock("@/services/sn/sn.field.service", () => ({
  TurSNFieldService: class {
    create = vi.fn();
    update = vi.fn();
  },
}));

import { SNSiteFacetedFieldForm } from "../sn.site.faceted.field.form";

function renderForm(type: string) {
  const field = {
    id: "field-1",
    name: "publication_date",
    type,
    facet: 1,
    facetRange: "MONTH",
  } as unknown as TurSNSiteField;
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <SNSiteFacetedFieldForm snSiteId="site-1" snField={field} isNew={false} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("SNSiteFacetedFieldForm — Range only for date fields (T1248)", () => {
  it("shows the Range select for a DATE field", async () => {
    renderForm("DATE");

    expect(await screen.findByText("forms.snFacetedField.range")).toBeInTheDocument();
  });

  it("hides the Range select for a STRING field", async () => {
    renderForm("STRING");

    expect(await screen.findByText("forms.snFacetedField.facetSort")).toBeInTheDocument();
    expect(screen.queryByText("forms.snFacetedField.range")).not.toBeInTheDocument();
  });
});
