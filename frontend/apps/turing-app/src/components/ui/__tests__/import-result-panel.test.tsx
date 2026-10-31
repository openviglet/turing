import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ImportResultPanel } from "@/components/ui/import-result-panel";
import type { TurImportResult } from "@/models/marketplace/import-result.model";

const imported: TurImportResult = {
  siteImported: true,
  siteName: "demo",
  contentDocuments: 0,
  contentFiles: 0,
  templateImported: false,
  templateName: null,
  templateFiles: 0,
  error: null,
  conflicts: [],
  searchEngineAvailable: true,
  agentSummary: null,
  agentConflicts: [],
};

/** T1192 — a reference the bundle names that resolved to nothing is shown, not dropped. */
describe("ImportResultPanel", () => {
  it("shows each warning the import reported", () => {
    render(<ImportResultPanel result={{ ...imported, warnings: ["SN Site 'demo' names AI Agent 'ghost'"] }} />);

    expect(screen.getByText("SN Site 'demo' names AI Agent 'ghost'")).toBeInTheDocument();
  });

  it("renders a result from a server that sends no warnings", () => {
    render(<ImportResultPanel result={imported} />);

    expect(screen.getByText("demo")).toBeInTheDocument();
  });
});
