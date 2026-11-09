import { defineConfig, devices } from "@playwright/test";

/**
 * T1273: the browser half of the viglet.org hub contract, against the BUILT
 * site (run `pnpm run build:site` from frontend/ first, so dist/docs exists).
 *
 * Two projects. `contract` is deterministic: local files, no network beyond
 * localhost, so a failure is a product regression. `external` follows the
 * destinations that leave the site with bounded retries; a network error or a
 * bot wall is reported as inconclusive, never as a regression. It runs only
 * when EXTERNAL_LINKS=1.
 *
 * Runs locally and as an opt-in input of the manually dispatched Pages
 * workflow, never on push or pull request.
 */
export default defineConfig({
  testDir: "e2e",
  outputDir: "test-results",
  reporter: [["list"], ["html", { open: "never", outputFolder: "playwright-report" }]],
  fullyParallel: true,
  use: {
    baseURL: "http://localhost:4800",
    ...devices["Desktop Chrome"],
  },
  projects: [
    { name: "contract", testIgnore: /external\.spec\.ts/ },
    { name: "external", testMatch: /external\.spec\.ts/, retries: 2 },
  ],
  webServer: {
    command: "node scripts/serve-dist.mjs 4800",
    url: "http://localhost:4800/",
    reuseExistingServer: true,
  },
});
