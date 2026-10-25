// The live-LLM suite (D174): Turing's generative features asked real questions, against the
// run-docker.cmd stack started with OPENAI_API_KEY. Never part of `npm run test:e2e`, which
// stays free and deterministic; run it on purpose:
//
//   npm run test:e2e:ai
//
// Each spec skips, with its reason, a feature D169's probe reports off. Answers are saved to
// tests/e2e/artifacts/ai/ for a reviewer to read.
import { defineConfig } from '@playwright/test';
import base, { authFile } from './playwright.config.mjs';

export default defineConfig( {
	...base,
	globalSetup: './tests/e2e/ai/global-setup.mjs',
	globalTeardown: './tests/e2e/ai/global-teardown.mjs',
	outputDir: 'tests/e2e/artifacts/ai-results',
	// A model takes seconds per answer, not milliseconds.
	timeout: 120_000,
	expect: { ...base.expect, timeout: 60_000 },
	reporter: [
		[ 'list' ],
		[
			'html',
			{ outputFolder: 'tests/e2e/artifacts/ai-report', open: 'never' },
		],
	],
	projects: [
		{
			name: 'ai',
			testMatch: 'ai/**/*.spec.mjs',
			// The global setup of the base config looks up this project for its login.
			use: { storageState: authFile },
		},
		// global-setup.mjs reads the admin project's options; it must exist here too.
		{ name: 'admin', testMatch: /^$/, use: { storageState: authFile } },
	],
} );
