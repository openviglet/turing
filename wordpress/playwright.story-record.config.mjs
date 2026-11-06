// The story recorded for the demo video (D204): the same spec as `npm run e2e:story`, at
// 1920×1080 and a human pace, with typing and the pointer visible. It leaves story.webm and
// chapters.json in tests/e2e/artifacts/story/, which bin/story-video.mjs cuts and assembles.
//
//   npm run e2e:story:record && npm run story:video
import { defineConfig } from '@playwright/test';
import story from './playwright.story.config.mjs';

const size = { width: 1920, height: 1080 };

export default defineConfig( {
	...story,
	outputDir: 'tests/e2e/artifacts/story-record-results',
	projects: story.projects.map( ( project ) =>
		'story' === project.name
			? {
					...project,
					metadata: { record: true },
					use: {
						...project.use,
						viewport: size,
						video: { mode: 'on', size },
						launchOptions: { slowMo: 120 },
					},
				}
			: project
	),
} );
