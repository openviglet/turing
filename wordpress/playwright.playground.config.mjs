// The WordPress Playground demo (D239), on playground.wordpress.net itself: the published
// blueprint boots the store in the browser against the hosted demo Turing.
//
//   npm run test:e2e:playground
//
// Needs no local stack, only the network: the Pages workflow publishes the blueprint and its
// zips, and turing-demo.viglet.org serves the tarnfell SN site. PLAYGROUND_BLUEPRINT points it
// at another published blueprint.
import { defineConfig } from '@playwright/test';

export default defineConfig( {
	testDir: 'tests/e2e/playground',
	outputDir: 'tests/e2e/artifacts/playground-results',
	// Booting WordPress and WooCommerce in the browser, then seeding the catalog, takes minutes.
	timeout: 10 * 60 * 1000,
	expect: { timeout: 30 * 1000 },
	retries: 0,
	reporter: [ [ 'list' ] ],
	use: {
		headless: true,
		viewport: { width: 1280, height: 900 },
	},
} );
