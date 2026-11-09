// The developer walkthrough (D281): what someone putting the plugin on a client's site does, on
// a WordPress it was never installed on (run-docker.cmd fresh). Install the release zip, connect
// it to Viglet Turing on the Setup page, choose what to index, run the first index, see the
// storefront search through it and the insights it keeps. Each chapter is a step, timed into
// chapters.json beside walkthrough.webm for the video, as the shopper's story is.
import { execFileSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { expect, test } from '@playwright/test';
import { narrationFor, recording } from '../marketing.mjs';

// The walkthrough's narration (demo/narration/developer.json in the marketing repository, loaded
// when a recording starts): how long each line runs, so the recording paces the action to it. A
// line with no current take runs 0 s.
let narration = null;
const takeLength = ( name ) => {
	const file = narration.takeFile( 'en', name );
	const text = Object.fromEntries( narration.lines() )[ name ]?.en;
	if ( ! narration.current( file, text ) ) {
		return 0;
	}
	return Number(
		execFileSync( 'ffprobe', [
			'-v',
			'error',
			'-show_entries',
			'format=duration',
			'-of',
			'default=noprint_wrappers=1:nokey=1',
			file,
		] )
			.toString()
			.trim()
	);
};

const turingURL = process.env.TURING_URL || 'http://localhost:2700';
const turingPassword = process.env.TURING_ADMIN_PASSWORD || 'dumont-dev';
// The SN site the Setup page creates. Deleted before each run, so the video shows it created.
const snSite = process.env.SN_FRESH_SITE || 'tarnfell-shop';
// Where the plugin reaches Turing from inside its container: the compose network's name. A
// production site types its Turing's public address here.
const connectURL = process.env.FRESH_TURING_URL || 'http://turing:2700';
// The recording goes to the marketing repository, which makes the video from it.
const out = recording( 'developer' );
const query = 'waterproof jacket';

/**
 * The release zip, laid out as bin/build-zip.sh lays it (one viglet-turing-search/ folder: src/
 * and composer.json), built here because that script needs `zip`, which a Windows shell lacks;
 * Windows' own tar writes the zip instead.
 */
function releaseZip() {
	// Outside the plugin's tree: a copy of src/ under it is a second copy of every class to phpcs.
	const stage = join( tmpdir(), 'viglet-turing-search-developer-zip' );
	rmSync( stage, { recursive: true, force: true } );
	mkdirSync( join( stage, 'viglet-turing-search' ), { recursive: true } );
	cpSync( 'src', join( stage, 'viglet-turing-search' ), { recursive: true } );
	cpSync(
		'composer.json',
		join( stage, 'viglet-turing-search', 'composer.json' )
	);
	const zip = join( stage, 'viglet-turing-search.zip' );
	if ( 'win32' === process.platform ) {
		execFileSync(
			join(
				process.env.SystemRoot || 'C:\\Windows',
				'System32',
				'tar.exe'
			),
			[ '-a', '-cf', zip, 'viglet-turing-search' ],
			{ cwd: stage }
		);
	} else {
		execFileSync( 'zip', [ '-qr', zip, 'viglet-turing-search' ], {
			cwd: stage,
		} );
	}
	return zip;
}

/**
 * A logged-in session on the local Turing, as the console holds one (bin/turing-setup.ps1 does
 * the same): its XSRF cookie goes back as a header on every write.
 *
 * @param {import('@playwright/test').PlaywrightWorkerArgs['playwright']} playwright The library.
 */
async function turingSession( playwright ) {
	const api = await playwright.request.newContext( { baseURL: turingURL } );
	const xsrf = async () =>
		(
			( await api.storageState() ).cookies.find(
				( cookie ) => 'XSRF-TOKEN' === cookie.name
			) || {}
		).value || '';
	const call = async ( method, path, data ) => {
		const response = await api.fetch( path, {
			method,
			data,
			headers: {
				'X-XSRF-TOKEN': await xsrf(),
				Accept: 'application/json',
			},
		} );
		expect( response.ok(), `${ method } ${ path }` ).toBeTruthy();
		const text = await response.text();
		return text ? JSON.parse( text ) : null;
	};
	await api.get( '/api/discovery' );
	await call( 'POST', '/api/login', {
		username: 'admin',
		password: turingPassword,
	} );
	return { api, call };
}

let apiKey = '';
let zip = '';

test.beforeAll( async ( { playwright } ) => {
	// The store must be as `run-docker.cmd fresh` leaves it: the plugin is what this installs.
	const installed = await fetch(
		new URL(
			'/wp-content/plugins/viglet-turing-search/viglet-turing.php',
			test.info().project.use.baseURL
		)
	);
	test.skip(
		200 === installed.status,
		'the fresh store already has the plugin: run-docker.cmd fresh reset, then run-docker.cmd fresh'
	);
	zip = releaseZip();
	const { api, call } = await turingSession( playwright );
	// The SN site goes, so the Setup page finds it missing and the video shows it created.
	for ( const site of ( await call( 'GET', '/api/sn' ) ) || [] ) {
		if ( site.name === snSite ) {
			await call( 'DELETE', `/api/sn/${ site.id }` );
		}
	}
	// The key a developer creates in Turing under API Tokens and pastes into the Setup page.
	apiKey = (
		await call( 'POST', '/api/dev/token', {
			title: 'viglet-turing-search developer walkthrough',
			description: 'WordPress plugin, the fresh store (D281)',
		} )
	).token;
	await api.dispose();
	expect( apiKey ).toBeTruthy();
} );

test( 'a developer installs the plugin, connects it and indexes a shop', async ( {
	page,
} ) => {
	const record = !! test.info().project.metadata?.record;
	if ( record ) {
		narration = await narrationFor( 'developer' );
	}
	const started = Date.now();
	const now = () => ( Date.now() - started ) / 1000;
	const chapters = [];
	let highlighted = null;
	let voiced = null;
	let focused = [];
	const linger = async ( seconds = 3 ) => {
		if ( record ) {
			await page.waitForTimeout( seconds * 1000 );
		}
	};
	const highlight = async () => {
		await linger( 5 );
		highlighted = now() - 2.5;
	};
	// The narrator talks over the action, as Ana does in the story (D261): a chapter's line
	// starts where `speak()` is called, and the step it describes waits for it to end. Its
	// length is the take the video will carry; a run that is not recorded waits for nothing.
	const speak = ( name ) => {
		const length = record ? takeLength( name ) : 0;
		voiced = { name, at: now() };
		return now() + ( length ? length + 0.4 : 0 );
	};
	const until = async ( deadline ) => {
		const left = ( deadline ?? 0 ) - now();
		if ( record && left > 0 ) {
			await page.waitForTimeout( left * 1000 );
		}
	};
	// The element a step acts on, for the video to push in on (D292).
	const focus = async ( locator ) => {
		if ( ! record ) {
			return;
		}
		const box = await locator.boundingBox().catch( () => null );
		if ( box ) {
			focused.push( { at: now(), box } );
		}
	};
	const chapter = ( title, body ) =>
		test.step( title, async () => {
			const start = now();
			highlighted = null;
			voiced = null;
			focused = [];
			await body();
			if ( null === highlighted ) {
				await highlight();
			}
			chapters.push( {
				title,
				start,
				highlight: highlighted,
				...( voiced ? { voice: voiced } : {} ),
				...( focused.length ? { focus: focused } : {} ),
				end: now(),
			} );
		} );
	const type = async ( field, text ) => {
		if ( ! record ) {
			return field.fill( text );
		}
		await field.fill( '' );
		return field.pressSequentially( text, { delay: 45 } );
	};

	await page.goto( '/wp-login.php' );
	await page.fill( '#user_login', 'admin' );
	await page.fill( '#user_pass', 'admin' );
	await Promise.all( [
		page.waitForURL( /wp-admin/ ),
		page.click( '#wp-submit' ),
	] );

	await chapter( '1. Installs the plugin from its release zip', async () => {
		await page.goto( '/wp-admin/plugin-install.php' );
		const installing = speak( 'chapter-1' );
		await page.locator( '.upload-view-toggle' ).click();
		await linger( 1.5 );
		await focus( page.locator( '.upload-plugin' ) );
		await page.locator( '#pluginzip' ).setInputFiles( zip );
		await until( installing );
		await page.locator( '#install-plugin-submit' ).click();
		const activate = page.getByRole( 'link', { name: 'Activate Plugin' } );
		await expect( activate ).toBeVisible();
		await linger( 2 );
		await activate.click();
		await expect(
			page.locator( '#adminmenu' ).getByRole( 'link', {
				name: 'Viglet Turing Search',
				exact: true,
			} )
		).toBeVisible();
	} );

	await chapter(
		'2. Connects it to Viglet Turing on the Setup page',
		async () => {
			await page.goto( '/wp-admin/admin.php?page=viglet-turing-setup' );
			const field = ( label ) =>
				page.getByLabel( label, { exact: true } );
			const connecting = speak( 'chapter-2' );
			await focus( field( 'Viglet Turing URL' ) );
			await type( field( 'Viglet Turing URL' ), connectURL );
			await type( field( 'API key' ), apiKey );
			await type( field( 'SN site' ), snSite );
			await type( field( 'Locale' ), 'en_US' );
			await until( connecting );
			await page
				.getByRole( 'button', { name: 'Save and test the connection' } )
				.click();
			const create = page.getByRole( 'button', {
				name: `Create the SN site ${ snSite }`,
			} );
			await expect( create ).toBeVisible();
			await linger( 3 );
			await create.click();
			await expect(
				page.getByRole( 'heading', { name: '3. First index' } )
			).toBeVisible();
			await focus(
				page
					.locator( '.components-card' )
					.filter( { hasText: '2. Test' } )
			);
			await highlight();
		}
	);

	await chapter(
		"3. Chooses what to index: the shop's products",
		async () => {
			await page.goto(
				'/wp-admin/admin.php?page=viglet-turing#indexing'
			);
			const choosing = speak( 'chapter-3' );
			await page.click( '.viglet-turing-tab[data-tab="indexing"]' );
			const products = page.locator(
				'input[name$="[index_custom_types][]"][value="product"]'
			);
			// The toggle draws over its checkbox, which is hidden: the label is what a person
			// sees and clicks.
			const toggle = page
				.locator( 'label.viglet-turing-toggle' )
				.filter( { has: products } );
			await toggle.scrollIntoViewIfNeeded();
			await focus(
				page.locator( '#panel-indexing .viglet-turing-card' ).first()
			);
			await linger( 1.5 );
			await toggle.click();
			await expect( products ).toBeChecked();
			await until( choosing );
			await Promise.all( [
				page.waitForURL( /settings-updated=true/ ),
				page.getByRole( 'button', { name: 'Save Settings' } ).click(),
			] );
			// Saved: the page comes back on its first tab, and Indexing shows Products on.
			await page.click( '.viglet-turing-tab[data-tab="indexing"]' );
			await expect( products ).toBeChecked();
			await toggle.scrollIntoViewIfNeeded();
			await highlight();
		}
	);

	await chapter( '4. Runs the first index, with its progress', async () => {
		await page.goto( '/wp-admin/admin.php?page=viglet-turing-setup' );
		await page
			.getByRole( 'button', { name: 'Save and test the connection' } )
			.click();
		const index = page.getByRole( 'button', {
			name: 'Index everything now',
		} );
		await expect( index ).toBeVisible();
		const indexing = speak( 'chapter-4' );
		await focus(
			page
				.locator( '.components-card' )
				.filter( { hasText: '3. First index' } )
		);
		await until( indexing - 2.5 );
		await index.click();
		await expect( page.locator( '[aria-live="polite"] p' ) ).toHaveText(
			/done|complete|indexed/i,
			{ timeout: 180_000 }
		);
		await highlight();
	} );

	await chapter(
		'5. The storefront searches through Viglet Turing',
		async () => {
			await page.goto( '/?pagename=search-gear' );
			const block = page.locator( '.viglet-turing-search' );
			const box = block.locator( 'input[type="search"]' ).first();
			const searching = speak( 'chapter-5' );
			await focus( box );
			await linger( 1.5 );
			await type( box, query );
			await until( searching );
			await box.press( 'Enter' );
			await expect(
				block.locator( '.viglet-turing-search__title a' ).first()
			).toBeVisible();
			await highlight();
		}
	);

	await chapter( '6. Insights show what visitors searched for', async () => {
		await page.goto( '/wp-admin/admin.php?page=viglet-turing-insights' );
		const looking = speak( 'chapter-6' );
		await page.getByLabel( 'Period' ).selectOption( 'today' );
		const top = page.locator( '.components-card' ).filter( {
			has: page.getByRole( 'heading', { name: 'Top searches' } ),
		} );
		await expect(
			top.locator( 'tbody tr' ).filter( { hasText: query } )
		).not.toHaveCount( 0 );
		await until( looking );
		await focus( top );
		await highlight();
	} );

	mkdirSync( out, { recursive: true } );
	writeFileSync(
		join( out, 'chapters.json' ),
		JSON.stringify( { record, language: 'en', chapters }, null, 2 )
	);
	const video = page.video();
	await page.close();
	if ( video && existsSync( await video.path() ) ) {
		await video.saveAs( join( out, 'walkthrough.webm' ) );
	}
} );
