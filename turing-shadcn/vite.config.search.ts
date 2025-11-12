import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  root: path.resolve(__dirname),
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      input: path.resolve(__dirname, 'search.html'),
      output: {
        manualChunks(id) {
          if (id.includes("node_modules")) {
            return "vendor";
          }
        },
      },
    },
    outDir: "../turing-app/src/main/resources/public/sn/templates",
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    open: '/search.html'
  }
});
