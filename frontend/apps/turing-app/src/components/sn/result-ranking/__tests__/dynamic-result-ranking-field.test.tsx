import { render, screen, waitFor } from "@testing-library/react";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";

vi.mock("@/services/sn/sn.field.service", () => ({
  TurSNFieldService: class {
    query() {
      return Promise.resolve([
        { id: "1", name: "eventEnd", type: "DATE" },
        { id: "2", name: "category", type: "STRING" },
      ]);
    }
  },
}));

import { DynamicResultRankingFields } from "../dynamic-result-ranking-field";

function Harness({ attribute, value }: Readonly<{ attribute: string; value: string }>) {
  const { control, register } = useForm({
    defaultValues: { turSNRankingConditions: [{ attribute, condition: 1, value }] },
  });
  return (
    <DynamicResultRankingFields
      control={control}
      register={register}
      fieldName={"turSNRankingConditions" as never}
      snSiteId="site-1"
    />
  );
}

describe("DynamicResultRankingFields (T1219)", () => {
  it("offers a DATE field the ranking forms instead of free text", async () => {
    render(<Harness attribute="eventEnd" value="upcoming" />);
    await waitFor(() =>
      expect(screen.getAllByText("forms.resultRankingField.upcomingFirst").length).toBeGreaterThan(0),
    );
    expect(screen.queryByPlaceholderText("forms.common.value")).toBeNull();
  });

  it("keeps the free-text value for a non-DATE field", async () => {
    render(<Harness attribute="category" value="news" />);
    await waitFor(() => expect(screen.getByPlaceholderText("forms.common.value")).toHaveValue("news"));
  });
});
