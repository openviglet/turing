// The story recorded at a phone's width for vertical cuts (D280): the same spec and the same
// takes as playwright.story-record.config.mjs, laid out 432 CSS pixels wide and painted into a
// 1080×1920 frame, so a 9:16 cut shows each result whole instead of cropping the desktop one.
// It leaves story.webm and chapters.json in tests/e2e/artifacts/story-vertical/, beside the
// desktop recording rather than over it.
//
// A device scale factor would not do it: Playwright records video at the viewport's CSS size,
// so a 432×768 viewport at 2.5x is filmed at 432×768 in a corner of the frame. The viewport is
// the frame instead, emulated as a phone, and the spec sets each page's meta viewport to
// `width=432` (`layoutWidth`), which a phone browser scales up to fill the screen.
//
//   npm run e2e:story:vertical && node bin/story-video.mjs --vertical --chapter 2
import { defineConfig } from '@playwright/test';
import record from './playwright.story-record.config.mjs';

const size = { width: 1080, height: 1920 };

export default defineConfig( {
	...record,
	outputDir: 'tests/e2e/artifacts/story-vertical-results',
	projects: record.projects.map( ( project ) =>
		'story' === project.name
			? {
					...project,
					metadata: {
						record: true,
						vertical: true,
						layoutWidth: 432,
					},
					use: {
						...project.use,
						viewport: size,
						isMobile: true,
						video: { mode: 'on', size },
					},
				}
			: project
	),
} );
