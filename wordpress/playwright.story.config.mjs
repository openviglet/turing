// The demo's story (D203): one shopper's visit, demo/story.md played end to end on the demo
// storefront (run-docker.cmd demo) with OPENAI_API_KEY set, and recorded as a video in
// tests/e2e/artifacts/story/.
//
//   npm run e2e:story
//
// Chapters 3 to 6 ask Turing's language model; without one the story stops there and says so.
import { defineConfig } from '@playwright/test';
import base, { demoProjects, demoURL } from './playwright.config.mjs';

export default defineConfig( {
	...base,
	globalSetup: undefined,
	outputDir: 'tests/e2e/artifacts/story-results',
	// Eight chapters, four of them waiting on a model.
	timeout: 600_000,
	expect: { ...base.expect, timeout: 60_000 },
	reporter: [ [ 'list' ] ],
	projects: [
		demoProjects[ 0 ],
		{
			name: 'story',
			testMatch: 'story/**/*.story.mjs',
			dependencies: [ 'demo-setup' ],
			use: {
				baseURL: demoURL,
				video: { mode: 'on', size: { width: 1280, height: 900 } },
			},
		},
	],
} );
