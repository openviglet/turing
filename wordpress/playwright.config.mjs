// Browser tests for the plugin's admin screens and the site, against the run-docker.cmd stack
// (WordPress on localhost:8080 with a local Turing). CI runs it too, in the workflow's e2e job.
//
//   run-docker.cmd          # once: the stack
//   run-docker.cmd demo     # once: the demo storefront beside it, on localhost:38082
//   npm run test:e2e        # seed, log in, run every spec
//   npm run test:e2e:demo   # the demo storefront's specs alone
//   npm run e2e:look -- /wp-admin/admin.php?page=viglet-dumont-health
//
// Drives the installed Microsoft Edge (`channel: msedge`), so no browser download is needed;
// set E2E_CHANNEL=chromium after `npx playwright install chromium` to use that instead.
import { defineConfig } from '@playwright/test';
import { demoSite } from './bin/demo-site.mjs';

export const baseURL = process.env.WP_URL || 'http://localhost:8080';
export const authFile = 'tests/e2e/.auth/admin.json';

// The site again under other themes, then the whole plugin in Portuguese. One WordPress serves
// them all, so each variant is a `switch:` project (setup/switch.setup.mjs) that changes the
// stack, chained by dependencies so nothing else runs while it holds: the default projects
// first, then each variant in this order. A failure skips the variants after it.
const english = [ 'admin', 'site', 'site-tablet', 'site-phone' ];
const variantSteps = [
	{
		name: 'site-classic',
		// Pinned like Ollie, so a theme update cannot move the baselines (D301).
		metadata: { theme: 'twentytwentyone', version: '2.9' },
		testMatch: 'site/search.spec.mjs',
	},
	{
		// A third-party block theme, pinned so its markup does not move under the baselines.
		name: 'site-ollie',
		metadata: { theme: 'ollie', version: '1.6.5' },
		testMatch: 'site/search.spec.mjs',
	},
	{
		name: 'pt-br',
		metadata: { theme: 'twentytwentyfive', locale: 'pt_BR' },
		testMatch: 'i18n/**/*.spec.mjs',
		use: { storageState: authFile },
	},
	{
		// A shop (D173): WooCommerce on, its products seeded and indexed, back in English.
		name: 'woo',
		metadata: {
			theme: 'twentytwentyfive',
			locale: 'en_US',
			plugins: [ 'woocommerce' ],
			seed: '/e2e/woo-seed.php',
		},
		testMatch: 'woo/**/*.spec.mjs',
	},
];

// The demo storefront (D194) on its own WordPress (run-docker.cmd demo, D219): its specs need no
// switch and leave the site above alone (D220). `npm run test:e2e:demo` runs them alone.
// Two demo stores run side by side (D283): Tarnfell in English, and with DEMO_LOCALE=pt_BR
// Recanto da Serra, each with the WP-CLI container that reaches its WordPress. Each is read from
// its descriptor, demo/sites/en.json or pt.json (D325).
const demoPortuguese = 'pt_BR' === process.env.DEMO_LOCALE;
export const demoStore = demoSite( demoPortuguese ? 'pt' : 'en' );
export const demoURL = process.env.WP_DEMO_URL || demoStore.url;
export const demoCli = demoStore.cli;
export const demoProjects = [
	{ name: 'demo-setup', testMatch: 'setup/demo.setup.mjs' },
	{
		name: 'demo',
		testMatch: `${ demoStore.specs }/**/*.spec.mjs`,
		dependencies: [ 'demo-setup' ],
		use: { baseURL: demoURL },
	},
];
// The school beside the stores (D325, run-docker.cmd demo edu): Lumen School in English, and with
// DEMO_LOCALE=pt_BR Escola Lumen (demo/sites/edu.json, edu-pt.json). Its specs run in their own
// config (npm run test:e2e:edu), never in the full run: a laptop that never started the school
// still runs everything else.
export const demoSchool = demoSite( demoPortuguese ? 'edu-pt' : 'edu' );
export const schoolURL = process.env.WP_DEMO_EDU_URL || demoSchool.url;
export const schoolProjects = [
	{ name: 'demo-edu-setup', testMatch: 'setup/demo-edu.setup.mjs' },
	{
		name: 'demo-edu',
		testMatch: `${ demoSchool.specs }/**/*.spec.mjs`,
		dependencies: [ 'demo-edu-setup' ],
		use: { baseURL: schoolURL },
	},
];
const variants = variantSteps.flatMap( ( step, i ) => [
	{
		name: `switch:${ step.name }`,
		testMatch: 'setup/switch.setup.mjs',
		metadata: step.metadata,
		dependencies: i ? [ variantSteps[ i - 1 ].name ] : english,
	},
	{ ...step, dependencies: [ `switch:${ step.name }` ] },
] );

export default defineConfig( {
	testDir: 'tests/e2e',
	testMatch: '**/*.spec.mjs',
	outputDir: 'tests/e2e/artifacts/results',
	globalSetup: './tests/e2e/global-setup.mjs',
	// The specs share one WordPress and one Turing, and some change their state.
	fullyParallel: false,
	workers: 1,
	timeout: 60_000,
	expect: {
		timeout: 15_000,
		// Visual regression: a few pixels absorb antialiasing, not a new or moved element. A
		// ratio was tried and was too loose: 1% of a results block let a whole sort control
		// and every changed date through. Refresh the baselines on purpose:
		// npm run test:e2e -- --update-snapshots
		toHaveScreenshot: {
			maxDiffPixels: 50,
			animations: 'disabled',
			caret: 'hide',
			// A result's crumb names the host, port included: kept in the layout, not its text,
			// so the baselines hold on whatever port the stack runs (WP_PORT).
			stylePath: './tests/e2e/screenshot.css',
		},
	},
	// Fonts render differently per OS, so each platform keeps its own baselines.
	snapshotPathTemplate:
		'tests/e2e/snapshots/{platform}/{projectName}/{arg}{ext}',
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
		// The site at desktop, plus the visitor's specs again on a tablet and a phone. Keyboard
		// paths and timing budgets are desktop-only: a phone has neither Tab nor that network.
		{ name: 'site', testMatch: 'site/**/*.spec.mjs' },
		{
			name: 'site-tablet',
			testMatch: 'site/search.spec.mjs',
			use: { viewport: { width: 820, height: 1180 }, isMobile: true },
		},
		{
			name: 'site-phone',
			testMatch: 'site/search.spec.mjs',
			use: { viewport: { width: 390, height: 844 }, isMobile: true },
		},
		...variants,
		// CI starts the test stack alone (bin/run-docker.sh), not the demo stores, so their
		// specs run on a laptop and in their own configs (D182).
		...( process.env.CI ? [] : demoProjects ),
		// The multisite network beside the single site (run-docker.cmd multisite); its specs
		// address both sites by URL, so it needs no switch and depends on nothing.
		...( process.env.E2E_MULTISITE
			? [ { name: 'network', testMatch: 'network/**/*.spec.mjs' } ]
			: [] ),
	],
} );
