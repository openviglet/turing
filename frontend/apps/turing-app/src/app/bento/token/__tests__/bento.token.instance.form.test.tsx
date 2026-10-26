import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

const update = vi.fn();

vi.mock("@/api/queries/token-instance.queries", () => ({
  useCreateTokenInstance: () => ({ mutateAsync: vi.fn() }),
  useUpdateTokenInstance: () => ({ mutateAsync: update }),
}));

import { BENTO_TOKEN_FORM_ID, BentoTokenInstanceForm } from "../bento.token.instance.form";

/** T1133 — the token form edits the key's partition and sends it back on save. */
describe("BentoTokenInstanceForm — partition", () => {
  it("shows the key's partition and saves an edited value", async () => {
    update.mockResolvedValue({ id: "t1" });
    render(
      <MemoryRouter>
        <BentoTokenInstanceForm
          isNew={false}
          staged={{ title: "Shared site A", description: "" }}
          value={{
            id: "t1",
            token: "",
            title: "Shared site A",
            description: "",
            partitionField: "site_key",
            partitionValue: "a.example.org",
          }}
        />
      </MemoryRouter>,
    );

    expect(screen.getByDisplayValue("site_key")).toBeInTheDocument();
    const value = screen.getByDisplayValue("a.example.org");
    fireEvent.change(value, { target: { value: "b.example.org" } });
    fireEvent.submit(document.getElementById(BENTO_TOKEN_FORM_ID) as HTMLFormElement);

    await waitFor(() => expect(update).toHaveBeenCalled());
    expect(update.mock.calls[0][0]).toMatchObject({
      partitionField: "site_key",
      partitionValue: "b.example.org",
    });
  });
});
