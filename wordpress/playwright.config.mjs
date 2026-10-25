// Browser tests for the plugin's admin screens and the site, against the run-docker.cmd stack
// (WordPress on localhost:8080 with a local Turing). Not run in CI, which has no Turing.
//
//   run-docker.cmd          # once: the stack
//   npm run test:e2e        # seed, log in, run every spec
//   npm run e2e:look -- /wp-admin/admin.php?page=viglet-dumont-health
//
// Drives the installed Microsoft Edge (`channel: msedge`), so no browser download is needed;
// set E2E_CHANNEL=chromium after `npx playwright install chromium` to use that instead.
import { defineConfig } from '@playwright/test';

export const baseURL = process.env.WP_URL || 'http://localhost:8080';
export const authFile = 'tests/e2e/.auth/admin.json';

export default defineConfig( {
	testDir: 'tests/e2e',
	testMatch: '**/*.spec.mjs',
	outputDir: 'tests/e2e/artifacts/results',
	globalSetup: './tests/e2e/global-setup.mjs',
	// The specs share one WordPress and one Turing, and some change their state.
	fullyParallel: false,
	workers: 1,
	timeout: 60_000,
	expect: { timeout: 15_000 },
	reporter: [
		[ 'list' ],
		[
			'html',
			{ outputFolder: 'tests/e2e/artifacts/report', open: 'never' },
		],
	],
	use: {
		baseURL,
		channel: process.env.E2E_CHANNEL || 'msedge',
		viewport: { width: 1280, height: 900 },
		screenshot: 'only-on-failure',
		trace: 'retain-on-failure',
	},
	projects: [
		{
			name: 'admin',
			testMatch: 'admin/**/*.spec.mjs',
			use: { storageState: authFile },
		},
		{ name: 'site', testMatch: 'site/**/*.spec.mjs' },
	],
} );
