// The school's agenda (D325, D307): a search block fixed to events (D306) whose day is a DATE
// field with the agenda's periods. The seed counts every day from today, so the open day is
// Today, the science fair three days on is in the Next 7 days, the concert twelve days on only
// Upcoming, and the parents' evening and sports day Past.
import { expect, test } from '../fixtures.mjs';
import { say, titleOf, titlesOn } from './school.mjs';

const period = ( name ) => `event_day_range:${ say().periods[ name ] }`;

test( 'the agenda lists only events, and its When facet offers the agenda periods', async ( {
	page,
	look,
	axe,
} ) => {
	const titles = await titlesOn( page, '/agenda/', '*' );
	expect( titles ).toContain( titleOf( 'science-fair' ) );
	// Fixed to events: a book or an opening is never on the agenda.
	expect( titles ).not.toContain( titleOf( 'tide-atlas' ) );
	expect( titles ).not.toContain( titleOf( 'arts-scholarship' ) );
	await expect(
		page.locator( '.viglet-dumont-search__chip' ),
		'the fixed filter is no chip a visitor could remove'
	).toHaveCount( 0 );

	const facets = page.locator( '.viglet-dumont-search__facets' );
	await expect( facets ).toContainText( say().when );
	for ( const name of [ 'today', 'week', 'upcoming', 'past' ] ) {
		await expect( facets ).toContainText( say().periods[ name ] );
	}
	await look( 'demo-edu-agenda' );
	await axe( 'demo-edu-agenda' );
} );

test( 'each period lists the events whose day falls in it, counted from today', async ( {
	page,
} ) => {
	const openDay = titleOf( 'open-day' );
	const fair = titleOf( 'science-fair' );
	const concert = titleOf( 'choir-concert' );
	const parents = titleOf( 'parents-evening' );
	const sports = titleOf( 'sports-day' );

	const today = await titlesOn( page, '/agenda/', '*', [
		period( 'today' ),
	] );
	expect( today ).toEqual( [ openDay ] );

	const week = await titlesOn( page, '/agenda/', '*', [ period( 'week' ) ] );
	expect( week ).toEqual( expect.arrayContaining( [ openDay, fair ] ) );
	expect( week ).not.toContain( concert );
	expect( week ).not.toContain( parents );

	const upcoming = await titlesOn( page, '/agenda/', '*', [
		period( 'upcoming' ),
	] );
	expect( upcoming ).toEqual(
		expect.arrayContaining( [
			fair,
			concert,
			titleOf( 'debate-car-free' ),
		] )
	);
	expect( upcoming ).not.toContain( sports );

	// Past is `* TO NOW/DAY`, and Turing's range includes its end: a day-only event today, indexed
	// at midnight UTC, is listed under Past as well as Today. That edge is the period set's, not
	// the demo's, so only the days on either side of it are checked here.
	const past = await titlesOn( page, '/agenda/', '*', [ period( 'past' ) ] );
	expect( past ).toEqual( expect.arrayContaining( [ parents, sports ] ) );
	expect( past ).not.toContain( fair );
	expect( past ).not.toContain( concert );
} );
