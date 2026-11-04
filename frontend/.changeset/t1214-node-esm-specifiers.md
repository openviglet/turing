---
"@viglet/turing-react-ui": patch
"@viglet/turing-react-sdk": patch
"@viglet/turing-sdk": patch
---

Load in plain Node, not only through a bundler (T1214): relative imports in the emitted JavaScript and type declarations now carry their `.js` extension, and `@viglet/turing-react-sdk` declares `"type": "module"`. A Node script, a test runner in plain Node mode or server rendering that keeps `node_modules` external can now `import` these packages, and a `moduleResolution: "NodeNext"` consumer gets the real types of `@viglet/turing-sdk` instead of `any`.
