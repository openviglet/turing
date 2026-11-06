// The recorded tour (D179): the demo's showcase with "How it works" on, a search made so the
// filters and the answer have something to show, then each numbered feature opened in turn.
// Each one leaves tests/e2e/artifacts/tour/<n>-<feature>.png, and the walk leaves tour.webm.
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';

const out = join( 'tests', 'e2e', 'artifacts', 'tour' );

/**
 * A feature's name as a file name.
 *
 * @param {string} label The marker's label, "Turing feature 2: Ask the catalog".
 */
const slug = ( label ) =>
	label
		.replace( /^.*?:\s*/, '' )
		.normalize( 'NFD' )
		.replace( /[̀-ͯ]/g, '' )
		.toLowerCase()
		.replace( /[^a-z0-9]+/g, '-' )
		.replace( /^-|-$/g, '' );

test( 'the showcase, one feature at a time', async ( { page } ) => {
	mkdirSync( out, { recursive: true } );
	await page.goto( '/showcase/?tour=1&q=waterproof%20jacket' );
	await expect(
		page.locator( '.viglet-dumont-search__title a' ).first()
	).toBeVisible();
	// The copilot and the assistant show once Turing answers their probes, and their markers
	// with them; give them that moment before counting.
	await page.waitForLoadState( 'networkidle' );

	const markers = page.locator( '.tarnfell-tour__marker' );
	const count = await markers.count();
	expect( count, 'at least the search is numbered' ).toBeGreaterThan( 0 );
	for ( let i = 0; i < count; i++ ) {
		const marker = markers.nth( i );
		const label = await marker.getAttribute( 'aria-label' );
		await marker.scrollIntoViewIfNeeded();
		await marker.click();
		const wrap = marker.locator( '..' );
		await expect( wrap.locator( '.tarnfell-tour__note' ) ).toBeVisible();
		// The note and the feature after it, together.
		const box = await wrap.evaluate( ( el ) => {
			const a = el.getBoundingClientRect();
			const b = el.nextElementSibling.getBoundingClientRect();
			const top = Math.min( a.top, b.top ) + window.scrollY;
			const bottom = Math.max( a.bottom, b.bottom ) + window.scrollY;
			return {
				x: 0,
				y: top,
				width: document.documentElement.clientWidth,
				height: Math.min( bottom - top, 1600 ),
			};
		} );
		await page.screenshot( {
			path: join( out, `${ i + 1 }-${ slug( label ) }.png` ),
			clip: box,
			fullPage: true,
		} );
		await page.keyboard.press( 'Escape' );
	}

	const video = page.video();
	await page.close();
	await video?.saveAs( join( out, 'tour.webm' ) );
} );
