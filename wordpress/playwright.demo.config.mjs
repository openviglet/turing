// The demo storefront's specs alone (D220), against its own WordPress (run-docker.cmd demo):
//
//   npm run test:e2e:demo
//
// No global setup: the default site is neither seeded nor logged into, so this runs while the
// rest of the stack is in use. setup/demo.setup.mjs seeds the demo only when its catalog is
// missing.
import { defineConfig } from '@playwright/test';
import base, { demoProjects } from './playwright.config.mjs';

export default defineConfig( {
	...base,
	globalSetup: undefined,
	outputDir: 'tests/e2e/artifacts/demo-results',
	reporter: [
		[ 'list' ],
		[
			'html',
			{ outputFolder: 'tests/e2e/artifacts/demo-report', open: 'never' },
		],
	],
	projects: demoProjects,
} );
