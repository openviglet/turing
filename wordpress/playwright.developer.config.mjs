// The developer walkthrough (D281), recorded for the technical video: on the fresh store
// (run-docker.cmd fresh), a WordPress the plugin was never installed on, it installs the release
// zip in wp-admin, connects it on the Setup page, chooses what to index, runs the first index,
// searches the storefront and opens the insights. 1920×1080 at a human pace, like the story. It
// leaves walkthrough.webm and chapters.json in tests/e2e/artifacts/developer/.
//
//   run-docker.cmd fresh reset && run-docker.cmd fresh && npm run e2e:developer:record
//
// It installs the plugin, so a second run needs the fresh store reset first; it refuses a store
// that already has it.
import { defineConfig } from '@playwright/test';
import base from './playwright.config.mjs';

export const freshURL = process.env.WP_FRESH_URL || 'http://localhost:38084';
const size = { width: 1920, height: 1080 };

export default defineConfig( {
	...base,
	globalSetup: undefined,
	outputDir: 'tests/e2e/artifacts/developer-results',
	timeout: 600_000,
	expect: { ...base.expect, timeout: 60_000 },
	reporter: [ [ 'list' ] ],
	projects: [
		{
			name: 'developer',
			testMatch: 'developer/**/*.developer.mjs',
			metadata: { record: true },
			use: {
				baseURL: freshURL,
				// A step that cannot act fails in half a minute, not at the test's ten.
				actionTimeout: 30_000,
				viewport: size,
				video: { mode: 'on', size },
				launchOptions: { slowMo: 120 },
			},
		},
	],
} );
