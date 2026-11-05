import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig } from "vite";

// Deployed by GitHub Pages at the root of the custom domain https://turing.viglet.org/
// (read back by src/lib/site-url.ts, which prefixes root-absolute links with it).
export default defineConfig({
  base: "/",
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
    dedupe: ["react", "react-dom"],
  },
  // The SSR prerender (scripts/prerender.mjs) bundles the workspace SDK rather
  // than externalizing it, so `node` doesn't have to resolve a linked ESM-only
  // package at prerender time.
  ssr: {
    noExternal: ["@viglet/turing-sdk"],
  },
});
