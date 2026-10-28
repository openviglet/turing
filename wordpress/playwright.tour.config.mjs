// The recorded tour (D179), on the demo storefront (run-docker.cmd demo): it walks the showcase
// with "How it works" on and leaves a video and one screenshot per Turing feature in
// tests/e2e/artifacts/tour/, the source of the docs' per-feature screenshots.
//
//   npm run e2e:tour
//
// It asks the stack as it is: a feature the stack has no language model for stays hidden and
// gets no screenshot, so the keyless part runs without OPENAI_API_KEY.
import { defineConfig } from '@playwright/test';
import base, { demoProjects, demoURL } from './playwright.config.mjs';

export default defineConfig( {
	...base,
	globalSetup: undefined,
	outputDir: 'tests/e2e/artifacts/tour-results',
	// A language model answers in seconds.
	timeout: 180_000,
	reporter: [ [ 'list' ] ],
	projects: [
		demoProjects[ 0 ],
		{
			name: 'tour',
			testMatch: 'tour/**/*.tour.mjs',
			dependencies: [ 'demo-setup' ],
			use: {
				baseURL: demoURL,
				video: { mode: 'on', size: { width: 1280, height: 900 } },
			},
		},
	],
} );
