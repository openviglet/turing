// The school's "How it works" (D325): on the agenda with ?tour=1, a numbered marker before the
// search box, the When facet and the results, each carrying its chapter in the school's story
// (demo/school/story.md), and a marker opens its note.
import { expect, test } from '../fixtures.mjs';
import { say } from './school.mjs';

test( "the agenda's tour numbers its features by the story's chapters", async ( {
	page,
	axe,
} ) => {
	await page.goto( '/agenda/?q=*&tour=1' );
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/result/
	);
	const markers = page.locator( '.lumen-tour__marker' );
	await expect( markers ).toHaveCount( 3 );
	const labels = await markers.evaluateAll( ( buttons ) =>
		buttons.map( ( b ) => b.getAttribute( 'aria-label' ) )
	);
	const { marker, features } = say();
	[ 1, 2, 3 ].forEach( ( chapter, i ) =>
		expect( labels ).toContain( marker( chapter, features[ i ] ) )
	);

	const first = markers.first();
	await first.click();
	await expect( first ).toHaveAttribute( 'aria-expanded', 'true' );
	await expect( page.locator( '.lumen-tour__note:visible' ) ).toHaveCount(
		1
	);
	await axe( 'demo-edu-tour' );
} );
