import React from "react";
import { createRoot, hydrateRoot } from "react-dom/client";
import App from "./App.tsx";
import { ThemeProvider } from "@/components/theme-provider";
import { stripBase } from "@/lib/site-url";
import "./index.css";

// The route is derived from the URL, with the same trivial rule the server used,
// once the base path ("/turing/") is stripped off.
const container = document.getElementById("root")!;
const tree = (
  <React.StrictMode>
    <ThemeProvider defaultTheme="system" storageKey="turing-site-theme">
      <App path={stripBase(globalThis.location.pathname)} />
    </ThemeProvider>
  </React.StrictMode>
);

// In production the route's HTML was prerendered (scripts/prerender.mjs) → hydrate
// it. In dev there's no prerender (empty #root) → mount fresh, avoiding a spurious
// hydration-mismatch warning.
if (container.hasChildNodes()) {
  hydrateRoot(container, tree);
} else {
  createRoot(container).render(tree);
}
