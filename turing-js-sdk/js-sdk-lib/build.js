const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const distPath = path.resolve(__dirname, "dist");

// Step 1: Clean previous build
if (fs.existsSync(distPath)) {
  fs.rmSync(distPath, { recursive: true, force: true });
  console.log("🧹 Cleaned dist folder");
}

// Step 2: Compile TypeScript
try {
  execSync("npx tsc", { stdio: "inherit" });
  console.log("✅ TypeScript compiled successfully");
} catch (err) {
  console.error("❌ TypeScript compilation failed");
  process.exit(1);
}

// Step 3: Confirm output
if (fs.existsSync(path.join(distPath, "index.js"))) {
  console.log("📦 Build complete: dist/index.js is ready for runtime");
} else {
  console.error("⚠️ Build incomplete: index.js not found");
}
