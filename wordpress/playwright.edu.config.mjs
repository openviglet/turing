// The demo school's specs alone (D325), against its own WordPress (run-docker.cmd demo edu; with
// DEMO_LOCALE=pt_BR, Escola Lumen from run-docker.cmd demo edu-pt):
//
//   npm run test:e2e:edu
//
// No global setup: the default site is neither seeded nor logged into, so this runs while the
// rest of the stack is in use. setup/demo-edu.setup.mjs seeds the school again, from today.
import { defineConfig } from '@playwright/test';
import base, { schoolProjects } from './playwright.config.mjs';

export default defineConfig( {
	...base,
	globalSetup: undefined,
	outputDir: 'tests/e2e/artifacts/demo-edu-results',
	reporter: [
		[ 'list' ],
		[
			'html',
			{
				outputFolder: 'tests/e2e/artifacts/demo-edu-report',
				open: 'never',
			},
		],
	],
	projects: schoolProjects,
} );
