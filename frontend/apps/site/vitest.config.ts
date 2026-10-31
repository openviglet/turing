import path from "node:path";
import { defineConfig } from "vitest/config";

/**
 * Standalone test config for the marketing site (T1193). Separate from
 * `vite.config.ts`, which is the client + SSR build, so the test run carries no
 * build-only options. Node environment: the tests cover the site's logic, not
 * its rendering.
 */
export default defineConfig({
  resolve: {
    alias: { "@": path.resolve(__dirname, "src") },
  },
  test: {
    include: ["src/**/*.test.ts"],
    clearMocks: true,
    restoreMocks: true,
  },
});
